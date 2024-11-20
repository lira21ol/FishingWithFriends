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
                    currentPlayer = player
                    navController.navigate("start_screen")
                },
                navController = navController
            )
        }

        composable("start_screen") {
            LaunchedEffect(Unit) {
                currentPlayer?.let { player ->
                    FirebaseManager.loadPlayer(player.playerId)?.let { loadedPlayer ->
                        currentPlayer = loadedPlayer
                    }
                }
            }

            currentPlayer?.let { player ->
                StartScreen(
                    onGoFishingClick = {
                        navController.navigate("map_selection_screen")
                    },
                    onGoFishTogetherClick = {
                        navController.navigate("multiplayer_fishing_game_screen")
                    },
                    onLeaderboardClick = {
                        navController.navigate("leaderboard_screen")
                    },
                    playerScore = player.score,
                    currentPlayer = player,
                    navController = navController
                )
            } ?: run {
                LaunchedEffect(Unit) {
                    navController.navigate("user_creation_screen")
                }
            }
        }

        composable("fishing_game_screen") {
            currentPlayer?.let { player ->
                FishingGameScreen(
                    currentPlayer = player,
                    navController = navController
                )
            } ?: run {
                LaunchedEffect(Unit) {
                    navController.navigate("user_creation_screen")
                }
            }
        }

        composable("fishing_game_screen2") {
            currentPlayer?.let { player ->
                FishingGameScreen2(
                    currentPlayer = player,
                    navController = navController
                )
            } ?: run {
                LaunchedEffect(Unit) {
                    navController.navigate("user_creation_screen")
                }
            }
        }

        composable("fishing_game_screen3") {
            currentPlayer?.let { player ->
                FishingGameScreen3(
                    currentPlayer = player,
                    navController = navController
                )
            } ?: run {
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

        composable("map_selection_screen") {
            currentPlayer?.let { player ->
                MapSelectionScreen(
                    currentPlayer = player,
                    navController = navController
                )
            } ?: run {
                LaunchedEffect(Unit) {
                    navController.navigate("user_creation_screen")
                }
            }
        }
    }
}