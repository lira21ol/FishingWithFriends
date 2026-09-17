package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

@Composable
fun InvitePopup(
    currentPlayer: Player,
    inviteData: Map<String, Any>,
    onJoinLobby: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val hostName = inviteData["fromPlayerName"] as String
    val lobbyCode = inviteData["lobbyCode"] as String
    val inviteId = inviteData["inviteId"] as String

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0A2472)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Spelinbjudan",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "$hostName bjuder in dig till ett spel!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                FirebaseManager.respondToInvite(
                                    currentPlayer.playerId,
                                    inviteId,
                                    false
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("Avböj", color = Color.White)
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                val success = FirebaseManager.respondToInvite(
                                    currentPlayer.playerId,
                                    inviteId,
                                    true
                                )
                                if (success) {
                                    onJoinLobby(lobbyCode)
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Acceptera", color = Color.White)
                    }
                }
            }
        }
    }
} 