package com.example.comrasmusfishingwithfriends

import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun isLocked(player: Player): Boolean {
    // Krav för att låsa upp denna nivå (t.ex. minst 1000 poäng)
    return player.score < 1000
}

@Composable
fun FishingGameScreen2(
    currentPlayer: Player,
    navController: NavHostController,
) {
    // State variabler
    var isFishing by remember { mutableStateOf(false) }
    var catchResult by remember { mutableStateOf("Väntar på fisk...") }
    var fishCaught by remember { mutableStateOf<Fish?>(null) }
    var isCatchSuccessful by remember { mutableStateOf(false) }
    var isCasting by remember { mutableStateOf(false) }
    val reeling = remember { Reeling() }
    var isFishCaught by remember { mutableStateOf(false) }
    var points by remember { mutableIntStateOf(currentPlayer.score) }
    var showReelingButton by remember { mutableStateOf(true) }
    var shouldRotate by remember { mutableStateOf(false) }
    var isReelRotating by remember { mutableStateOf(false) }
    val rotationState = remember { Animatable(0f) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Setup för videobakgrund
    val player = remember { setupVideoPlayer(context) }

    // Cleanup när komponenten förstörs
    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    // Ladda spelardata
    LaunchedEffect(Unit) {
        FirebaseManager.loadPlayer(currentPlayer.playerId)?.let { loadedPlayer ->
            currentPlayer.apply {
                score = loadedPlayer.score
                achievements = loadedPlayer.achievements
                fishCatalog = loadedPlayer.fishCatalog
                unlockedRods = loadedPlayer.unlockedRods
                currentRodId = loadedPlayer.currentRodId
            }
        }
    }

    // Lägg till en funktion för att hantera navigation till nästa nivå
    fun tryNavigateToNextLevel() {
        if (!isLocked(currentPlayer)) {
            navController.navigate("fishing_game_screen3")
        }
    }

    // Huvudlayout
    Box(modifier = Modifier.fillMaxSize()) {
        // Videobakgrund
        OceanBackground(modifier = Modifier.fillMaxSize())
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Poängvisning
            Text(
                text = "Poäng: $points",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Fiskespö
            if (!isFishCaught) {
                FishingRod(
                    rodImage = when {
                        isCasting -> painterResource(id = R.drawable.rod)
                        reeling.isReeling -> painterResource(id = R.drawable.rodstruggling)
                        else -> painterResource(id = R.drawable.rod)
                    },
                    isCasting = isCasting,
                    isReeling = reeling.isReeling
                )
            }

            // Visa fångad fisk
            fishCaught?.let {
                FishDisplay2(fish = it, isReelingComplete = reeling.isComplete())
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Kastknapp eller Reeling-kontroller (inte båda)
            if (reeling.isReeling && showReelingButton) {
                ReelingControls(
                    reeling = reeling,
                    rotationState = rotationState,
                    shouldRotate = shouldRotate,
                    fishCaught = fishCaught,
                    onReelIn = {
                        handleReelIn(
                            reeling = reeling,
                            scope = scope,
                            fishCaught = fishCaught,
                            onComplete = { fish ->
                                handleCatchComplete(
                                    fish = fish,
                                    currentPlayer = currentPlayer,
                                    onPointsUpdate = { points = it },
                                    onReelingComplete = {
                                        isFishing = false
                                        showReelingButton = false
                                        shouldRotate = false
                                        scope.launch {
                                            delay(5000)
                                            showReelingButton = true
                                        }
                                    },
                                    onResetState = {
                                        scope.launch {
                                            delay(5000)
                                            fishCaught = null
                                            isFishCaught = false
                                            isCasting = false
                                            reeling.stopReeling()
                                            catchResult = "Väntar på fisk..."
                                        }
                                    },
                                    scope = scope
                                )
                            }
                        )
                    }
                )
            } else if (!isFishing && showReelingButton) {
                CastButton(
                    onClick = {
                        handleCasting(
                            scope = scope,
                            onCastStart = {
                                isFishing = true
                                isCasting = true
                                catchResult = "Kastar..."
                                fishCaught = null
                                isFishCaught = false
                                shouldRotate = false
                            },
                            onFishBite = { fish ->
                                catchResult = "Fisk på kroken! Dra in den!"
                                fishCaught = fish
                                isCasting = false
                                reeling.startReeling()
                            }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Visa fångstresultat
            Text(
                text = catchResult,
                color = if (isCatchSuccessful) Color.Green else Color.White
            )
        }

        // Tillbaka-knapp
        BackToMenuButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            onClick = { navController.navigate("start_screen") }
        )
    }
}

// Hjälpfunktioner
@OptIn(UnstableApi::class)
private fun setupVideoPlayer(context: android.content.Context): ExoPlayer {
    return ExoPlayer.Builder(context)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS / 2,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 2
                )
                .build()
        )
        .build().apply {
            val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/raw/oceanbakgrund")
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        }
}

@Composable
private fun OceanBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS / 2,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 2
                    )
                    .build()
            )
            .build().apply {
                val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/raw/oceanbakgrund")
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                useController = false
                setKeepContentOnPlayerReset(true)
                useArtwork = false
            }
        },
        modifier = modifier
    )
}

