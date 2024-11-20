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
    var isFishing by remember { mutableStateOf(false) }
    var fishCaught by remember { mutableStateOf<Fish?>(null) }
    var isCasting by remember { mutableStateOf(false) }
    val reeling = remember { Reeling() }
    var isFishCaught by remember { mutableStateOf(false) }
    var points by remember { mutableIntStateOf(currentPlayer.score) }
    val scope = rememberCoroutineScope()
    var showReelingButton by remember { mutableStateOf(true) }
    var shouldRotate by remember { mutableStateOf(false) }
    var isReelRotating by remember { mutableStateOf(false) }
    val rotationState = remember { Animatable(0f) }

    // Video setup
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val displayMetrics = context.resources.displayMetrics
    val screenHeightPx = displayMetrics.heightPixels / density
    val screenHeightDp = screenHeightPx.dp
    val twoThirdsHeight = screenHeightDp * 2 / 3f

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

    fun handleCatchComplete() {
        fishCaught?.let { fish ->
            points += fish.points
            isFishCaught = true
            reeling.stopReeling()
            isFishing = false
            currentPlayer.score = points
            updatePlayerProgress(currentPlayer, fish)
            showReelingButton = false
            
            scope.launch {
                delay(5000)
                // Återställ alla tillstånd
                fishCaught = null
                isFishCaught = false
                showReelingButton = true
                reeling.reset()
                isCasting = false
                shouldRotate = false
                isReelRotating = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87ACF6))
    ) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Poäng: $points",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(100.dp))

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
            } else if (fishCaught != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FishDisplay3(fish = fishCaught!!, isReelingComplete = !reeling.isReeling)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (reeling.isReeling && !isFishCaught) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                ) {
                    Reeling.phases.forEachIndexed { index, phase ->
                        if (index <= reeling.currentPhase) {
                            val endFraction = if (index == reeling.currentPhase) {
                                (index.toFloat() + reeling.phaseProgress) / Reeling.phases.size
                            } else {
                                (index + 1f) / Reeling.phases.size
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(endFraction)
                                    .background(phase.color)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Image(
                    painter = painterResource(id = R.drawable.reelbilden),
                    contentDescription = "Reel In",
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            rotationZ = if (shouldRotate) rotationState.value else 0f
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    scope.launch {
                                        reeling.isHolding = true
                                        shouldRotate = true
                                        isReelRotating = true
                                        
                                        while (reeling.isHolding) {
                                            if (reeling.updateProgress(true)) {
                                                if (reeling.completePhase()) {
                                                    handleCatchComplete()
                                                    break
                                                }
                                            }
                                            delay(16)
                                        }

                                        awaitRelease()
                                        reeling.isHolding = false
                                        isReelRotating = false
                                        shouldRotate = false
                                    }
                                },
                                onTap = {
                                    if (reeling.updateProgress(false)) {
                                        if (reeling.completePhase()) {
                                            handleCatchComplete()
                                        }
                                    }
                                }
                            )
                        }
                )
            } else if (!isFishing && showReelingButton && !isFishCaught) {
                Image(
                    painter = painterResource(id = R.drawable.reelbilden),
                    contentDescription = "Kasta",
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            if (!isFishing) {
                                isFishing = true
                                isCasting = true
                                fishCaught = null
                                isFishCaught = false
                                shouldRotate = false
                                reeling.stopReeling()
                                
                                scope.launch {
                                    delay(2000)
                                    val fishType = getRandomFish3()
                                    fishCaught = Fish(type = fishType, points = getFishPoints3(fishType))
                                    isCasting = false
                                    reeling.startReeling()
                                }
                            }
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

// Hjälpfunktioner behålls samma som tidigare

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
        enter = expandIn(
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        ),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Du fångade en ${fish.type}!",
                color = Color(0xFF90EE90),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Image(
                painter = painterResource(id = fishImage),
                contentDescription = "Fiskbild",
                modifier = Modifier.size(200.dp)
            )
            
            Text(
                text = "Vikt: ${fish.weight}kg",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Text(
                text = "+${fish.points} poäng!",
                color = Color(0xFFFFD700),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun getFishPoints3(fishType: String): Int {
    return when (fishType) {
        "Rainbow Trout" -> 150
        "Brown Trout" -> 120
        "Salmon" -> 200
        "Pike" -> 180
        "Perch" -> 100
        "Catfish" -> 250
        "Carp" -> 160
        "Sturgeon" -> 300
        "Grayling" -> 140
        "Arctic Char" -> 170
        else -> 100
    }
}