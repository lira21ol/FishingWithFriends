package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Ray as MathRay
import io.github.sceneview.Scene
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.RenderableNode
import io.github.sceneview.geometries.Plane
import io.github.sceneview.math.Size
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
import kotlin.math.sqrt

// ============================================================
// MAIN SCREEN
// ============================================================

@Composable
fun ThreeDGameScreen(currentPlayer: Player, navController: NavHostController) {
    var selectedCharacter by remember { mutableStateOf<String?>(null) }
    var isGameStarted by remember { mutableStateOf(false) }

    if (!isGameStarted) {
        CharacterSelectionScreen(
            onCharacterSelected = { character ->
                selectedCharacter = character
                isGameStarted = true
            },
            onBack = {
                navController.popBackStack()
            }
        )
    } else {
        selectedCharacter?.let { character ->
            FreeModeGameScreen(
                currentPlayer = currentPlayer,
                characterModel = character,
                onBack = {
                    isGameStarted = false
                }
            )
        }
    }
}

// ============================================================
// CHARACTER SELECTION
// ============================================================

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

    var selectedIndex by remember { mutableIntStateOf(0) }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var previewNode by remember { mutableStateOf<ModelNode?>(null) }

    LaunchedEffect(selectedIndex) {
        previewNode = null
        modelLoader.loadModelInstanceAsync(characters[selectedIndex]) { instance ->
            if (instance != null) {
                previewNode = ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.5f
                ).apply {
                    position = Position(0f, -1f, 0f)
                    rotation = Rotation(0f, 180f, 0f)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            childNodes = listOfNotNull(previewNode)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.65f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose your fisherman",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(characters) { index, path ->
                            val name = path.substringAfterLast("/").removeSuffix(".glb")
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(70.dp)
                                    .clickable { selectedIndex = index },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedIndex == index) Color(0xFF00BCD4) else Color(0xFF303840)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = name, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onCharacterSelected(characters[selectedIndex]) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "Start Fishing")
                    }

                    TextButton(onClick = onBack) {
                        Text(text = "Back", color = Color.LightGray)
                    }
                }
            }
        }
    }
}

// ============================================================
// FREE MODE
// ============================================================

