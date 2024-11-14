package com.example.comrasmusfishingwithfriends

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.comrasmusfishingwithfriends.ui.theme.ComrasmusfishingwithfriendsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComrasmusfishingwithfriendsTheme {

                var currentPlayer by remember { mutableStateOf<Player?>(null) }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (currentPlayer == null) {

                        UserCreationScreen { newPlayer ->

                            currentPlayer = newPlayer
                        }
                    } else {

                        FishingGameScreen(currentPlayer = currentPlayer!!)
                    }
                }
            }
        }
    }
}