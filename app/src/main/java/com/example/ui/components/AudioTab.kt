package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioFrame
import com.example.audio.DemoTrack

@Composable
fun AudioTab(
    currentTrackName: String,
    audioFrame: AudioFrame,
    sensitivity: Float,
    bassGain: Float,
    onSelectDemoTrack: (DemoTrack) -> Unit,
    onImportAudio: (Uri, String) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onBassGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "Imported Track"
            onImportAudio(uri, fileName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Track Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Audio Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Now Playing: $currentTrackName",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = { audioPickerLauncher.launch("audio/*") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("import_audio_button")
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import Audio", fontSize = 12.sp)
            }
        }

        // Demo Tracks Selector
        Text("Built-in Synthesizer Tracks:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DemoTrack.values()) { track ->
                val isSelected = currentTrackName == track.title
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onSelectDemoTrack(track) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = track.title,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${track.bpm} BPM • ${track.artist}",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Real-Time Live Spectrum Monitor
        Text("Real-Time FFT Spectrum Analysis:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D1117))
                .border(1.dp, Color(0xFF21262D), RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bins = audioFrame.spectrum.size.coerceAtMost(48)
                val barWidth = (size.width / bins) - 2f

                for (i in 0 until bins) {
                    val mag = audioFrame.spectrum[i]
                    val barHeight = mag * size.height * 0.95f
                    val x = i * (barWidth + 2f)
                    val y = size.height - barHeight

                    drawRect(
                        color = when {
                            i < 10 -> Color(0xFF00F5D4)
                            i < 26 -> Color(0xFFFF007F)
                            else -> Color(0xFFFFD166)
                        },
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
        }

        // Live Frequency Band Meters
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BandMeter(label = "BASS", value = audioFrame.bass, color = Color(0xFF00F5D4), modifier = Modifier.weight(1f))
            BandMeter(label = "MID", value = audioFrame.mid, color = Color(0xFFFF007F), modifier = Modifier.weight(1f))
            BandMeter(label = "TREBLE", value = audioFrame.treble, color = Color(0xFFFFD166), modifier = Modifier.weight(1f))
            BandMeter(label = "BEAT", value = if (audioFrame.isBeat) 1f else audioFrame.beatEnergy, color = Color(0xFF06D6A0), modifier = Modifier.weight(1f))
        }

        // Sensitivity Sliders
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Audio Sensitivity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format("%.1fx", sensitivity), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = sensitivity,
                onValueChange = onSensitivityChange,
                valueRange = 0.5f..3.0f,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bass Boost / Subwoofer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format("%.1fx", bassGain), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
            Slider(
                value = bassGain,
                onValueChange = onBassGainChange,
                valueRange = 0.5f..2.5f,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
private fun BandMeter(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = Color(0xFF21262D)
        )
    }
}