@Composable
private fun CastButton(onClick: () -> Unit) {
    Image(
        painter = painterResource(id = R.drawable.reelbilden),
        contentDescription = "Kasta",
        modifier = Modifier
            .size(80.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ReelingControls(
    reeling: Reeling,
    rotationState: Animatable<Float, AnimationVector1D>,
    shouldRotate: Boolean,
    fishCaught: Fish?,
    onReelIn: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LinearProgressIndicator(
            progress = { reeling.rollProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.reelbilden),
            contentDescription = "Dra in",
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { rotationZ = if (shouldRotate) rotationState.value else 0f }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onReelIn()
                            awaitRelease()
                        }
                    )
                }
        )
    }
}

@Composable
private fun BackToMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text("Tillbaka till menyn")
    }
}

private fun handleCasting(
    scope: CoroutineScope,
    onCastStart: () -> Unit,
    onFishBite: (Fish) -> Unit,
) {
    onCastStart()
    scope.launch {
        delay(2000)
        val fishType = getRandomOceanFish2()
        val fish = Fish(type = fishType, points = getFishPoints2(fishType))
        onFishBite(fish)
    }
}

private fun handleReelIn(
    reeling: Reeling,
    scope: CoroutineScope,
    fishCaught: Fish?,
    onComplete: (Fish) -> Unit,
) {
    reeling.reelIn(scope)
    if (reeling.isComplete()) {
        fishCaught?.let { fish ->
            onComplete(fish)
        }
    }
}

private fun handleCatchComplete(
    fish: Fish,
    currentPlayer: Player,
    onPointsUpdate: (Int) -> Unit,
    onReelingComplete: () -> Unit,
    onResetState: () -> Unit,
    scope: CoroutineScope
) {
    currentPlayer.score += fish.points
    onPointsUpdate(currentPlayer.score)
    updatePlayerProgress(currentPlayer, fish)
    onReelingComplete()
    
    scope.launch {
        delay(3000)
        onResetState()
    }
}

private fun getRandomOceanFish2(): String {
    return listOf(
        "Blue Whale",
        "Great White Shark",
        "Giant Squid",
        "Hammerhead Shark",
        "Manta Ray",
        "Dolphin",
        "Tuna",
        "Swordfish",
        "Marlin"
    ).random()
}

private fun getFishPoints2(fishType: String): Int {
    return when (fishType) {
        "Blue Whale" -> 500
        "Great White Shark" -> 400
        "Giant Squid" -> 350
        "Hammerhead Shark" -> 300
        "Manta Ray" -> 250
        "Dolphin" -> 200
        "Tuna" -> 150
        "Swordfish" -> 175
        "Marlin" -> 225
        else -> 50
    }
}

@Composable
private fun FishDisplay2(fish: Fish, isReelingComplete: Boolean) {
    val fishImages = mapOf(
        "Blue Whale" to R.drawable.blue_whale,
        "Great White Shark" to R.drawable.great_white_shark,
        "Giant Squid" to R.drawable.giant_squid,
        "Hammerhead Shark" to R.drawable.hammerhead_shark,
        "Manta Ray" to R.drawable.manta_ray,
        "Dolphin" to R.drawable.dolphin,
        "Tuna" to R.drawable.tuna,
        "Swordfish" to R.drawable.swordfish,
        "Marlin" to R.drawable.marlin
    )
    
    val fishImage = fishImages[fish.type] ?: R.drawable.bones
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isReelingComplete) {
        if (isReelingComplete) {
            delay(1000)
            isVisible = true
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = fishImage),
                contentDescription = "Fish Image",
                modifier = Modifier.size(200.dp)
            )
            Text(
                text = "Du fångade en ${fish.type} som väger ${fish.weight}kg!",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "+${fish.points} poäng!",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}