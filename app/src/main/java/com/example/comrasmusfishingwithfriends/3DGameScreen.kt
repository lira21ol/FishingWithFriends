package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode

@Composable
fun ThreeDGameScreen(navController: NavHostController) {
    var selectedCharacter by remember { mutableStateOf<String?>(null) }
    var isGameStarted by remember { mutableStateOf(value = false) }

    if (!isGameStarted) {
        CharacterSelectionScreen(
            onCharacterSelected = { character ->
                selectedCharacter = character
                isGameStarted = true
            },
            onBack = {
                navController.popBackStack()
            },
        )
    } else {
        selectedCharacter?.let { character ->
            FreeModeGameScreen(
                characterModel = character,
                onBack = { isGameStarted = false }
            )
        }
    }
}

@Composable
fun CharacterSelectionScreen(
    onCharacterSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val characters = listOf(
        "models/Adventurer.glb",
        "models/Astronaut.glb",
        "models/Beach Character.glb"
    )
    var currentPreviewIdx by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // 3D Preview
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val modelNode = remember(currentPreviewIdx) {
            ModelNode(
                modelInstance = modelLoader.createModelInstance(characters[currentPreviewIdx]),
                scaleToUnits = 1.0f
            )
        }

        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            childNodes = listOf(modelNode)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Välj din karaktär",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            LazyRow(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(characters.indices.toList()) { index ->
                    Card(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable { currentPreviewIdx = index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentPreviewIdx == index) Color.Cyan else Color.DarkGray
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = characters[index].split("/").last().replace(".glb", ""),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onCharacterSelected(characters[currentPreviewIdx]) },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
            ) {
                Text("Välj Karaktär")
            }

            TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                Text("Tillbaka", color = Color.Gray)
            }
        }
    }
}

@Composable
fun FreeModeGameScreen(
    characterModel: String,
    onBack: () -> Unit
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    // Player Node
    val playerNode = rememberNode {
        ModelNode(
            modelInstance = modelLoader.createModelInstance(characterModel),
            scaleToUnits = 1.0f
        )
    }

    // Environment Nodes
    val environmentNodes = remember {
        listOf(
            "models/House.glb" to Offset3D(x = 5f, z = 5f),
            "models/Trees.glb" to Offset3D(x = -5f, z = -5f),
            "models/Dock.glb" to Offset3D(x = 0f, z = 10f),
            "models/Rock.glb" to Offset3D(x = 3f, z = -3f)
        ).map { (path, pos) ->
            ModelNode(
                modelInstance = modelLoader.createModelInstance(path),
                scaleToUnits = 2.0f
            ).apply {
                position = io.github.sceneview.math.Position(pos.x, 0f, pos.z)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            childNodes = listOf(playerNode) + environmentNodes
        )

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onBack) {
                    Text("Avsluta")
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Free Mode - Utforska ön",
                        color = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            // Movement controls (Placeholder for Joystick)
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(75.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Joystick", color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

data class Offset3D(val x: Float, val z: Float)
