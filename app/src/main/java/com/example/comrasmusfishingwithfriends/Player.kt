package com.example.comrasmusfishingwithfriends

data class Player(
    val playerId: String = "",
    val playerName: String = "",
    var score: Int = 0,
    var achievements: MutableList<Achievement> = mutableListOf(),
    var fishCatalog: FishCatalog = FishCatalog(),
    var unlockedRods: MutableList<String> = mutableListOf("basic_rod"),
    var currentRodId: String = "basic_rod",
    var dailyChallengeProgress: MutableMap<String, Int> = mutableMapOf(),
    var completedChallenges: Int = 0,
    var totalChallengeBonus: Int = 0
)