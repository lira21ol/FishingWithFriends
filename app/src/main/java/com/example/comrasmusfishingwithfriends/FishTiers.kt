package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FishTiers(currentPlayer: Player) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E).copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Fisketier: ${getCurrentTier(currentPlayer.score)}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            
            LinearProgressIndicator(
                progress = getProgressToNextTier(currentPlayer.score),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

private fun getCurrentTier(score: Int): String {
    return when {
        score >= 1000 -> "Platinum"
        score >= 500 -> "Guld"
        score >= 100 -> "Silver"
        score >= 50 -> "Brons"
        else -> "Ingen tier"
    }
}

private fun getProgressToNextTier(score: Int): Float {
    return when {
        score >= 1000 -> 1f
        score >= 500 -> (score - 500f) / 500f
        score >= 100 -> (score - 100f) / 400f
        score >= 50 -> (score - 50f) / 50f
        else -> score / 50f
    }
} 