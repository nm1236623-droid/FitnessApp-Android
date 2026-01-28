package com.example.fitness.timer

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class Metronome {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _tick = MutableSharedFlow<Unit>(extraBufferCapacity = 10)
    val tick = _tick.asSharedFlow()

    fun start(bpm: Int) {
        stop()
        val intervalMs = (60_000 / bpm)
        job = scope.launch {
            while (isActive) {
                _tick.tryEmit(Unit)
                delay(intervalMs.toLong())
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

