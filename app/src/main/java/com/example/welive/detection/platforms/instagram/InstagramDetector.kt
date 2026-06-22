package com.example.welive.detection.platforms.instagram

import com.example.welive.detection.ContentDetector
import com.example.welive.detection.ContentSurface
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.Platform
import com.example.welive.detection.WindowSnapshot

class InstagramDetector(
    private val classifier: InstagramScreenClassifier = InstagramScreenClassifier(),
    private val blockingThreshold: Float = 0.82f
) : ContentDetector {
    override fun detect(snapshot: WindowSnapshot): DetectionResult {
        if (snapshot.packageName != InstagramPackageConfig.PACKAGE_NAME) {
            return DetectionResult(
                platform = Platform.UNKNOWN,
                surface = ContentSurface.UNKNOWN,
                confidence = 0f,
                packageName = snapshot.packageName,
                reasons = listOf("Package is not Instagram"),
                recommendedAction = InterventionAction.NONE
            )
        }

        val classification = classifier.classify(snapshot)
        val action = when {
            classification.surface in INSTAGRAM_REELS_SURFACES &&
                classification.confidence >= blockingThreshold -> InterventionAction.BLOCK_AND_RETURN
            classification.surface == ContentSurface.INSTAGRAM_SEARCH_REELS_GRID &&
                classification.confidence >= blockingThreshold -> InterventionAction.BLOCK
            else -> InterventionAction.NONE
        }

        return DetectionResult(
            platform = Platform.INSTAGRAM,
            surface = classification.surface,
            confidence = classification.confidence,
            packageName = snapshot.packageName,
            reasons = classification.reasons,
            recommendedAction = action
        )
    }

    private companion object {
        val INSTAGRAM_REELS_SURFACES = setOf(
            ContentSurface.INSTAGRAM_REELS,
            ContentSurface.INSTAGRAM_REELS_FROM_FRIEND
        )
    }
}
