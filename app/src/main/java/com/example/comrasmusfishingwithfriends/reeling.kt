package com.example.comrasmusfishingwithfriends
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlin.random.Random

class Reeling {
    var rollProgress by mutableStateOf(0f)
    var isReeling by mutableStateOf(false)
    private var reelingJob: Job? = null

    fun startReeling() {
        isReeling = true
        rollProgress = 0f
    }

    fun reelIn(coroutineScope: CoroutineScope) {
        if (rollProgress < 1f) {
            // Avbryt tidigare jobb om det finns
            reelingJob?.cancel()
            
            // Starta nytt jobb för kontinuerlig ökning
            reelingJob = coroutineScope.launch {
                while (isActive && rollProgress < 1f) {
                    val resetChance = Random.nextFloat()
                    if (resetChance < 0.2f) {
                        rollProgress = 0f
                    } else {
                        // Öka med 10% per sekund (0.1 per sekund)
                        rollProgress = (rollProgress + 0.1f).coerceAtMost(1f)
                    }
                    delay(1000) // Vänta en sekund innan nästa ökning
                }
            }
        }
    }

    fun stopReeling() {
        isReeling = false
        reelingJob?.cancel()  // Avbryt bara jobbet, behåll progress
        reelingJob = null
    }

    fun isComplete() = rollProgress >= 1f
}