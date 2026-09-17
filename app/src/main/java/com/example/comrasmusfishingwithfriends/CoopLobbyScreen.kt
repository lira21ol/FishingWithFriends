package com.example.comrasmusfishingwithfriends

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CoopLobbyScreen(
    currentPlayer: Player,
    navController: NavHostController,
    gameMode: String
) {
    var lobbyCode by remember { mutableStateOf("") }
    var isHost by remember { mutableStateOf(false) }
    var partnerPlayer by remember { mutableStateOf<Player?>(null) }
    var isStarting by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(5) }
    var showJoinOptions by remember { mutableStateOf(false) }
    var invitePlayerName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteData by remember { mutableStateOf<Map<String, Any>?>(null) }
    val scope = rememberCoroutineScope()

    // Lyssna efter inbjudningar via InviteManager
    LaunchedEffect(Unit) {
        InviteManager.startListeningForInvites(currentPlayer.playerId) { data ->
            inviteData = data
            showInviteDialog = true
        }
    }

    // Rensa lyssnaren när komponenten tas bort
    DisposableEffect(Unit) {
        onDispose {
            InviteManager.stopListeningForInvites()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF001F3F))
    ) {
        // Bakgrund och kraken-silhuett
        Image(
            painter = painterResource(id = R.drawable.bakgrunden),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )

        Image(
            painter = painterResource(id = R.drawable.kraken_phase1),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.TopCenter)
                .alpha(0.2f),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Kraken Co-op Boss Fight",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    shadow = Shadow(
                        color = Color(0xFF00008B),
                        offset = Offset(2f, 2f),
                        blurRadius = 3f
                    )
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (lobbyCode.isEmpty()) {
                // Inbjudningskort
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0A2472).copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = invitePlayerName,
                            onValueChange = { 
                                invitePlayerName = it
                                showError = false 
                            },
                            label = { Text("Ange spelarnamn", color = Color.White) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        if (showError) {
                            Text(
                                text = errorMessage,
                                color = Color.Red,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val player = FirebaseManager.findPlayerByName(invitePlayerName)
                                        if (player != null) {
                                            if (player.playerId == currentPlayer.playerId) {
                                                showError = true
                                                errorMessage = "Du kan inte bjuda in dig själv!"
                                                return@launch
                                            }
                                            val newLobbyCode = InviteManager.sendInvite(currentPlayer, player.playerId)
                                            lobbyCode = newLobbyCode
                                            isHost = true
                                        } else {
                                            showError = true
                                            errorMessage = "Ingen fiskare med det namnet finns"
                                        }
                                    } catch (e: Exception) {
                                        showError = true
                                        errorMessage = "Något gick fel vid inbjudan"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Bjud in fiskare")
                        }

                        Button(
                            onClick = { showJoinOptions = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text("Gå med i Lobby")
                        }
                    }
                }
            } else {
                // Visa lobby-information
                LobbyInfo(
                    lobbyCode = lobbyCode,
                    isHost = isHost,
                    currentPlayer = currentPlayer,
                    partnerPlayer = partnerPlayer,
                    isStarting = isStarting,
                    onStartGame = {
                        scope.launch {
                            FirebaseManager.startLobbyCountdown(lobbyCode)
                            isStarting = true
                        }
                    },
                    onLeaveLobby = {
                        lobbyCode = ""
                        isHost = false
                        partnerPlayer = null
                        isStarting = false
                    }
                )
            }
        }
    }

    // Observera lobby-uppdateringar
    LaunchedEffect(lobbyCode) {
        if (lobbyCode.isNotEmpty()) {
            FirebaseManager.observeLobby(lobbyCode) { lobby ->
                lobby?.let {
                    isStarting = it.isStarting
                    if (it.hostId == currentPlayer.playerId) {
                        isHost = true
                        partnerPlayer = it.guestPlayer
                    } else {
                        isHost = false
                        partnerPlayer = it.hostPlayer
                    }
                }
            }
        }
    }

    // Hantera spelstart
    LaunchedEffect(isStarting) {
        if (isStarting) {
            countdown = 5
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            // Markera lobbyn som inaktiv innan vi navigerar
            scope.launch {
                FirebaseManager.markLobbyAsInactive(lobbyCode)
            }
            navController.navigate("kraken_boss_coop")
        }
    }

    if (showInviteDialog && inviteData != null) {
        InvitePopup(
            currentPlayer = currentPlayer,
            inviteData = inviteData!!,
            onJoinLobby = { receivedLobbyCode ->
                lobbyCode = receivedLobbyCode
                currentPlayer.currentLobbyCode = receivedLobbyCode
                isHost = false
            },
            onDismiss = {
                showInviteDialog = false
                inviteData = null
            }
        )
    }

    if (showJoinOptions) {
        AlertDialog(
            onDismissRequest = { showJoinOptions = false },
            title = { Text("Gå med i lobby") },
            text = {
                OutlinedTextField(
                    value = lobbyCode,
                    onValueChange = { newCode -> 
                        // Konvertera till versaler och begränsa längden till 6
                        if (newCode.length <= 6) {
                            lobbyCode = newCode.uppercase()
                        }
                    },
                    label = { Text("Ange lobbykod") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (lobbyCode.length == 6) {
                                val success = FirebaseManager.joinLobby(lobbyCode, currentPlayer)
                                if (success) {
                                    showJoinOptions = false
                                    isHost = false
                                }
                            }
                        }
                    },
                    enabled = lobbyCode.length == 6
                ) {
                    Text("Gå med")
                }
            },
            dismissButton = {
                Button(onClick = { showJoinOptions = false }) {
                    Text("Avbryt")
                }
            }
        )
    }
}

@Composable
private fun LobbyInfo(
    lobbyCode: String,
    isHost: Boolean,
    currentPlayer: Player,
    partnerPlayer: Player?,
    isStarting: Boolean,
    onStartGame: () -> Unit,
    onLeaveLobby: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lobby-kod: $lobbyCode",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PlayerList(
                isHost = isHost,
                currentPlayer = currentPlayer,
                partnerPlayer = partnerPlayer
            )

            if (isHost && partnerPlayer != null && !isStarting) {
                Button(
                    onClick = onStartGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Starta spelet")
                }
            } else if (!isHost && partnerPlayer != null) {
                Text(
                    text = "Väntar på att värden ska starta spelet...",
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        FirebaseManager.leaveLobby(lobbyCode, currentPlayer.playerId)
                        onLeaveLobby()
                    }
                },
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Lämna Lobby")
            }
        }
    }
}

@Composable
private fun PlayerList(
    isHost: Boolean,
    currentPlayer: Player,
    partnerPlayer: Player?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Spelare:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            PlayerRow(
                playerName = if (isHost) currentPlayer.playerName else (partnerPlayer?.playerName ?: "Väntar..."),
                role = "Värd",
                color = MaterialTheme.colorScheme.primary
            )
            
            PlayerRow(
                playerName = if (!isHost) currentPlayer.playerName else (partnerPlayer?.playerName ?: "Väntar på spelare..."),
                role = "Gäst",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun PlayerRow(
    playerName: String,
    role: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = playerName,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "($role)",
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
} 