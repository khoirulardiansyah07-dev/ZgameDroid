package com.example.model

import com.example.audio.AudioFrame
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

enum class GraphNodeType(val title: String, val category: String) {
    AUDIO_INPUT("Audio Stream In", "Input"),
    FFT_SPECTRUM("FFT Spectrum", "Analysis"),
    BASS_EXTRACTOR("Bass Extractor", "Filter"),
    MID_EXTRACTOR("Mid Extractor", "Filter"),
    TREBLE_EXTRACTOR("Treble Extractor", "Filter"),
    BEAT_DETECTOR("Beat Pulse", "Filter"),
    MATH_MULTIPLIER("Math Gain (x)", "Math"),
    LFO_SINE("Sine Oscillator", "Generator"),
    GLOW_OUTPUT("Glow Effect Out", "Output"),
    SCALE_OUTPUT("Scale Transform Out", "Output")
}

data class GraphNode(
    val id: String = UUID.randomUUID().toString(),
    val type: GraphNodeType,
    val title: String = type.title,
    var x: Float = 100f,
    var y: Float = 100f,
    var paramValue: Float = 1.0f
)

data class GraphConnection(
    val id: String = UUID.randomUUID().toString(),
    val fromNodeId: String,
    val toNodeId: String
)

object ExpressionEvaluator {

    fun evaluate(expression: String, frame: AudioFrame, timeSec: Float): Float {
        val clean = expression.lowercase().replace(" ", "")
        return try {
            when {
                clean.contains("bass*") -> {
                    val mult = clean.substringAfter("bass*").toFloatOrNull() ?: 1f
                    frame.bass * mult
                }
                clean.contains("mid*") -> {
                    val mult = clean.substringAfter("mid*").toFloatOrNull() ?: 1f
                    frame.mid * mult
                }
                clean.contains("treble*") -> {
                    val mult = clean.substringAfter("treble*").toFloatOrNull() ?: 1f
                    frame.treble * mult
                }
                clean.contains("amplitude*") -> {
                    val mult = clean.substringAfter("amplitude*").toFloatOrNull() ?: 1f
                    frame.amplitude * mult
                }
                clean.contains("sin(time)") -> {
                    sin(timeSec)
                }
                clean.contains("cos(time)") -> {
                    cos(timeSec)
                }
                clean == "beat" -> {
                    if (frame.isBeat) 1.0f else 0.0f
                }
                else -> {
                    clean.toFloatOrNull() ?: 1.0f
                }
            }
        } catch (e: Exception) {
            1.0f
        }
    }
}
