package com.example.comrasmusfishingwithfriends

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val db: FirebaseFirestore
    get() = FirebaseFirestore.getInstance()

@Composable
fun FishingGameScreen(currentPlayer: Player, navController: NavHostController) {
    Log.d("FishingGameScreen", "Current player: ${currentPlayer.playerName}, ID: ${currentPlayer.playerId}, Score: ${currentPlayer.score}")
    var isFishing by remember { mutableStateOf(false) }
    var fishCaught by remember { mutableStateOf<Fish?>(null) }
    var isCasting by remember { mutableStateOf(false) }
    val reeling = remember { Reeling() }
    var isFishCaught by remember { mutableStateOf(false) }
    var points by remember { mutableIntStateOf(currentPlayer.score) }  // Use initial score from currentPlayer
    val scope = rememberCoroutineScope()
    val rodIdleImage = painterResource(id = R.drawable.rod)
    val rodCastingImage = painterResource(id = R.drawable.rod)
    var showSaveButton by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }
    var showTierMenu by remember { mutableStateOf(false) }
    var isReelRotating by remember { mutableStateOf(false) }
    val rotationState = remember { Animatable(0f) }
    var showReelingButton by remember { mutableStateOf(true) }
    var shouldRotate by remember { mutableStateOf(false) }
    var shakeOffset by remember { mutableStateOf(0f) }
    val shakeAnimation = rememberInfiniteTransition(label = "shake")
    val shake = shakeAnimation.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

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
                val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/raw/fishingbakgrund")
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

    LaunchedEffect(Unit) {
        // Ladda spelarens data när skärmen öppnas
        FirebaseManager.loadPlayer(currentPlayer.playerId)?.let { loadedPlayer ->
            currentPlayer.apply {
                score = loadedPlayer.score
                achievements = loadedPlayer.achievements
                fishCatalog = loadedPlayer.fishCatalog
                unlockedRods = loadedPlayer.unlockedRods
                currentRodId = loadedPlayer.currentRodId
                dailyChallengeProgress = loadedPlayer.dailyChallengeProgress
                completedChallenges = loadedPlayer.completedChallenges
                totalChallengeBonus = loadedPlayer.totalChallengeBonus
            }
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
                showReelingButton = true
                fishCaught = null  // Återställ fishCaught när tiden är ute
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6AC87))
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    useController = false  // Disable default controls
                    setKeepContentOnPlayerReset(true)
                    useArtwork = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(twoThirdsHeight)
                .zIndex(-1f)
        )

        // Row med View Tiers och tillbaka-knapp
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            // View Tiers knapp
            Row(
                modifier = Modifier
                    .clickable { showTierMenu = !showTierMenu },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "View Tiers",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Tillbaka-ikon under View Tiers
            Image(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Tillbaka",
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(32.dp)
                    .clickable { 
                        navController.navigate("start_screen") {
                            popUpTo("start_screen") { inclusive = true }
                        }
                    }
            )
        }

        // Tier-meny popup
        AnimatedVisibility(
            visible = showTierMenu,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 56.dp)
        ) {
            Card(
                modifier = Modifier
                    .width(300.dp)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Fishing Tiers",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    TierInfoRow(
                        name = "Platinum",
                        requirement = "1000+ points",
                        iconId = R.drawable.tier_platinum
                    )
                    TierInfoRow(
                        name = "Gold",
                        requirement = "500+ points",
                        iconId = R.drawable.tier_gold
                    )
                    TierInfoRow(
                        name = "Silver",
                        requirement = "100+ points",
                        iconId = R.drawable.tier_silver
                    )
                    TierInfoRow(
                        name = "Bronze",
                        requirement = "50+ points",
                        iconId = R.drawable.tier_bronze
                    )
                    TierInfoRow(
                        name = "No Tier",
                        requirement = "0-49 points",
                        iconId = R.drawable.ic_star
                    )

                    Button(
                        onClick = { showTierMenu = false },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp)
                    ) {
                        Text("Stäng")
                    }
                }
            }
        }

        // Väderinfo (högst upp till höger)
        var currentWeather by remember { mutableStateOf(WeatherSystem.getCurrentWeather()) }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = when (currentWeather.type) {
                            WeatherType.SUNNY -> R.drawable.ic_sunny
                            WeatherType.RAINY -> R.drawable.ic_rainy
                            WeatherType.STORMY -> R.drawable.ic_stormy
                            WeatherType.CLOUDY -> R.drawable.ic_cloudy
                        }
                    ),
                    contentDescription = "Väder",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = currentWeather.description,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Poäng: $points",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                letterSpacing = 1.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    offset = Offset(3f, 3f),
                    blurRadius = 4f
                )
            ),
            color = Color(0xFF90EE90),  // Ljusgrön färg som passar fiske-temat
            modifier = Modifier
                .padding(16.dp)
                .graphicsLayer {
                    translationY = 4f  // Ger en lätt "flytande" effekt
                }
        )

        Spacer(modifier = Modifier.height(100.dp))

        // Visa antingen fiskespöt eller den fångade fisken
        if (!isFishCaught) {
            FishingRod(
                rodImage = when {
                    isCasting -> rodCastingImage
                    reeling.isReeling -> painterResource(id = R.drawable.rodstruggling)
                    else -> rodIdleImage
                },
                isCasting = isCasting,
                isReeling = reeling.isReeling
            )
        } else {
            fishCaught?.let {
                FishDisplay(fish = it, isReelingComplete = true)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!isFishing && showReelingButton) {
            Image(
                painter = painterResource(id = R.drawable.reelbilden),
                contentDescription = "Cast Reel",
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
                                val fishType = getRandomFishType()
                                fishCaught = Fish(type = fishType, points = getFishPoints(fishType))

                                isCasting = false
                                reeling.startReeling()
                            }
                        }
                    }
            )
        }

        if (reeling.isReeling && showReelingButton) {
            Spacer(modifier = Modifier.height(40.dp))

            // Progress bar för nuvarande fas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                // Rita ut alla faser som har progress
                Reeling.phases.forEachIndexed { index, phase ->
                    if (index <= reeling.currentPhase) {
                        val startFraction = index.toFloat() / Reeling.phases.size
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
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (points > 0 && !isFishing) {
            Spacer(modifier = Modifier.height(20.dp))


            saveStatus?.let {
                Text(
                    text = it,
                    color = if (it.startsWith("Poäng")) Color.Green else Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // Rotation animation
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

fun updatePlayerScore(playerName: String, player: Player, onComplete: (Boolean) -> Unit) {
    Log.d("Firestore", "Trying to update score for player: $playerName with ID: ${player.playerId}")

    // Först kontrollera om vi har ett giltigt ID
    if (player.playerId.isBlank()) {
        Log.e("Firestore", "Invalid player ID")
        onComplete(false)
        return
    }

    FirebaseFirestore.getInstance()
        .collection("players")
        .document(player.playerId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                // Dokumentet finns, uppdatera poängen
                document.reference.update("score", player.score)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Successfully updated score for player $playerName (ID: ${player.playerId})")
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error updating score", e)
                        onComplete(false)
                    }
            } else {
                Log.e("Firestore", "Document does not exist for ID: ${player.playerId}")
                onComplete(false)
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error fetching document", e)
            onComplete(false)
        }
}
@Composable
private fun FishDisplay(fish: Fish, isReelingComplete: Boolean) {
    val fishImages = mapOf(
        "Trout" to R.drawable.trout,
        "Bass" to R.drawable.bass,
        "Catfish" to R.drawable.catfish,
        "Carp" to R.drawable.carp,
        "Pike" to R.drawable.pike,
        "Goldfish" to R.drawable.goldfish,
        "Tuna" to R.drawable.tuna,
        "Swordfish" to R.drawable.swordfish,
        "Shark" to R.drawable.shark,
        "Dolphinfish" to R.drawable.dolphinfish,
        "Marlin" to R.drawable.marlin,
        "Octopus" to R.drawable.octopus,
        "Giant Squid" to R.drawable.giantsquid,
        "Salmon" to R.drawable.salmon,
        "Rainbow Trout" to R.drawable.rainbowtrout,
        "Sturgeon" to R.drawable.sturgeon,
        "Eel" to R.drawable.eel,
        "Arctic Char" to R.drawable.arcticchar,
        "Grayling" to R.drawable.grayling
    )
    
    val fishImage = fishImages[fish.type] ?: R.drawable.bones
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isReelingComplete) {
        if (isReelingComplete) {
            delay(1000)  // Vänta lite längre innan fisken visas
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
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = 0.5.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(2f, 2f),
                        blurRadius = 3f
                    )
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Image(
                painter = painterResource(id = fishImage),
                contentDescription = "Fiskbild",
                modifier = Modifier.size(200.dp)
            )
        }
    }
}


@Composable
private fun TierInfoRow(
    name: String,
    requirement: String,
    iconId: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = iconId),
            contentDescription = "$name tier icon",
            modifier = Modifier.size(32.dp)
        )
        Column {
            Text(
                text = name,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = requirement,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun updateAchievements(player: Player, fish: Fish) {
    Achievements.allAchievements.forEach { achievement ->
        when (achievement.id) {
            "first_catch" -> {
                achievement.currentCount = 1
                achievement.isUnlocked = true
            }
            "shark_hunter" -> {
                if (fish.type == "Great White Shark") {
                    achievement.currentCount = 1
                    achievement.isUnlocked = true
                }
            }
            "master_fisher" -> {
                achievement.currentCount++
                if (achievement.currentCount >= achievement.requiredCount) {
                    achievement.isUnlocked = true
                }
            }
            "point_collector" -> {
                if (player.score >= achievement.requiredCount) {
                    achievement.isUnlocked = true
                }
            }
        }
    }
}

fun updatePlayerProgress(player: Player, fish: Fish) {
    // Uppdatera fiskekatalog
    player.fishCatalog.addCatch(fish)

    // Uppdatera achievements
    updateAchievements(player, fish)


    
    // Spara alla ändringar till Firebase
    FirebaseManager.updatePlayer(player) { success ->
        if (success) {
            Log.d("Firebase", "Successfully saved player progress")
        } else {
            Log.e("Firebase", "Failed to save player progress")
        }
    }
}