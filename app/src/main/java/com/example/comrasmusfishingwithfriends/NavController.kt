package com.example.comrasmusfishingwithfriends

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


@Composable
fun FishingApp(navController: NavHostController) {

    NavHost(navController = navController, startDestination = "start_screen") {
        composable("start_screen") {
            StartScreen(
                onGoFishingClick = {
                    navController.navigate("fishing_game_screen")
                },
                onGoFishTogetherClick = {
                    navController.navigate("fishing_game_screen")
                }
            )
        }
        composable("fishing_game_screen") {

            FishingGameScreen(
                currentPlayer = TODO()
            )
        }
    }
}