package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSource
import com.example.model.AudioTarget
import com.example.model.BlendModeType
import com.example.model.EffectType
import com.example.model.Keyframe
import com.example.model.LayerEffect
import com.example.model.LayerType
import com.example.model.Mesh3DType
import com.example.model.ParticlePreset
import com.example.model.ShapeType
import com.example.model.VisualizerLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerInspectorSheet(
    layer: VisualizerLayer,
    onUpdateLayer: (VisualizerLayer) -> Unit,
    onAddKeyframe: (Keyframe) -> Unit,
    onDeleteKeyframe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Transform", "Audio Mapping", "Style", "Effects", "Keyframes")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Layer title bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = layer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${layer.type.displayName} Settings",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Contents
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                0 -> TransformTab(layer, onUpdateLayer)
                1 -> AudioMappingTab(layer, onUpdateLayer)
                2 -> StyleTab(layer, onUpdateLayer)
                3 -> EffectsTab(layer, onUpdateLayer)
                4 -> KeyframesTab(layer, onAddKeyframe, onDeleteKeyframe)
            }
        }
    }
}

@Composable
private fun TransformTab(layer: VisualizerLayer, onUpdateLayer: (VisualizerLayer) -> Unit) {
    val t = layer.transform

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SliderRow(label = "Position X", value = t.x, range = 0f..1f) {
            onUpdateLayer(layer.copy(transform = t.copy(x = it)))
        }
        SliderRow(label = "Position Y", value = t.y, range = 0f..1f) {
            onUpdateLayer(layer.copy(transform = t.copy(y = it)))
        }
        SliderRow(label = "Scale", value = t.scaleX, range = 0.2f..3.0f) {
            onUpdateLayer(layer.copy(transform = t.copy(scaleX = it, scaleY = it)))
        }
        SliderRow(label = "Rotation", value = t.rotation, range = 0f..360f, format = "%.0f°") {
            onUpdateLayer(layer.copy(transform = t.copy(rotation = it)))
        }
        SliderRow(label = "Opacity", value = t.opacity, range = 0f..1f) {
            onUpdateLayer(layer.copy(transform = t.copy(opacity = it)))
        }
    }
}

