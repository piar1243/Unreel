package com.example.welive.detection.platforms.instagramweb

import com.example.welive.detection.ContentSurface
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.Platform
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramWebDetectorTest {
    private val detector = InstagramWebDetector()

    @Test
    fun blocksInstagramUrlInChrome() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.chrome",
                texts = setOf("https://www.instagram.com/", "Instagram"),
                viewIds = setOf("com.android.chrome:id/url_bar"),
                nodeFeatures = listOf(
                    WindowNodeFeature(
                        text = "https://www.instagram.com/",
                        contentDescription = null,
                        viewId = "com.android.chrome:id/url_bar",
                        className = "android.widget.EditText",
                        isClickable = true,
                        isScrollable = false,
                        isVisibleToUser = true,
                        boundsLeft = 0,
                        boundsTop = 0,
                        boundsRight = 1080,
                        boundsBottom = 120,
                        isFocused = false,
                        isEditable = true
                    )
                )
            )
        )

        assertEquals(Platform.INSTAGRAM_WEB, result.platform)
        assertEquals(ContentSurface.INSTAGRAM_WEB, result.surface)
        assertEquals(InterventionAction.BLOCK, result.recommendedAction)
        assertTrue(result.toString(), result.confidence >= 0.86f)
    }

    @Test
    fun ignoresNonBrowserPackages() {
        val result = detector.detect(
            snapshot(
                packageName = "com.instagram.android",
                texts = setOf("https://www.instagram.com/", "Instagram")
            )
        )

        assertEquals(Platform.UNKNOWN, result.platform)
        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun ignoresInstagramUrlInBraveSuggestionRows() {
        val result = detector.detect(
            snapshot(
                packageName = "com.brave.browser",
                texts = setOf("Example Domain", "instagram.com"),
                nodeFeatures = listOf(
                    WindowNodeFeature(
                        text = "instagram.com",
                        contentDescription = "Search suggestion instagram.com",
                        viewId = "com.brave.browser:id/omnibox_suggestion",
                        className = "android.widget.TextView",
                        isClickable = true,
                        isScrollable = false,
                        isVisibleToUser = true,
                        boundsLeft = 0,
                        boundsTop = 120,
                        boundsRight = 1080,
                        boundsBottom = 200
                    )
                )
            )
        )

        assertEquals(Platform.UNKNOWN, result.platform)
        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun ignoresNonInstagramBrowserPages() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.chrome",
                texts = setOf("Example Domain", "News")
            )
        )

        assertEquals(Platform.UNKNOWN, result.platform)
        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun ignoresGenericWebsiteNavigationWords() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.chrome",
                texts = setOf("Home", "Explore", "Messages", "Notifications", "Profile", "Create"),
                viewIds = setOf("com.android.chrome:id/url_bar")
            )
        )

        assertEquals(Platform.UNKNOWN, result.platform)
        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun ignoresInstagramUrlWhileBrowserAddressBarIsBeingEdited() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.chrome",
                texts = setOf("instagram.com"),
                viewIds = setOf("com.android.chrome:id/url_bar"),
                nodeFeatures = listOf(
                    WindowNodeFeature(
                        text = "instagram.com",
                        contentDescription = null,
                        viewId = "com.android.chrome:id/url_bar",
                        className = "android.widget.EditText",
                        isClickable = true,
                        isScrollable = false,
                        isVisibleToUser = true,
                        boundsLeft = 0,
                        boundsTop = 0,
                        boundsRight = 1080,
                        boundsBottom = 120,
                        isFocused = true,
                        isEditable = true
                    )
                )
            )
        )

        assertEquals(Platform.UNKNOWN, result.platform)
        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun blocksInstagramUrlWhenBrowserAddressBarIsVisibleButNotFocused() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.chrome",
                texts = setOf("instagram.com", "Home", "Explore", "Messages"),
                viewIds = setOf("com.android.chrome:id/url_bar"),
                nodeFeatures = listOf(
                    WindowNodeFeature(
                        text = "instagram.com",
                        contentDescription = null,
                        viewId = "com.android.chrome:id/url_bar",
                        className = "android.widget.EditText",
                        isClickable = true,
                        isScrollable = false,
                        isVisibleToUser = true,
                        boundsLeft = 0,
                        boundsTop = 0,
                        boundsRight = 1080,
                        boundsBottom = 120,
                        isFocused = false,
                        isEditable = true
                    ),
                    WindowNodeFeature(
                        text = "Home",
                        contentDescription = null,
                        viewId = null,
                        className = "android.widget.TextView",
                        isClickable = true,
                        isScrollable = false,
                        isVisibleToUser = true,
                        boundsLeft = 0,
                        boundsTop = 120,
                        boundsRight = 200,
                        boundsBottom = 220
                    )
                )
            )
        )

        assertEquals(Platform.INSTAGRAM_WEB, result.platform)
        assertEquals(ContentSurface.INSTAGRAM_WEB, result.surface)
        assertEquals(InterventionAction.BLOCK, result.recommendedAction)
    }

    private fun snapshot(
        packageName: String,
        texts: Set<String>,
        descriptions: Set<String> = emptySet(),
        viewIds: Set<String> = emptySet(),
        nodeFeatures: List<WindowNodeFeature> = emptyList()
    ): WindowSnapshot {
        return WindowSnapshot(
            packageName = packageName,
            rootPackageName = packageName,
            eventPackageName = packageName,
            eventType = 0,
            texts = texts,
            contentDescriptions = descriptions,
            viewIds = viewIds,
            classNames = emptySet(),
            nodeCount = 20,
            scrollableNodeCount = 1,
            nodeFeatures = nodeFeatures
        )
    }
}
