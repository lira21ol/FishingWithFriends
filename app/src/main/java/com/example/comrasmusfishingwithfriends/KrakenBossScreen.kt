package com.example.comrasmusfishingwithfriends

import Kraken
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.times
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.times
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs


@Composable
fun KrakenBossScreen(
    currentPlayer: Player,
    navController: NavHostController
) {
    BackHandler {
        navController.navigate("start_screen") {
            popUpTo("start_screen") { inclusive = true }
        }
    }

    var kraken by remember { mutableStateOf(Kraken()) }
    var isFishing by remember { mutableStateOf(false) }
    var isReeling by remember { mutableStateOf(false) }
    var showTentacle by remember { mutableStateOf(false) }
    var sliceCount by remember { mutableStateOf(0) }
    var reelingStartTime by remember { mutableStateOf(0L) }
    var sliceStartTime by remember { mutableStateOf(0L) }
    var isGameOver by remember { mutableStateOf(false) }
    val reeling = remember { Reeling() }
    val scope = rememberCoroutineScope()
    
    var isReelRotating by remember { mutableStateOf(false) }
    val rotationState = remember { Animatable(0f) }
    var shouldRotate by remember { mutableStateOf(false) }
    
    // Gesture state
    var sliceProgress by remember { mutableStateOf(0f) }
    var currentSliceX by remember { mutableStateOf(0f) }

    var slashProgress by remember { mutableFloatStateOf(0f) }
    var isSlashing by remember { mutableStateOf(false) }
    var showCutTentacle by remember { mutableStateOf(false) }
    
    // Lägg till nya state-variabler
    var showSuccessCheck by remember { mutableStateOf(false) }
    
    // Timer för slicing
    LaunchedEffect(showTentacle) {
        if (showTentacle) {
            sliceStartTime = System.currentTimeMillis()
            while (showTentacle && !isGameOver) {
                delay(100)
                if (System.currentTimeMillis() - sliceStartTime > 5000) {
                    isGameOver = true
                    showTentacle = false
                }
            }
        }
    }

    var isCasting by remember { mutableStateOf(false) }

    var showFishingRod by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000B2E))
    ) {
        // Bakgrundsbild som täcker 70% av skärmen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .align(Alignment.TopCenter)
                .background(
                    Color(0xFF001F3F),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                )
        ) {
            // Kraken bild centrerad i bakgrunden (50% av skärmen)
            Image(
                painter = painterResource(
                    id = when (kraken.currentPhase) {
                        1 -> R.drawable.kraken_phase1
                        2 -> R.drawable.kraken_phase2
                        else -> R.drawable.kraken_phase3
                    }
                ),
                contentDescription = "Kraken Fas ${kraken.currentPhase}",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight(0.7f)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )

            // Förbättrad hälsomätare
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Kraken HP: ${kraken.health}/1000",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LinearProgressIndicator(
                    progress = { kraken.health / 1000f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Color(0xFFFF4444),
                    trackColor = Color(0x33FFFFFF)
                )
            }
        }

        // Förbättrad tentakel-skärningsanimation
        if (showTentacle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tentacle),
                    contentDescription = "Tentacle",
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationX = currentSliceX
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                currentSliceX += dragAmount.x
                                sliceProgress += abs(dragAmount.x) / size.width
                                
                                if (sliceProgress >= 1f) {
                                    showTentacle = false
                                    showCutTentacle = true
                                    kraken.takeDamage(50)
                                    sliceProgress = 0f
                                    currentSliceX = 0f
                                    
                                    // Återställ för nästa runda
                                    scope.launch {
                                        delay(1000)
                                        showCutTentacle = false
                                        showFishingRod = true
                                        isFishing = false
                                        reeling.stopReeling()
                                    }
                                }
                            }
                        }
                )
            }
        }

        // Förbättrade spelkontroller
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reeling-kontroller med förbättrad visuell feedback
            if (reeling.isReeling) {
                LinearProgressIndicator(
                    progress = { reeling.rollProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0x33FFFFFF)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Visa fiskespö och reeling progress
                if (showFishingRod) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reeling progress bar
                        if (reeling.isReeling) {
                            LinearProgressIndicator(
                                progress = { reeling.rollProgress },
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }

                        // Fiskespö-knapp
                        Image(
                            painter = painterResource(
                                id = when {
                                    isCasting -> R.drawable.rod
                                    reeling.isReeling -> R.drawable.rodstruggling
                                    else -> R.drawable.rod
                                }
                            ),
                            contentDescription = if (reeling.isReeling) "Dra in" else "Kasta",
                            modifier = Modifier
                                .size(80.dp)
                                .graphicsLayer {
                                    rotationZ = if (shouldRotate) rotationState.value else 0f
                                }
                                .clickable {
                                    if (!isFishing && !showTentacle) {
                                        // Starta fisket
                                        isFishing = true
                                        isCasting = true
                                        reeling.startReeling()
                                        scope.launch {
                                            delay(1000)
                                            isCasting = false
                                            
                                            // Börja dra in
                                            while (reeling.isReeling && !reeling.isComplete()) {
                                                reeling.reelIn(scope)
                                                delay(100)
                                            }
                                            
                                            // När reeling är klar, visa tentakeln
                                            if (reeling.isComplete()) {
                                                showFishingRod = false
                                                showTentacle = true
                                                kraken.isVulnerable = true
                                            }
                                        }
                                    } else if (reeling.isReeling) {
                                        // Öka reeling progress när spelaren klickar
                                        scope.launch {
                                            shouldRotate = true
                                            isReelRotating = true
                                            reeling.reelIn(scope)
                                            delay(100)
                                            isReelRotating = false
                                            shouldRotate = false
                                        }
                                    }
                                }
                        )
                    }
                }

                // Kniv (visas endast när tentakeln är synlig)
                if (showTentacle) {
                    Image(
                        painter = painterResource(id = R.drawable.knife),
                        contentDescription = "Kniv",
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                rotationZ = slashProgress * 360f
                            }
                    )
                }
            }
        }

        // Tillbaka-knapp med förbättrad design
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clickable { 
                    navController.navigate("start_screen") {
                        popUpTo("start_screen") { inclusive = true }
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Tillbaka",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Victory screen med förbättrad design
        if (kraken.isDead()) {
            VictoryScreen(
                currentPlayer = currentPlayer,
                navController = navController
            )
        }
    }

    // Rotationsanimation
    LaunchedEffect(isReelRotating) {
        if (isReelRotating && shouldRotate) {
            rotationState.animateTo(
                targetValue = rotationState.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotationState.snapTo(0f)
        }
    }
}

@Composable
private fun VictoryScreen(
    currentPlayer: Player,
    navController: NavHostController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Du besegrade Kraken!",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "+1000 poäng",
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.headlineSmall
                )
                Button(
                    onClick = {
                        // Uppdatera spelarens poäng
                        currentPlayer.score += 1000
                        
                        // Skapa en FishEntry istället för Fish
                        val fishEntry = FishEntry(
                            fishType = "Kraken",
                            timesCaught = 1,
                            largestWeight = 5000.0,
                            totalPoints = 1000,
                            location = "Boss Arena"
                        )
                        
                        // Uppdatera fiskkatalogen direkt
                        currentPlayer.fishCatalog.catalog["Kraken"] = fishEntry
                        currentPlayer.fishCatalog.caughtFish["Kraken"] = 1
                        
                        // Uppdatera Firebase
                        FirebaseManager.updatePlayer(currentPlayer) { success ->
                            if (success) {
                                navController.navigate("start_screen") {
                                    popUpTo("start_screen") { inclusive = true }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("Fortsätt")
                }
            }
        }
    }
}
 