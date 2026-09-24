package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioFrame
import com.example.model.PreviewQuality
import com.example.model.ResolutionPreset
import com.example.model.VisualizerLayer
import com.example.model.VisualizerScene
import com.example.renderer.VisualizerRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CanvasPreview(
    scene: VisualizerScene,
    audioFrame: AudioFrame,
    resolution: ResolutionPreset,
    quality: PreviewQuality,
    selectedLayer: VisualizerLayer?,
    onUpdateLayerTransform: (Float, Float, Float, Float) -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val renderer = remember { VisualizerRenderer() }

    // Real-time FPS calculator
    var fps by remember { mutableIntStateOf(60) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsCheckTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val aspect = when (resolution) {
        ResolutionPreset.HD_720P, ResolutionPreset.FULL_HD_1080P -> 16f / 9f
        ResolutionPreset.SQUARE_1080P -> 1f
        ResolutionPreset.PORTRAIT_1080P -> 9f / 16f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090E)),
        contentAlignment = Alignment.Center
    ) {
        // Preview Frame with aspect ratio
        Box(
            modifier = Modifier
                .padding(8.dp)
                .aspectRatio(aspect, matchHeightConstraintsFirst = maxWidth > maxHeight)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (audioFrame.isBeat) 2.dp else 1.dp,
                    color = if (audioFrame.isBeat) MaterialTheme.colorScheme.primary else Color(0xFF1E2633),
                    shape = RoundedCornerShape(8.dp)
                )
                .pointerInput(selectedLayer?.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        if (selectedLayer != null) {
                            val dx = pan.x / size.width
                            val dy = pan.y / size.height
                            onUpdateLayerTransform(dx, dy, zoom, rotation)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onDoubleTap() }
                    )
                }
                .testTag("visualizer_canvas_preview")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                frameCount++
                val now = System.currentTimeMillis()
                if (now - lastFpsCheckTime >= 1000) {
                    fps = frameCount
                    frameCount = 0
                    lastFpsCheckTime = now
                }

                renderer.renderScene(
                    drawScope = this,
                    scene = scene,
                    audioFrame = audioFrame,
                    timeMs = audioFrame.timeMs
                )
            }

            // Top-left HUD: Resolution & FPS
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "${resolution.width}x${resolution.height} • $fps FPS • ${quality.displayName.substringBefore(" ")}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            // Beat Indicator Pill
            if (audioFrame.isBeat) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "BEAT",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
