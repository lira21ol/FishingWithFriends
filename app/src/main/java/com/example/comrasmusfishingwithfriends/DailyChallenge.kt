package com.example.comrasmusfishingwithfriends

data class DailyChallenge(
    val id: String,
    val description: String,
    val targetFishType: String? = null,
    val targetCount: Int,
    val rewardPoints: Int
)

object DailyChallengeSystem {
    fun generateDailyChallenge(): DailyChallenge {
        val challenges = listOf(
            DailyChallenge("catch_salmon", "Fånga 3 laxar", "Salmon", 3, 50),
            DailyChallenge("catch_any", "Fånga 10 fiskar", null, 10, 100),
            DailyChallenge("score_points", "Samla 100 poäng", null, 100, 50),
            DailyChallenge("catch_shark", "Fånga en haj", "Great White Shark", 1, 200)
        )
        return challenges.random()
    }
} 