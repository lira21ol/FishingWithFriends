package com.example.comrasmusfishingwithfriends

data class FishEntry(
    val fishType: String,
    var timesCaught: Int = 0,
    var largestWeight: Double = 0.0,
    var totalPoints: Int = 0,
    val location: String
)

class FishCatalog(
    val discoveredFish: MutableMap<String, Int> = mutableMapOf(),
    val caughtFish: MutableMap<String, Int> = mutableMapOf()
) {
    val catalog = mutableMapOf<String, FishEntry>()
    
    companion object {
        val pondFish = listOf(
            "Trout",
            "Bass", 
            "Catfish",
            "Carp",
            "Pike",
            "Goldfish"
        )
        
        val oceanFish = listOf(
            "Tuna",
            "Swordfish",
            "Shark",
            "Dolphinfish",
            "Marlin",
            "Octopus",
            "Giant Squid"
        )
        
        val riverFish = listOf(
            "Salmon",
            "Rainbow Trout",
            "Sturgeon",
            "Eel",
            "Arctic Char",
            "Grayling"
        )

        val bossArenaFish = listOf(
            "Kraken",
            "Megalodon",
            "Leviathan"
        )

        val allFishTypes = listOf(
            FishLocation("Dammen", pondFish),
            FishLocation("Havet", oceanFish),
            FishLocation("Floden", riverFish),
            FishLocation("Boss Arena", bossArenaFish)
        )
    }

    fun addCatch(fish: Fish) {
        val location = when (fish.type) {
            in pondFish -> "Dammen"
            in oceanFish -> "Havet"
            in riverFish -> "Floden"
            in bossArenaFish -> "Boss Arena"
            else -> "Okänd plats"
        }
        
        val entry = catalog.getOrPut(fish.type) { 
            FishEntry(
                fishType = fish.type,
                timesCaught = 0,
                largestWeight = 0.0,
                totalPoints = 0,
                location = location
            ) 
        }
        entry.timesCaught++
        entry.largestWeight = maxOf(entry.largestWeight, fish.weight)
        entry.totalPoints += fish.points

        // Uppdatera caughtFish map
        caughtFish[fish.type] = (caughtFish[fish.type] ?: 0) + 1
    }

    fun isFishCaught(fishType: String): Boolean {
        return caughtFish.containsKey(fishType)
    }

    fun getEntries(): List<FishEntry> = catalog.values.toList()
    
    fun getAllFishEntries(): List<Pair<String, List<FishEntry>>> {
        return allFishTypes.map { location ->
            location.name to location.fish.map { fishType ->
                catalog.getOrElse(fishType) { 
                    FishEntry(fishType, timesCaught = 0, location = location.name) 
                }
            }
        }
    }

    fun addDiscovery(fishType: String) {
        discoveredFish[fishType] = discoveredFish.getOrDefault(fishType, 0) + 1
    }

    fun hasDiscovered(fishType: String): Boolean {
        return discoveredFish.containsKey(fishType)
    }

    fun hasCaught(fishType: String): Boolean {
        return caughtFish.containsKey(fishType)
    }

    fun getCatchCount(fishType: String): Int {
        return caughtFish.getOrDefault(fishType, 0)
    }
}

data class FishLocation(
    val name: String,
    val fish: List<String>
) 