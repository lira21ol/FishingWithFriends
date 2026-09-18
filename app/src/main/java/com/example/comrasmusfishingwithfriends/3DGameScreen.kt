package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    val cameraNode = rememberCameraNode(engine)

    var playerPos by remember { mutableStateOf(Offset(0f, 0f)) }
    var playerRotation by remember { mutableStateOf(0f) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }

    // Player Node
    val playerNode = rememberNode {
        ModelNode(
            modelInstance = modelLoader.createModelInstance(characterModel),
            scaleToUnits = 0.3f
        ).apply {
            position = Position(0f, 0f, 0f)
        }
    }

    // Update player node position and rotation
    LaunchedEffect(playerPos, playerRotation) {
        playerNode.position = Position(playerPos.x, 0f, playerPos.y)
        playerNode.rotation = Rotation(0f, playerRotation, 0f)
    }

    // Movement loop
    LaunchedEffect(joystickOffset) {
        if (joystickOffset != Offset.Zero) {
            while (true) {
                val speed = 0.2f
                val dx = joystickOffset.x * speed
                val dy = joystickOffset.y * speed
                
                playerPos = Offset(playerPos.x + dx, playerPos.y + dy)
                playerRotation = Math.toDegrees(atan2(-dx.toDouble(), -dy.toDouble())).toFloat()
                
                delay(16)
                if (joystickOffset == Offset.Zero) break
            }
        }
    }

    // Environment Nodes
    val environmentNodes = remember {
        val nodes = mutableListOf<ModelNode>()
        
        // Ground - Grass Island
        nodes.add(ModelNode(
            modelInstance = modelLoader.createModelInstance("models/Grass.glb"),
            scaleToUnits = 50f
        ).apply {
            position = Position(0f, -0.02f, 0f)
        })

        // 4 Docks
        val dockDist = 22f
        nodes.add(ModelNode(modelLoader.createModelInstance("models/Dock Long.glb"), scaleToUnits = 3f).apply { 
            position = Position(0f, 0f, dockDist) 
        })
        nodes.add(ModelNode(modelLoader.createModelInstance("models/Dock Long.glb"), scaleToUnits = 3f).apply { 
            position = Position(0f, 0f, -dockDist)
            rotation = Rotation(0f, 180f, 0f)
        })
        nodes.add(ModelNode(modelLoader.createModelInstance("models/Dock Long.glb"), scaleToUnits = 3f).apply { 
            position = Position(dockDist, 0f, 0f)
            rotation = Rotation(0f, 90f, 0f)
        })
        nodes.add(ModelNode(modelLoader.createModelInstance("models/Dock Long.glb"), scaleToUnits = 3f).apply { 
            position = Position(-dockDist, 0f, 0f)
            rotation = Rotation(0f, -90f, 0f)
        })

        // Paths
        for (i in 1..6) {
            nodes.add(ModelNode(modelLoader.createModelInstance("models/Rock Path Round Wide.glb"), scaleToUnits = 2f).apply {
                position = Position(0f, 0.01f, i * 3.5f)
            })
            nodes.add(ModelNode(modelLoader.createModelInstance("models/Rock Path Round Wide.glb"), scaleToUnits = 2f).apply {
                position = Position(0f, 0.01f, -i * 3.5f)
            })
            nodes.add(ModelNode(modelLoader.createModelInstance("models/Rock Path Round Wide.glb"), scaleToUnits = 2f).apply {
                position = Position(i * 3.5f, 0.01f, 0f)
                rotation = Rotation(0f, 90f, 0f)
            })
            nodes.add(ModelNode(modelLoader.createModelInstance("models/Rock Path Round Wide.glb"), scaleToUnits = 2f).apply {
                position = Position(-i * 3.5f, 0.01f, 0f)
                rotation = Rotation(0f, 90f, 0f)
            })
        }

        // Beach decoration
        for (angle in 0 until 360 step 45) {
            val rad = Math.toRadians(angle.toDouble())
            val x = (cos(rad) * 20f).toFloat()
            val z = (sin(rad) * 20f).toFloat()
            nodes.add(ModelNode(modelLoader.createModelInstance("models/Palm Tree.glb"), scaleToUnits = 4f).apply {
                position = Position(x, 0f, z)
            })
            nodes.add(ModelNode(modelLoader.createModelInstance("models/Rocks.glb"), scaleToUnits = 3f).apply {
                position = Position(x * 1.1f, 0f, z * 1.1f)
            })
        }

        // Ship and Treasure
        nodes.add(ModelNode(modelLoader.createModelInstance("models/Ship.glb"), scaleToUnits = 10f).apply { 
            position = Position(30f, -1.5f, 30f) 
            rotation = Rotation(0f, 45f, 0f)
        })
        nodes.add(ModelNode(modelLoader.createModelInstance("models/Chest Gold.glb"), scaleToUnits = 2f).apply { 
            position = Position(5f, 0f, 5f) 
        })
        
        nodes
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)) // Sky Blue
    ) {
        // Sea layer (simulated with a large colored box or just background)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0077BE).copy(alpha = 0.5f)) // Sea Blue transparent overlay
        )

        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            childNodes = listOf(playerNode) + environmentNodes,
            onFrame = { _ ->
                // Camera follow
                cameraNode.position = Position(
                    playerNode.position.x,
                    playerNode.position.y + 7f,
                    playerNode.position.z + 10f
                )
                cameraNode.lookAt(playerNode.position)
            }
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
            
            Joystick(
                modifier = Modifier
                    .padding(bottom = 64.dp, start = 32.dp)
                    .size(150.dp),
                onJoystickMove = { offset ->
                    joystickOffset = offset
                }
            )
        }
    }
}

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    onJoystickMove: (Offset) -> Unit
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = modifier
            .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        dragOffset = Offset.Zero
                        onJoystickMove(Offset.Zero)
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                        onJoystickMove(Offset.Zero)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = dragOffset + dragAmount
                        val distance = newOffset.getDistance()
                        val maxRadius = 150f // Px approximation
                        
                        dragOffset = if (distance > maxRadius) {
                            newOffset * (maxRadius / distance)
                        } else {
                            newOffset
                        }
                        
                        // Normalize offset to -1..1
                        onJoystickMove(Offset(dragOffset.x / maxRadius, dragOffset.y / maxRadius))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Joystick handle
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        dragOffset.x.roundToInt(),
                        dragOffset.y.roundToInt()
                    )
                }
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.7f))
        )
    }
}


