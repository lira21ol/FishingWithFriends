package com.example.comrasmusfishingwithfriends

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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
                    text = "Fishing With Friends Game, by Rallter",
                    style = TextStyle(
                        color = Color(0xFF90EE90),
                        fontSize = 30.sp,
                    ),
                    modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Name your fisher:",
                    style = TextStyle(
                        color = Color(0xFF90EE90),
                        fontSize = 30.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
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
                    fontSize = 30.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isUserNameValid()) {
                            isCreatingUser = true
                            creationError = null

                            val newPlayer = Player(playerId = fisherName, playerName = fisherName, score = 0)

                            addPlayerToFirestore(newPlayer) { success ->
                                if (success) {
                                    onUserCreated(newPlayer)
                                    // After user creation, navigate to the start screen
                                    navController.navigate("start_screen")
                                } else {
                                    creationError = "Error creating user. Please try again."
                                }
                                isCreatingUser = false
                            }
                        } else {
                            creationError = "Please enter a valid name."
                        }
                    },
                    enabled = !isCreatingUser && fisherName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color(0xFFD3B1F3)
                    ),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = if (isCreatingUser) "Creating..." else "Create User",
                        style = TextStyle(
                            color = Color(0xFFD3B1F3),
                            fontSize = 30.sp
                        )
                    )
                }
            }

            creationError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

fun addPlayerToFirestore(player: Player, onComplete: (Boolean) -> Unit) {
    val playerData = hashMapOf(
        "playerId" to player.playerId,
        "playerName" to player.playerName,
        "score" to player.score
    )
    FirebaseFirestore.getInstance().collection("players").document(player.playerId).set(playerData)
        .addOnSuccessListener {
            onComplete(true)
        }
        .addOnFailureListener { e ->
            Log.e("UserCreationScreen", "Error adding player to Firestore", e)
            onComplete(false)
        }
}

@Preview
@Composable
fun PreviewUserCreationScreen() {

    UserCreationScreen(onUserCreated = {}, navController = rememberNavController())
}
