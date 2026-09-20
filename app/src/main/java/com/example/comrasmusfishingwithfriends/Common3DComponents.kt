package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    onJoystickMove: (Offset) -> Unit
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragOffset = Offset.Zero },
                    onDragEnd = { dragOffset = Offset.Zero; onJoystickMove(Offset.Zero) },
                    onDragCancel = { dragOffset = Offset.Zero; onJoystickMove(Offset.Zero) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = dragOffset + dragAmount
                        val maxRadius = 100f
                        val distance = newOffset.getDistance()
                        dragOffset = if (distance > maxRadius) newOffset * (maxRadius / distance) else newOffset
                        onJoystickMove(Offset(dragOffset.x / maxRadius, dragOffset.y / maxRadius))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))
        Box(modifier = Modifier.offset { IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt()) }.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)))
    }
}
