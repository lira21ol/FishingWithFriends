package com.example.comrasmusfishingwithfriends

import kotlin.random.Random

enum class WeatherType {
    SUNNY, RAINY, STORMY, CLOUDY
}

data class Weather(
    val type: WeatherType,
    val fishingModifier: Float, // Påverkar chansen att fånga fisk
    val description: String
)

object WeatherSystem {
    fun getCurrentWeather(): Weather {
        return when (Random.nextInt(4)) {
            0 -> Weather(WeatherType.SUNNY, 1.0f, "Perfekt fiskeväder!")
            1 -> Weather(WeatherType.RAINY, 1.2f, "Regnet lockar fram fisken!")
            2 -> Weather(WeatherType.STORMY, 0.7f, "Stormigt väder gör fisket svårare")
            else -> Weather(WeatherType.CLOUDY, 1.1f, "Molnigt väder är bra för fiske")
        }
    }
} 