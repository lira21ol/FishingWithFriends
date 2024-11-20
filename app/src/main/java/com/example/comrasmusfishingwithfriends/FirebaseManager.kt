package com.example.comrasmusfishingwithfriends

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val db = FirebaseFirestore.getInstance()

    fun updatePlayer(player: Player, onComplete: (Boolean) -> Unit) {
        val catalogData = player.fishCatalog.catalog.map { (fishType, entry) ->
            hashMapOf(
                "fishType" to entry.fishType,
                "timesCaught" to entry.timesCaught,
                "largestWeight" to entry.largestWeight,
                "totalPoints" to entry.totalPoints,
                "location" to entry.location
            )
        }

        val playerData = hashMapOf(
            "playerName" to player.playerName,
            "score" to player.score,
            "fishCatalog" to catalogData
        )

        db.collection("players")
            .document(player.playerId)
            .set(playerData)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                Log.e("Firebase", "Error updating player: ${it.message}")
                onComplete(false)
            }
    }

    suspend fun loadPlayer(playerId: String): Player? {
        return try {
            val document = db.collection("players")
                .document(playerId)
                .get()
                .await()

            if (document != null && document.exists()) {
                val data = document.data
                if (data != null) {
                    val fishCatalog = FishCatalog()
                    
                    // Ladda fiskekatalog
                    (data["fishCatalog"] as? List<Map<String, Any>>)?.forEach { fishData ->
                        val fishType = fishData["fishType"] as String
                        val timesCaught = (fishData["timesCaught"] as Number).toInt()
                        val largestWeight = (fishData["largestWeight"] as Number).toDouble()
                        val totalPoints = (fishData["totalPoints"] as Number).toInt()
                        
                        // Bestäm plats baserat på fisktyp
                        val location = when (fishType) {
                            in FishCatalog.pondFish -> "Dammen"
                            in FishCatalog.oceanFish -> "Havet"
                            in FishCatalog.riverFish -> "Floden"
                            in FishCatalog.bossArenaFish -> "Boss Arena"
                            else -> "Okänd plats"
                        }
                        
                        // Uppdatera både catalog och caughtFish
                        fishCatalog.catalog[fishType] = FishEntry(
                            fishType = fishType,
                            timesCaught = timesCaught,
                            largestWeight = largestWeight,
                            totalPoints = totalPoints,
                            location = location
                        )
                        fishCatalog.caughtFish[fishType] = timesCaught
                    }

                    return Player(
                        playerId = playerId,
                        playerName = data["playerName"] as? String ?: "",
                        score = (data["score"] as? Number)?.toInt() ?: 0,
                        fishCatalog = fishCatalog
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e("Firebase", "Error loading player: ${e.message}")
            null
        }
    }

    fun updateFishCatalog(playerId: String, fishCatalog: FishCatalog) {
        val db = FirebaseFirestore.getInstance()
        
        // Konvertera fiskkatalogen till ett Map-format som Firebase kan hantera
        val catalogData = hashMapOf(
            "catalog" to fishCatalog.catalog.mapValues { (_, entry) ->
                hashMapOf(
                    "fishType" to entry.fishType,
                    "timesCaught" to entry.timesCaught,
                    "largestWeight" to entry.largestWeight,
                    "totalPoints" to entry.totalPoints,
                    "location" to entry.location
                )
            },
            "caughtFish" to fishCatalog.caughtFish
        )
        
        // Uppdatera bara fiskkatalog-delen av spelarens dokument
        db.collection("players")
            .document(playerId)
            .update("fishCatalog", catalogData)
            .addOnSuccessListener {
                Log.d("FirebaseManager", "Fiskkatalog uppdaterad framgångsrikt")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseManager", "Fel vid uppdatering av fiskkatalog", e)
            }
    }
} 