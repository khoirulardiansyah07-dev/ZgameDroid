package com.example.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import com.example.audio.AudioFrame
import com.example.model.AudioMappingConfig
import com.example.model.AudioSource
import com.example.model.AudioTarget
import com.example.model.EffectType
import com.example.model.LayerEffect
import com.example.model.LayerTransform
import com.example.model.LayerType
import com.example.model.ShapeType
import com.example.model.VisualizerLayer
import com.example.model.VisualizerScene
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class VisualizerRenderer {

    private val engine3D = Engine3D()
    private val particleSimulation = ParticleSimulation(120)

    fun renderScene(
        drawScope: DrawScope,
        scene: VisualizerScene,
        audioFrame: AudioFrame,
        timeMs: Long
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        if (width <= 0 || height <= 0) return

        // 1. Update particle simulations
        val particleLayer = scene.layers.firstOrNull { it.type == LayerType.PARTICLES && it.isVisible }
        if (particleLayer != null) {
            val pColor = Color(particleLayer.primaryColorHex)
            particleSimulation.update(width, height, particleLayer.particlePreset, pColor, audioFrame)
        }

        // 2. Render layers in order
        for (layer in scene.layers) {
            if (!layer.isVisible) continue
            renderLayer(drawScope, layer, audioFrame, width, height, timeMs)
        }
    }

    private fun renderLayer(
        drawScope: DrawScope,
        layer: VisualizerLayer,
        audioFrame: AudioFrame,
        width: Float,
        height: Float,
        timeMs: Long
    ) {
        val baseTransform = layer.transform
        val audioValue = computeAudioFactor(layer.audioMapping, audioFrame)

        // Apply audio mapping to transform parameters
        var scaleFactor = 1.0f
        var rotOffset = 0.0f
        var yOffset = 0.0f
        var opacityMult = 1.0f

        if (layer.audioMapping.enabled) {
            when (layer.audioMapping.target) {
                AudioTarget.SCALE -> scaleFactor = audioValue
                AudioTarget.ROTATION -> rotOffset = (audioValue - 1.0f) * 90f
                AudioTarget.POSITION_Y -> yOffset = (audioValue - 1.0f) * 80f
                AudioTarget.OPACITY -> opacityMult = audioValue.coerceIn(0f, 1f)
                else -> Unit
            }
        }

        val centerX = baseTransform.x * width
        val centerY = (baseTransform.y * height) + yOffset
        val currentScaleX = baseTransform.scaleX * scaleFactor
        val currentScaleY = baseTransform.scaleY * scaleFactor
        val currentRotation = baseTransform.rotation + rotOffset
        val currentOpacity = (baseTransform.opacity * opacityMult).coerceIn(0f, 1f)

        if (currentOpacity <= 0.01f) return

        val primaryColor = Color(layer.primaryColorHex).copy(alpha = currentOpacity)
        val secondaryColor = Color(layer.secondaryColorHex).copy(alpha = currentOpacity)

        drawScope.translate(centerX, centerY) {
            drawScope.rotate(currentRotation, pivot = Offset.Zero) {
                drawScope.scale(currentScaleX, currentScaleY, pivot = Offset.Zero) {
                    when (layer.type) {
                        LayerType.SOLID_BACKGROUND -> {
                            drawScope.drawRect(
                                color = primaryColor,
                                topLeft = Offset(-width, -height),
                                size = Size(width * 2f, height * 2f)
                            )
                        }
                        LayerType.GRADIENT_BACKGROUND -> {
                            drawScope.drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(primaryColor, secondaryColor),
                                    startY = -height * 0.5f,
                                    endY = height * 0.5f
                                ),
                                topLeft = Offset(-width, -height),
                                size = Size(width * 2f, height * 2f)
                            )
                        }
                        LayerType.SPECTRUM -> {
                            renderSpectrumBars(drawScope, layer, primaryColor, secondaryColor, audioFrame, width)
                        }
                        LayerType.CIRCULAR_SPECTRUM -> {
                            renderCircularSpectrum(drawScope, layer, primaryColor, secondaryColor, audioFrame)
                        }
                        LayerType.WAVEFORM -> {
                            renderWaveform(drawScope, primaryColor, secondaryColor, audioFrame, width)
                        }
                        LayerType.SHAPE -> {
                            renderShape(drawScope, layer.shapeType, primaryColor, secondaryColor, audioFrame)
                        }
                        LayerType.PARTICLES -> {
                            particleSimulation.render(drawScope)
                        }
                        LayerType.MESH_3D -> {
                            engine3D.render(
                                drawScope = drawScope,
                                meshType = layer.meshType,
                                color = primaryColor,
                                secondaryColor = secondaryColor,
                                audioFrame = audioFrame,
                                centerX = 0f,
                                centerY = 0f,
                                scale = 140f
                            )
                        }
                        LayerType.TEXT_REACTIVE -> {
                            renderText(drawScope, layer, primaryColor, secondaryColor, audioFrame)
                        }
                        LayerType.IMAGE -> {
                            // Fallback graphic emblem if imageUri is not loaded
                            drawScope.drawCircle(
                                brush = Brush.radialGradient(listOf(secondaryColor, primaryColor)),
                                radius = 70f,
                                center = Offset.Zero
                            )
                            drawScope.drawCircle(
                                color = Color.White,
                                radius = 68f,
                                center = Offset.Zero,
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                }
            }
        }

        // Apply post-effects (Scanlines, Vignette, Glitch, etc.)
        for (effect in layer.effects) {
            if (effect.enabled) {
                applyPostEffect(drawScope, effect, width, height, audioFrame, timeMs)
            }
        }
    }

    private fun computeAudioFactor(config: AudioMappingConfig, frame: AudioFrame): Float {
        if (!config.enabled) return 1.0f

        val raw = when (config.source) {
            AudioSource.BASS -> frame.bass
            AudioSource.MID -> frame.mid
            AudioSource.TREBLE -> frame.treble
            AudioSource.AMPLITUDE -> frame.amplitude
            AudioSource.BEAT -> if (frame.isBeat) 1.0f else frame.beatEnergy
        }

        val scaled = (raw * config.sensitivity).coerceIn(0f, 1f)
        val inverted = if (config.invert) (1f - scaled) else scaled
        return config.minVal + inverted * (config.maxVal - config.minVal)
    }

    private fun renderSpectrumBars(
        drawScope: DrawScope,
        layer: VisualizerLayer,
        primaryColor: Color,
        secondaryColor: Color,
        frame: AudioFrame,
        totalWidth: Float
    ) {
        val bars = layer.barCount.coerceIn(16, 64)
        val totalBarSpace = totalWidth * 0.85f
        val gap = 4f
        val barWidth = ((totalBarSpace - (bars * gap)) / bars).coerceAtLeast(3f)
        val startX = -totalBarSpace * 0.5f

        for (i in 0 until bars) {
            val specIdx = (i.toFloat() / bars * (frame.spectrum.size - 1)).toInt()
            val magnitude = frame.spectrum[specIdx]
            val barHeight = (magnitude * 220f + 6f)

            val x = startX + i * (barWidth + gap)
            val y = 0f

            // Equalizer bar with gradient
            drawScope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    startY = y - barHeight,
                    endY = y
                ),
                topLeft = Offset(x, y - barHeight),
                size = Size(barWidth, barHeight)
            )

            // Glowing Peak Cap
            drawScope.drawRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(x, y - barHeight - 4f),
                size = Size(barWidth, 3f)
            )
        }
    }

    private fun renderCircularSpectrum(
        drawScope: DrawScope,
        layer: VisualizerLayer,
        primaryColor: Color,
        secondaryColor: Color,
        frame: AudioFrame
    ) {
        val bars = layer.barCount.coerceIn(24, 96)
        val radius = layer.spectrumRadius * (1.0f + frame.bass * 0.15f)

        // Inner glowing core
        drawScope.drawCircle(
            brush = Brush.radialGradient(listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent)),
            radius = radius,
            center = Offset.Zero
        )

        drawScope.drawCircle(
            color = primaryColor,
            radius = radius,
            center = Offset.Zero,
            style = Stroke(width = 2.5f)
        )

        for (i in 0 until bars) {
            val angle = i * 2 * PI.toFloat() / bars
            val specIdx = (i.toFloat() / bars * (frame.spectrum.size - 1)).toInt()
            val magnitude = frame.spectrum[specIdx]
            val spikeLength = magnitude * 110f + 6f

            val startX = cos(angle) * radius
            val startY = sin(angle) * radius
            val endX = cos(angle) * (radius + spikeLength)
            val endY = sin(angle) * (radius + spikeLength)

            drawScope.drawLine(
                color = if (i % 2 == 0) primaryColor else secondaryColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = layer.barWidth.coerceIn(2f, 8f),
                cap = StrokeCap.Round
            )
        }
    }

    private fun renderWaveform(
        drawScope: DrawScope,
        primaryColor: Color,
        secondaryColor: Color,
        frame: AudioFrame,
        width: Float
    ) {
        val waveWidth = width * 0.85f
        val startX = -waveWidth * 0.5f
        val points = frame.waveform
        if (points.isEmpty()) return

        val path = Path()
        val step = waveWidth / (points.size - 1)

        for (i in points.indices) {
            val x = startX + i * step
            val y = points[i] * 90f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Glow pass
        drawScope.drawPath(
            path = path,
            color = secondaryColor.copy(alpha = 0.4f),
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
        // Main crisp line
        drawScope.drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )
    }

    private fun renderShape(
        drawScope: DrawScope,
        shapeType: ShapeType,
        primaryColor: Color,
        secondaryColor: Color,
        frame: AudioFrame
    ) {
        val size = 90f * (1.0f + frame.bass * 0.35f)

        when (shapeType) {
            ShapeType.CIRCLE -> {
                drawScope.drawCircle(
                    brush = Brush.radialGradient(listOf(secondaryColor, primaryColor)),
                    radius = size,
                    center = Offset.Zero
                )
            }
            ShapeType.RING -> {
                drawScope.drawCircle(
                    color = primaryColor,
                    radius = size,
                    center = Offset.Zero,
                    style = Stroke(width = 6f)
                )
                drawScope.drawCircle(
                    color = secondaryColor.copy(alpha = 0.5f),
                    radius = size * 1.08f,
                    center = Offset.Zero,
                    style = Stroke(width = 3f)
                )
            }
            ShapeType.HEXAGON -> {
                val path = Path()
                for (i in 0 until 6) {
                    val angle = i * PI.toFloat() / 3f
                    val x = cos(angle) * size
                    val y = sin(angle) * size
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawScope.drawPath(path, color = primaryColor, style = Stroke(width = 5f))
            }
            ShapeType.STAR -> {
                val path = Path()
                val points = 5
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) size else size * 0.45f
                    val angle = i * PI.toFloat() / points - (PI.toFloat() / 2f)
                    val x = cos(angle) * r
                    val y = sin(angle) * r
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawScope.drawPath(path, color = primaryColor)
            }
            ShapeType.RECTANGLE -> {
                drawScope.drawRect(
                    color = primaryColor,
                    topLeft = Offset(-size, -size * 0.7f),
                    size = Size(size * 2f, size * 1.4f),
                    style = Stroke(width = 5f)
                )
            }
        }
    }

    private fun renderText(
        drawScope: DrawScope,
        layer: VisualizerLayer,
        primaryColor: Color,
        secondaryColor: Color,
        frame: AudioFrame
    ) {
        val paint = android.graphics.Paint().apply {
            color = primaryColor.hashCode()
            textSize = layer.textSize * (1.0f + frame.beatEnergy * 0.25f)
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
            setShadowLayer(
                16f + frame.bass * 20f,
                0f,
                0f,
                secondaryColor.hashCode()
            )
        }

        drawScope.drawContext.canvas.nativeCanvas.drawText(
            layer.textContent,
            0f,
            paint.textSize * 0.35f,
            paint
        )
    }

    private fun applyPostEffect(
        drawScope: DrawScope,
        effect: LayerEffect,
        width: Float,
        height: Float,
        frame: AudioFrame,
        timeMs: Long
    ) {
        when (effect.type) {
            EffectType.SCANLINES -> {
                val lineSpacing = 6f
                var y = 0f
                val scanColor = Color.Black.copy(alpha = (0.25f * effect.intensity).coerceIn(0f, 0.6f))
                while (y < height) {
                    drawScope.drawLine(
                        color = scanColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 2f
                    )
                    y += lineSpacing
                }
            }
            EffectType.VIGNETTE -> {
                drawScope.drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f * effect.intensity)),
                        center = Offset(width * 0.5f, height * 0.5f),
                        radius = (width * 0.65f)
                    ),
                    topLeft = Offset.Zero,
                    size = Size(width, height)
                )
            }
            EffectType.CHROMATIC_ABERRATION -> {
                // Subtle RGB edge shift line indicator
                val shift = (frame.bass * 8f * effect.intensity).coerceIn(0f, 15f)
                if (shift > 1f) {
                    drawScope.drawLine(
                        color = Color.Red.copy(alpha = 0.3f),
                        start = Offset(0f, height * 0.05f),
                        end = Offset(width, height * 0.05f),
                        strokeWidth = shift
                    )
                    drawScope.drawLine(
                        color = Color.Cyan.copy(alpha = 0.3f),
                        start = Offset(0f, height * 0.95f),
                        end = Offset(width, height * 0.95f),
                        strokeWidth = shift
                    )
                }
            }
            EffectType.GLITCH -> {
                if (frame.isBeat || Math.random() < 0.06 * effect.intensity) {
                    val bandY = (Math.random() * height).toFloat()
                    val bandHeight = (Math.random() * 25f + 5f).toFloat()
                    drawScope.drawRect(
                        color = Color.Cyan.copy(alpha = 0.35f * effect.intensity),
                        topLeft = Offset(0f, bandY),
                        size = Size(width, bandHeight)
                    )
                }
            }
            EffectType.VHS -> {
                // Bottom noise bar
                drawScope.drawRect(
                    color = Color.White.copy(alpha = 0.15f * effect.intensity),
                    topLeft = Offset(0f, height * 0.92f),
                    size = Size(width, height * 0.04f)
                )
            }
            else -> Unit
        }
    }
}
