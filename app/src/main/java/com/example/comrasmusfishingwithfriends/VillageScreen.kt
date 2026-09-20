package com.example.comrasmusfishingwithfriends

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VillageScreen(currentPlayer: Player, navController: NavHostController) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine)
    
    var villageNode by remember { mutableStateOf<ModelNode?>(null) }

    // Camera movement state
    var cameraPosition by remember { mutableStateOf(Position(0f, 5f, 20f)) }
    var cameraYaw by remember { mutableFloatStateOf(0f) }
    var cameraPitch by remember { mutableFloatStateOf(-15f) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        modelLoader.loadModelInstanceAsync("fishingiland/free_demo_of_low_poly_pirates_village_life_pack.glb") { instance ->
            instance?.let {
                villageNode = ModelNode(it, scaleToUnits = 50f).apply {
                    position = Position(0f, 0f, 0f)
                }
            }
        }
    }

    // Movement loop
    LaunchedEffect(Unit) {
        while (true) {
            val joystick = joystickOffset
            if (joystick != Offset.Zero) {
                val speed = 0.3f
                val yawRad = Math.toRadians(cameraYaw.toDouble())
                
                val dx = (joystick.x * cos(yawRad) + joystick.y * sin(yawRad)).toFloat() * speed
                val dz = (joystick.x * -sin(yawRad) + joystick.y * cos(yawRad)).toFloat() * speed
                
                cameraPosition = Position(cameraPosition.x + dx, cameraPosition.y, cameraPosition.z + dz)
            }
            delay(16)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF87CEEB))
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                cameraYaw -= dragAmount.x * 0.2f
                cameraPitch = (cameraPitch - dragAmount.y * 0.2f).coerceIn(-80f, 80f)
            }
        }
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            childNodes = listOfNotNull(villageNode),
            onFrame = {
                cameraNode.position = cameraPosition
                cameraNode.rotation = Rotation(cameraPitch, cameraYaw, 0f)
            }
        )

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
        ) {
            Text("Back")
        }

        Joystick(
            modifier = Modifier.align(Alignment.BottomStart).padding(32.dp).size(150.dp),
            onJoystickMove = { joystickOffset = it }
        )

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 200.dp, end = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pirate Village", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("Explore the town and meet other fishers.", color = Color.LightGray)
            }
        }
    }
}
