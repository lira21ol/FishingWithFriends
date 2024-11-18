package com.example.comrasmusfishingwithfriends
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlin.random.Random


class Reeling {
    var rollProgress by mutableStateOf(0f)
    var isReeling by mutableStateOf(false)


    fun startReeling() {
        isReeling = true
        rollProgress = 0f
    }


    fun reelIn() {
        if (rollProgress < 1f) {

            val resetChance = Random.nextFloat()


            if (resetChance < 0.2f) {

                rollProgress = 0f
            } else {

                rollProgress += 0.1f
            }
        }
    }

    fun stopReeling() {
        isReeling = false
    }


    fun isComplete() = rollProgress >= 1f
}