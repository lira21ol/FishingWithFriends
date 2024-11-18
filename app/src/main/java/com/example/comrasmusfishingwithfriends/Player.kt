package com.example.comrasmusfishingwithfriends

data class Player(
    val playerId: String = "",     // Default value, allows deserialization
    val playerName: String = "",   // Default value, allows deserialization
    var score: Int = 0             // var is needed to update the score
)