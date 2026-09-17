package com.example.comrasmusfishingwithfriends

import com.google.firebase.Timestamp

data class Lobby(
    val lobbyCode: String,
    val hostId: String,
    val hostPlayer: Player,
    val guestPlayer: Player?,
    val isStarting: Boolean = false,
    val createdAt: Timestamp
) {
    companion object {
        fun fromMap(lobbyCode: String, data: Map<String, Any>): Lobby? {
            return try {
                val hostPlayerData = data["hostPlayer"] as? Map<String, Any> ?: return null
                val guestPlayerData = data["guestPlayer"] as? Map<String, Any>
                
                Lobby(
                    lobbyCode = lobbyCode,
                    hostId = data["hostId"] as String,
                    hostPlayer = Player.fromMap(hostPlayerData),
                    guestPlayer = guestPlayerData?.let { Player.fromMap(it) },
                    isStarting = data["isStarting"] as? Boolean ?: false,
                    createdAt = data["createdAt"] as Timestamp
                )
            } catch (e: Exception) {
                null
            }
        }
    }
} 