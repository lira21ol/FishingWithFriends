package com.example.comrasmusfishingwithfriends

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconResId: Int,
    val requiredCount: Int,
    var currentCount: Int = 0,
    var isUnlocked: Boolean = false
)

object Achievements {
    val allAchievements = listOf(
        Achievement(
            "first_catch",
            "Första fångsten",
            "Fånga din första fisk",
            R.drawable.ic_star,
            1
        ),
        Achievement(
            "shark_hunter",
            "Hajjägaren",
            "Fånga din första haj",
            R.drawable.shark,
            1
        ),
        Achievement(
            "master_fisher",
            "Mästerfiskare",
            "Fånga 100 fiskar",
            R.drawable.ic_star,
            100
        ),
        Achievement(
            "point_collector",
            "Poängsamlare",
            "Samla 1000 poäng",
            R.drawable.ic_star,
            1000
        )
    )
} 