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
fun MyHouseScreen(currentPlayer: Player, navController: NavHostController) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine)
    
    var houseNode by remember { mutableStateOf<ModelNode?>(null) }

    // Camera movement state
    var cameraPosition by remember { mutableStateOf(Position(0f, 1.5f, 5f)) }
    var cameraYaw by remember { mutableFloatStateOf(0f) }
    var cameraPitch by remember { mutableFloatStateOf(0f) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        modelLoader.loadModelInstanceAsync("house/sea_shack.glb") { instance ->
            instance?.let {
                houseNode = ModelNode(it, scaleToUnits = 10f).apply {
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
                val speed = 0.15f
                val yawRad = Math.toRadians(cameraYaw.toDouble())
                
                // Calculate movement relative to camera look direction
                // Forward/Backward is y, Left/Right is x
                val dx = (joystick.x * cos(yawRad) + joystick.y * sin(yawRad)).toFloat() * speed
                val dz = (joystick.x * -sin(yawRad) + joystick.y * cos(yawRad)).toFloat() * speed
                
                cameraPosition = Position(cameraPosition.x + dx, cameraPosition.y, cameraPosition.z + dz)
            }
            delay(16)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))
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
            childNodes = listOfNotNull(houseNode),
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
                Text("My Sea Shack", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("Move with joystick, Drag to look around", color = Color.LightGray)
            }
        }
    }
}
