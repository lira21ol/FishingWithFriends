package com.example.comrasmusfishingwithfriends

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID


@Composable
fun UserCreationScreen(onUserCreated: (Player) -> Unit) {
    var fisherName by remember { mutableStateOf("") }
    var isCreatingUser by remember { mutableStateOf(false) }
    var creationError by remember { mutableStateOf<String?>(null) }

    val db = FirebaseFirestore.getInstance()

    fun isUserNameValid(): Boolean {
        return fisherName.trim().isNotEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = fisherName,
            onValueChange = { fisherName = it },
            label = { Text("Name your fisher:") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isUserNameValid()) {
                    isCreatingUser = true
                    creationError = null


                    val newPlayer = Player(playerId = fisherName, playerName = fisherName, score = 0)


                    addPlayerToFirestore(newPlayer) { success ->
                        if (success) {
                            onUserCreated(newPlayer)
                        } else {
                            creationError = "Error creating user. Please try again."
                        }
                        isCreatingUser = false
                    }
                } else {
                    creationError = "Please enter a valid name."
                }
            },
            enabled = !isCreatingUser && fisherName.isNotBlank()
        ) {
            Text(if (isCreatingUser) "Creating..." else "Create User")
        }

        creationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}


fun addPlayerToFirestore(player: Player, onComplete: (Boolean) -> Unit) {
    val playerData = hashMapOf(
        "playerId" to player.playerId,
        "playerName" to player.playerName,
        "score" to player.score
    )
    db.collection("players").document(player.playerId).set(playerData)
        .addOnSuccessListener {
            onComplete(true)
        }
        .addOnFailureListener { e ->
            Log.e("UserCreationScreen", "Error adding player to Firestore", e)
            onComplete(false)
        }
}