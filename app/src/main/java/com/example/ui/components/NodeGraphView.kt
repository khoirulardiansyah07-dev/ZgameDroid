package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioFrame
import com.example.model.GraphConnection
import com.example.model.GraphNode
import com.example.model.GraphNodeType

@Composable
fun NodeGraphView(
    audioFrame: AudioFrame,
    modifier: Modifier = Modifier
) {
    // Initial nodes setup
    val nodes = remember {
        mutableStateListOf(
            GraphNode(id = "1", type = GraphNodeType.AUDIO_INPUT, x = 40f, y = 80f),
            GraphNode(id = "2", type = GraphNodeType.FFT_SPECTRUM, x = 200f, y = 80f),
            GraphNode(id = "3", type = GraphNodeType.BASS_EXTRACTOR, x = 360f, y = 50f),
            GraphNode(id = "4", type = GraphNodeType.BEAT_DETECTOR, x = 360f, y = 200f),
            GraphNode(id = "5", type = GraphNodeType.MATH_MULTIPLIER, x = 520f, y = 70f, paramValue = 1.5f),
            GraphNode(id = "6", type = GraphNodeType.SCALE_OUTPUT, x = 680f, y = 90f)
        )
    }

    val connections = remember {
        mutableStateListOf(
            GraphConnection(fromNodeId = "1", toNodeId = "2"),
            GraphConnection(fromNodeId = "2", toNodeId = "3"),
            GraphConnection(fromNodeId = "2", toNodeId = "4"),
            GraphConnection(fromNodeId = "3", toNodeId = "5"),
            GraphConnection(fromNodeId = "5", toNodeId = "6")
        )
    }

    var showAddMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D14))
    ) {
        // Draw Connecting Bezier Cables
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (conn in connections) {
                val from = nodes.firstOrNull { it.id == conn.fromNodeId }
                val to = nodes.firstOrNull { it.id == conn.toNodeId }
                if (from != null && to != null) {
                    val startX = from.x + 130f
                    val startY = from.y + 45f
                    val endX = to.x
                    val endY = to.y + 45f

                    val path = Path().apply {
                        moveTo(startX, startY)
                        val controlX1 = startX + (endX - startX) * 0.5f
                        val controlY1 = startY
                        val controlX2 = startX + (endX - startX) * 0.5f
                        val controlY2 = endY
                        cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                    }

                    // Cable Glow
                    drawPath(
                        path = path,
                        color = Color(0xFF00F5D4).copy(alpha = 0.3f),
                        style = Stroke(width = 7f, cap = StrokeCap.Round)
                    )
                    // Cable Line
                    drawPath(
                        path = path,
                        color = Color(0xFF00F5D4),
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Render Nodes
        nodes.forEach { node ->
            DraggableNodeCard(
                node = node,
                audioFrame = audioFrame,
                onPositionChange = { dx, dy ->
                    node.x += dx
                    node.y += dy
                }
            )
        }

        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF21262D))
            ) {
                Text(
                    text = "Node Graph Flow • Drag nodes to reposition",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Box {
                Button(
                    onClick = { showAddMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Node", fontSize = 12.sp)
                }

                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    GraphNodeType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text("${type.category}: ${type.title}") },
                            onClick = {
                                nodes.add(GraphNode(type = type, x = 150f, y = 150f))
                                showAddMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableNodeCard(
    node: GraphNode,
    audioFrame: AudioFrame,
    onPositionChange: (Float, Float) -> Unit
) {
    var posX by remember { mutableStateOf(node.x) }
    var posY by remember { mutableStateOf(node.y) }

    val liveVal = when (node.type) {
        GraphNodeType.BASS_EXTRACTOR -> audioFrame.bass
        GraphNodeType.MID_EXTRACTOR -> audioFrame.mid
        GraphNodeType.TREBLE_EXTRACTOR -> audioFrame.treble
        GraphNodeType.BEAT_DETECTOR -> if (audioFrame.isBeat) 1f else audioFrame.beatEnergy
        GraphNodeType.MATH_MULTIPLIER -> (audioFrame.bass * node.paramValue).coerceIn(0f, 1f)
        GraphNodeType.AUDIO_INPUT, GraphNodeType.FFT_SPECTRUM -> audioFrame.amplitude
        else -> 0.8f
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(posX.toInt(), posY.toInt()) }
            .width(135.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161B22))
            .border(1.5.dp, if (audioFrame.isBeat) MaterialTheme.colorScheme.secondary else Color(0xFF30363D), RoundedCornerShape(8.dp))
            .pointerInput(node.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    posX += dragAmount.x
                    posY += dragAmount.y
                    onPositionChange(dragAmount.x, dragAmount.y)
                }
            }
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = node.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = node.type.category,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Value meter
            LinearProgressIndicator(
                progress = { liveVal },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFF21262D)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Pins (In and Out dots)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFFFF007F), shape = CircleShape)
                )
                Text(String.format("%.2f", liveVal), fontSize = 10.sp, color = Color.White)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF00F5D4), shape = CircleShape)
                )
            }
        }
    }
}
