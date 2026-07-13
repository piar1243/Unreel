package com.example.welive.detection.platforms.linkedin

import com.example.welive.detection.ContentDetector
import com.example.welive.detection.ContentSurface
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.Platform
import com.example.welive.detection.WindowSnapshot
import com.example.welive.protection.ProtectedApp

class LinkedInDetector : ContentDetector {
    override fun detect(snapshot: WindowSnapshot): DetectionResult {
        val linkedInActive = listOf(
            snapshot.packageName,
            snapshot.rootPackageName,
            snapshot.eventPackageName
        ).any { it in ProtectedApp.LINKEDIN.packageNames }
        if (!linkedInActive) return unknown(snapshot, "LinkedIn is not active")

        val ids = snapshot.viewIds.map(String::lowercase)
        val labels = (snapshot.texts + snapshot.contentDescriptions).map(String::lowercase)
        val selectedVideoTab = snapshot.nodeFeatures.any { feature ->
            feature.isVisibleToUser && feature.isSelected &&
                listOfNotNull(feature.text, feature.contentDescription)
                    .any { it.trim().equals("video", true) || it.trim().equals("videos", true) }
        }
        val videoResourceHits = VIDEO_RESOURCE_MARKERS.count { marker -> ids.any { it.contains(marker) } }
        val immersiveLabelHit = labels.any { label -> IMMERSIVE_LABEL_MARKERS.any(label::contains) }
        val actionHits = ACTION_LABEL_GROUPS.count { alternatives ->
            labels.any { label -> alternatives.any { marker -> label == marker || label.startsWith("$marker,") } } ||
                ids.any { id -> alternatives.any(id::contains) }
        }
        val largeMediaNode = snapshot.nodeFeatures.any { feature ->
            feature.isVisibleToUser &&
                (feature.className?.contains("SurfaceView", true) == true ||
                    feature.className?.contains("TextureView", true) == true ||
                    feature.viewId?.contains("video", true) == true) &&
                (feature.boundsBottom - feature.boundsTop) >= 900
        }
        val strongVideoContext = selectedVideoTab || videoResourceHits >= 2 ||
            (videoResourceHits >= 1 && immersiveLabelHit)
        val shortFormVisible = strongVideoContext && actionHits >= 3 &&
            (largeMediaNode || selectedVideoTab || videoResourceHits >= 2)

        val reasons = buildList {
            if (selectedVideoTab) add("Selected LinkedIn Video tab is visible")
            if (videoResourceHits > 0) add("$videoResourceHits LinkedIn video-feed resource markers are visible")
            if (immersiveLabelHit) add("Immersive video label is visible")
            if (actionHits > 0) add("$actionHits short-video action groups are visible")
            if (largeMediaNode) add("Large video player structure is visible")
        }

        return DetectionResult(
            platform = Platform.LINKEDIN,
            surface = if (shortFormVisible) ContentSurface.LINKEDIN_SHORTFORM else ContentSurface.UNKNOWN,
            confidence = if (shortFormVisible) 0.96f else 0.25f,
            packageName = snapshot.packageName,
            reasons = reasons.ifEmpty { listOf("LinkedIn short-video signature is not present") },
            recommendedAction = if (shortFormVisible) {
                InterventionAction.BLOCK_AND_RETURN
            } else {
                InterventionAction.NONE
            }
        )
    }

    private fun unknown(snapshot: WindowSnapshot, reason: String) = DetectionResult(
        platform = Platform.UNKNOWN,
        surface = ContentSurface.UNKNOWN,
        confidence = 0f,
        packageName = snapshot.packageName,
        reasons = listOf(reason),
        recommendedAction = InterventionAction.NONE
    )

    private companion object {
        val VIDEO_RESOURCE_MARKERS = setOf(
            "video_feed", "vertical_video", "immersive_video", "video_player", "video_surface"
        )
        val IMMERSIVE_LABEL_MARKERS = setOf("video feed", "full screen video", "fullscreen video")
        val ACTION_LABEL_GROUPS = listOf(
            setOf("like", "react"),
            setOf("comment", "comments"),
            setOf("repost"),
            setOf("send", "share"),
            setOf("more", "more options")
        )
    }
}
