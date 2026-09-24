package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AudioEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val fftProcessor = FftProcessor(64)

    private val _audioFrame = MutableStateFlow(AudioFrame())
    val audioFrame: StateFlow<AudioFrame> = _audioFrame.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(45000L)
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

    private val _currentTrackName = MutableStateFlow(DemoTrack.SYNTHWAVE_PULSE.title)
    val currentTrackName: StateFlow<String> = _currentTrackName.asStateFlow()

    private var activeDemoTrack: DemoTrack = DemoTrack.SYNTHWAVE_PULSE
    private var customMediaPlayer: MediaPlayer? = null
    private var customAudioUri: Uri? = null

    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private var tickerJob: Job? = null

    var sensitivity: Float = 1.0f
    var bassGain: Float = 1.2f

    init {
        initSynthEngine()
    }

    private fun initSynthEngine() {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    fun selectDemoTrack(track: DemoTrack) {
        pause()
        customAudioUri = null
        customMediaPlayer?.release()
        customMediaPlayer = null
        activeDemoTrack = track
        _currentTrackName.value = track.title
        _totalDurationMs.value = track.durationMs
        _playbackPositionMs.value = 0L
    }

    fun loadCustomAudio(uri: Uri, title: String) {
        pause()
        customAudioUri = uri
        _currentTrackName.value = title
        try {
            customMediaPlayer?.release()
            customMediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                _totalDurationMs.value = duration.toLong()
                setOnCompletionListener {
                    seekTo(0L)
                    pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true

        if (customMediaPlayer != null) {
            try {
                customMediaPlayer?.seekTo(_playbackPositionMs.value.toInt())
                customMediaPlayer?.start()
                startCustomMediaTicker()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            audioTrack?.play()
            startSynthesizerLoop()
        }
    }

    fun pause() {
        _isPlaying.value = false
        customMediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }
        audioTrack?.pause()
        synthJob?.cancel()
        tickerJob?.cancel()
        fftProcessor.reset()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    fun seekTo(timeMs: Long) {
        val clamped = timeMs.coerceIn(0L, _totalDurationMs.value)
        _playbackPositionMs.value = clamped
        customMediaPlayer?.let {
            try {
                it.seekTo(clamped.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startCustomMediaTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            val sampleBuffer = FloatArray(512)
            var phase = 0.0
            while (isActive && _isPlaying.value) {
                val mp = customMediaPlayer
                if (mp != null && mp.isPlaying) {
                    val pos = mp.currentPosition.toLong()
                    _playbackPositionMs.value = pos

                    // Generate simulated spectrum correlating with track playback
                    val t = pos / 1000.0
                    for (i in sampleBuffer.indices) {
                        phase += 0.05
                        val kick = exp(-((t % 0.5) * 8.0)).toFloat() * 0.8f
                        val bass = sin(2.0 * PI * 65.0 * (t + i / 44100.0)).toFloat() * 0.5f
                        val lead = sin(2.0 * PI * 440.0 * (t + i / 44100.0)).toFloat() * 0.3f
                        sampleBuffer[i] = (kick + bass + lead) * sensitivity
                    }

                    val frame = fftProcessor.processSamples(sampleBuffer, pos, bpm = 120)
                    _audioFrame.value = frame
                }
                delay(16) // ~60fps analysis rate
            }
        }
    }

    private fun startSynthesizerLoop() {
        synthJob?.cancel()
        synthJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val chunkSize = 1024
            val shortBuffer = ShortArray(chunkSize)
            val floatBuffer = FloatArray(chunkSize)

            val bpm = activeDemoTrack.bpm
            val beatIntervalSec = 60.0 / bpm
            var currentSec = _playbackPositionMs.value / 1000.0

            while (isActive && _isPlaying.value) {
                val beatTime = currentSec % beatIntervalSec
                val barTime = currentSec % (beatIntervalSec * 4.0)

                // Synthesize kick drum, sub bass, hi-hat and lead arpeggio
                for (i in 0 until chunkSize) {
                    val t = currentSec + (i.toDouble() / sampleRate)
                    val localBeat = t % beatIntervalSec

                    // Kick drum (heavy exponential pitch-drop sine)
                    val kickEnvelope = exp(-localBeat * 14.0).toFloat().coerceIn(0f, 1f)
                    val kickFreq = 45.0 + 120.0 * exp(-localBeat * 25.0)
                    val kick = (sin(2.0 * PI * kickFreq * t) * kickEnvelope * 0.9f).toFloat()

                    // Sub Bass line (arpeggio based on bar)
                    val subFreq = when ((barTime / beatIntervalSec).toInt()) {
                        0 -> 55.0  // A1
                        1 -> 65.4  // C2
                        2 -> 73.4  // D2
                        else -> 49.0 // G1
                    }
                    val sub = (sin(2.0 * PI * subFreq * t) * 0.5f * bassGain).toFloat()

                    // Hi-hat (metallic high frequencies on off-beats)
                    val hatTime = (t + beatIntervalSec / 2.0) % beatIntervalSec
                    val hatEnv = exp(-hatTime * 30.0).toFloat().coerceIn(0f, 1f)
                    val hihat = if (hatEnv > 0.01f) {
                        ((sin(2.0 * PI * 8000.0 * t) + sin(2.0 * PI * 12000.0 * t)) * hatEnv * 0.25f).toFloat()
                    } else 0f

                    // Melodic Lead synth (Cyberpunk vibe)
                    val leadFreq = subFreq * 4.0
                    val leadEnv = (0.5f + 0.5f * sin(2.0 * PI * 2.0 * t).toFloat()) * 0.35f
                    val lead = (sin(2.0 * PI * leadFreq * t) * leadEnv).toFloat()

                    val mix = (kick + sub + hihat + lead) * sensitivity
                    val clamped = mix.coerceIn(-1.0f, 1.0f)

                    floatBuffer[i] = clamped
                    shortBuffer[i] = (clamped * 32767).toInt().toShort()
                }

                // Write to hardware speaker output
                audioTrack?.write(shortBuffer, 0, chunkSize)

                currentSec += chunkSize.toDouble() / sampleRate
                val currentMs = (currentSec * 1000).toLong()

                // Loop playback if duration exceeded
                if (currentMs >= _totalDurationMs.value) {
                    currentSec = 0.0
                    _playbackPositionMs.value = 0L
                } else {
                    _playbackPositionMs.value = currentMs
                }

                // Process FFT for real-time visualization
                val frame = fftProcessor.processSamples(floatBuffer, currentMs, bpm = bpm)
                _audioFrame.value = frame

                // Yield to allow other coroutines
                delay(4)
            }
        }
    }

    fun release() {
        pause()
        audioTrack?.release()
        audioTrack = null
        customMediaPlayer?.release()
        customMediaPlayer = null
    }
}
