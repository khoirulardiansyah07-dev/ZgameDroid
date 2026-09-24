package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import com.example.audio.AudioFrame
import com.example.model.ResolutionPreset
import com.example.model.VisualizerProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ExportConfig(
    val resolution: ResolutionPreset = ResolutionPreset.HD_720P,
    val fps: Int = 30,
    val durationSeconds: Int = 10,
    val bitrate: Int = 4_000_000 // 4 Mbps
)

class VideoExportEngine(private val context: Context) {

    private var isCancelled = false

    fun cancel() {
        isCancelled = true
    }

    suspend fun exportVideo(
        project: VisualizerProject,
        config: ExportConfig,
        onProgress: (Float, String) -> Unit
    ): File? = withContext(Dispatchers.Default) {
        isCancelled = false
        val width = config.resolution.width
        val height = config.resolution.height
        val fps = config.fps
        val totalFrames = config.durationSeconds * fps
        val frameDurationUs = 1_000_000L / fps

        val outputFile = File(context.cacheDir, "visualizer_${System.currentTimeMillis()}.mp4")

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null

        try {
            onProgress(0.05f, "Configuring H.264 Encoder...")

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframe interval
            }

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            onProgress(0.1f, "Rendering video frames...")

            for (frameIdx in 0 until totalFrames) {
                if (isCancelled) {
                    onProgress(0f, "Export cancelled")
                    return@withContext null
                }

                val timeSec = frameIdx.toFloat() / fps
                val timeMs = (timeSec * 1000).toLong()

                // Calculate procedural audio reaction for frame
                val simulatedBass = (0.5f + 0.45f * sin(timeSec * 2 * PI.toFloat() * 2.2f)).coerceIn(0f, 1f)
                val simulatedMid = (0.4f + 0.4f * sin(timeSec * 2 * PI.toFloat() * 4.5f)).coerceIn(0f, 1f)
                val simulatedTreble = (0.3f + 0.3f * sin(timeSec * 2 * PI.toFloat() * 8.0f)).coerceIn(0f, 1f)
                val isBeat = (frameIdx % (fps / 2)) == 0

                val audioFrame = AudioFrame(
                    bass = simulatedBass,
                    mid = simulatedMid,
                    treble = simulatedTreble,
                    amplitude = (simulatedBass + simulatedMid) * 0.5f,
                    isBeat = isBeat,
                    beatEnergy = if (isBeat) 1f else 0f,
                    timeMs = timeMs
                )

                // Lock Canvas and draw frame
                val canvas: Canvas = inputSurface.lockHardwareCanvas()
                try {
                    renderFrameToCanvas(canvas, project, audioFrame, width, height, timeSec, paint)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Drain encoder
                while (true) {
                    val status = codec.dequeueOutputBuffer(bufferInfo, 1000L)
                    if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) throw RuntimeException("Format changed after muxer started")
                        val newFormat = codec.outputFormat
                        videoTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (status >= 0) {
                        val encodedData = codec.getOutputBuffer(status)
                        if (encodedData != null && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(status, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    } else {
                        break
                    }
                }

                val progress = 0.1f + 0.85f * (frameIdx.toFloat() / totalFrames)
                onProgress(progress, "Rendering frame ${frameIdx + 1}/$totalFrames (${(progress * 100).toInt()}%)")
            }

            // Signal End of Stream
            codec.signalEndOfInputStream()

            // Drain remaining buffers
            var eos = false
            while (!eos) {
                val status = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                if (status >= 0) {
                    val encodedData = codec.getOutputBuffer(status)
                    if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(status, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                } else if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }

            onProgress(1.0f, "Export Complete!")
            return@withContext outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(0f, "Error: ${e.localizedMessage}")
            return@withContext null
        } finally {
            try {
                codec?.stop()
                codec?.release()
                inputSurface?.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    private fun renderFrameToCanvas(
        canvas: Canvas,
        project: VisualizerProject,
        audioFrame: AudioFrame,
        width: Int,
        height: Int,
        timeSec: Float,
        paint: Paint
    ) {
        // Clear background
        canvas.drawColor(Color.rgb(10, 10, 20))

        val scene = project.activeScene
        val centerX = width * 0.5f
        val centerY = height * 0.5f

        // Draw radial visualizer core
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.CYAN
        val baseRadius = 140f * (1.0f + audioFrame.bass * 0.25f)
        canvas.drawCircle(centerX, centerY, baseRadius, paint)

        // Draw spectrum bars radiating outward
        val barCount = 48
        for (i in 0 until barCount) {
            val angle = i * 2 * PI.toFloat() / barCount + (timeSec * 0.2f)
            val barLen = (30f + sin(timeSec * 4f + i) * 20f + audioFrame.bass * 120f).coerceAtLeast(10f)

            val x1 = centerX + cos(angle) * baseRadius
            val y1 = centerY + sin(angle) * baseRadius
            val x2 = centerX + cos(angle) * (baseRadius + barLen)
            val y2 = centerY + sin(angle) * (baseRadius + barLen)

            paint.color = if (i % 2 == 0) Color.rgb(0, 245, 212) else Color.rgb(255, 0, 127)
            canvas.drawLine(x1, y1, x2, y2, paint)
        }

        // Project title overlay
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 42f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(project.name, centerX, centerY + 15f, paint)

        // Subtitle
        paint.color = Color.rgb(180, 180, 200)
        paint.textSize = 24f
        canvas.drawText(project.audioTitle, centerX, centerY + 65f, paint)
    }
}
