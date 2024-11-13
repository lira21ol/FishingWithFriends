package com.example.comrasmusfishingwithfriends
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

class Reeling {
    var rollProgress by mutableStateOf(0f)
    var isReeling by mutableStateOf(false)


    fun startReeling() {
        isReeling = true
        rollProgress = 0f
    }


    fun reelIn() {
        if (rollProgress < 1f) {
            rollProgress += 0.1f
        }
    }


    fun stopReeling() {
        isReeling = false
    }


    fun isComplete() = rollProgress >= 1f
}