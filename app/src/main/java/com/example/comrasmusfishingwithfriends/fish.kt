package com.example.comrasmusfishingwithfriends
data class Fish(
    val type: String,  // Explicitly take the type from the outside (not random)
    val weight: Double = (1..10).random().toDouble(),  // Random weight between 1 and 10 kg
    val points: Int  // Points are assigned based on the type
) {
    // Constructor to create a Fish object based on the random type and calculated points
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

