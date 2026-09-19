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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.filament.Texture
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Ray as MathRay
import io.github.sceneview.Scene
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.geometries.Plane
import io.github.sceneview.math.Size
import io.github.sceneview.node.RenderableNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberView
import io.github.sceneview.texture.ImageTexture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        var modelNode by remember(currentPreviewIdx) { mutableStateOf<ModelNode?>(null) }

        LaunchedEffect(currentPreviewIdx) {
            modelLoader.loadModelInstanceAsync(characters[currentPreviewIdx]) { instance ->
                modelNode = instance?.let {
                    ModelNode(
                        modelInstance = it,
                        scaleToUnits = 1.0f
                    )
                }
            }
        }

        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            childNodes = listOfNotNull(modelNode)
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
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberCameraNode(engine)
    val view = rememberView(engine)
    val collisionSystem = remember(view) { CollisionSystem(view) }

    var playerPos by remember { mutableStateOf(Offset(0f, 0f)) }
    var playerRotation by remember { mutableStateOf(0f) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    
    // Camera rotation (Free camera)
    var cameraYaw by remember { mutableStateOf(0f) }
    var cameraPitch by remember { mutableStateOf(20f) }

    var playerNode by remember { mutableStateOf<ModelNode?>(null) }
    var islandNode by remember { mutableStateOf<ModelNode?>(null) }
    var dockNodes = remember { mutableStateListOf<ModelNode>() }
    var waterNode by remember { mutableStateOf<RenderableNode?>(null) }
    val decorNodes = remember { mutableStateListOf<ModelNode>() }

    LaunchedEffect(characterModel) {
        modelLoader.loadModelInstanceAsync(characterModel) { instance ->
            playerNode = instance?.let {
                ModelNode(
                    modelInstance = it,
                    scaleToUnits = 0.3f
                ).apply {
                    position = Position(0f, 0f, 0f)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        modelLoader.loadModelInstanceAsync("models/island/Island.glb") { instance ->
            islandNode = instance?.let {
                ModelNode(
                    modelInstance = it,
                    scaleToUnits = 50f
                ).apply {
                    position = Position(0f, -0.5f, 0f)
                }
            }
        }

        // Add multiple docks around the island
        val dockConfigs = listOf(
            Position(20f, -0.2f, 0f) to Rotation(0f, 90f, 0f),
            Position(-20f, -0.2f, 5f) to Rotation(0f, -90f, 0f),
            Position(0f, -0.2f, 20f) to Rotation(0f, 0f, 0f)
        )

        dockConfigs.forEach { (pos, rot) ->
            modelLoader.loadModelInstanceAsync("models/Dock Long.glb") { instance ->
                instance?.let {
                    dockNodes.add(ModelNode(it, scaleToUnits = 5f).apply {
                        position = pos
                        rotation = rot
                    })
                }
            }
        }

        // Create water plane
        val waterMaterial = materialLoader.createColorInstance(
            color = Color(0x880077BE), // Semi-transparent blue
            metallic = 0.9f,
            roughness = 0.1f,
            reflectance = 0.8f
        )
        waterNode = RenderableNode(engine).apply {
            setGeometry(Plane.Builder()
                .size(Size(1000f, 1000f)) // Even larger water
                .build(engine))
            setMaterialInstances(waterMaterial)
            position = Position(0f, -1.0f, 0f)
            isHittable = false
        }

        // Add more decor
        val decors = listOf(
            "models/Palm Tree.glb" to Position(-5f, 0f, -5f),
            "models/Palm Tree.glb" to Position(5f, 0f, 8f),
            "models/Palm Tree.glb" to Position(-15f, 0f, 15f),
            "models/Rock.glb" to Position(-10f, -0.2f, 3f),
            "models/Rock.glb" to Position(15f, -0.2f, -8f),
            "models/Rock.glb" to Position(0f, -0.2f, -20f)
        )

        decors.forEach { (model, pos) ->
            modelLoader.loadModelInstanceAsync(model) { instance ->
                instance?.let {
                    decorNodes.add(ModelNode(it, scaleToUnits = if (model.contains("Rock")) 2f else 4f).apply {
                        position = pos
                    })
                }
            }
        }
    }

    // Apply textures to island manually with fallback
    LaunchedEffect(islandNode) {
        islandNode?.let { island ->
            val textures = mapOf(
                "Ground" to "models/island/Ground.png",
                "Palms" to "models/island/Palms.png",
                "Tents" to "models/island/Tents.png"
            )

            textures.forEach { (nodeName, assetPath) ->
                launch {
                    try {
                        val texture = ImageTexture.Builder()
                            .bitmap(context.assets, assetPath)
                            .build(engine)
                        
                        val materialInstance = materialLoader.createTextureInstance(texture)
                        
                        // If names don't match, apply Ground texture to everything as a fallback
                        val targetNodes = island.renderableNodes.filter { 
                            it.name?.contains(nodeName, ignoreCase = true) == true 
                        }
                        
                        if (targetNodes.isEmpty() && nodeName == "Ground") {
                            island.renderableNodes.forEach { it.setMaterialInstances(materialInstance) }
                        } else {
                            targetNodes.forEach { it.setMaterialInstances(materialInstance) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Update player node position and rotation
    LaunchedEffect(playerPos, playerRotation, playerNode) {
        playerNode?.let { node ->
            node.position = Position(playerPos.x, node.position.y, playerPos.y)
            node.rotation = Rotation(0f, playerRotation, 0f)
        }
    }

    // Movement loop
    LaunchedEffect(joystickOffset, cameraYaw, playerNode, islandNode) {
        if (joystickOffset != Offset.Zero && playerNode != null) {
            while (true) {
                val speed = 0.3f
                val walkableNodes = listOfNotNull(islandNode) + dockNodes
                
                // Calculate movement direction relative to camera yaw
                val yawRad = Math.toRadians(cameraYaw.toDouble())
                val forwardX = -sin(yawRad).toFloat()
                val forwardZ = -cos(yawRad).toFloat()
                val rightX = cos(yawRad).toFloat()
                val rightZ = -sin(yawRad).toFloat()

                val dx = (joystickOffset.x * rightX + joystickOffset.y * forwardX) * speed
                val dz = (joystickOffset.x * rightZ + joystickOffset.y * forwardZ) * speed
                
                val nextX = playerPos.x + dx
                val nextZ = playerPos.y + dz

                // Boundary check: Raycast at next position
                val ray = MathRay(
                    Float3(nextX, 20f, nextZ),
                    Float3(0f, -1f, 0f)
                )
                val hit = collisionSystem.hitTest(ray).firstOrNull { 
                    walkableNodes.contains(it.node)
                }

                if (hit != null) {
                    playerPos = Offset(nextX, nextZ)
                    // Update rotation to face movement direction
                    playerRotation = Math.toDegrees(atan2(-dx.toDouble(), -dz.toDouble())).toFloat()
                }
                
                delay(16)
                if (joystickOffset == Offset.Zero) break
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)) // Sky Blue
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Rotate camera
                    cameraYaw += dragAmount.x * 0.5f
                    cameraPitch = (cameraPitch - dragAmount.y * 0.5f).coerceIn(5f, 80f)
                }
            }
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            view = view,
            isOpaque = false,
            childNodes = listOfNotNull(playerNode, islandNode, waterNode) + dockNodes + decorNodes,
            collisionSystem = collisionSystem,
            onFrame = { frameTimeNanos ->
                // Water animation
                waterNode?.let { water ->
                    val time = frameTimeNanos / 1_000_000_000f
                    water.position = Position(0f, -1.0f + sin(time * 2f) * 0.05f, 0f)
                }

                playerNode?.let { node ->
                    // Grounding logic
                    val walkableNodes = listOfNotNull(islandNode) + dockNodes
                    // Raycast down from above the player to find the surface height
                    val ray = MathRay(
                        Float3(node.position.x, 20f, node.position.z),
                        Float3(0f, -1f, 0f)
                    )
                    val hitResult = collisionSystem.hitTest(ray)
                    
                    hitResult.firstOrNull { walkableNodes.contains(it.node) }?.let { hit ->
                        node.position = Position(node.position.x, hit.worldPosition.y, node.position.z)
                    }

                    // Camera follow / Orbit
                    val distance = 8f // Even further for better view
                    val yawRad = Math.toRadians(cameraYaw.toDouble())
                    val pitchRad = Math.toRadians(cameraPitch.toDouble())
                    
                    val camX = node.position.x + (distance * sin(yawRad) * cos(pitchRad)).toFloat()
                    val camZ = node.position.z + (distance * cos(yawRad) * cos(pitchRad)).toFloat()
                    val camY = node.position.y + (distance * sin(pitchRad)).toFloat() + 2f // Slightly higher camera

                    cameraNode.position = Position(camX, camY, camZ)
                    cameraNode.lookAt(Position(node.position.x, node.position.y + 1f, node.position.z))
                }
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
                        text = "Free Mode - Svep för att rotera kameran",
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


