package com.example.fitness.timer

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple app-wide stopwatch that counts up in milliseconds.
 * It's implemented as a singleton object so its state survives composable recompositions.
 */
object StopwatchService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs = _elapsedMs.asStateFlow()

    private var lastStartTime = 0L // system millis when last started/resumed

    val isRunning: Boolean
        get() = job != null

    fun start() {
        // start from zero
        resetInternal()
        resumeInternal()
    }

    fun resume() {
        if (isRunning) return
        resumeInternal()
    }

    private fun resumeInternal() {
        lastStartTime = System.currentTimeMillis()
        job = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                _elapsedMs.value = (_elapsedMs.value.coerceAtLeast(0L) + (now - lastStartTime))
                lastStartTime = now
                delay(200L)
            }
        }
    }

    fun pause() {
        if (job == null) return
        job?.cancel()
        job = null
    }

    /**
     * Stop and return the elapsed ms then reset to zero.
     */
    fun stopAndReset(): Long {
        pause()
        val elapsed = _elapsedMs.value
        resetInternal()
        return elapsed
    }

    private fun resetInternal() {
        _elapsedMs.value = 0L
        lastStartTime = 0L
    }

    fun release() {
        scope.cancel()
    }
}

