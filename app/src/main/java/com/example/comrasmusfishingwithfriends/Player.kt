package com.example.comrasmusfishingwithfriends



data class Player(
    val playerId: String,
    val playerName: String,
    var score: Int = 0
)