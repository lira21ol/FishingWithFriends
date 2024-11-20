package com.example.comrasmusfishingwithfriends

data class FishEntry(
    val fishType: String,
    var timesCaught: Int = 0,
    var largestWeight: Double = 0.0,
    var totalPoints: Int = 0
)

class FishCatalog {
    private val catalog = mutableMapOf<String, FishEntry>()

    fun addCatch(fish: Fish) {
        val entry = catalog.getOrPut(fish.type) { 
            FishEntry(fish.type) 
        }
        entry.timesCaught++
        entry.largestWeight = maxOf(entry.largestWeight, fish.weight)
        entry.totalPoints += fish.points
    }

    fun getEntries(): List<FishEntry> = catalog.values.toList()
} 