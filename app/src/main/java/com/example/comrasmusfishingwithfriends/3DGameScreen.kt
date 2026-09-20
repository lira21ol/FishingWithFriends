package com.example.comrasmusfishingwithfriends

import android.util.Log
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
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
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberEnvironment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================
// DATA MODELS
// ============================================================

enum class WorldAssetType {
    BUILDING,
    VEGETATION,
    ROCK,
    DOCK,
    BOAT,
    TERRAIN
}

data class WorldAsset(
    val type: WorldAssetType,
    val modelPath: String,
    val position: Position,
    val rotation: Rotation = Rotation(0f, 0f, 0f),
    val scale: Float = 1f
)

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
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberCameraNode(engine)
    val view = rememberView(engine)
    val environment = rememberEnvironment(engine)
    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 250_000f 
    }

    // --------------------------------------------------------
    // STATE
    // --------------------------------------------------------
    var playerPosition by remember { mutableStateOf(Offset(0f, 0f)) }
    var playerRotation by remember { mutableFloatStateOf(0f) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    
    var playerNode by remember { mutableStateOf<ModelNode?>(null) }
    var rodNode by remember { mutableStateOf<ModelNode?>(null) }
    
    val worldNodes = remember { mutableStateListOf<ModelNode>() }
    val dockNodes = remember { mutableStateListOf<ModelNode>() }
    var waterNode by remember { mutableStateOf<RenderableNode?>(null) }

    var cameraYaw by remember { mutableFloatStateOf(0f) }
    var cameraPitch by remember { mutableFloatStateOf(45f) } 
    var cameraDistance by remember { mutableFloatStateOf(24f) }
    
    var showFishingButton by remember { mutableStateOf(false) }

    // --------------------------------------------------------
    // RECALIBRATED WORLD SCALE (DIAGNOSTIC MODE)
    // --------------------------------------------------------
    val islandRadius = 30f

    fun canWalkAt(x: Float, z: Float): Boolean {
        val dist = sqrt(x * x + z * z)
        
        // Main island
        if (dist <= islandRadius) return true
        
        // Docks (Recalibrated for scale 30)
        // North
        if (x in -5f..5f && z in -45f..-25f) return true
        // South
        if (x in -5f..5f && z in 25f..45f) return true
        // East
        if (z in -5f..5f && x in 25f..45f) return true
        // West
        if (z in -5f..5f && x in -45f..-25f) return true
        
        return false
    }

    // Initialize Camera
    LaunchedEffect(Unit) {
        cameraNode.position = Position(0f, 30f, 40f)
        cameraNode.lookAt(Position(0f, 0f, 0f))
    }

    // Load Player
    LaunchedEffect(characterModel) {
        playerNode = null
        modelLoader.loadModelInstanceAsync(characterModel) { instance ->
            instance?.let {
                playerNode = ModelNode(it, scaleToUnits = 4.0f).apply {
                    position = Position(0f, 0.5f, 0f) 
                }
            }
        }
    }

    // Load Rod
    LaunchedEffect(currentPlayer.currentRodId) {
        val rodType = EquipmentShop.availableRods.find { it.id == currentPlayer.currentRodId } ?: EquipmentShop.availableRods.first()
        modelLoader.loadModelInstanceAsync(rodType.modelPath) { instance ->
            instance?.let {
                rodNode = ModelNode(it, scaleToUnits = 2.5f).apply {
                    rotation = Rotation(0f, 0f, -20f)
                }
            }
        }
    }

    // Build World (DIAGNOSTIC & CLEAN)
    LaunchedEffect(Unit) {
        // 1. Create Opaque Ocean (Following Tip 3)
        val waterMaterial = materialLoader.createColorInstance(
            color = Color(0xFF1687A8),
            metallic = 0.0f,
            roughness = 0.4f,
            reflectance = 0.5f
        )
        waterNode = RenderableNode(engine).apply {
            setGeometry(Plane.Builder().size(Size(2000f, 2000f)).build(engine))
            setMaterialInstances(waterMaterial)
            position = Position(0f, -0.2f, 0f)
            isHittable = false
        }

        // 2. Define Level Layout (Recalibrated Scales - Following Tip 4 & 5)
        val assets = listOf(
            // TERRAIN (Following Tip 6: One terrain model to test scale)
            WorldAsset(WorldAssetType.TERRAIN, "models/island/platform_grass.glb", Position(0f, 0f, 0f), scale = 40f),
            WorldAsset(WorldAssetType.TERRAIN, "models/island/platform_beach.glb", Position(0f, -0.05f, 0f), Rotation(0f, 45f, 0f), scale = 55f),

            // HOUSES (Following Tip: scale ≈ 8)
            WorldAsset(WorldAssetType.BUILDING, "models/House.glb", Position(-10f, 0.2f, -10f), scale = 8f),
            WorldAsset(WorldAssetType.BUILDING, "models/House.glb", Position(10f, 0.2f, -8f), Rotation(0f, 45f, 0f), scale = 7f),
            
            // DOCKS (Following Tip: scale ≈ 10, connected to land)
            WorldAsset(WorldAssetType.DOCK, "models/Dock Long.glb", Position(0f, -0.05f, -28f), Rotation(0f, 180f, 0f), scale = 10f),
            WorldAsset(WorldAssetType.DOCK, "models/Dock Long.glb", Position(0f, -0.05f, 28f), Rotation(0f, 0f, 0f), scale = 10f),
            WorldAsset(WorldAssetType.DOCK, "models/Dock Long.glb", Position(28f, -0.05f, 0f), Rotation(0f, 90f, 0f), scale = 10f),
            WorldAsset(WorldAssetType.DOCK, "models/Dock Long.glb", Position(-28f, -0.05f, 0f), Rotation(0f, -90f, 0f), scale = 10f),

            // VEGETATION (Following Tip: scale ≈ 5)
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(-20f, 0f, -15f), scale = 5f),
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(20f, 0f, -15f), scale = 5f),
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(-15f, 0f, 20f), scale = 5f),
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(15f, 0f, 20f), scale = 5f),

            // ROCKS (Following Tip: scale ≈ 4)
            WorldAsset(WorldAssetType.ROCK, "models/island/rock_largeA.glb", Position(-25f, 0f, 5f), scale = 4f),
            WorldAsset(WorldAssetType.ROCK, "models/island/rock_largeB.glb", Position(25f, 0f, 5f), scale = 4f),

            // BOATS (Following Tip: Put them next to docks)
            WorldAsset(WorldAssetType.BOAT, "models/Boat.glb", Position(5f, -0.2f, -35f), Rotation(0f, 180f, 0f), scale = 5f),
            WorldAsset(WorldAssetType.BOAT, "models/Boat.glb", Position(35f, -0.2f, 5f), Rotation(0f, 90f, 0f), scale = 5f)
        )

        assets.forEach { asset ->
            modelLoader.loadModelInstanceAsync(asset.modelPath) { instance ->
                instance?.let {
                    val node = ModelNode(it, scaleToUnits = asset.scale).apply {
                        position = asset.position
                        rotation = asset.rotation
                    }
                    if (asset.type == WorldAssetType.DOCK) dockNodes.add(node) else worldNodes.add(node)
                }
            }
        }
    }

    // Movement Loop (Continuous)
    LaunchedEffect(Unit) {
        while (true) {
            val joystick = joystickOffset
            if (joystick != Offset.Zero) {
                val speed = 0.5f
                val yawRad = Math.toRadians(cameraYaw.toDouble())
                val forwardX = -sin(yawRad).toFloat()
                val forwardZ = -cos(yawRad).toFloat()
                val rightX = cos(yawRad).toFloat()
                val rightZ = -sin(yawRad).toFloat()

                val dx = (joystick.x * rightX + joystick.y * forwardX) * speed
                val dz = (joystick.x * rightZ + joystick.y * forwardZ) * speed
                
                val nextX = playerPosition.x + dx
                val nextZ = playerPosition.y + dz

                if (canWalkAt(nextX, nextZ)) {
                    playerPosition = Offset(nextX, nextZ)
                    playerRotation = Math.toDegrees(atan2(-dx.toDouble(), -dz.toDouble())).toFloat()
                }
            }
            delay(16)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF87CEEB))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    cameraYaw += dragAmount.x * 0.45f
                    cameraPitch = (cameraPitch - dragAmount.y * 0.35f).coerceIn(10f, 85f)
                }
            }
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            view = view,
            isOpaque = true, // Following Tip 2: Use solid background for diagnostic
            environment = environment,
            mainLightNode = mainLightNode,
            childNodes = listOfNotNull(playerNode, rodNode, waterNode) + worldNodes + dockNodes,
            onFrame = { frameTimeNanos ->
                val time = frameTimeNanos / 1_000_000_000f
                
                playerNode?.let { player ->
                    // Simplified grounding for diagnostic scene
                    val dist = sqrt(playerPosition.x * playerPosition.x + playerPosition.y * playerPosition.y)
                    val targetY = if (dist <= islandRadius) 0.2f else 0f
                    
                    player.position = Position(playerPosition.x, targetY, playerPosition.y)
                    player.rotation = Rotation(0f, playerRotation, 0f)
                    
                    rodNode?.let { rod ->
                        val rad = Math.toRadians(playerRotation.toDouble())
                        val offX = (cos(rad) * 0.4 + sin(rad) * 0.2).toFloat()
                        val offZ = (-sin(rad) * 0.4 + cos(rad) * 0.2).toFloat()
                        rod.position = Position(player.position.x + offX, player.position.y + 1.2f, player.position.z + offZ)
                        rod.rotation = Rotation(0f, playerRotation - 20f, 0f)
                    }

                    // Camera Follow (Following Tip 1: Corrected conventional 3rd person)
                    val yawRad = Math.toRadians(cameraYaw.toDouble())
                    val pitchRad = Math.toRadians(cameraPitch.toDouble())
                    val hDist = cameraDistance * cos(pitchRad)
                    val camX = player.position.x + (hDist * sin(yawRad)).toFloat()
                    val camZ = player.position.z + (hDist * cos(yawRad)).toFloat()
                    val camY = player.position.y + (cameraDistance * sin(pitchRad)).toFloat() + 2.0f

                    cameraNode.position = Position(camX, camY, camZ)
                    cameraNode.lookAt(Position(player.position.x, player.position.y + 1.5f, player.position.z))
                    
                    // Specific distance to dock centers
                    showFishingButton = dist > 24f && dist <= 40f
                }
            }
        )

        // Overlay UI
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) { Text("Exit") }

            if (showFishingButton) {
                Card(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 180.dp),
                    shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))) {
                    Button(onClick = { /* Nav to Fishing */ }, modifier = Modifier.padding(8.dp).height(50.dp).width(200.dp)) { 
                        Text("🎣 Start Fishing", style = MaterialTheme.typography.titleMedium) 
                    }
                }
            }

            Joystick(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 32.dp, start = 16.dp).size(150.dp),
                onJoystickMove = { joystickOffset = it })
        }
    }
}

@Composable
fun Joystick(modifier: Modifier = Modifier, onJoystickMove: (Offset) -> Unit) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape).pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragOffset = Offset.Zero },
                    onDragEnd = { dragOffset = Offset.Zero; onJoystickMove(Offset.Zero) },
                    onDragCancel = { dragOffset = Offset.Zero; onJoystickMove(Offset.Zero) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = dragOffset + dragAmount
                        val maxRadius = 100f
                        val distance = newOffset.getDistance()
                        dragOffset = if (distance > maxRadius) newOffset * (maxRadius / distance) else newOffset
                        onJoystickMove(Offset(dragOffset.x / maxRadius, dragOffset.y / maxRadius))
                    })
            }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))
        Box(modifier = Modifier.offset { IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt()) }.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)))
    }
}
