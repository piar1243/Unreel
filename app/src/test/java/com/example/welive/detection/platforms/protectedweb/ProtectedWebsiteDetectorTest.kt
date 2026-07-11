package com.example.welive.detection.platforms.protectedweb

import com.example.welive.detection.ContentSurface
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectedWebsiteDetectorTest {
    private val detector = ProtectedWebsiteDetector()

    @Test
    fun loadedRedditAddressIsDetected() {
        val result = detector.detect(browserSnapshot("https://www.reddit.com/r/android", focused = false))

        assertEquals(ContentSurface.PROTECTED_WEBSITE_REDDIT, result.surface)
        assertEquals(InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun typedAddressIsNeverBlocked() {
        val result = detector.detect(browserSnapshot("reddit.com", focused = true))

        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun lookalikeDomainIsNotBlocked() {
        val result = detector.detect(browserSnapshot("https://notreddit.com", focused = false))

        assertEquals(ContentSurface.UNKNOWN, result.surface)
    }

    private fun browserSnapshot(url: String, focused: Boolean): WindowSnapshot {
        val node = WindowNodeFeature(
            text = url,
            contentDescription = null,
            viewId = "com.brave.browser:id/url_bar",
            className = "android.widget.EditText",
            isClickable = true,
            isScrollable = false,
            isVisibleToUser = true,
            boundsLeft = 0,
            boundsTop = 0,
            boundsRight = 1080,
            boundsBottom = 120,
            isFocused = focused,
            isEditable = true
        )
        return WindowSnapshot(
            packageName = "com.brave.browser",
            rootPackageName = "com.brave.browser",
            eventPackageName = "com.brave.browser",
            eventType = 0,
            texts = setOf(url),
            contentDescriptions = emptySet(),
            viewIds = setOf(node.viewId!!),
            classNames = setOf(node.className!!),
            nodeCount = 1,
            scrollableNodeCount = 0,
            nodeFeatures = listOf(node),
            isMusicActive = false
        )
    }
}
