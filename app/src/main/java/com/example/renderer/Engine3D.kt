package com.example.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.audio.AudioFrame
import com.example.model.Mesh3DType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Vector3D(var x: Float, var y: Float, var z: Float)

data class Edge3D(val start: Int, val end: Int)

class Engine3D {

    private var rotationX = 0f
    private var rotationY = 0f
    private var rotationZ = 0f
    private var tunnelOffset = 0f

    fun render(
        drawScope: DrawScope,
        meshType: Mesh3DType,
        color: Color,
        secondaryColor: Color,
        audioFrame: AudioFrame,
        centerX: Float,
        centerY: Float,
        scale: Float
    ) {
        // Auto-rotation accelerated by audio mid & treble
        val rotSpeed = 0.015f + audioFrame.mid * 0.03f
        rotationX += rotSpeed * 0.7f
        rotationY += rotSpeed
        rotationZ += rotSpeed * 0.4f
        tunnelOffset += 0.04f + audioFrame.treble * 0.08f

        // Audio beat shake
        val shakeX = if (audioFrame.isBeat) (Math.random().toFloat() - 0.5f) * 15f else 0f
        val shakeY = if (audioFrame.isBeat) (Math.random().toFloat() - 0.5f) * 15f else 0f

        when (meshType) {
            Mesh3DType.CUBE -> renderCube(drawScope, color, secondaryColor, audioFrame, centerX + shakeX, centerY + shakeY, scale)
            Mesh3DType.SPHERE -> renderSphere(drawScope, color, audioFrame, centerX + shakeX, centerY + shakeY, scale)
            Mesh3DType.TORUS -> renderTorus(drawScope, color, secondaryColor, audioFrame, centerX + shakeX, centerY + shakeY, scale)
            Mesh3DType.TUNNEL -> renderTunnel(drawScope, color, secondaryColor, audioFrame, centerX + shakeX, centerY + shakeY, scale)
            Mesh3DType.TERRAIN -> renderTerrain(drawScope, color, audioFrame, centerX + shakeX, centerY + shakeY, scale)
        }
    }

    private fun project(v: Vector3D, centerX: Float, centerY: Float, fov: Float = 350f): Offset? {
        val distance = 3.5f
        val z = v.z + distance
        if (z <= 0.1f) return null
        val factor = fov / z
        return Offset(v.x * factor + centerX, v.y * factor + centerY)
    }

    private fun rotate(v: Vector3D, rx: Float, ry: Float, rz: Float): Vector3D {
        // Rotate Y
        var x1 = v.x * cos(ry) + v.z * sin(ry)
        var y1 = v.y
        var z1 = -v.x * sin(ry) + v.z * cos(ry)

        // Rotate X
        val y2 = y1 * cos(rx) - z1 * sin(rx)
        val z2 = y1 * sin(rx) + z1 * cos(rx)
        val x2 = x1

        // Rotate Z
        val x3 = x2 * cos(rz) - y2 * sin(rz)
        val y3 = x2 * sin(rz) + y2 * cos(rz)
        val z3 = z2

        return Vector3D(x3, y3, z3)
    }

    private fun renderCube(
        drawScope: DrawScope,
        color: Color,
        secondaryColor: Color,
        audioFrame: AudioFrame,
        centerX: Float,
        centerY: Float,
        scale: Float
    ) {
        val s = (1.0f + audioFrame.bass * 0.4f) * (scale / 120f)
        val vertices = arrayOf(
            Vector3D(-s, -s, -s), Vector3D(s, -s, -s), Vector3D(s, s, -s), Vector3D(-s, s, -s),
            Vector3D(-s, -s, s), Vector3D(s, -s, s), Vector3D(s, s, s), Vector3D(-s, s, s)
        )

        val edges = arrayOf(
            Edge3D(0, 1), Edge3D(1, 2), Edge3D(2, 3), Edge3D(3, 0),
            Edge3D(4, 5), Edge3D(5, 6), Edge3D(6, 7), Edge3D(7, 4),
            Edge3D(0, 4), Edge3D(1, 5), Edge3D(2, 6), Edge3D(3, 7)
        )

        val rotated = vertices.map { rotate(it, rotationX, rotationY, rotationZ) }
        val projected = rotated.map { project(it, centerX, centerY) }

        for (edge in edges) {
            val p1 = projected[edge.start]
            val p2 = projected[edge.end]
            if (p1 != null && p2 != null) {
                drawScope.drawLine(
                    color = color,
                    start = p1,
                    end = p2,
                    strokeWidth = 3f
                )
            }
        }
    }

    private fun renderSphere(
        drawScope: DrawScope,
        color: Color,
        audioFrame: AudioFrame,
        centerX: Float,
        centerY: Float,
        scale: Float
    ) {
        val radius = (1.0f + audioFrame.bass * 0.35f) * (scale / 120f)
        val rings = 8
        val segments = 12
        val vertices = mutableListOf<Vector3D>()

        for (i in 0..rings) {
            val theta = i * PI.toFloat() / rings
            for (j in 0 until segments) {
                val phi = j * 2 * PI.toFloat() / segments
                val deform = 1.0f + (audioFrame.spectrum[j % audioFrame.spectrum.size] * 0.3f)
                val r = radius * deform
                val x = r * sin(theta) * cos(phi)
                val y = r * cos(theta)
                val z = r * sin(theta) * sin(phi)
                vertices.add(Vector3D(x, y, z))
            }
        }

        val rotated = vertices.map { rotate(it, rotationX, rotationY, 0f) }
        val projected = rotated.map { project(it, centerX, centerY) }

        for (i in 0 until rings) {
            for (j in 0 until segments) {
                val current = i * segments + j
                val nextInRing = i * segments + ((j + 1) % segments)
                val nextRing = (i + 1) * segments + j

                val p1 = projected[current]
                val p2 = projected[nextInRing]
                if (p1 != null && p2 != null) {
                    drawScope.drawLine(color = color.copy(alpha = 0.7f), start = p1, end = p2, strokeWidth = 2f)
                }

                if (nextRing < projected.size) {
                    val p3 = projected[nextRing]
                    if (p1 != null && p3 != null) {
                        drawScope.drawLine(color = color.copy(alpha = 0.7f), start = p1, end = p3, strokeWidth = 2f)
                    }
                }
            }
        }
    }

