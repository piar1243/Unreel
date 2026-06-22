package com.example.welive.diagnostics

import com.example.welive.detection.DetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object DetectionDiagnostics {
    private const val MaxResults = 40

    private val _recentResults = MutableStateFlow<List<DetectionResult>>(emptyList())
    val recentResults: StateFlow<List<DetectionResult>> = _recentResults

    fun record(result: DetectionResult) {
        _recentResults.update { current ->
            (listOf(result) + current).take(MaxResults)
        }
    }
}
