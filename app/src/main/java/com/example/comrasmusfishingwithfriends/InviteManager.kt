package com.example.comrasmusfishingwithfriends

import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

data class Invite(
    val hostName: String,
    val lobbyId: String
)

object InviteManager {
    private var currentListener: (() -> Unit)? = null

    suspend fun sendInvite(fromPlayer: Player, toPlayerId: String): String {
        try {
            val lobbyCode = FirebaseManager.createLobby(fromPlayer)
            fromPlayer.currentLobbyCode = lobbyCode

            val inviteData = hashMapOf(
                "fromPlayerId" to fromPlayer.playerId,
                "fromPlayerName" to fromPlayer.playerName,
                "lobbyCode" to lobbyCode,
                "timestamp" to Timestamp.now(),
                "status" to "pending"
            )

            val inviteRef = FirebaseManager.db.collection("players")
                .document(toPlayerId)
                .collection("invites")
                .document()

            inviteRef.set(inviteData).await()

            return lobbyCode
        } catch (e: Exception) {
            throw Exception("Failed to send invite: ${e.message}")
        }
    }

    fun startListeningForInvites(playerId: String, onInvite: (Map<String, Any>) -> Unit) {
        stopListeningForInvites()

        currentListener = FirebaseManager.observeInvites(playerId) { data ->
            if (data != null) {
                onInvite(data)
            }
        }
    }

    fun stopListeningForInvites() {
        currentListener?.invoke()
        currentListener = null
    }
} 