package com.example.comrasmusfishingwithfriends

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout

@Composable
fun UserCreationScreen(onUserCreated: (Player) -> Unit, navController: NavHostController) {
    var fisherName by remember { mutableStateOf("") }
    var isCreatingUser by remember { mutableStateOf(false) }
    var creationError by remember { mutableStateOf<String?>(null) }

    FirebaseFirestore.getInstance()

    fun isUserNameValid(): Boolean {
        return fisherName.trim().isNotEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                val player = ExoPlayer.Builder(context).build()
                val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/raw/startvideo")
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
                val playerView = PlayerView(context).apply {
                    this.player = player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    layoutParams = android.widget.RelativeLayout.LayoutParams(
                        android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                        android.widget.RelativeLayout.LayoutParams.MATCH_PARENT
                    )
                }
                playerView
            },
            modifier = Modifier
                .fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(40.dp)
            ) {
                Text(
                    text = "Fishing With Friends",
                    style = TextStyle(
                        color = Color(0xFF90EE90),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            offset = Offset(2f, 2f),
                            blurRadius = 3f
                        )
                    ),
                    modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
                )
            }

            Image(
                painter = painterResource(id = R.drawable.fish_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .padding(vertical = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Skriv ditt fiskarnamn:",
                    style = TextStyle(
                        color = Color(0xFF90EE90),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    ),
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            OutlinedTextField(
                value = fisherName,
                onValueChange = { fisherName = it },
                label = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textStyle = TextStyle(
                    color = Color(0xFFD3B1F3),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isUserNameValid()) {
                        isCreatingUser = true
                        creationError = null
                        checkIfUserExists(fisherName) { exists, player ->
                            if (exists) {
                                if (player != null) {
                                    onUserCreated(player)
                                    navController.navigate("start_screen")
                                }
                            } else {
                                val newPlayer = Player(
                                    playerId = "",
                                    playerName = fisherName,
                                    score = 0
                                )
                                addPlayerToFirestore(
                                    player = newPlayer,
                                    onUserCreated = onUserCreated,
                                    onComplete = { success ->
                                        if (success) {
                                            navController.navigate("start_screen")
                                        } else {
                                            creationError = "Något gick fel, försök igen."
                                        }
                                    }
                                )
                            }
                            isCreatingUser = false
                        }
                    }
                },
                enabled = !isCreatingUser && fisherName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color(0xFFD3B1F3)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Börja fiska!",
                    style = TextStyle(
                        color = Color(0xFFD3B1F3),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            creationError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
fun checkIfUserExists(playerName: String, onComplete: (Boolean, Player?) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("players")
        .whereEqualTo("playerName", playerName)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val document = documents.documents[0]
                val player = Player(
                    playerId = document.id,
                    playerName = document.getString("playerName") ?: "",
                    score = document.getLong("score")?.toInt() ?: 0
                )
                onComplete(true, player)
            } else {
                onComplete(false, null)
            }
        }
        .addOnFailureListener { e ->
            Log.e("UserCreationScreen", "Error checking user existence", e)
            onComplete(false, null)
        }
}
fun addPlayerToFirestore(player: Player, onUserCreated: (Player) -> Unit, onComplete: (Boolean) -> Unit) {
    val playerData = hashMapOf(
        "playerName" to player.playerName,
        "score" to player.score
    )

    Log.d("Firestore", "Adding player: ${player.playerName}")

    FirebaseFirestore.getInstance()
        .collection("players")
        .add(playerData)
        .addOnSuccessListener { documentReference ->
            val newPlayerId = documentReference.id
            documentReference.update("playerId", newPlayerId)
                .addOnSuccessListener {
                    Log.d("Firestore", "Player successfully created with ID: $newPlayerId")
                    val updatedPlayer = Player(
                        playerId = newPlayerId,
                        playerName = player.playerName,
                        score = player.score
                    )
                    onUserCreated(updatedPlayer)
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error updating player ID", e)
                    onComplete(false)
                }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error adding player to Firestore", e)
            onComplete(false)
        }
}
fun updatePlayerScore(playerName: String, newScore: Int, onComplete: (Boolean) -> Unit) {
    val playerRef = FirebaseFirestore.getInstance()
        .collection("players")
        .document(playerName)
    playerRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            // Document exists, now update the score
            playerRef.update("score", newScore)
                .addOnSuccessListener {
                    Log.d("Firestore", "Successfully updated score for $playerName")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error updating score", e)
                    onComplete(false)
                }
        } else {
            Log.e("Firestore", "Player document does not exist: $playerName")
            onComplete(false)
        }
    }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error checking player document", e)
            onComplete(false)
        }
}
@Preview
@Composable
fun PreviewUserCreationScreen() {

    UserCreationScreen(onUserCreated = {}, navController = rememberNavController())
}
