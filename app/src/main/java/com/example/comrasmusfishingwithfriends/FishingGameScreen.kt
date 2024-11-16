package com.example.comrasmusfishingwithfriends

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val db: FirebaseFirestore
    get() = FirebaseFirestore.getInstance()

@Composable
fun FishingGameScreen(currentPlayer: Player) {
    var isFishing by remember { mutableStateOf(false) }
    var catchResult by remember { mutableStateOf("Waiting for fish...") }
    var fishCaught by remember { mutableStateOf<Fish?>(null) }
    var isCatchSuccessful by remember { mutableStateOf(false) }
    var isCasting by remember { mutableStateOf(false) }
    val reeling = remember { Reeling() }
    var isFishCaught by remember { mutableStateOf(false) }
    var points by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var currentPlayer by remember { mutableStateOf<Player?>(null) }
    val rodIdleImage = painterResource(id = R.drawable.rod)
    val rodCastingImage = painterResource(id = R.drawable.rod)

    val context = LocalContext.current
    val density = LocalDensity.current.density
    val displayMetrics = context.resources.displayMetrics
    val screenHeightPx = displayMetrics.heightPixels / density
    val screenHeightDp = screenHeightPx.dp
    val twoThirdsHeight = screenHeightDp * 2 / 3f

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/raw/fishingbakgrund")
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AndroidView(
            factory = { context ->
                val playerView = PlayerView(context).apply {
                    this.player = player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
                playerView
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(twoThirdsHeight)
                .zIndex(-1f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Points: $points",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(100.dp))

        if (!isFishCaught) {
            FishingRod(
                rodImage = if (isCasting || reeling.isReeling) rodCastingImage else rodIdleImage,
                isCasting = isCasting,
                isReeling = reeling.isReeling
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        fishCaught?.let {
            FishDisplay(fish = it, isReelingComplete = reeling.isComplete())
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (!isFishing) {
                    isFishing = true
                    isCasting = true
                    catchResult = "Casting..."
                    fishCaught = null
                    isFishCaught = false

                    scope.launch {
                        delay(2000)
                        catchResult = "Fish waiting! Reel it in!"

                        val fishType = getRandomFishType()
                        fishCaught = Fish(type = fishType, points = getFishPoints(fishType))

                        isCasting = false
                        reeling.startReeling()
                    }
                }
            },
            enabled = !isFishing,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = if (isFishing) "Fishing..." else "Cast")
        }

        if (reeling.isReeling) {
            Spacer(modifier = Modifier.height(40.dp))

            LinearProgressIndicator(
                progress = { reeling.rollProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    reeling.reelIn()
                    if (reeling.isComplete()) {
                        catchResult = "You caught a ${fishCaught?.type ?: "fish"}!"
                        points += fishCaught?.points ?: 0
                        isCatchSuccessful = true
                        isFishCaught = true
                        reeling.stopReeling()
                        isFishing = false

                        currentPlayer?.let {
                            it.score = points
                            updatePlayerScore(it)
                        }
                    }
                },
                enabled = reeling.rollProgress < 1f,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Roll-In")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        CatchResult(isSuccessful = isCatchSuccessful, fish = fishCaught)
    }
}

fun updatePlayerScore(player: Player) {
    val playerRef = FirebaseFirestore.getInstance().collection("players").document(player.playerId)
    playerRef.update("score", player.score)
        .addOnSuccessListener {
            Log.d("FishingGame", "Successfully updated player score")
        }
        .addOnFailureListener { e ->
            Log.e("FishingGame", "Error updating player score", e)
        }
}

@Composable
fun FishDisplay(fish: Fish, isReelingComplete: Boolean) {
    val fishImages = mapOf(
        "Salmon" to R.drawable.salmon,
        "Trout" to R.drawable.trout,
        "Bass" to R.drawable.bass,
        "Catfish" to R.drawable.catfish,
        "Carp" to R.drawable.carp,
        "Pike" to R.drawable.pike,
        "Goldfish" to R.drawable.goldfish,
        "Dragonfish" to R.drawable.dragonfish,
        "Great White Shark" to R.drawable.shark
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
                text = "Caught a ${fish.type} weighing ${fish.weight}kg!",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
