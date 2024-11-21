package com.example.comrasmusfishingwithfriends

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController

@Composable
fun StartScreen(
    onGoFishingClick: () -> Unit,
    onGoFishTogetherClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    playerScore: Int,
    currentPlayer: Player,
    navController: NavHostController
) {
    LaunchedEffect(Unit) {
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

    val backgroundImage = painterResource(id = R.drawable.bakgrunden)
    val tier = getTier(playerScore)
    var showInfoMenu by remember { mutableStateOf(false) }
    var showAchievementsMenu by remember { mutableStateOf(false) }
    var showEquipmentMenu by remember { mutableStateOf(false) }
    var showCatalogMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = backgroundImage,
            contentDescription = "Background Image",
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Crop
        )

        // Menyrad högst upp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Övre rad med spelinfo, prestationer och utrustning
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Info-knapp
                MenuButton(
                    icon = R.drawable.ic_info,
                    text = "Spelinfo",
                    onClick = { showInfoMenu = !showInfoMenu }
                )

                // Achievements-knapp
                MenuButton(
                    icon = R.drawable.ic_star,
                    text = "Prestationer",
                    onClick = { showAchievementsMenu = !showAchievementsMenu }
                )

                // Utrustning-knapp
                MenuButton(
                    icon = R.drawable.rod,
                    text = "Utrustning",
                    onClick = { showEquipmentMenu = !showEquipmentMenu }
                )
            }

            // Fiskekatalog på egen rad under
            Spacer(modifier = Modifier.height(8.dp))
            MenuButton(
                icon = R.drawable.ic_menu,
                text = "Fiskekatalog",
                onClick = { showCatalogMenu = !showCatalogMenu }
            )
        }

        // Huvudinnehåll i mitten (utan fiskekatalog)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = tier.image),
                contentDescription = "Tier Icon",
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 8.dp)
            )

            Text(
                text = tier.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Fishing with Friends",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                modifier = Modifier.padding(bottom = 16.dp),
                onClick = {
                    navController.navigate("map_selection_screen")
                }
            ) {
                Text(text = "Go Fishing")
            }

            Button(
                onClick = onGoFishTogetherClick
            ) {
                Text(text = "Go Fish Together")
            }

            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = onLeaderboardClick
            ) {
                Text(text = "Leaderboard")
            }
        }

        // Achievements-meny
        if (showAchievementsMenu) {
            AchievementsMenu(
                currentPlayer = currentPlayer,
                onClose = { showAchievementsMenu = false }
            )
        }

        // Utrustning-meny
        if (showEquipmentMenu) {
            EquipmentMenu(
                currentPlayer = currentPlayer,
                onClose = { showEquipmentMenu = false }
            )
        }

        // Katalog-meny
        if (showCatalogMenu) {
            FishCatalogMenu(
                currentPlayer = currentPlayer,
                onClose = { showCatalogMenu = false }
            )
        }

        if (showInfoMenu) {
            InfoMenu(onClose = { showInfoMenu = false })
        }
    }
}

