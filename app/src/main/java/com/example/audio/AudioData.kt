package com.example.audio

data class AudioFrame(
    val bass: Float = 0f,           // 0.0 to 1.0 (smoothed bass intensity)
    val mid: Float = 0f,            // 0.0 to 1.0 (smoothed mid intensity)
    val treble: Float = 0f,         // 0.0 to 1.0 (smoothed treble intensity)
    val amplitude: Float = 0f,      // 0.0 to 1.0 (overall RMS amplitude)
    val isBeat: Boolean = false,    // True when transient beat/kick detected
    val beatEnergy: Float = 0f,     // 0.0 to 1.0 instantaneous beat impact
    val spectrum: FloatArray = FloatArray(64) { 0f },   // Normalized FFT frequency bins [0..1]
    val waveform: FloatArray = FloatArray(128) { 0f },  // Raw oscilloscope samples [-1..1]
    val bpm: Int = 128,
    val timeMs: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioFrame
        return timeMs == other.timeMs && isBeat == other.isBeat
    }

    override fun hashCode(): Int {
        var result = timeMs.hashCode()
        result = 31 * result + isBeat.hashCode()
        return result
    }
}

enum class DemoTrack(val title: String, val artist: String, val bpm: Int, val durationMs: Long) {
    SYNTHWAVE_PULSE("Cyberpunk Synthwave", "Z-Engine Studio", 128, 45000L),
    BASS_TRAP_DROP("Heavy Bass Trap", "Subwoofer Labs", 140, 40000L),
    LOFI_CHILL("Ambient Lo-Fi Chill", "Tokyo Rain", 85, 50000L)
}
