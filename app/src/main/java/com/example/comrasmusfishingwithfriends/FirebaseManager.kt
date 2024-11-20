package com.example.comrasmusfishingwithfriends

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val db = FirebaseFirestore.getInstance()

    fun updatePlayer(player: Player, onComplete: (Boolean) -> Unit) {
        val playerData = hashMapOf(
            "playerId" to player.playerId,
            "playerName" to player.playerName,
            "score" to player.score,
            "achievements" to player.achievements.map { achievement ->
                hashMapOf(
                    "id" to achievement.id,
                    "currentCount" to achievement.currentCount,
                    "isUnlocked" to achievement.isUnlocked
                )
            },
            "fishCatalog" to player.fishCatalog.getEntries().map { entry ->
                hashMapOf(
                    "fishType" to entry.fishType,
                    "timesCaught" to entry.timesCaught,
                    "largestWeight" to entry.largestWeight,
                    "totalPoints" to entry.totalPoints
                )
            },
            "unlockedRods" to player.unlockedRods,
            "currentRodId" to player.currentRodId,
            "dailyChallengeProgress" to player.dailyChallengeProgress,
            "completedChallenges" to player.completedChallenges,
            "totalChallengeBonus" to player.totalChallengeBonus
        )

        db.collection("players")
            .document(player.playerId)
            .set(playerData)
            .addOnSuccessListener {
                Log.d("Firebase", "Player data successfully updated")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error updating player data", e)
                onComplete(false)
            }
    }

    suspend fun loadPlayer(playerId: String): Player? {
        return try {
            val document = db.collection("players")
                .document(playerId)
                .get()
                .await()

            if (document.exists()) {
                val data = document.data
                if (data != null) {
                    Player(
                        playerId = document.id,
                        playerName = data["playerName"] as String,
                        score = (data["score"] as Number).toInt(),
                        achievements = (data["achievements"] as? List<Map<String, Any>>)?.map { achievementData ->
                            val achievement = Achievements.allAchievements.find { 
                                it.id == achievementData["id"] 
                            } ?: Achievement("", "", "", R.drawable.ic_star, 0)
                            achievement.apply {
                                currentCount = (achievementData["currentCount"] as Number).toInt()
                                isUnlocked = achievementData["isUnlocked"] as Boolean
                            }
                        }?.toMutableList() ?: mutableListOf(),
                        fishCatalog = FishCatalog().apply {
                            (data["fishCatalog"] as? List<Map<String, Any>>)?.forEach { fishData ->
                                addCatch(Fish(
                                    type = fishData["fishType"] as String,
                                    weight = (fishData["largestWeight"] as Number).toDouble(),
                                    points = (fishData["totalPoints"] as Number).toInt()
                                ))
                            }
                        },
                        unlockedRods = (data["unlockedRods"] as? List<String>)?.toMutableList() 
                            ?: mutableListOf("basic_rod"),
                        currentRodId = data["currentRodId"] as? String ?: "basic_rod",
                        dailyChallengeProgress = (data["dailyChallengeProgress"] as? Map<String, Int>)?.toMutableMap()
                            ?: mutableMapOf(),
                        completedChallenges = (data["completedChallenges"] as? Number)?.toInt() ?: 0,
                        totalChallengeBonus = (data["totalChallengeBonus"] as? Number)?.toInt() ?: 0
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("Firebase", "Error loading player data", e)
            null
        }
    }
} 