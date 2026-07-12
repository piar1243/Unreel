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
    fun loadedTikTokAddressIsDetected() {
        val result = detector.detect(browserSnapshot("https://www.tiktok.com/@creator/video/123", focused = false))

        assertEquals(ContentSurface.PROTECTED_WEBSITE_TIKTOK, result.surface)
        assertEquals(InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun loadedLinkedInAddressIsDetected() {
        val result = detector.detect(browserSnapshot("https://www.linkedin.com/feed/", focused = false))

        assertEquals(ContentSurface.PROTECTED_WEBSITE_LINKEDIN, result.surface)
        assertEquals(InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun pageSearchFieldBeforeLinkedInAddressDoesNotHideLoadedUrl() {
        val pageSearch = WindowNodeFeature(
            text = "",
            contentDescription = null,
            viewId = "search-input",
            className = "android.widget.EditText",
            isClickable = true,
            isScrollable = false,
            isVisibleToUser = true,
            boundsLeft = 140,
            boundsTop = 270,
            boundsRight = 940,
            boundsBottom = 365,
            isFocused = false,
            isEditable = true
        )
        val loadedAddress = addressNode("linkedin.com/feed/", focused = false)
        val result = detector.detect(browserSnapshot(listOf(pageSearch, loadedAddress)))

        assertEquals(ContentSurface.PROTECTED_WEBSITE_LINKEDIN, result.surface)
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
        return browserSnapshot(listOf(addressNode(url, focused)))
    }

    private fun addressNode(url: String, focused: Boolean): WindowNodeFeature {
        return WindowNodeFeature(
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
    }

    private fun browserSnapshot(nodes: List<WindowNodeFeature>): WindowSnapshot {
        return WindowSnapshot(
            packageName = "com.brave.browser",
            rootPackageName = "com.brave.browser",
            eventPackageName = "com.brave.browser",
            eventType = 0,
            texts = nodes.mapNotNullTo(mutableSetOf()) { it.text },
            contentDescriptions = emptySet(),
            viewIds = nodes.mapNotNullTo(mutableSetOf()) { it.viewId },
            classNames = nodes.mapNotNullTo(mutableSetOf()) { it.className },
            nodeCount = nodes.size,
            scrollableNodeCount = 0,
            nodeFeatures = nodes,
            isMusicActive = false
        )
    }
}
