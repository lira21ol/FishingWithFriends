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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.times
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.random.Random

// Lägg till dessa högst upp i filen, utanför KrakenBossScreen
enum class AttackType {
    INK,
    TENTACLE
}

data class InkSpot(
    val id: Int,
    val position: Offset,
    var isActive: Boolean = true
)

// Lägg till denna funktion utanför KrakenBossScreen composable
fun getRandomAttack(): AttackType {
    return if (Random.nextFloat() < 0.5f) {
        AttackType.INK
    } else {
        AttackType.TENTACLE
    }
}

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
    var showTentacle by remember { mutableStateOf(false) }
    var sliceCount by remember { mutableStateOf(0) }
    var sliceStartTime by remember { mutableStateOf(0L) }
    var isGameOver by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    var shouldRotate by remember { mutableStateOf(false) }
    
    // Gesture state
    var sliceProgress by remember { mutableStateOf(0f) }
    var currentSliceX by remember { mutableStateOf(0f) }

    var slashProgress by remember { mutableFloatStateOf(0f) }
    var isSlashing by remember { mutableStateOf(false) }
    var showCutTentacle by remember { mutableStateOf(false) }
    
    // Lägg till nya state-variabler
    var showSuccessCheck by remember { mutableStateOf(false) }
    
    var isInkAttack by remember { mutableStateOf(false) }
    var inkSpots by remember { mutableStateOf(listOf<InkSpot>()) }
    var inkAttackTimer by remember { mutableIntStateOf(0) }
    
    // Lägg till efter andra state-variabler
    var currentAttackType by remember { mutableStateOf<AttackType?>(null) }
    
    // Lägg till en ny state-variabel för attack-meddelanden
    var attackMessage by remember { mutableStateOf("") }
    var showAttackMessage by remember { mutableStateOf(false) }
    
    // Funktion för att visa attack-meddelande
    fun showAttackMessage(message: String) {
        attackMessage = message
        showAttackMessage = true
    }
    
    // Timer för slicing
    LaunchedEffect(showTentacle) {
        if (showTentacle) {
            showAttackMessage = true
            sliceStartTime = System.currentTimeMillis()
            while (showTentacle && !isGameOver) {
                delay(100)
                if (System.currentTimeMillis() - sliceStartTime > 5000) {
                    isGameOver = true
                    showTentacle = false
                }
            }
            showAttackMessage = false
        }
    }

    var showAttackButton by remember { mutableStateOf(true) }

    // Lägg till dessa state-variabler i början av KrakenBossScreen
    var attackProgress by remember { mutableFloatStateOf(0f) }
    var isChargingAttack by remember { mutableStateOf(false) }

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
            // Visa meddelande för tentakel-attack
            showAttackMessage("Kraken slår med sin tentakel - Dra åt höger för att hugga av den!")
            
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
                                    
                                    scope.launch {
                                        delay(1000)
                                        showCutTentacle = false
                                        showAttackButton = true
                                    }
                                }
                            }
                        }
                )
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

        // Lägg till inuti den yttersta Box, efter andra element
        if (isInkAttack) {
            // Overlay för bläckattacken
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // Timer och räknare högst upp
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tid kvar: ${10 - inkAttackTimer} sekunder",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Aktiva bläckfläckar: ${inkSpots.count { it.isActive }}/2",
                        color = when {
                            inkSpots.count { it.isActive } > 2 -> Color.Red
                            inkSpots.count { it.isActive } == 2 -> Color(0xFFFFAA00) // Orange varningsfärg
                            else -> Color.White
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Spelområde för bläckfläckar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp)
                        .padding(top = 300.dp) // Säkerställ att detta område börjar under all infotext
                ) {
                    inkSpots.forEach { inkSpot ->
                        if (inkSpot.isActive) {
                            Image(
                                painter = painterResource(id = R.drawable.ink_splat),
                                contentDescription = "Bläckfläck",
                                modifier = Modifier
                                    .size(100.dp) // Något mindre storlek för bättre spelbarhet
                                    .offset(
                                        x = inkSpot.position.x.dp,
                                        y = (inkSpot.position.y - 300f).dp // Justera y-position relativt till spelområdet
                                    )
                                    .clickable {
                                        inkSpots = inkSpots.map {
                                            if (it.id == inkSpot.id) it.copy(isActive = false)
                                            else it
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        // Lägg till attack-meddelande överst i Box
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Existerande innehåll...

            // Attack-meddelande
            AnimatedVisibility(
                visible = showAttackMessage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = attackMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Attack-knapp och progress bar
            if (!isInkAttack && !showTentacle && showAttackButton) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(attackProgress)
                                .background(
                                    when {
                                        attackProgress < 0.5f -> Color(0xFF4CAF50)
                                        attackProgress < 0.8f -> Color(0xFFFFEB3B)
                                        else -> Color(0xFFFF5252)
                                    }
                                )
                        )
                    }

                    // Kniv-bild istället för knapp
                    Image(
                        painter = painterResource(id = R.drawable.knife),
                        contentDescription = "Attack",
                        modifier = Modifier
                            .size(100.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { pressPosition ->
                                        try {
                                            isChargingAttack = true
                                            scope.launch {
                                                while (isChargingAttack) {
                                                    attackProgress = (attackProgress + 0.05f).coerceIn(0f, 1f)
                                                    if (attackProgress >= 1f) {
                                                        break
                                                    }
                                                    delay(50)
                                                }
                                                
                                                if (attackProgress >= 1f) {
                                                    showAttackButton = false
                                                    showAttackMessage("Kraken förbereder sin attack!")
                                                    delay(1000)
                                                    
                                                    val attack = getRandomAttack()
                                                    when (attack) {
                                                        AttackType.INK -> {
                                                            isInkAttack = true
                                                            inkSpots = emptyList()
                                                            inkAttackTimer = 0
                                                        }
                                                        AttackType.TENTACLE -> {
                                                            showTentacle = true
                                                            kraken.isVulnerable = true
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            awaitRelease()
                                        } finally {
                                            isChargingAttack = false
                                            attackProgress = 0f
                                        }
                                    }
                                )
                            }
                    )
                }
            }

            // Resten av innehållet...
        }
    }



    // Modifiera LaunchedEffect för bläckattacken för att visa attack-knappen efter attacken
    LaunchedEffect(isInkAttack) {
        if (isInkAttack) {
            showAttackMessage("Kraken sprutar bläck - Klicka på bläckfläckarna!")
            inkAttackTimer = 0
            inkSpots = emptyList()
            
            // Definiera ett tydligare spelområde
            val topMargin = 300f  // Ökat utrymme för att säkert komma under all infotext
            val playableHeight = 600f  // Minskat spelområde för bättre kontroll
            val playableWidth = 300f   // Minskat för att hålla fläckarna mer centrerade
            
            while (inkAttackTimer < 10 && !isGameOver) {
                delay(1000)
                inkAttackTimer++
                
                if (Random.nextFloat() < 0.7f) {
                    val numSpots = Random.nextInt(1, 2) // Minskat till max 1 ny fläck åt gången
                    repeat(numSpots) {
                        inkSpots = inkSpots + InkSpot(
                            id = inkSpots.size,
                            position = Offset(
                                // Centrera fläckarna mer på skärmen
                                x = Random.nextFloat() * playableWidth + 150f, // Lägg till offset för centrering
                                y = Random.nextFloat() * playableHeight + topMargin
                            )
                        )
                    }
                }
            }
            
            if (inkSpots.count { it.isActive } > 2) {
                isGameOver = true
            } else {
                kraken.takeDamage(50)
            }
            
            // Återställ för nästa attack
            isInkAttack = false
            inkSpots = emptyList()
            showAttackMessage = false
            showAttackButton = true  // Visa attack-knappen igen
        }
    }

    if (isGameOver) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Game Over!",
                    color = Color.Red,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        navController.navigate("start_screen") {
                            popUpTo("start_screen") { inclusive = true }
                        }
                    }
                ) {
                    Text("Återvänd till start")
                }
            }
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
                        currentPlayer.score += 1000
                        
                        val fishEntry = FishEntry(
                            fishType = "Kraken",
                            timesCaught = 1,
                            largestWeight = 5000.0,
                            totalPoints = 1000,
                            location = "Boss Arena"
                        )
                        
                        currentPlayer.fishCatalog.catalog["Kraken"] = fishEntry
                        currentPlayer.fishCatalog.caughtFish["Kraken"] = 1
                        
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
 