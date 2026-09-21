package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberEnvironment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun ThreeDGameScreen(currentPlayer: Player, navController: NavHostController) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine)
    val view = rememberView(engine).apply { isPostProcessingEnabled = true }

    val environment = rememberEnvironment(engine)
    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 100_000f
        isShadowCaster = true
        rotation = Rotation(-45f, 45f, 0f)
    }

    // 3D Nodes
    var oceanNode by remember { mutableStateOf<ModelNode?>(null) }
    var playerNode by remember { mutableStateOf<ModelNode?>(null) }
    var rodNode by remember { mutableStateOf<ModelNode?>(null) }
    var bobberNode by remember { mutableStateOf<ModelNode?>(null) }

    // Speltillstånd (Game States)
    var isFishing by remember { mutableStateOf(false) }
    var hasBite by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Tryck på knappen för att kasta ut spöt i det nya havet!") }
    var scoreText by remember { mutableStateOf("Fångade fiskar: ${currentPlayer.fishCatalog.caughtFish.values.sum()}") }

    // Animeringstillstånd för onFrame
    var lastBiteTime by remember { mutableLongStateOf(0L) }
    
    // Fast position för gubben vid vattenbrynet
    val playerX = 0f
    val playerY = -0.5f 
    val playerZ = -2.0f
    val playerRotation = 180f 

    val coroutineScope = rememberCoroutineScope()

    // 1. ASYNKRON LADDNING AV NYA MODELLER
    LaunchedEffect(Unit) {
        // Ladda det nya fina 3D-havet
        modelLoader.loadModelInstanceAsync("ocean/animated_ocean_scene_tutorial_example_1.glb") { instance ->
            instance?.let {
                oceanNode = ModelNode(it, scaleToUnits = 50f).apply {
                    position = Position(0f, -1.0f, 0f)
                }
                oceanNode?.playAnimation(0, loop = true)
            }
        }

        // Ladda fiskargubben
        modelLoader.loadModelInstanceAsync("character/fisherman_fishing.glb") { instance ->
            instance?.let {
                playerNode = ModelNode(it, scaleToUnits = 1.2f).apply {
                    position = Position(playerX, playerY, playerZ)
                    rotation = Rotation(0f, playerRotation, 0f)
                }
                playerNode?.playAnimation(0, loop = true)
            }
        }

        // Ladda 3D Fishing Rod
        modelLoader.loadModelInstanceAsync("models/Fishing Rod.glb") { instance ->
            instance?.let {
                rodNode = ModelNode(it, scaleToUnits = 0.5f).apply {
                    // Positionera spöt i närheten av karaktärens händer
                    position = Position(playerX + 0.2f, playerY + 0.8f, playerZ + 0.3f)
                    rotation = Rotation(45f, 180f, 0f)
                }
            }
        }

        // Ladda flöte / Lure
        modelLoader.loadModelInstanceAsync("models/Lure.glb") { instance ->
            instance?.let {
                bobberNode = ModelNode(it, scaleToUnits = 0.1f).apply {
                    position = Position(0f, -20f, 0f) 
                }
            }
        }
    }

    // 2. FISKE-LOGIK MED ALLA FISKAR FRÅN ALLA SKÄRMAR
    fun startBiteTimer() {
        coroutineScope.launch {
            statusText = "Kastar ut linan... 🎣"
            // Starta utkast-animation på spöt och gubben
            playerNode?.playAnimation(1, loop = false)
            delay(1000)
            
            statusText = "Väntar på napp i vattnet..."
            playerNode?.playAnimation(0, loop = true) // Väntar-animation

            val waitTime = (4000..8000).random().toLong()
            delay(waitTime)

            if (isFishing) {
                hasBite = true
                statusText = "NAPP! DRA UPP FISKEN SNABBT! 🎣🐟"
                lastBiteTime = System.currentTimeMillis()

                // Vänta max 2.5 sekunder på att spelaren ska reagera
                delay(2500)

                if (hasBite && isFishing) {
                    statusText = "Fisken slet sig... Du var för långsam! 😢"
                    isFishing = false
                    hasBite = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            environment = environment,
            mainLightNode = mainLightNode,
            view = view,
            isOpaque = true,
            childNodes = listOfNotNull(oceanNode, playerNode, rodNode, bobberNode, mainLightNode),
            onFrame = { frameTimeNanos ->
                val timeSeconds = frameTimeNanos / 1_000_000_000f

                // Kameravinkel för att se både gubben och havet perfekt
                cameraNode.position = Position(playerX + 1.5f, playerY + 1.8f, playerZ + 3.0f)
                cameraNode.lookAt(Position(playerX, playerY + 0.6f, playerZ - 4f))

                bobberNode?.let { bobber ->
                    if (isFishing) {
                        var currentBobberY = -0.4f
                        if (hasBite) {
                            // Kraftig napp-animation i vattnet
                            currentBobberY = -0.5f + (sin(timeSeconds * 25f) * 0.08f)
                        } else {
                            // Mjukt guppande i vågorna
                            currentBobberY = -0.4f + (sin(timeSeconds * 3f) * 0.02f)
                        }
                        bobber.position = Position(playerX, currentBobberY, playerZ - 4.0f)
                    } else {
                        bobber.position = Position(0f, -20f, 0f)
                    }
                }
            }
        )

        // UI Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { navController.popBackStack() }) {
                Text("Meny")
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
            ) {
                Text(text = scoreText, color = Color.White, modifier = Modifier.padding(12.dp))
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
        ) {
            Text(text = statusText, color = Color.White, modifier = Modifier.padding(16.dp))
        }

        Button(
            onClick = {
                if (!isFishing) {
                    isFishing = true
                    startBiteTimer()
                } else {
                    if (hasBite) {
                        // Slå samman alla fiskpooler från appen så man kan få ALLA olika sorters fiskar
                        val allAvailableFishPool = listOf(
                            "Trout", "Bass", "Catfish", "Carp", "Pike", "Goldfish", "Eel",
                            "Tuna", "Swordfish", "Shark", "Dolphinfish", "Marlin", "Octopus", "Giant Squid",
                            "Hammerhead Shark", "Dolphin", "Manta Ray", "Crocodile",
                            "Salmon", "Rainbow Trout", "Sturgeon", "Arctic Char", "Grayling", "Brown Trout", "Perch"
                        )
                        val chosenFishType = allAvailableFishPool.random()
                        val fishPoints = getFishPoints(chosenFishType).let { if (it == 1) getRiverFishPoints(chosenFishType) else it }
                        
                        // Bestäm sällsynthet baserat på poäng
                        val rarity = when {
                            fishPoints >= 40 -> FishRarity.LEGENDARY
                            fishPoints >= 30 -> FishRarity.EPIC
                            fishPoints >= 20 -> FishRarity.RARE
                            fishPoints >= 10 -> FishRarity.UNCOMMON
                            else -> FishRarity.COMMON
                        }

                        val caughtFish = Fish(
                            type = chosenFishType,
                            rarity = rarity,
                            points = fishPoints
                        )

                        // Uppdatera spelarens poäng och lägg till i framstegskatalogen (Firebase-sparande ingår automatiskt)
                        currentPlayer.score += caughtFish.points
                        updatePlayerProgress(currentPlayer, caughtFish)

                        statusText = "🎉 Snyggt drag! Du fångade en ${caughtFish.type} (+${caughtFish.points} poäng)!"
                        scoreText = "Fångade fiskar: ${currentPlayer.fishCatalog.caughtFish.values.sum()}"
                    } else {
                        statusText = "Du drog in spöt för tidigt och skrämde fisken... 💨"
                    }
                    isFishing = false
                    hasBite = false
                    playerNode?.playAnimation(0, loop = true)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .size(100.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasBite) Color.Red else if (isFishing) Color(0xFFFFA500) else Color(0xFF4CAF50)
            )
        ) {
            Text(
                text = if (hasBite) "DRA!" else if (isFishing) "Veva in" else "Fiska",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
