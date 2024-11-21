package com.example.comrasmusfishingwithfriends

enum class FishRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

data class Fish(
    val type: String,
    val rarity: FishRarity = FishRarity.COMMON,
    val points: Int = 10,
    val weight: Double = generateRandomWeight(),
    val description: String = ""
) {
    companion object {
        fun generateRandomWeight(): Double {
            return (1..20).random() + Math.random()
        }
    }
}

fun getRandomFishType(): String {
    return when ((1..3).random()) {
        1 -> getRandomPondFish()
        2 -> getRandomOceanFish()
        3 -> getRandomRiverFish()
        else -> getRandomPondFish()
    }
}

fun getRandomPondFish(): String {
    val pondFish = listOf("Trout", "Bass", "Catfish", "Carp", "Pike", "Goldfish")
    return pondFish.random()
}

fun getRandomOceanFish(): String {
    val oceanFish = listOf(
        "Tuna", 
        "Swordfish", 
        "Shark", 
        "Dolphinfish", 
        "Marlin", 
        "Octopus", 
        "Giant Squid"
    )
    return oceanFish.random()
}

fun getRandomRiverFish(): String {
    val riverFish = listOf(
        "Salmon", 
        "Rainbow Trout", 
        "Sturgeon", 
        "Eel", 
        "Arctic Char", 
        "Grayling"
    )
    return riverFish.random()
}

fun getRandomFish3(): String {
    val riverFish = listOf(
        "Rainbow Trout",
        "Brown Trout", 
        "Salmon",
        "Pike",
        "Perch",
        "Catfish",
        "Carp",
        "Sturgeon",
        "Grayling",
        "Arctic Char"
    )
    return riverFish.random()
}

fun getFishPoints(fishType: String): Int {
    return when (fishType) {
        "Trout" -> 8
        "Bass" -> 5
        "Catfish" -> 7
        "Carp" -> 6
        "Pike" -> 9
        "Goldfish" -> 3
        "Tuna" -> 15
        "Swordfish" -> 25
        "Shark" -> 30
        "Dolphinfish" -> 20
        "Marlin" -> 35
        "Octopus" -> 40
        "Giant Squid" -> 50
        "Salmon" -> 12
        "Rainbow Trout" -> 14
        "Sturgeon" -> 45
        "Eel" -> 18
        "Arctic Char" -> 16
        "Grayling" -> 10
        "Hammerhead Shark" -> 45
        "Dolphin" -> 35
        "Manta Ray" -> 40
        "Crocodile" -> 50
        else -> 1
    }
}

fun getRiverFishPoints(fishType: String): Int {
    return when (fishType) {
        "Rainbow Trout" -> 25
        "Brown Trout" -> 20
        "Salmon" -> 30
        "Pike" -> 35
        "Perch" -> 15
        "Catfish" -> 40
        "Carp" -> 20
        "Sturgeon" -> 45
        "Grayling" -> 25
        "Arctic Char" -> 30
        else -> 10
    }
}