@Composable
private fun AudioMappingTab(layer: VisualizerLayer, onUpdateLayer: (VisualizerLayer) -> Unit) {
    val map = layer.audioMapping

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Audio Reaction", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Switch(
                checked = map.enabled,
                onCheckedChange = { onUpdateLayer(layer.copy(audioMapping = map.copy(enabled = it))) }
            )
        }

        if (map.enabled) {
            // Source selector
            Text("Audio Source:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(AudioSource.values()) { src ->
                    val isSelected = map.source == src
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable {
                            onUpdateLayer(layer.copy(audioMapping = map.copy(source = src)))
                        }
                    ) {
                        Text(
                            text = src.displayName.substringBefore(" "),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Target selector
            Text("Visual Property Target:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(AudioTarget.values()) { tgt ->
                    val isSelected = map.target == tgt
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable {
                            onUpdateLayer(layer.copy(audioMapping = map.copy(target = tgt)))
                        }
                    ) {
                        Text(
                            text = tgt.displayName.substringBefore(" "),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            SliderRow(label = "Sensitivity", value = map.sensitivity, range = 0.2f..4.0f) {
                onUpdateLayer(layer.copy(audioMapping = map.copy(sensitivity = it)))
            }
            SliderRow(label = "Min Scale/Value", value = map.minVal, range = 0.2f..2.0f) {
                onUpdateLayer(layer.copy(audioMapping = map.copy(minVal = it)))
            }
            SliderRow(label = "Max Scale/Value", value = map.maxVal, range = 1.0f..3.5f) {
                onUpdateLayer(layer.copy(audioMapping = map.copy(maxVal = it)))
            }
        }
    }
}

@Composable
private fun StyleTab(layer: VisualizerLayer, onUpdateLayer: (VisualizerLayer) -> Unit) {
    val palette = listOf(
        0xFF00F5D4, 0xFFFF007F, 0xFFFFD166, 0xFF06D6A0,
        0xFF7B2CBF, 0xFFFF5722, 0xFFFFFFFF, 0xFF00E5FF
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Color pickers
        Text("Primary Color:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(palette) { hex ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (layer.primaryColorHex == hex) 2.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onUpdateLayer(layer.copy(primaryColorHex = hex)) }
                )
            }
        }

        Text("Secondary / Accent Color:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(palette) { hex ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (layer.secondaryColorHex == hex) 2.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onUpdateLayer(layer.copy(secondaryColorHex = hex)) }
                )
            }
        }

        // Type specific settings
        when (layer.type) {
            LayerType.SPECTRUM, LayerType.CIRCULAR_SPECTRUM -> {
                SliderRow(label = "Frequency Bar Count", value = layer.barCount.toFloat(), range = 16f..64f, format = "%.0f") {
                    onUpdateLayer(layer.copy(barCount = it.toInt()))
                }
                SliderRow(label = "Radius", value = layer.spectrumRadius, range = 60f..220f, format = "%.0f px") {
                    onUpdateLayer(layer.copy(spectrumRadius = it))
                }
            }
            LayerType.MESH_3D -> {
                Text("3D Mesh Object:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Mesh3DType.values()) { mesh ->
                        val isSelected = layer.meshType == mesh
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onUpdateLayer(layer.copy(meshType = mesh)) }
                        ) {
                            Text(
                                text = mesh.displayName,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
            LayerType.PARTICLES -> {
                Text("Particle Preset:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ParticlePreset.values()) { preset ->
                        val isSelected = layer.particlePreset == preset
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onUpdateLayer(layer.copy(particlePreset = preset)) }
                        ) {
                            Text(
                                text = preset.displayName,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
            LayerType.TEXT_REACTIVE -> {
                OutlinedTextField(
                    value = layer.textContent,
                    onValueChange = { onUpdateLayer(layer.copy(textContent = it)) },
                    label = { Text("Display Text") },
                    modifier = Modifier.fillMaxWidth()
                )
                SliderRow(label = "Text Size", value = layer.textSize, range = 18f..64f, format = "%.0f sp") {
                    onUpdateLayer(layer.copy(textSize = it))
                }
            }
            LayerType.SHAPE -> {
                Text("Shape Geometry:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ShapeType.values()) { shape ->
                        val isSelected = layer.shapeType == shape
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onUpdateLayer(layer.copy(shapeType = shape)) }
                        ) {
                            Text(
                                text = shape.displayName,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun EffectsTab(layer: VisualizerLayer, onUpdateLayer: (VisualizerLayer) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Active Post-Effects:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

        EffectType.values().forEach { effType ->
            val existing = layer.effects.firstOrNull { it.type == effType }
            val isEnabled = existing?.enabled == true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(effType.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    if (isEnabled && existing != null) {
                        Slider(
                            value = existing.intensity,
                            onValueChange = { newInt ->
                                val updated = layer.effects.map {
                                    if (it.id == existing.id) it.copy(intensity = newInt) else it
                                }
                                onUpdateLayer(layer.copy(effects = updated))
                            },
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        val newEffects = if (checked) {
                            layer.effects.filter { it.type != effType } + LayerEffect(type = effType, enabled = true)
                        } else {
                            layer.effects.filter { it.type != effType }
                        }
                        onUpdateLayer(layer.copy(effects = newEffects))
                    }
                )
            }
        }
    }
}

@Composable
private fun KeyframesTab(
    layer: VisualizerLayer,
    onAddKeyframe: (Keyframe) -> Unit,
    onDeleteKeyframe: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Keyframes (${layer.keyframes.size})", fontWeight = FontWeight.SemiBold)
            Button(
                onClick = { onAddKeyframe(Keyframe(property = "scale", value = 1.2f)) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Insert Keyframe", fontSize = 12.sp)
            }
        }

        if (layer.keyframes.isEmpty()) {
            Text("No keyframes yet. Tap 'Insert Keyframe' to animate properties.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            layer.keyframes.forEach { kf ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${kf.property.uppercase()}: ${kf.value} @ ${kf.timeMs}ms", fontSize = 12.sp)
                    IconButton(onClick = { onDeleteKeyframe(kf.id) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String = "%.2f",
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(String.format(format, value), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
