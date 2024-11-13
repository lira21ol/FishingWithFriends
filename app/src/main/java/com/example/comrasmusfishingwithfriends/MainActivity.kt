package com.example.comrasmusfishingwithfriends

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.comrasmusfishingwithfriends.ui.theme.ComrasmusfishingwithfriendsTheme
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Player(
    val playerId: String = "",
    var name: String = "",
    var invitation: String = "",
    var score: Int = 0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComrasmusfishingwithfriendsTheme  {

                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val db = Firebase.firestore
    val playerList = MutableStateFlow<List<Player>>(emptyList())

    db.collection("players")
        .addSnapshotListener{ value, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (value != null) {
                playerList.value = value.toObjects()
            }
        }

    val players by playerList.asStateFlow().collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(players) { player ->

                ListItem(
                    headlineContent = {
                        Text("Name: ${player.name}")
                    },
                    supportingContent = {
                        Text("Score: ${player.score}")
                    },
                    trailingContent = {
                        Button(onClick = {
                            val query = db.collection("players").whereEqualTo("playerId", player.playerId)

                            query.get().addOnSuccessListener { querySnapshot ->
                                for (documentSnapshot in querySnapshot) {
                                    documentSnapshot.reference.update("invitation", "Hello!")
                                }}



                        }) {
                            Text("Invite")
                        }
                    }
                )


            }
        }
    }
}





