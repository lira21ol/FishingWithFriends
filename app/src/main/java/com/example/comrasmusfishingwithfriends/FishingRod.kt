package com.example.comrasmusfishingwithfriends

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FishingRod(
    rodImage: Painter,
    isCasting: Boolean,
    isReeling: Boolean,
    modifier: Modifier = Modifier
) {
    var rodPosition by remember { mutableStateOf(0.dp) }
    var rodAngle by remember { mutableStateOf(0f) }

    val animatedRodPosition by animateDpAsState(targetValue = rodPosition)
    val animatedRodAngle by animateFloatAsState(targetValue = rodAngle)

    LaunchedEffect(isCasting, isReeling) {
        if (isCasting) {
            rodPosition = 200.dp
            rodAngle = 0f
            delay(1000)
            rodPosition = 0.dp
        } else if (isReeling) {
            rodAngle = -45f
            rodPosition = -200.dp
            delay(1500)
            rodAngle = 0f
            rodPosition = 0.dp
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .wrapContentSize()
            .height(150.dp)
    ) {

        Image(
            painter = rodImage,
            contentDescription = "Fishing Rod",
            modifier = Modifier
                .rotate(animatedRodAngle)
                .offset(y = animatedRodPosition)
                .width(100.dp)
        )
    }
}