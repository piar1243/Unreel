package com.example.welive.detection.platforms.linkedin

import com.example.welive.detection.ContentSurface
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedInDetectorTest {
    private val detector = LinkedInDetector()

    @Test
    fun detectsImmersiveLinkedInVideoFeed() {
        val result = detector.detect(
            snapshot(
                texts = setOf("Video", "Like", "Comment", "Repost", "Send"),
                ids = setOf(
                    "com.linkedin.android:id/video_feed",
                    "com.linkedin.android:id/vertical_video_player",
                    "com.linkedin.android:id/comment_button"
                ),
                features = listOf(
                    feature(text = "Video", selected = true),
                    feature(id = "video_player", className = "android.view.SurfaceView", bottom = 1800)
                )
            )
        )

        assertEquals(ContentSurface.LINKEDIN_SHORTFORM, result.surface)
        assertEquals(InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
        assertTrue(result.confidence >= 0.95f)
    }

    @Test
    fun leavesNormalLinkedInFeedUnblocked() {
        val result = detector.detect(
            snapshot(
                texts = setOf("Home", "My Network", "Post", "Like", "Comment", "Repost", "Send"),
                ids = setOf("com.linkedin.android:id/feed_recycler_view")
            )
        )

        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    private fun snapshot(
        texts: Set<String>,
        ids: Set<String>,
        features: List<WindowNodeFeature> = emptyList()
    ) = WindowSnapshot(
        packageName = "com.linkedin.android",
        rootPackageName = "com.linkedin.android",
        eventPackageName = "com.linkedin.android",
        eventType = 0,
        texts = texts,
        contentDescriptions = emptySet(),
        viewIds = ids,
        classNames = features.mapNotNull { it.className }.toSet(),
        nodeCount = 30,
        scrollableNodeCount = 1,
        nodeFeatures = features
    )

    private fun feature(
        text: String? = null,
        id: String? = null,
        className: String? = null,
        selected: Boolean = false,
        bottom: Int = 100
    ) = WindowNodeFeature(
        text = text,
        contentDescription = null,
        viewId = id,
        className = className,
        isClickable = true,
        isScrollable = false,
        isVisibleToUser = true,
        boundsLeft = 0,
        boundsTop = 0,
        boundsRight = 1080,
        boundsBottom = bottom,
        isSelected = selected
    )
}
