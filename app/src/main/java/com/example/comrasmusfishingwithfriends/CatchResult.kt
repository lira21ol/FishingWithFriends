package com.example.comrasmusfishingwithfriends

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CatchResult(isSuccessful: Boolean, fish: Fish?) {
    val resultMessage = if (isSuccessful) {
        "You caught a ${fish?.type ?: "fish"}!"
    } else {
        "The fish got away!"
    }

    Text(text = resultMessage, color = if (isSuccessful) Color.Green else Color.Red)
}
