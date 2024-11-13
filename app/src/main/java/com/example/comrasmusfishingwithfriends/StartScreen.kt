package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale

@Composable
fun StartScreen(
    onGoFishingClick: () -> Unit,
    onGoFishTogetherClick: () -> Unit
) {

    val backgroundImage = painterResource(id = R.drawable.bakgrunden)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Image(
            painter = backgroundImage,
            contentDescription = "Background Image",
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Crop
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        ) {
            Text(
                text = "Fishing with Friends",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .border(2.dp, Color.Red),
                onClick = onGoFishingClick
            ) {
                Text(text = "Go Fishing")
            }

            Button(
                onClick = onGoFishTogetherClick
            ) {
                Text(text = "Go Fish Together")
            }
        }
    }
}

@Preview
@Composable
fun PreviewStartScreen() {
    StartScreen(
        onGoFishingClick = {},
        onGoFishTogetherClick = {}
    )
}