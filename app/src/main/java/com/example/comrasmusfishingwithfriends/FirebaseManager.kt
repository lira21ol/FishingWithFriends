package com.example.comrasmusfishingwithfriends

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    val db = FirebaseFirestore.getInstance()

    private fun generateLobbyCode(): String {
        val allowedChars = ('A'..'Z').toList()
        return (1..6).map { allowedChars.random() }.joinToString("")
    }

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

    fun startLobbyCountdown(lobbyCode: String) {
        db.collection("lobbies")
            .document(lobbyCode)
            .update("isStarting", true)
            .addOnSuccessListener {
                Log.d("Firebase", "Successfully started countdown for lobby: $lobbyCode")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error starting countdown for lobby: $lobbyCode", e)
            }
    }

    suspend fun createLobby(hostPlayer: Player): String {
        // Rensa gamla lobbies först
        cleanupOldLobbies()
        
        // Skapa sedan ny lobby
        val lobbyCode = generateLobbyCode()
        val lobbyData = hashMapOf(
            "hostId" to hostPlayer.playerId,
            "hostPlayer" to hashMapOf(
                "playerId" to hostPlayer.playerId,
                "playerName" to hostPlayer.playerName,
                "score" to hostPlayer.score
            ),
            "active" to true,
            "isStarting" to false,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("lobbies")
            .document(lobbyCode)
            .set(lobbyData)
            .await()

        return lobbyCode
    }

    suspend fun joinLobby(lobbyCode: String, player: Player): Boolean {
        try {
            // Kontrollera om lobbyn finns och är aktiv
            val lobbyDoc = db.collection("lobbies")
                .document(lobbyCode)
                .get()
                .await()

            if (!lobbyDoc.exists() || !(lobbyDoc.data?.get("active") as? Boolean ?: false)) {
                Log.e("Firebase", "Lobby does not exist or is not active")
                return false
            }

            // Kontrollera om spelaren redan är i en annan aktiv lobby
            val existingLobbies = db.collection("lobbies")
                .whereEqualTo("guestId", player.playerId)
                .whereEqualTo("active", true)
                .get()
                .await()

            // Lämna alla andra lobbies först
            existingLobbies.documents.forEach { doc ->
                doc.reference.update("active", false).await()
            }

            // Uppdatera den nya lobbyn med gästspelaren
            db.collection("lobbies")
                .document(lobbyCode)
                .update(
                    mapOf(
                        "guestId" to player.playerId,
                        "guestPlayer" to hashMapOf(
                            "playerId" to player.playerId,
                            "playerName" to player.playerName,
                            "score" to player.score
                        )
                    )
                )
                .await()

            return true
        } catch (e: Exception) {
            Log.e("Firebase", "Error joining lobby: ${e.message}")
            return false
        }
    }

    fun observeLobby(lobbyCode: String, onUpdate: (Lobby?) -> Unit) {
        db.collection("lobbies")
            .document(lobbyCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firebase", "Error observing lobby: ${error.message}")
                    onUpdate(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val lobby = Lobby.fromMap(lobbyCode, snapshot.data ?: mapOf())
                    onUpdate(lobby)
                } else {
                    onUpdate(null)
                }
            }
    }

    // Skicka inbjudan till en spelare
    suspend fun sendInvite(fromPlayer: Player, toPlayerId: String): String {
        try {
            val lobbyCode = createLobby(fromPlayer)  // Skapa lobby först
            
            val inviteData = hashMapOf(
                "fromPlayerId" to fromPlayer.playerId,
                "fromPlayerName" to fromPlayer.playerName,
                "lobbyCode" to lobbyCode,
                "status" to "pending",
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            // Spara inbjudan i mottagarens collection
            db.collection("players")
                .document(toPlayerId)
                .collection("invites")
                .document()
                .set(inviteData)
                .await()

            return lobbyCode
        } catch (e: Exception) {
            Log.e("Firebase", "Error sending invite: ${e.message}")
            throw e
        }
    }

    // Lyssna efter inbjudningar
    fun observeInvites(playerId: String, onUpdate: (Map<String, Any>?) -> Unit): () -> Unit {
        val listenerRegistration = db.collection("players")
            .document(playerId)
            .collection("invites")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firebase", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val invite = snapshot.documents.first()
                    val inviteData = invite.data
                    if (inviteData != null) {
                        // Lägg till invite ID i datan
                        val inviteWithId = inviteData.toMutableMap()
                        inviteWithId["inviteId"] = invite.id
                        onUpdate(inviteWithId)
                    }
                } else {
                    onUpdate(null)
                }
            }

        // Returnera en funktion som kan användas för att stoppa lyssnaren
        return { listenerRegistration.remove() }
    }

    // Acceptera eller avvisa inbjudan
    suspend fun respondToInvite(playerId: String, inviteId: String, accept: Boolean): Boolean {
        return try {
            val inviteRef = db.collection("players")
                .document(playerId)
                .collection("invites")
                .document(inviteId)

            val invite = inviteRef.get().await()
            val lobbyCode = invite.getString("lobbyCode")

            // Ta bort alla andra väntande inbjudningar för denna spelare
            db.collection("players")
                .document(playerId)
                .collection("invites")
                .whereEqualTo("status", "pending")
                .get()
                .await()
                .documents
                .forEach { doc ->
                    if (doc.id != inviteId) {
                        doc.reference.update("status", "cancelled").await()
                    }
                }

            if (accept && lobbyCode != null) {
                val playerDoc = db.collection("players").document(playerId).get().await()
                val data = playerDoc.data
                if (data != null) {
                    val player = Player(
                        playerId = playerId,
                        playerName = data["playerName"] as? String ?: "",
                        score = (data["score"] as? Number)?.toInt() ?: 0
                    )
                    
                    val joinSuccess = joinLobby(lobbyCode, player)
                    if (joinSuccess) {
                        inviteRef.update("status", "accepted").await()
                        return true
                    }
                }
            } else {
                inviteRef.update("status", "declined").await()
                return true
            }
            false
        } catch (e: Exception) {
            Log.e("Firebase", "Error responding to invite: ${e.message}")
            false
        }
    }

    suspend fun getAvailablePlayers(currentPlayerId: String): List<Player> {
        return try {
            val snapshot = db.collection("players")
                .whereNotEqualTo("playerId", currentPlayerId)  // Exkludera nuvarande spelare
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Player(
                    playerId = doc.id,
                    playerName = data["playerName"] as? String ?: return@mapNotNull null,
                    score = (data["score"] as? Number)?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error getting available players: ${e.message}")
            emptyList()
        }
    }

    suspend fun findPlayerByName(playerName: String): Player? {
        return try {
            val snapshot = db.collection("players")
                .whereEqualTo("playerName", playerName)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                val data = doc.data ?: return null
                Player(
                    playerId = doc.id,
                    playerName = data["playerName"] as String,
                    score = (data["score"] as? Number)?.toInt() ?: 0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error finding player: ${e.message}")
            null
        }
    }

    fun getCurrentPlayerId(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }

    suspend fun leaveLobby(lobbyCode: String, playerId: String) {
        try {
            val lobbyRef = db.collection("lobbies").document(lobbyCode)
            val lobby = lobbyRef.get().await()
            
            if (lobby.exists()) {
                val hostId = lobby.getString("hostId")
                
                if (hostId == playerId) {
                    // Om värden lämnar, stäng lobbyn och rensa inbjudningar
                    lobbyRef.update("active", false).await()
                    
                    // Hämta och rensa gästens inbjudningar
                    val guestId = lobby.getString("guestId")
                    if (guestId != null) {
                        cleanupPlayerInvites(guestId, lobbyCode)
                    }
                } else {
                    // Om gästen lämnar, ta bort gästen från lobbyn
                    lobbyRef.update(
                        mapOf(
                            "guestId" to null,
                            "guestPlayer" to null
                        )
                    ).await()
                }
                
                // Rensa värdens inbjudningar
                cleanupPlayerInvites(playerId, lobbyCode)
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error leaving lobby: ${e.message}")
        }
    }

    private suspend fun cleanupPlayerInvites(playerId: String, lobbyCode: String) {
        try {
            // Hitta och rensa alla inbjudningar relaterade till denna lobby
            val invites = db.collection("players")
                .document(playerId)
                .collection("invites")
                .whereEqualTo("lobbyCode", lobbyCode)
                .get()
                .await()

            invites.documents.forEach { doc ->
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error cleaning up invites: ${e.message}")
        }
    }

    // Lägg till denna funktion för att rensa gamla lobbies
    suspend fun cleanupOldLobbies() {
        try {
            val oneHourAgo = com.google.firebase.Timestamp.now().toDate().time - (60 * 60 * 1000)
            val oldLobbies = db.collection("lobbies")
                .whereLessThan("createdAt", com.google.firebase.Timestamp(oneHourAgo / 1000, 0))
                .whereEqualTo("active", true)
                .get()
                .await()

            oldLobbies.documents.forEach { doc ->
                doc.reference.update("active", false).await()
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error cleaning up old lobbies: ${e.message}")
        }
    }

    suspend fun markLobbyAsInactive(lobbyCode: String) {
        try {
            db.collection("lobbies")
                .document(lobbyCode)
                .update(
                    mapOf(
                        "active" to false,
                        "gameStarted" to true
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e("Firebase", "Error marking lobby as inactive: ${e.message}")
        }
    }
} 