package com.example.welive.detection.platforms.tiktok

import com.example.welive.detection.ContentSurface
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class TikTokDetectorTest {
    private val detector = TikTokDetector()

    @Test
    fun messageSurfaceIsAllowed() {
        val result = detector.detect(snapshot(
            feature("com.zhiliaoapp.musically:id/inbox", "Inbox"),
            feature("com.zhiliaoapp.musically:id/message_list", "Messages"),
            feature("com.zhiliaoapp.musically:id/chat_input", "Send a message")
        ))

        assertEquals(ContentSurface.TIKTOK_MESSAGES, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun verticalVideoControlsAreBlocked() {
        val result = detector.detect(snapshot(
            feature("com.zhiliaoapp.musically:id/video_view", "For You"),
            feature("com.zhiliaoapp.musically:id/like_button", "Like video"),
            feature("com.zhiliaoapp.musically:id/comment_button", "Comments"),
            feature("com.zhiliaoapp.musically:id/share_button", "Share")
        ))

        assertEquals(ContentSurface.TIKTOK_SHORTFORM, result.surface)
        assertEquals(InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    private fun snapshot(vararg nodes: WindowNodeFeature) = WindowSnapshot(
        packageName = "com.zhiliaoapp.musically",
        rootPackageName = "com.zhiliaoapp.musically",
        eventPackageName = "com.zhiliaoapp.musically",
        eventType = 0,
        texts = nodes.mapNotNull { it.text }.toSet(),
        contentDescriptions = nodes.mapNotNull { it.contentDescription }.toSet(),
        viewIds = nodes.mapNotNull { it.viewId }.toSet(),
        classNames = emptySet(),
        nodeCount = nodes.size,
        scrollableNodeCount = 1,
        nodeFeatures = nodes.toList(),
        isMusicActive = true
    )

    private fun feature(id: String, description: String) = WindowNodeFeature(
        text = null,
        contentDescription = description,
        viewId = id,
        className = "android.view.View",
        isClickable = true,
        isScrollable = false,
        isVisibleToUser = true,
        boundsLeft = 0,
        boundsTop = 0,
        boundsRight = 1080,
        boundsBottom = 200
    )
}
