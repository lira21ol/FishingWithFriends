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
    onGoFishTogetherClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    playerScore: Int
) {
    val backgroundImage = painterResource(id = R.drawable.bakgrunden)
    val tier = getTier(playerScore)

    Box(
        modifier = Modifier.fillMaxSize()
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
            Image(
                painter = painterResource(id = tier.image),
                contentDescription = "Tier Icon",
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 8.dp)
            )
            
            Text(
                text = tier.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Fishing with Friends",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                modifier = Modifier.padding(bottom = 16.dp),
                onClick = onGoFishingClick
            ) {
                Text(text = "Go Fishing")
            }

            Button(
                onClick = onGoFishTogetherClick
            ) {
                Text(text = "Go Fish Together")
            }

            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = onLeaderboardClick
            ) {
                Text(text = "Leaderboard")
            }
        }
    }
}


fun getTier(score: Int): Tier {
    return when {
        score >= 1000 -> Tier("Platinum", R.drawable.tier_platinum, "You are a Platinum tier player!")
        score >= 500 -> Tier("Gold", R.drawable.tier_gold, "You are a Gold tier player!")
        score >= 100 -> Tier("Silver", R.drawable.tier_silver, "You are a Silver tier player!")
        score >= 50 -> Tier("Bronze", R.drawable.tier_bronze, "You are a Bronze tier player!")
        else -> Tier("No Tier", R.drawable.ic_star, "No tier, but you are on your way!")
    }
}


data class Tier(
    val name: String,
    val image: Int,
    val description: String
)

@Preview
@Composable
fun PreviewStartScreen() {
    StartScreen(
        onGoFishingClick = {},
        onGoFishTogetherClick = {},
        onLeaderboardClick = {},
        playerScore = 350 // exempel
    )
}
