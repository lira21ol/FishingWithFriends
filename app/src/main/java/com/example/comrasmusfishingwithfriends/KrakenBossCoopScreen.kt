package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KrakenBossCoopScreen(
    currentPlayer: Player,
    navController: NavHostController
) {
    var partnerPlayer by remember { mutableStateOf<Player?>(null) }
    var krakenHealth by remember { mutableStateOf(1000) }
    var playerDamage by remember { mutableStateOf(0) }
    var partnerDamage by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Observera partner-spelarens handlingar
    LaunchedEffect(currentPlayer.currentLobbyCode) {
        currentPlayer.currentLobbyCode?.let { lobbyCode ->
            FirebaseManager.observeLobby(lobbyCode) { lobby ->
                lobby?.let {
                    partnerPlayer = if (it.hostId == currentPlayer.playerId) {
                        it.guestPlayer
                    } else {
                        it.hostPlayer
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF001F3F))
    ) {
        // Kraken-bild
        Image(
            painter = painterResource(id = R.drawable.kraken_phase1),
            contentDescription = "Kraken Boss",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
        )

        // Hälsomätare för Kraken
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Text(
                text = "Kraken HP: $krakenHealth/1000",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = krakenHealth / 1000f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(top = 8.dp),
                color = Color.Red
            )
        }

        // Spelarinformation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Nuvarande spelare
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "${currentPlayer.playerName} (Du)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Skada: $playerDamage",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Partner-spelare
            partnerPlayer?.let { partner ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "${partner.playerName} (Partner)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Skada: $partnerDamage",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Attack-knapp
            Button(
                onClick = {
                    scope.launch {
                        // Implementera attack-logik här
                        val damage = (20..50).random()
                        playerDamage += damage
                        krakenHealth -= damage
                        
                        if (krakenHealth <= 0) {
                            // Implementera seger-logik här
                            delay(2000)
                            navController.navigate("start_screen")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("Attackera Kraken!")
            }
        }
    }
} 