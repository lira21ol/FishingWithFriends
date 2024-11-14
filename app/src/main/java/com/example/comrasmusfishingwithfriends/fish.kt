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
    val fishTypes = listOf("Trout", "Salmon", "Bass", "Catfish", "Carp", "Pike", "Goldfish", "Dragonfish", "Great White Shark")
    return fishTypes.random()
}


fun getFishPoints(fishType: String): Int {
    return when (fishType) {
        "Salmon" -> 10
        "Trout" -> 8
        "Bass" -> 5
        "Catfish" -> 7
        "Carp" -> 6
        "Pike" -> 9
        "Goldfish" -> 3
        "Dragonfish" -> 15
        "Great White Shark" -> 20
        else -> 1
    }
}

