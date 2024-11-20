package com.example.comrasmusfishingwithfriends
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.*

class Reeling {
    var isReeling by mutableStateOf(false)
    var rollProgress by mutableStateOf(0f)
    var currentPhase by mutableStateOf(0)
    var phaseProgress by mutableStateOf(0f)
    var isHolding by mutableStateOf(false)
    private var lastUpdateTime = 0L
    
    companion object {
        val phases = listOf(
            Phase(color = Color(0xFF4CAF50), holdSpeed = 0.15f, clickSpeed = 0.3f),  // Grön
            Phase(color = Color(0xFFFFEB3B), holdSpeed = 0.12f, clickSpeed = 0.25f),  // Gul
            Phase(color = Color(0xFFFF9800), holdSpeed = 0.1f, clickSpeed = 0.2f),    // Orange
            Phase(color = Color(0xFFFF5252), holdSpeed = 0.08f, clickSpeed = 0.15f)   // Röd
        )
    }

    fun startReeling() {
        isReeling = true
        currentPhase = 0
        phaseProgress = 0f
        rollProgress = 0f
        lastUpdateTime = System.currentTimeMillis()
    }

    fun stopReeling() {
        isReeling = false
        currentPhase = 0
        phaseProgress = 0f
        rollProgress = 0f
        isHolding = false
        lastUpdateTime = 0L
    }

    fun isComplete(): Boolean {
        return currentPhase >= phases.size && phaseProgress >= 1f
    }

    fun completePhase(): Boolean {
        if (phaseProgress >= 1f) {
            phaseProgress = 0f
            if (currentPhase < phases.size - 1) {
                currentPhase++
                return false
            }
            return true
        }
        return false
    }

    fun updateProgress(isHoldingClick: Boolean, elapsedTime: Long = 0L): Boolean {
        if (currentPhase >= phases.size) return true
        
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000f // Konvertera till sekunder
        lastUpdateTime = currentTime

        if (deltaTime > 0) {
            val currentPhaseData = phases[currentPhase]
            
            if (isHoldingClick) {
                // Långsammare ökning när man håller in
                phaseProgress += currentPhaseData.holdSpeed * deltaTime
            } else {
                // Snabbare ökning vid klick
                phaseProgress += currentPhaseData.clickSpeed
            }
            
            // Begränsa progress till mellan 0 och 1
            phaseProgress = phaseProgress.coerceIn(0f, 1f)
            
            // Uppdatera total progress
            rollProgress = (currentPhase + phaseProgress) / phases.size
        }
        
        return phaseProgress >= 1f
    }

    fun reelIn(scope: CoroutineScope) {
        if (!isComplete()) {
            updateProgress(false)
        }
    }

    fun reset() {
        currentPhase = 0
        phaseProgress = 0f
        rollProgress = 0f
        isHolding = false
    }

    fun completeAllPhases() {
        currentPhase = phases.size - 1
        phaseProgress = 1f
        rollProgress = 1f
    }
}

data class Phase(
    val color: Color,
    val holdSpeed: Float,  // Hastighet när knappen hålls in
    val clickSpeed: Float  // Hastighet vid klick
)