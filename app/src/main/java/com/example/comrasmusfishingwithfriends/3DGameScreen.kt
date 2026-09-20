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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

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
        intensity = 150_000f // Safer brightness to avoid bloom flickering
    }

    // --------------------------------------------------------
    // STATE
    // --------------------------------------------------------
    var playerPosition by remember { mutableStateOf(Offset(0f, 0f)) }
    var playerRotation by remember { mutableFloatStateOf(0f) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    
    var playerNode by remember { mutableStateOf<ModelNode?>(null) }
    var rodNode by remember { mutableStateOf<ModelNode?>(null) }
    
    val allWorldNodes = remember { mutableStateListOf<Node>() }
    var waterNode by remember { mutableStateOf<RenderableNode?>(null) }

    var cameraYaw by remember { mutableFloatStateOf(0f) }
    var cameraPitch by remember { mutableFloatStateOf(40f) } 
    var cameraDistance by remember { mutableFloatStateOf(32f) } 
    
    // Optimized: Only recompose when fishing zone status changes
    val showFishingButton by remember {
        derivedStateOf {
            val dist = sqrt(playerPosition.x * playerPosition.x + playerPosition.y * playerPosition.y)
            dist > 65f
        }
    }

    // Keyboard Zoom Support
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // --------------------------------------------------------
    // LOGIC
    // --------------------------------------------------------
    val beachRadius = 65f

    fun canWalkAt(x: Float, z: Float): Boolean {
        val distance = sqrt(x * x + z * z)
        if (distance <= beachRadius) return true
        
        // Docks (As tipped, generous zones)
        if (x in -15f..15f && z in -100f..-50f) return true // North
        if (x in -15f..15f && z in 50f..100f) return true  // South
        if (x in 50f..100f && z in -15f..15f) return true  // East
        if (x in -100f..-50f && z in -15f..15f) return true // West
        
        return false
    }

    // Load Player
    LaunchedEffect(characterModel) {
        playerNode = null
        modelLoader.loadModelInstanceAsync(characterModel) { instance ->
            instance?.let {
                playerNode = ModelNode(it, scaleToUnits = 4.8f).apply {
                    position = Position(0f, 1f, 0f) 
                }
            }
        }
    }

    // Load Rod
    LaunchedEffect(currentPlayer.currentRodId) {
        val rodType = EquipmentShop.availableRods.find { it.id == currentPlayer.currentRodId } ?: EquipmentShop.availableRods.first()
        modelLoader.loadModelInstanceAsync(rodType.modelPath) { instance ->
            instance?.let {
                rodNode = ModelNode(it, scaleToUnits = 3.2f).apply {
                    rotation = Rotation(0f, 0f, -20f)
                }
            }
        }
    }

    // Build World
    LaunchedEffect(Unit) {
        // High-Quality Materials
        val groundMat = materialLoader.createColorInstance(Color(0xFF2E7D32), roughness = 0.8f) // Grass
        val sandMat = materialLoader.createColorInstance(Color(0xFFF3E5AB), roughness = 0.9f) // Sand
        val waterMat = materialLoader.createColorInstance(Color(0xFF00ACC1), roughness = 0.2f) // Ocean

        // Terrain
        allWorldNodes.add(RenderableNode(engine).apply {
            setGeometry(Plane.Builder().size(Size(110f, 110f)).build(engine))
            setMaterialInstances(groundMat)
            position = Position(0f, 0.6f, 0f)
        })
        allWorldNodes.add(RenderableNode(engine).apply {
            setGeometry(Plane.Builder().size(Size(170f, 170f)).build(engine))
            setMaterialInstances(sandMat)
            position = Position(0f, 0.1f, 0f)
        })

        // Ocean
        waterNode = RenderableNode(engine).apply {
            setGeometry(Plane.Builder().size(Size(10000f, 10000f)).build(engine))
            setMaterialInstances(waterMat)
            position = Position(0f, -0.6f, 0f)
            isHittable = false
        }

        // Layout
        val assetsConfig = listOf(
            // Houses
            WorldAsset(WorldAssetType.BUILDING, "models/House.glb", Position(-18f, 0.6f, -18f), scale = 16f),
            WorldAsset(WorldAssetType.BUILDING, "models/House.glb", Position(22f, 0.6f, -15f), Rotation(0f, -45f, 0f), scale = 14f),
            WorldAsset(WorldAssetType.BUILDING, "models/House.glb", Position(0f, 0.6f, -48f), Rotation(0f, 180f, 0f), scale = 15f),
            WorldAsset(WorldAssetType.BUILDING, "models/House.glb", Position(35f, 0.6f, 20f), Rotation(0f, 90f, 0f), scale = 12f),
            
            // Docks
            WorldAsset(WorldAssetType.DOCK, "models/Dock Long.glb", Position(65f, 0f, 0f), Rotation(0f, 90f, 0f), scale = 35f),
            WorldAsset(WorldAssetType.DOCK, "models/Dock Long.glb", Position(-65f, 0f, 0f), Rotation(0f, -90f, 0f), scale = 35f),
            WorldAsset(WorldAssetType.DOCK, "models/Dock Long.glb", Position(0f, 0f, 65f), Rotation(0f, 0f, 0f), scale = 35f),
            WorldAsset(WorldAssetType.DOCK, "models/Dock Long.glb", Position(0f, 0f, -65f), Rotation(0f, 180f, 0f), scale = 35f),

            // Vegetation
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(-38f, 0.1f, -35f), scale = 18f),
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(38f, 0.1f, -35f), scale = 17f),
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(-52f, 0.1f, 38f), scale = 20f),
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(48f, 0.1f, 42f), scale = 18f),
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(-25f, 0.6f, 45f), scale = 19f),
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(28f, 0.6f, 45f), scale = 16f),
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(0f, 0.6f, 25f), scale = 15f),
            WorldAsset(WorldAssetType.VEGETATION, "models/Palm Tree.glb", Position(-10f, 0.6f, -30f), scale = 14f),

            // Rocks
            WorldAsset(WorldAssetType.ROCK, "models/island/rock_largeA.glb", Position(-58f, 0.1f, 15f), scale = 12f),
            WorldAsset(WorldAssetType.ROCK, "models/island/rock_largeB.glb", Position(58f, 0.1f, 25f), scale = 13f),
            WorldAsset(WorldAssetType.ROCK, "models/island/rock_largeA.glb", Position(22f, 0.1f, 62f), scale = 11f),
            WorldAsset(WorldAssetType.ROCK, "models/island/rock_largeB.glb", Position(-22f, 0.1f, -62f), scale = 10f),
            
            // Boats
            WorldAsset(WorldAssetType.BOAT, "models/Boat.glb", Position(100f, -0.6f, 20f), Rotation(0f, 45f, 0f), scale = 14f),
            WorldAsset(WorldAssetType.BOAT, "models/Boat.glb", Position(-20f, -0.6f, -100f), Rotation(0f, 220f, 0f), scale = 14f)
        )

        for (asset in assetsConfig) {
            modelLoader.loadModelInstanceAsync(asset.modelPath) { instance ->
                instance?.let {
                    val node = ModelNode(it, scaleToUnits = asset.scale).apply {
                        position = asset.position
                        rotation = asset.rotation
                    }
                    allWorldNodes.add(node)
                }
            }
            delay(16)
        }
    }

    // Movement Loop
    LaunchedEffect(Unit) {
        while (true) {
            val joystick = joystickOffset
            if (joystick != Offset.Zero) {
                val speed = 0.8f 
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF73CFF2))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Z -> { cameraDistance = (cameraDistance + 2f).coerceAtMost(150f); true }
                        Key.X -> { cameraDistance = (cameraDistance - 2f).coerceAtLeast(10f); true }
                        else -> false
                    }
                } else false
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    cameraYaw += dragAmount.x * 0.5f
                    cameraPitch = (cameraPitch - dragAmount.y * 0.45f).coerceIn(10f, 85f)
                }
            }
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            view = view,
            isOpaque = false, // Back to transparent to use Box background stably
            environment = environment,
            mainLightNode = mainLightNode,
            childNodes = listOfNotNull(playerNode, rodNode, waterNode) + allWorldNodes,
            onFrame = { frameTimeNanos ->
                val time = frameTimeNanos / 1_000_000_000f
                waterNode?.let { it.position = Position(0f, -0.6f + sin(time * 2.5f) * 0.04f, 0f) }

                playerNode?.let { player ->
                    val dist = sqrt(playerPosition.x * playerPosition.x + playerPosition.y * playerPosition.y)
                    val targetY = when {
                        dist <= 52f -> 0.65f 
                        dist <= 68f -> 0.15f 
                        else -> 0.15f        
                    }
                    player.position = Position(playerPosition.x, targetY, playerPosition.y)
                    player.rotation = Rotation(0f, playerRotation, 0f)
                    
                    rodNode?.let { rod ->
                        val rad = Math.toRadians(playerRotation.toDouble())
                        val offX = (cos(rad) * 0.4 + sin(rad) * 0.2).toFloat()
                        val offZ = (-sin(rad) * 0.4 + cos(rad) * 0.2).toFloat()
                        rod.position = Position(player.position.x + offX, player.position.y + 1.3f, player.position.z + offZ)
                        rod.rotation = Rotation(0f, playerRotation - 20f, 0f)
                    }

                    val yawRad = Math.toRadians(cameraYaw.toDouble())
                    val pitchRad = Math.toRadians(cameraPitch.toDouble())
                    val hDist = cameraDistance * cos(pitchRad)
                    val camX = player.position.x + (hDist * sin(yawRad)).toFloat()
                    val camZ = player.position.z + (hDist * cos(yawRad)).toFloat()
                    val camY = player.position.y + (cameraDistance * sin(pitchRad)).toFloat() + 5.5f

                    cameraNode.position = Position(camX, camY, camZ)
                    cameraNode.lookAt(Position(player.position.x, player.position.y + 2.2f, player.position.z))
                }
            }
        )

        // Overlay UI
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) { Text("Exit") }

            Card(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "🔍 Zoom: [X] In / [Z] Out",
                    color = Color.White,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }

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
