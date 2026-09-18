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
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun FishingApp() {
    val navController = rememberNavController()
    var currentPlayer by remember { mutableStateOf<Player?>(null) }

    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            FirebaseManager.loadPlayer(auth.currentUser!!.uid)?.let { loadedPlayer ->
                val newCatalog = FishCatalog().apply {
                    loadedPlayer.fishCatalog.discoveredFish.forEach { (fishType, count) ->
                        this.discoveredFish[fishType] = count
                    }
                    loadedPlayer.fishCatalog.caughtFish.forEach { (fishType, count) ->
                        this.caughtFish[fishType] = count
                    }
                }

                currentPlayer = loadedPlayer.copy(
                    fishCatalog = newCatalog,
                    score = loadedPlayer.score,
                    achievements = loadedPlayer.achievements,
                    unlockedRods = loadedPlayer.unlockedRods,
                    currentRodId = loadedPlayer.currentRodId,
                    dailyChallengeProgress = loadedPlayer.dailyChallengeProgress,
                    completedChallenges = loadedPlayer.completedChallenges,
                    totalChallengeBonus = loadedPlayer.totalChallengeBonus
                )
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "user_creation_screen"
    ) {
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

        composable("kraken_boss") {
            currentPlayer?.let { player ->
                KrakenBossScreen(
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

        composable("boss_arena_coop") {
            currentPlayer?.let { player ->
                if (player.score >= 2000) {
                    CoopLobbyScreen(
                        currentPlayer = player,
                        navController = navController,
                        gameMode = "boss_arena"
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate("start_screen")
                    }
                }
            }
        }

        composable("kraken_boss_coop") {
            currentPlayer?.let { player ->
                KrakenBossCoopScreen(
                    currentPlayer = player,
                    navController = navController
                )
            }
        }

        composable("free_mode") {
            ThreeDGameScreen(navController = navController)
        }
    }
}