package com.example.comrasmusfishingwithfriends

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.example.comrasmusfishingwithfriends.ui.theme.ComrasmusfishingwithfriendsTheme
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComrasmusfishingwithfriendsTheme {


                val navController = rememberNavController()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    FishingApp(navController)
                }
            }
        }
    }
}