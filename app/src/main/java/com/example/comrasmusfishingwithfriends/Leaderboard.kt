package com.example.comrasmusfishingwithfriends


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign

import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.util.Log

// A Composable to show the leaderboard
@Composable
fun LeaderboardScreen(navController: NavHostController) {
    var leaderboardEntries by remember { mutableStateOf<List<LeaderboardEntryData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("players")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("Leaderboard", "Antal hämtade dokument: ${documents.size()}")
                val entries = documents.mapNotNull { doc ->
                    Log.d("Leaderboard", "Spelardata: ${doc.data}")
                    val name = doc.getString("playerName") ?: doc.getString("playerId")
                    val score = doc.getLong("score")?.toInt() ?: 0
                    
                    if (name != null) {
                        LeaderboardEntryData(
                            rank = 0,
                            playerName = name,
                            playerScore = score
                        )
                    } else null
                }.sortedByDescending { it.playerScore }
                 .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
                
                Log.d("Leaderboard", "Antal entries efter mappning: ${entries.size}")
                leaderboardEntries = entries
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("Leaderboard", "Fel vid hämtning av data: ${e.message}")
                errorMessage = "Kunde inte ladda topplistan: ${e.message}"
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Topplista",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF2E7D32),
            modifier = Modifier.padding(bottom = 16.dp),
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )

        Image(
            painter = painterResource(id = R.drawable.pokal),
            contentDescription = "Topplista Ikon",
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(bottom = 16.dp)
        )

        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }
            leaderboardEntries.isEmpty() -> {
                Text(
                    text = "Inga fiskare har registrerat några poäng än!",
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(leaderboardEntries) { entry ->
                        LeaderboardEntry(entry)
                    }
                }
            }
        }

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Tillbaka", color = Color.White)
        }
    }
}

@Composable
fun LeaderboardEntry(entry: LeaderboardEntryData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                when (entry.rank) {
                    1 -> Color(0xFFFFD700) // Guld
                    2 -> Color(0xFFC0C0C0) // Silver
                    3 -> Color(0xFFCD7F32) // Brons
                    else -> MaterialTheme.colorScheme.secondary
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${entry.rank}",
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            color = if (entry.rank <= 3) Color.Black else Color.White
        )
        Text(
            text = entry.playerName,
            modifier = Modifier.weight(2f),
            fontSize = 18.sp,
            color = if (entry.rank <= 3) Color.Black else Color.White
        )
        Text(
            text = "${entry.playerScore} poäng",
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            color = if (entry.rank <= 3) Color.Black else Color.White
        )
    }
}

// Sample data class to hold leaderboard entry data
data class LeaderboardEntryData(
    val rank: Int,
    val playerName: String,
    val playerScore: Int
)

