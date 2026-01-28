package com.example.fitness.timer

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs = _remainingMs.asStateFlow()

    fun start(durationMs: Long, tickMs: Long = 1000L, onFinish: (() -> Unit)? = null) {
        stop()
        _remainingMs.value = durationMs
        job = scope.launch {
            var left = durationMs
            while (isActive && left > 0) {
                delay(tickMs)
                left -= tickMs
                _remainingMs.value = left.coerceAtLeast(0L)
            }
            if (left <= 0) onFinish?.invoke()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun release() {
        scope.cancel()
    }
}

