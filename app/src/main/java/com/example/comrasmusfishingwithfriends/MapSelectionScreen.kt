package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun MapSelectionScreen(
    currentPlayer: Player,
    navController: NavHostController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)) // Ljusblå bakgrund
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Välj Fiskeplats",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(top = 32.dp)
            )

            // Dammen (Original map)
            MapCard(
                title = "Dammen",
                description = "Den klassiska dammen med vanliga fiskar",
                imageId = R.drawable.bakgrunden,
                isLocked = false,
                onClick = { navController.navigate("fishing_game_screen") }
            )

            // Havet
            MapCard(
                title = "Havet",
                description = "Djupt vatten med sällsynta havsfiskar",
                imageId = R.drawable.bakgrunden, // Ändra till havsbild
                isLocked = currentPlayer.score < 500,
                requiredScore = 500,
                onClick = { 
                    if (currentPlayer.score >= 500) navController.navigate("fishing_game_screen2") 
                }
            )

            // Floden
            MapCard(
                title = "Floden",
                description = "Strömmande vatten med unika flodsfiskar",
                imageId = R.drawable.bakgrunden, // Ändra till flodbild
                isLocked = currentPlayer.score < 1000,
                requiredScore = 1000,
                onClick = { 
                    if (currentPlayer.score >= 1000) navController.navigate("fishing_game_screen3") 
                }
            )

            // Boss Arena
            MapCard(
                title = "Boss Arena",
                description = "Utmana den legendariska bossfisken!",
                imageId = R.drawable.bakgrunden, // Ändra till bossbild
                isLocked = currentPlayer.score < 2000,
                requiredScore = 2000,
                onClick = { /* Implementera senare */ }
            )
        }

        // Tillbaka-knapp
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clickable { navController.navigate("start_screen") },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.7f)
            )
        ) {
            Text(
                text = "Tillbaka till huvudmenyn",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White
            )
        }
    }
}

@Composable
private fun MapCard(
    title: String,
    description: String,
    imageId: Int,
    isLocked: Boolean,
    requiredScore: Int = 0,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(enabled = !isLocked) { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) 
                Color.Gray.copy(alpha = 0.7f) 
            else 
                Color(0xFF1E1E1E).copy(alpha = 0.9f)
        )
    ) {
        Box {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = imageId),
                    contentDescription = title,
                    modifier = Modifier
                        .size(120.dp)
                        .padding(8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (isLocked) {
                        Text(
                            text = "Låst - Kräver $requiredScore poäng",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Lägg till låsikon som overlay när kartan är låst
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
} 