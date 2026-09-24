package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FftProcessor(val binCount: Int = 64) {

    private val smoothedSpectrum = FloatArray(binCount) { 0f }
    private val smoothedWaveform = FloatArray(128) { 0f }
    private var smoothedBass = 0f
    private var smoothedMid = 0f
    private var smoothedTreble = 0f
    private var smoothedAmp = 0f
    private var beatDecayEnergy = 0f

    // History for adaptive beat detection
    private val energyHistory = FloatArray(32) { 0.05f }
    private var historyIndex = 0

    fun processSamples(samples: FloatArray, timeMs: Long, bpm: Int = 128): AudioFrame {
        val n = samples.size
        if (n == 0) return AudioFrame(timeMs = timeMs, bpm = bpm)

        // Waveform copy with smoothing
        val waveSize = smoothedWaveform.size
        val step = (n / waveSize).coerceAtLeast(1)
        for (i in 0 until waveSize) {
            val sampleIdx = (i * step).coerceIn(0, n - 1)
            smoothedWaveform[i] = smoothedWaveform[i] * 0.3f + samples[sampleIdx] * 0.7f
        }

        // Discrete Cosine / Windowed DFT for frequency bands
        val rawBins = FloatArray(binCount)
        val windowSize = n.coerceAtMost(512)
        for (k in 0 until binCount) {
            var real = 0f
            var imag = 0f
            // Logarithmic frequency distribution
            val freq = (k + 1).toFloat() / binCount
            val cycles = freq * 32f

            for (t in 0 until windowSize) {
                // Hann window
                val window = 0.5f * (1f - cos(2f * PI.toFloat() * t / windowSize))
                val sample = samples[t] * window
                val angle = 2f * PI.toFloat() * cycles * t / windowSize
                real += sample * cos(angle)
                imag -= sample * sin(angle)
            }
            val mag = sqrt(real * real + imag * imag) / (windowSize * 0.35f)
            rawBins[k] = mag.coerceIn(0f, 1.5f)
        }

        // Smooth bins (Attack / Decay)
        for (k in 0 until binCount) {
            val target = rawBins[k]
            val current = smoothedSpectrum[k]
            smoothedSpectrum[k] = if (target > current) {
                current + (target - current) * 0.6f
            } else {
                current + (target - current) * 0.2f
            }
        }

        // Calculate band energies
        // Bass: bins 0 to 12
        var bassSum = 0f
        val bassBins = (binCount * 0.2f).toInt().coerceAtLeast(3)
        for (i in 0 until bassBins) {
            bassSum += smoothedSpectrum[i]
        }
        val currentBass = (bassSum / bassBins * 1.8f).coerceIn(0f, 1f)

        // Mid: bins 13 to 38
        var midSum = 0f
        val midEnd = (binCount * 0.6f).toInt()
        val midCount = (midEnd - bassBins).coerceAtLeast(1)
        for (i in bassBins until midEnd) {
            midSum += smoothedSpectrum[i]
        }
        val currentMid = (midSum / midCount * 1.5f).coerceIn(0f, 1f)

        // Treble: bins 39 to 63
        var trebleSum = 0f
        val trebleCount = (binCount - midEnd).coerceAtLeast(1)
        for (i in midEnd until binCount) {
            trebleSum += smoothedSpectrum[i]
        }
        val currentTreble = (trebleSum / trebleCount * 2.0f).coerceIn(0f, 1f)

        // RMS amplitude
        var sumSquares = 0f
        for (s in samples) {
            sumSquares += s * s
        }
        val currentAmp = (sqrt(sumSquares / n) * 2.2f).coerceIn(0f, 1f)

        smoothedBass = smoothedBass * 0.25f + currentBass * 0.75f
        smoothedMid = smoothedMid * 0.3f + currentMid * 0.7f
        smoothedTreble = smoothedTreble * 0.35f + currentTreble * 0.65f
        smoothedAmp = smoothedAmp * 0.25f + currentAmp * 0.75f

        // Dynamic Beat Detection (Instantaneous bass energy vs local average)
        var averageEnergy = 0f
        for (e in energyHistory) {
            averageEnergy += e
        }
        averageEnergy /= energyHistory.size

        val varianceThreshold = 1.35f
        val isBeatDetected = currentBass > (averageEnergy * varianceThreshold) && currentBass > 0.25f && beatDecayEnergy < 0.3f

        if (isBeatDetected) {
            beatDecayEnergy = 1.0f
        } else {
            beatDecayEnergy = (beatDecayEnergy - 0.12f).coerceAtLeast(0f)
        }

        energyHistory[historyIndex] = currentBass
        historyIndex = (historyIndex + 1) % energyHistory.size

        return AudioFrame(
            bass = smoothedBass,
            mid = smoothedMid,
            treble = smoothedTreble,
            amplitude = smoothedAmp,
            isBeat = isBeatDetected,
            beatEnergy = beatDecayEnergy,
            spectrum = smoothedSpectrum.clone(),
            waveform = smoothedWaveform.clone(),
            bpm = bpm,
            timeMs = timeMs
        )
    }

    fun reset() {
        smoothedSpectrum.fill(0f)
        smoothedWaveform.fill(0f)
        smoothedBass = 0f
        smoothedMid = 0f
        smoothedTreble = 0f
        smoothedAmp = 0f
        beatDecayEnergy = 0f
    }
}
