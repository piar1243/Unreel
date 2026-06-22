package com.example.welive.training

import com.example.welive.detection.WindowSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TrainingCaptureState {
    private val _latestInstagramSnapshot = MutableStateFlow<WindowSnapshot?>(null)
    val latestInstagramSnapshot: StateFlow<WindowSnapshot?> = _latestInstagramSnapshot

    fun recordInstagramSnapshot(snapshot: WindowSnapshot) {
        _latestInstagramSnapshot.value = snapshot
    }
}