    private fun renderTorus(
        drawScope: DrawScope,
        color: Color,
        secondaryColor: Color,
        audioFrame: AudioFrame,
        centerX: Float,
        centerY: Float,
        scale: Float
    ) {
        val rMajor = (1.2f + audioFrame.bass * 0.3f) * (scale / 120f)
        val rMinor = 0.45f * (scale / 120f)
        val uSteps = 12
        val vSteps = 8
        val vertices = mutableListOf<Vector3D>()

        for (u in 0 until uSteps) {
            val uAngle = u * 2 * PI.toFloat() / uSteps
            for (v in 0 until vSteps) {
                val vAngle = v * 2 * PI.toFloat() / vSteps
                val x = (rMajor + rMinor * cos(vAngle)) * cos(uAngle)
                val y = (rMajor + rMinor * cos(vAngle)) * sin(uAngle)
                val z = rMinor * sin(vAngle)
                vertices.add(Vector3D(x, y, z))
            }
        }

        val rotated = vertices.map { rotate(it, rotationX, rotationY, rotationZ) }
        val projected = rotated.map { project(it, centerX, centerY) }

        for (u in 0 until uSteps) {
            for (v in 0 until vSteps) {
                val idx = u * vSteps + v
                val nextU = ((u + 1) % uSteps) * vSteps + v
                val nextV = u * vSteps + ((v + 1) % vSteps)

                val p1 = projected[idx]
                val p2 = projected[nextU]
                val p3 = projected[nextV]

                if (p1 != null && p2 != null) {
                    drawScope.drawLine(color = color, start = p1, end = p2, strokeWidth = 2f)
                }
                if (p1 != null && p3 != null) {
                    drawScope.drawLine(color = secondaryColor.copy(alpha = 0.8f), start = p1, end = p3, strokeWidth = 2f)
                }
            }
        }
    }

    private fun renderTunnel(
        drawScope: DrawScope,
        color: Color,
        secondaryColor: Color,
        audioFrame: AudioFrame,
        centerX: Float,
        centerY: Float,
        scale: Float
    ) {
        val ringCount = 10
        val sides = 8
        val tunnelDepth = 6.0f

        for (r in 0 until ringCount) {
            val zPos = (tunnelDepth - (r.toFloat() * 0.6f + (tunnelOffset % 0.6f)))
            if (zPos <= 0.2f) continue

            val ringScale = (scale / 60f)
            val ringPoints = mutableListOf<Offset?>()

            for (s in 0 until sides) {
                val angle = s * 2 * PI.toFloat() / sides + rotationZ * 0.2f
                val deform = 1.0f + audioFrame.spectrum[s % audioFrame.spectrum.size] * 0.5f
                val x = cos(angle) * ringScale * deform
                val y = sin(angle) * ringScale * deform
                val p = project(Vector3D(x, y, zPos - 3.5f), centerX, centerY)
                ringPoints.add(p)
            }

            // Draw ring
            val alpha = ((tunnelDepth - zPos) / tunnelDepth).coerceIn(0.1f, 1f)
            val ringColor = if (r % 2 == 0) color.copy(alpha = alpha) else secondaryColor.copy(alpha = alpha)

            for (s in 0 until sides) {
                val p1 = ringPoints[s]
                val p2 = ringPoints[(s + 1) % sides]
                if (p1 != null && p2 != null) {
                    drawScope.drawLine(color = ringColor, start = p1, end = p2, strokeWidth = 2.5f)
                }
            }
        }
    }

    private fun renderTerrain(
        drawScope: DrawScope,
        color: Color,
        audioFrame: AudioFrame,
        centerX: Float,
        centerY: Float,
        scale: Float
    ) {
        val rows = 8
        val cols = 10
        val grid = mutableListOf<Vector3D>()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = (c - cols / 2f) * 0.5f
                val z = (r - rows / 2f) * 0.5f
                val waveIdx = (r * cols + c) % audioFrame.spectrum.size
                val y = 0.8f - audioFrame.spectrum[waveIdx] * 0.9f
                grid.add(Vector3D(x, y, z))
            }
        }

        val rotated = grid.map { rotate(it, 0.5f, rotationY * 0.3f, 0f) }
        val projected = rotated.map { project(it, centerX, centerY) }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val p1 = projected[idx]
                if (c < cols - 1) {
                    val p2 = projected[idx + 1]
                    if (p1 != null && p2 != null) {
                        drawScope.drawLine(color = color.copy(alpha = 0.7f), start = p1, end = p2, strokeWidth = 2f)
                    }
                }
                if (r < rows - 1) {
                    val p3 = projected[idx + cols]
                    if (p1 != null && p3 != null) {
                        drawScope.drawLine(color = color.copy(alpha = 0.5f), start = p1, end = p3, strokeWidth = 2f)
                    }
                }
            }
        }
    }
}
