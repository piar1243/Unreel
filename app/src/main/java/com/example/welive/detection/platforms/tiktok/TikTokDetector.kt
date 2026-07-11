package com.example.welive.detection.platforms.tiktok

import com.example.welive.detection.ContentDetector
import com.example.welive.detection.ContentSurface
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.Platform
import com.example.welive.detection.WindowSnapshot
import com.example.welive.protection.ProtectedApp

class TikTokDetector : ContentDetector {
    override fun detect(snapshot: WindowSnapshot): DetectionResult {
        val isTikTok = listOf(snapshot.packageName, snapshot.rootPackageName, snapshot.eventPackageName)
            .any { it in ProtectedApp.TIKTOK.packageNames }
        if (!isTikTok) return unknown(snapshot, "TikTok is not active")

        val searchable = snapshot.searchableText
        val messageIdHits = MESSAGE_ID_MARKERS.count { marker ->
            snapshot.viewIds.any { it.lowercase().contains(marker) }
        }
        val messageLabelHits = MESSAGE_LABEL_MARKERS.count(searchable::contains)
        val videoIdHits = VIDEO_ID_MARKERS.count { marker ->
            snapshot.viewIds.any { it.lowercase().contains(marker) }
        }
        val videoLabelHits = VIDEO_LABEL_MARKERS.count(searchable::contains)

        val messagesVisible = messageIdHits >= 2 ||
            (messageIdHits >= 1 && messageLabelHits >= 1) ||
            messageLabelHits >= 3
        if (messagesVisible) {
            return DetectionResult(
                platform = Platform.TIKTOK,
                surface = ContentSurface.TIKTOK_MESSAGES,
                confidence = 0.94f,
                packageName = snapshot.packageName,
                reasons = listOf("TikTok inbox or conversation markers are visible"),
                recommendedAction = InterventionAction.NONE
            )
        }

        val shortFormVisible = videoIdHits >= 2 ||
            (videoIdHits >= 1 && videoLabelHits >= 2) ||
            videoLabelHits >= 4
        if (shortFormVisible) {
            return DetectionResult(
                platform = Platform.TIKTOK,
                surface = ContentSurface.TIKTOK_SHORTFORM,
                confidence = 0.92f,
                packageName = snapshot.packageName,
                reasons = listOf("TikTok vertical-video controls are visible"),
                recommendedAction = InterventionAction.BLOCK_AND_RETURN
            )
        }

        return DetectionResult(
            platform = Platform.TIKTOK,
            surface = ContentSurface.UNKNOWN,
            confidence = 0.25f,
            packageName = snapshot.packageName,
            reasons = listOf("TikTok surface is inconclusive; leaving it unblocked"),
            recommendedAction = InterventionAction.NONE
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
        val MESSAGE_ID_MARKERS = listOf(
            "inbox", "message", "chat", "conversation", "im_", "dm_"
        )
        val MESSAGE_LABEL_MARKERS = listOf(
            "inbox", "messages", "new message", "send a message", "chat", "conversation"
        )
        val VIDEO_ID_MARKERS = listOf(
            "video_view", "video_player", "feed_item", "like", "comment", "share", "aweme"
        )
        val VIDEO_LABEL_MARKERS = listOf(
            "for you", "following", "like video", "comments", "share", "sound", "use this sound"
        )
    }
}