@Composable
fun FreeModeGameScreen(
    currentPlayer: Player,
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

    // --------------------------------------------------------
    // PLAYER & ROD
    // --------------------------------------------------------
    var playerPosition by remember { mutableStateOf(Offset(0f, 0f)) }
    var playerRotation by remember { mutableFloatStateOf(0f) }
    var playerNode by remember { mutableStateOf<ModelNode?>(null) }
    var rodNode by remember { mutableStateOf<ModelNode?>(null) }

    // --------------------------------------------------------
    // WORLD
    // --------------------------------------------------------
    var islandNode by remember { mutableStateOf<ModelNode?>(null) }
    var waterNode by remember { mutableStateOf<RenderableNode?>(null) }
    val dockNodes = remember { mutableStateListOf<ModelNode>() }
    val decorationNodes = remember { mutableStateListOf<ModelNode>() }

    // --------------------------------------------------------
    // INPUT & CAMERA
    // --------------------------------------------------------
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    var cameraYaw by remember { mutableFloatStateOf(0f) }
    var cameraPitch by remember { mutableFloatStateOf(28f) }
    var cameraDistance by remember { mutableFloatStateOf(12f) }

    // --------------------------------------------------------
    // INTERACTION
    // --------------------------------------------------------
    var showFishingButton by remember { mutableStateOf(false) }

    // Helper to check if a node is part of another node's hierarchy
    fun isPartOf(hitNode: Node, targetNode: ModelNode?): Boolean {
        if (targetNode == null) return false
        var current: Node? = hitNode
        while (current != null) {
            if (current == targetNode) return true
            current = current.parent
        }
        return false
    }

    // Initialize Camera
    LaunchedEffect(Unit) {
        cameraNode.position = Position(0f, 8f, 14f)
        cameraNode.lookAt(Position(0f, 0f, 0f))
    }

    // Load Player
    LaunchedEffect(characterModel) {
        playerNode = null
        modelLoader.loadModelInstance(characterModel)?.let { instance ->
            playerNode = ModelNode(
                modelInstance = instance,
                scaleToUnits = 3.5f // Set realistic scale
            ).apply {
                position = Position(0f, 1f, 0f)
            }
        }
    }

    // Load Rod
    LaunchedEffect(currentPlayer.currentRodId) {
        val rodType = EquipmentShop.availableRods.find { it.id == currentPlayer.currentRodId } ?: EquipmentShop.availableRods.first()
        modelLoader.loadModelInstance(rodType.modelPath)?.let { instance ->
            rodNode = ModelNode(
                modelInstance = instance,
                scaleToUnits = 2.0f
            ).apply {
                position = Position(0f, 1.2f, 0f)
                rotation = Rotation(0f, 0f, -20f)
            }
        }
    }

    // Load World
    LaunchedEffect(Unit) {
        // Island
        modelLoader.loadModelInstance("models/island/Island.glb")?.let { instance ->
            islandNode = ModelNode(
                modelInstance = instance,
                scaleToUnits = 130f // Adjusted based on island model
            ).apply {
                position = Position(0f, 0f, 0f)
            }
        }

        // Water
        val waterMaterial = materialLoader.createColorInstance(
            color = Color(0xCC00AACC),
            metallic = 1.0f,
            roughness = 0.02f,
            reflectance = 1.0f
        )
        waterNode = RenderableNode(engine).apply {
            setGeometry(Plane.Builder().size(Size(4000f, 4000f)).build(engine))
            setMaterialInstances(waterMaterial)
            position = Position(0f, -0.8f, 0f)
            isHittable = false
        }

        // Docks
        val dockConfigs = listOf(
            Position(52f, -0.7f, 0f) to Rotation(0f, 90f, 0f),
            Position(-52f, -0.7f, 0f) to Rotation(0f, -90f, 0f),
            Position(0f, -0.7f, 52f) to Rotation(0f, 0f, 0f),
            Position(0f, -0.7f, -52f) to Rotation(0f, 180f, 0f)
        )
        dockConfigs.forEach { (pos, rot) ->
            modelLoader.loadModelInstance("models/Dock Long.glb")?.let { instance ->
                dockNodes.add(ModelNode(instance, scaleToUnits = 18f).apply {
                    position = pos
                    rotation = rot
                })
            }
        }

        // Decor
        val decorationPositions = listOf(
            Position(-25f, 0f, -20f), Position(20f, 0f, -25f),
            Position(-30f, 0f, 10f), Position(30f, 0f, 10f),
            Position(-10f, 0f, 30f), Position(10f, 0f, 28f)
        )
        decorationPositions.forEachIndexed { index, pos ->
            val path = if (index % 2 == 0) "models/Palm Tree.glb" else "models/Rock.glb"
            modelLoader.loadModelInstance(path)?.let { instance ->
                decorationNodes.add(ModelNode(instance, scaleToUnits = if (path.contains("Palm")) 10f else 5f).apply {
                    position = pos
                })
            }
        }
    }

    // Grounding & Interaction Logic
    LaunchedEffect(playerPosition, playerNode, islandNode, decorationNodes.size, dockNodes.size) {
        val island = islandNode ?: return@LaunchedEffect
        
        // Ground player
        playerNode?.let { node ->
            val ray = MathRay(Float3(node.position.x, 30f, node.position.z), Float3(0f, -1f, 0f))
            collisionSystem.hitTest(ray).firstOrNull { isPartOf(it.node, island) }?.let { hit ->
                node.position = Position(node.position.x, hit.worldPosition.y + 0.05f, node.position.z)
            }
        }

        // Ground decorations
        decorationNodes.forEach { decor ->
            val ray = MathRay(Float3(decor.position.x, 50f, decor.position.z), Float3(0f, -1f, 0f))
            collisionSystem.hitTest(ray).firstOrNull { isPartOf(it.node, island) }?.let { hit ->
                decor.position = Position(decor.position.x, hit.worldPosition.y, decor.position.z)
            }
        }

        // Interaction distance to docks
        playerNode?.let { node ->
            val closestDock = dockNodes.firstOrNull { dock ->
                val dx = node.position.x - dock.position.x
                val dz = node.position.z - dock.position.z
                sqrt(dx * dx + dz * dz) < 8f
            }
            showFishingButton = closestDock != null
        }
    }

    // Update Player & Rod Visuals
    LaunchedEffect(playerPosition, playerRotation, playerNode, rodNode) {
        playerNode?.let { node ->
            node.position = Position(playerPosition.x, node.position.y, playerPosition.y)
            node.rotation = Rotation(0f, playerRotation, 0f)
            
            rodNode?.let { rod ->
                val rad = Math.toRadians(playerRotation.toDouble())
                val offsetX = (cos(rad) * 0.4 + sin(rad) * 0.2).toFloat()
                val offsetZ = (-sin(rad) * 0.4 + cos(rad) * 0.2).toFloat()
                rod.position = Position(node.position.x + offsetX, node.position.y + 1.2f, node.position.z + offsetZ)
                rod.rotation = Rotation(0f, playerRotation - 20f, 0f)
            }
        }
    }

    // Movement Loop
    LaunchedEffect(joystickOffset, cameraYaw, playerNode, islandNode) {
        if (joystickOffset != Offset.Zero && playerNode != null && islandNode != null) {
            while (joystickOffset != Offset.Zero) {
                val speed = 0.35f
                val yawRad = Math.toRadians(cameraYaw.toDouble())
                val forwardX = -sin(yawRad).toFloat()
                val forwardZ = -cos(yawRad).toFloat()
                val rightX = cos(yawRad).toFloat()
                val rightZ = -sin(yawRad).toFloat()

                val dx = (joystickOffset.x * rightX + joystickOffset.y * forwardX) * speed
                val dz = (joystickOffset.x * rightZ + joystickOffset.y * forwardZ) * speed
                
                val nextX = playerPosition.x + dx
                val nextZ = playerPosition.y + dz

                // Collision: Check if landing spot exists on island or docks
                val ray = MathRay(Float3(nextX, 30f, nextZ), Float3(0f, -1f, 0f))
                val hit = collisionSystem.hitTest(ray).firstOrNull { hitResult ->
                    isPartOf(hitResult.node, islandNode) || dockNodes.any { isPartOf(hitResult.node, it) }
                }

                if (hit != null) {
                    playerPosition = Offset(nextX, nextZ)
                    playerRotation = Math.toDegrees(atan2(-dx.toDouble(), -dz.toDouble())).toFloat()
                }
                delay(16)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    cameraYaw += dragAmount.x * 0.4f
                    cameraPitch = (cameraPitch - dragAmount.y * 0.3f).coerceIn(10f, 65f)
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
            collisionSystem = collisionSystem,
            childNodes = listOfNotNull(playerNode, rodNode, islandNode, waterNode) + dockNodes + decorationNodes,
            onFrame = { frameTimeNanos ->
                val time = frameTimeNanos / 1_000_000_000f
                
                // Water animation
                waterNode?.let { water ->
                    val waveY = -0.8f + sin(time * 2f) * 0.15f + cos(time * 1.2f) * 0.05f
                    val shiftX = sin(time * 0.3f) * 0.5f
                    val shiftZ = cos(time * 0.3f) * 0.5f
                    water.position = Position(shiftX, waveY, shiftZ)
                }

                playerNode?.let { player ->
                    // Camera Orbit
                    val yawRad = Math.toRadians(cameraYaw.toDouble())
                    val pitchRad = Math.toRadians(cameraPitch.toDouble())
                    val horizDist = cameraDistance * cos(pitchRad)
                    val camX = player.position.x + (horizDist * sin(yawRad)).toFloat()
                    val camZ = player.position.z + (horizDist * cos(yawRad)).toFloat()
                    val camY = player.position.y + (cameraDistance * sin(pitchRad)).toFloat() + 3.0f

                    cameraNode.position = Position(camX, camY, camZ)
                    cameraNode.lookAt(Position(player.position.x, player.position.y + 1.2f, player.position.z))
                }
            }
        )

        // UI Overlay
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
                Text("Exit")
            }

            if (showFishingButton) {
                Button(
                    onClick = { /* Open Fishing Screen */ },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 180.dp).height(58.dp).width(200.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("🎣 Start Fishing", style = MaterialTheme.typography.titleMedium)
                }
            }

            Joystick(
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 32.dp, start = 16.dp).size(150.dp),
                onJoystickMove = { joystickOffset = it }
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
            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragOffset = Offset.Zero },
                    onDragEnd = { dragOffset = Offset.Zero; onJoystickMove(Offset.Zero) },
                    onDragCancel = { dragOffset = Offset.Zero; onJoystickMove(Offset.Zero) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = dragOffset + dragAmount
                        val maxRadius = 55f
                        val distance = newOffset.getDistance()
                        dragOffset = if (distance > maxRadius) newOffset * (maxRadius / distance) else newOffset
                        onJoystickMove(Offset(dragOffset.x / maxRadius, dragOffset.y / maxRadius))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))
        Box(modifier = Modifier.offset { IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt()) }.size(62.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
    }
}