@Composable
private fun MenuButton(
    icon: Int,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AchievementsMenu(
    currentPlayer: Player,
    onClose: () -> Unit
) {
    PopupMenu {
        Text(
            text = "Prestationer",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Achievements.allAchievements.forEach { achievement ->
            val isUnlocked = currentPlayer.achievements.find { it.id == achievement.id }?.isUnlocked ?: false
            AchievementRow(
                achievement = achievement,
                isUnlocked = isUnlocked
            )
        }

        CloseButton(onClose)
    }
}

@Composable
private fun EquipmentMenu(
    currentPlayer: Player,
    onClose: () -> Unit
) {
    PopupMenu {
        Text(
            text = "Fiskeutrustning",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        EquipmentShop.availableRods.forEach { rod ->
            val isUnlocked = currentPlayer.unlockedRods.contains(rod.id)
            EquipmentRow(
                rod = rod,
                isUnlocked = isUnlocked,
                isSelected = rod.id == currentPlayer.currentRodId
            )
        }

        CloseButton(onClose)
    }
}

@Composable
fun FishCatalogMenu(
    currentPlayer: Player,
    onClose: () -> Unit
) {
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        FirebaseManager.loadPlayer(currentPlayer.playerId)?.let { loadedPlayer ->
            currentPlayer.fishCatalog = loadedPlayer.fishCatalog
            Log.d("FishCatalog", "Loaded catalog: ${loadedPlayer.fishCatalog.caughtFish}")
        }
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Fiskkatalog",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Visa fiskar för varje plats
                FishCatalog.allFishTypes.forEach { location ->
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    location.fish.forEach { fishType ->
                        val entry = currentPlayer.fishCatalog.catalog[fishType]
                        val isCaught = currentPlayer.fishCatalog.caughtFish.containsKey(fishType)
                        
                        Log.d("FishCatalog", "Fish: $fishType, Caught: $isCaught, Entry: $entry")

                        FishCatalogDetailRow(
                            fishType = fishType,
                            entry = entry,
                            isCaught = isCaught
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = { 
                        refreshTrigger += 1
                        onClose() 
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Stäng")
                }
            }
        }
    }
}

@Composable
private fun FishCatalogDetailRow(
    fishType: String,
    entry: FishEntry?,
    isCaught: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCaught) Color(0xFF2D2D2D) else Color(0xFF1A1A1A)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val fishImage = if (isCaught) {
                    when (fishType) {
                        "Trout" -> R.drawable.trout
                        "Bass" -> R.drawable.bass
                        "Catfish" -> R.drawable.catfish
                        "Carp" -> R.drawable.carp
                        "Pike" -> R.drawable.pike
                        "Goldfish" -> R.drawable.goldfish
                        
                        "Tuna" -> R.drawable.tuna
                        "Swordfish" -> R.drawable.swordfish
                        "Shark" -> R.drawable.shark
                        "Dolphinfish" -> R.drawable.dolphinfish
                        "Marlin" -> R.drawable.marlin
                        "Octopus" -> R.drawable.octopus
                        "Giant Squid" -> R.drawable.giantsquid
                        
                        "Salmon" -> R.drawable.salmon
                        "Rainbow Trout" -> R.drawable.rainbowtrout
                        "Sturgeon" -> R.drawable.sturgeon
                        "Eel" -> R.drawable.eel
                        "Arctic Char" -> R.drawable.arcticchar
                        "Grayling" -> R.drawable.grayling
                        
                        "Kraken" -> R.drawable.kraken_phase1
                        "Leviathan" -> R.drawable.leviathan
                        
                        else -> R.drawable.bones
                    }
                } else {
                    R.drawable.bones
                }

                Image(
                    painter = painterResource(id = fishImage),
                    contentDescription = fishType,
                    modifier = Modifier.size(40.dp)
                )

                Column {
                    Text(
                        text = if (isCaught) fishType else "Okänd fisk",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (isCaught && entry != null) {
                        Text(
                            text = entry.location,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (isCaught && entry != null) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Fångad: ${entry.timesCaught} gånger",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Största vikt: ${entry.largestWeight.toInt()} kg",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Totala poäng: ${entry.totalPoints}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun FishCatalogDialog(player: Player, onClose: () -> Unit) {
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        // Ladda spelarens uppdaterade data från Firebase
        FirebaseManager.loadPlayer(player.playerId)?.let { loadedPlayer ->
            player.fishCatalog = loadedPlayer.fishCatalog
        }
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Fiskkatalog",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Gruppera fiskar efter plats
                FishCatalog.allFishTypes.forEach { location ->
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    location.fish.forEach { fishType ->
                        val isCaught = player.fishCatalog.isFishCaught(fishType)
                        val entry = if (isCaught) player.fishCatalog.catalog[fishType] else null

                        FishCatalogEntry(
                            fishType = fishType,
                            isCaught = isCaught,
                            entry = entry
                        )
                    }
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 16.dp)
                ) {
                    Text("Stäng")
                }
            }
        }
    }
}

@Composable
private fun FishCatalogEntry(
    fishType: String,
    isCaught: Boolean,
    entry: FishEntry?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D2D)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visa fisknamn och status
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isCaught) fishType else "???",
                    color = Color.White.copy(alpha = if (isCaught) 1f else 0.5f),
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (isCaught && entry != null) {
                    Text(
                        text = "Fångad: ${entry.timesCaught} gånger | Största vikt: ${entry.largestWeight}kg | Poäng: ${entry.totalPoints}",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun getFishImageResource(fishType: String): Int {
    return when (fishType) {
        "Trout" -> R.drawable.trout
        "Bass" -> R.drawable.bass
        "Catfish" -> R.drawable.catfish
        "Carp" -> R.drawable.carp
        "Pike" -> R.drawable.pike
        "Goldfish" -> R.drawable.goldfish
        "Tuna" -> R.drawable.tuna
        "Swordfish" -> R.drawable.swordfish
        "Shark" -> R.drawable.shark
        "Dolphinfish" -> R.drawable.dolphinfish
        "Marlin" -> R.drawable.marlin
        "Octopus" -> R.drawable.octopus
        "Giant Squid" -> R.drawable.giantsquid
        "Salmon" -> R.drawable.salmon
        "Rainbow Trout" -> R.drawable.rainbowtrout
        "Brown Trout" -> R.drawable.trout
        "Sturgeon" -> R.drawable.sturgeon
        "Eel" -> R.drawable.eel
        "Arctic Char" -> R.drawable.arcticchar
        "Grayling" -> R.drawable.grayling
        "Perch" -> R.drawable.crocodile
        else -> R.drawable.bones
    }
}

@Composable
private fun PopupMenu(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .width(300.dp)
            .wrapContentHeight()
            .zIndex(Float.MAX_VALUE),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun AchievementRow(
    achievement: Achievement,
    isUnlocked: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = achievement.iconResId),
                contentDescription = achievement.title,
                modifier = Modifier
                    .size(32.dp)
                    .alpha(if (isUnlocked) 1f else 0.5f)
            )
            Column {
                Text(
                    text = achievement.title,
                    color = Color.White.copy(alpha = if (isUnlocked) 1f else 0.5f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = achievement.description,
                    color = Color.White.copy(alpha = if (isUnlocked) 0.7f else 0.3f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Icon(
            painter = painterResource(
                id = if (isUnlocked) R.drawable.ic_check else R.drawable.ic_lock
            ),
            contentDescription = if (isUnlocked) "Unlocked" else "Locked",
            tint = if (isUnlocked) Color.Green else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun EquipmentRow(
    rod: FishingRodType,
    isUnlocked: Boolean,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = rod.imageResId),
                contentDescription = rod.name,
                modifier = Modifier
                    .size(32.dp)
                    .alpha(if (isUnlocked) 1f else 0.5f)
            )
            Column {
                Text(
                    text = rod.name,
                    color = Color.White.copy(alpha = if (isUnlocked) 1f else 0.5f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = rod.description,
                    color = Color.White.copy(alpha = if (isUnlocked) 0.7f else 0.3f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (!isUnlocked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${rod.cost} poäng",
                    color = Color.Yellow,
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = "Locked",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = "Selected",
                tint = Color.Green,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    Button(
        onClick = onClose,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text("Stäng")
    }
}

fun getTier(score: Int): Tier {
    return when {
        score >= 1000 -> Tier("Platinum", R.drawable.tier_platinum, "You are a Platinum tier player!")
        score >= 500 -> Tier("Gold", R.drawable.tier_gold, "You are a Gold tier player!")
        score >= 100 -> Tier("Silver", R.drawable.tier_silver, "You are a Silver tier player!")
        score >= 50 -> Tier("Bronze", R.drawable.tier_bronze, "You are a Bronze tier player!")
        else -> Tier("No Tier", R.drawable.ic_star, "No tier, but you are on your way!")
    }
}

data class Tier(
    val name: String,
    val image: Int,
    val description: String
)



@Composable
private fun InfoMenu(onClose: () -> Unit) {
    PopupMenu {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Spelregler",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Grundregler
            Text(
                text = "Grundregler",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "1. Klicka på rullen för att kasta ut linan",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "2. När fisken nappar, håll in rullen för att dra in fisken",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Poängsystem
            Text(
                text = "Poängsystem",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "• Varje fisk ger olika mycket poäng",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "• Klara dagliga utmaningar för bonuspoäng",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Väder och fiske
            Text(
                text = "Väder och fiske",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "• Soligt: Normala fiskeförhållanden",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "• Regnigt: Ökad chans att fånga fisk",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "• Stormigt: Minskad chans att fånga fisk",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "• Molnigt: Något ökad chans att fånga fisk",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CloseButton(onClose)
        }
    }
}

data class FishCatalogEntry(
    val name: String,
    val points: String,
    val iconId: Int
)

@Composable
private fun FishCatalogRow(
    fish: FishCatalogEntry,
    isCaught: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = fish.iconId),
                contentDescription = fish.name,
                modifier = Modifier
                    .size(32.dp)
                    .alpha(if (isCaught) 1f else 0.5f)
            )
            Column {
                Text(
                    text = fish.name,
                    color = Color.White.copy(alpha = if (isCaught) 1f else 0.5f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = fish.points,
                    color = Color.White.copy(alpha = if (isCaught) 0.7f else 0.3f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (isCaught) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = "Caught",
                tint = Color.Green,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ChallengeStatsRow(
    title: String,
    count: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = count,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
