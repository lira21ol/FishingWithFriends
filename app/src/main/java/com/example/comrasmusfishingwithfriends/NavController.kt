package com.example.comrasmusfishingwithfriends

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun FishingApp(navController: NavHostController) {
    var currentPlayer by remember { mutableStateOf<Player?>(null) }

    NavHost(navController = navController, startDestination = "user_creation_screen") {
        composable("user_creation_screen") {
            UserCreationScreen(
                onUserCreated = { player ->
                    currentPlayer = player  // Spara spelaren
                    navController.navigate("start_screen")
                },
                navController = navController
            )
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
                    navController.navigate("leaderboard_screen")
                }
            )
        }

        composable("fishing_game_screen") {
            currentPlayer?.let { player ->
                FishingGameScreen(currentPlayer = player)  // Skicka med den sparade spelaren
            } ?: run {
                // Om ingen spelare finns, gå tillbaka till användarregistrering
                LaunchedEffect(Unit) {
                    navController.navigate("user_creation_screen")
                }
            }
        }

        composable("multiplayer_fishing_game_screen") {
            MultiplayerFishingGameScreen()
        }

        composable("leaderboard_screen") {
            LeaderboardScreen(navController = navController)
        }
    }
}