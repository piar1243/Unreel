package com.example.welive.detection

interface ContentDetector {
    fun detect(snapshot: WindowSnapshot): DetectionResult
}
