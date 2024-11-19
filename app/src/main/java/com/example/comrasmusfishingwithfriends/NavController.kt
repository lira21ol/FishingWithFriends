package com.example.comrasmusfishingwithfriends

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun FishingApp(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "user_creation_screen") {
        composable("user_creation_screen") {
            UserCreationScreen(onUserCreated = { player ->
                navController.navigate("start_screen")
            }, navController = navController)
        }

        composable("start_screen") {
            StartScreen(
                onGoFishingClick = {
                    navController.navigate("fishing_game_screen")
                },
                onGoFishTogetherClick = {
                    navController.navigate("multiplayer_fishing_game_screen")
                },
                onLeaderboardClick = {
                    navController.navigate("leaderboard_screen") // Navigate to the leaderboard screen
                }
            )
        }

        composable("fishing_game_screen") {
            FishingGameScreen(currentPlayer = Player("playerId", "Fisher", 0)) // Route to solo player fishing game
        }

        composable("multiplayer_fishing_game_screen") {
            MultiplayerFishingGameScreen()  // Route to multiplayer fishing game
        }

        composable("leaderboard_screen") {
            LeaderboardScreen(navController = navController)  // Leaderboard Screen
        }
    }
}