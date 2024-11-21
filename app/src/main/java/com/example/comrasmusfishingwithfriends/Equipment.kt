package com.example.comrasmusfishingwithfriends

data class FishingRodType(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val catchModifier: Float,
    val imageResId: Int,
    var isUnlocked: Boolean = false
)

object EquipmentShop {
    val availableRods = listOf(
        FishingRodType(
            "basic_rod",
            "Grundspö",
            "Ett enkelt fiskespö för nybörjare",
            0,
            1.0f,
            R.drawable.rod,
            true
        ),
        FishingRodType(
            "pro_rod",
            "Proffsspö",
            "Ett bättre spö för erfarna fiskare",
            500,
            1.5f,
            R.drawable.golden_rod,
            false
        ),
        FishingRodType(
            "knife",
            "Killerknife",
            "Kniven för att döda kraken",
            1000,
            2.0f,
            R.drawable.knife,
            false
        )
    )
} 