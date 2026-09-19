package com.example.comrasmusfishingwithfriends

data class FishingRodType(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val catchModifier: Float,
    val imageResId: Int,
    val modelPath: String, // Path to 3D model in assets
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
            "models/Fishing Rod.glb",
            true
        ),
        FishingRodType(
            "pro_rod",
            "Proffsspö",
            "Ett bättre spö för erfarna fiskare",
            500,
            1.5f,
            R.drawable.golden_rod,
            "models/Fishing Rod-aOabqWh68m.glb",
            false
        ),
        FishingRodType(
            "carbon_rod",
            "Kolfiberspö",
            "Lätt och extremt starkt spö",
            750,
            1.8f,
            R.drawable.rodstruggling,
            "models/Fishing Rod-9AOHhRPHE7.glb",
            false
        ),
        FishingRodType(
            "ultra_rod",
            "Ultraspö",
            "Det ultimata fiskespöt",
            1500,
            2.5f,
            R.drawable.rod,
            "models/Fishing Rod-lDlWQjn9Zg.glb",
            false
        ),
        FishingRodType(
            "knife",
            "Killerknife",
            "Kniven för att döda kraken",
            1000,
            2.0f,
            R.drawable.knife,
            "models/Axe.glb",
            false
        )
    )
}
