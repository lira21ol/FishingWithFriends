package com.example.comrasmusfishingwithfriends

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FishingGameScreen3(currentPlayer: Player, navController: NavHostController) {
    // State variabler (samma som FishingGameScreen men med några tillägg)
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

    // Setup för videobakgrund med flodbakgrund istället
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val displayMetrics = context.resources.displayMetrics
    val screenHeightPx = displayMetrics.heightPixels / density
    val screenHeightDp = screenHeightPx.dp
    val twoThirdsHeight = screenHeightDp * 2 / 3f

    val player = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS / 2,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 2
                )
                .build()
            )
            .build().apply {
                // Använd flodbakgrundsvideo istället
                val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/raw/riverbakgrund")
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87ACF6)) // Ändrad bakgrundsfärg för flodtema
    ) {
        // Videobakgrund
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
            modifier = Modifier
                .fillMaxWidth()
                .height(twoThirdsHeight)
                .zIndex(-1f)
        )

        // Huvudinnehåll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Poängvisning
            Text(
                text = "Poäng: $points",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(100.dp))

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
                FishDisplay3(fish = it, isReelingComplete = reeling.isComplete())
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Kastknapp
            if (!isFishing && showReelingButton) {
                Image(
                    painter = painterResource(id = R.drawable.reelbilden),
                    contentDescription = "Kasta",
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            if (!isFishing) {
                                isFishing = true
                                isCasting = true
                                catchResult = "Kastar..."
                                fishCaught = null
                                isFishCaught = false
                                shouldRotate = false

                                scope.launch {
                                    delay(2000)
                                    catchResult = "Fisk på kroken! Dra in den!"
                                    
                                    // Använd det nya funktionsnamnet
                                    val fishType = getRandomFish3()
                                    fishCaught = Fish(type = fishType, points = getRiverFishPoints(fishType))

                                    isCasting = false
                                    reeling.startReeling()
                                }
                            }
                        }
                )
            }

            // Reeling-kontroller
            if (reeling.isReeling && showReelingButton) {
                Spacer(modifier = Modifier.height(40.dp))

                LinearProgressIndicator(
                    progress = { reeling.rollProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Image(
                    painter = painterResource(id = R.drawable.reelbilden),
                    contentDescription = "Dra in",
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            rotationZ = if (shouldRotate) rotationState.value else 0f
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    shouldRotate = true
                                    isReelRotating = true
                                    reeling.reelIn(scope)
                                    if (reeling.isComplete()) {
                                        fishCaught?.let { fish ->
                                            catchResult = "Du fångade en ${fish.type}!"
                                            points += fish.points
                                            isCatchSuccessful = true
                                            isFishCaught = true
                                            reeling.stopReeling()
                                            isFishing = false
                                            currentPlayer.score = points

                                            updatePlayerProgress(currentPlayer, fish)

                                            showReelingButton = false
                                            shouldRotate = false
                                            scope.launch {
                                                delay(3000)
                                                showReelingButton = true
                                            }
                                        }
                                    }
                                    awaitRelease()
                                    isReelRotating = false
                                    shouldRotate = false
                                }
                            )
                        }
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.ic_back),
            contentDescription = "Tillbaka",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(32.dp)
                .clickable { 
                    navController.navigate("start_screen") {
                        popUpTo("start_screen") { inclusive = true }
                    }
                }
        )
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

// Hjälpfunktioner för flodsfiske
private fun getRandomFish3(): String {
    return listOf(
        "Rainbow Trout",
        "Brown Trout",
        "Salmon",
        "Pike",
        "Perch",
        "Catfish",
        "Carp",
        "Sturgeon",
        "Grayling",
        "Arctic Char"
    ).random()
}

private fun getRiverFishPoints(fishType: String): Int {
    return when (fishType) {
        "Sturgeon" -> 400
        "Salmon" -> 350
        "Catfish" -> 300
        "Pike" -> 250
        "Rainbow Trout" -> 200
        "Brown Trout" -> 175
        "Carp" -> 150
        "Arctic Char" -> 125
        "Perch" -> 100
        "Grayling" -> 75
        else -> 50
    }
}

@Composable
private fun FishDisplay3(fish: Fish, isReelingComplete: Boolean) {
    val fishImages = mapOf(
        "Rainbow Trout" to R.drawable.rainbowtrout,
        "Brown Trout" to R.drawable.trout,
        "Salmon" to R.drawable.salmon,
        "Pike" to R.drawable.pike,
        "Perch" to R.drawable.crocodile,
        "Catfish" to R.drawable.catfish,
        "Carp" to R.drawable.carp,
        "Sturgeon" to R.drawable.sturgeon,
        "Grayling" to R.drawable.grayling,
        "Arctic Char" to R.drawable.arcticchar
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
                contentDescription = "Fiskbild",
                modifier = Modifier.size(200.dp)
            )
            Text(
                text = "Du fångade en ${fish.type} som väger ${fish.weight}kg!",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
} 