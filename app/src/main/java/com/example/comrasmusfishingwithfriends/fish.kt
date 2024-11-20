package com.example.comrasmusfishingwithfriends

data class Fish(
    val type: String,
    val weight: Double = (1..10).random().toDouble(),
    val points: Int
) {
    constructor() : this(
        type = getRandomFishType(),
        points = getFishPoints(getRandomFishType())
    )
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
        else -> 1
    }
}

