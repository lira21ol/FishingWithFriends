package com.example.comrasmusfishingwithfriends

data class Player(
    val playerId: String,
    var playerName: String,
    var score: Int = 0,
    var fishCatalog: FishCatalog = FishCatalog(),
    var achievements: MutableList<Achievement> = mutableListOf(),
    var unlockedRods: MutableSet<String> = mutableSetOf("basic_rod"),
    var currentRodId: String = "basic_rod",
    var dailyChallengeProgress: MutableMap<String, Int> = mutableMapOf(),
    var completedChallenges: MutableSet<String> = mutableSetOf(),
    var totalChallengeBonus: Int = 0,
    var isHost: Boolean = false,
    var currentLobbyCode: String? = null
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "playerId" to playerId,
            "playerName" to playerName,
            "score" to score,
            "isHost" to isHost,
            "currentLobbyCode" to (currentLobbyCode ?: "")
        )
    }

    companion object {
        fun fromMap(data: Map<String, Any>): Player {
            return Player(
                playerId = data["playerId"] as String,
                playerName = data["playerName"] as String,
                score = (data["score"] as Number).toInt(),
                isHost = data["isHost"] as? Boolean ?: false,
                currentLobbyCode = data["currentLobbyCode"] as? String
            )
        }
    }
}