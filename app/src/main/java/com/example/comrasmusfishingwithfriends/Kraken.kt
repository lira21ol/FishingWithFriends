class Kraken(
    var currentPhase: Int = 1,
    var health: Int = 1000,
    var isVulnerable: Boolean = false
) {
    companion object {
        const val TOTAL_PHASES = 3
        const val INITIAL_HEALTH = 1000
        const val PHASE_THRESHOLD = INITIAL_HEALTH / 3
    }

    fun getDamageMultiplier(): Float {
        return when (currentPhase) {
            1 -> 1.0f
            2 -> 0.7f
            3 -> 0.4f
            else -> 1.0f
        }
    }

    fun getRequiredSlices(): Int {
        return when (currentPhase) {
            1 -> 1
            2 -> 2
            3 -> 3
            else -> 1
        }
    }

    fun takeDamage(damage: Int) {
        if (isVulnerable) {
            val adjustedDamage = (damage * getDamageMultiplier()).toInt()
            health = maxOf(0, health - adjustedDamage)
            
            // Kontrollera fasövergångar
            if (health <= INITIAL_HEALTH - (currentPhase * PHASE_THRESHOLD) && currentPhase < TOTAL_PHASES) {
                currentPhase++
                isVulnerable = false
            }
        }
    }

    fun isDead() = health <= 0
} 