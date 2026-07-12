package com.example.welive.detection.platforms.youtube

import com.example.welive.detection.ContentSurface
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeDetectorTest {
    private val detector = YouTubeDetector()

    @Test
    fun blocksNativeYouTubeApp() {
        val result = detector.detect(snapshot(packageName = YouTubePackageConfig.PACKAGE_NAME))

        assertEquals(ContentSurface.YOUTUBE_APP, result.surface)
        assertEquals(InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun blocksLoadedYouTubeShortsAddress() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.chrome",
                features = listOf(
                    addressFeature(
                        text = "https://www.youtube.com/shorts/abc123",
                        focused = false
                    )
                )
            )
        )

        assertEquals(ContentSurface.YOUTUBE_SHORTS_WEB, result.surface)
        assertEquals(InterventionAction.BLOCK, result.recommendedAction)
    }

    @Test
    fun focusedPageSearchDoesNotDisableLoadedShortsDetection() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.chrome",
                features = listOf(
                    pageSearchFeature(focused = true),
                    addressFeature(
                        text = "https://www.youtube.com/shorts/abc123",
                        focused = false
                    )
                )
            )
        )

        assertEquals(ContentSurface.YOUTUBE_SHORTS_WEB, result.surface)
        assertEquals(InterventionAction.BLOCK, result.recommendedAction)
    }

    @Test
    fun allowsRegularYouTubeVideoAddress() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.chrome",
                features = listOf(
                    addressFeature(
                        text = "https://www.youtube.com/watch?v=abc123",
                        focused = false
                    )
                )
            )
        )

        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun allowsShortsUrlWhileAddressBarIsBeingEdited() {
        val result = detector.detect(
            snapshot(
                packageName = "com.brave.browser",
                features = listOf(
                    addressFeature(
                        text = "youtube.com/shorts/abc123",
                        focused = true
                    )
                )
            )
        )

        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    private fun snapshot(
        packageName: String,
        features: List<WindowNodeFeature> = emptyList()
    ): WindowSnapshot {
        return WindowSnapshot(
            packageName = packageName,
            rootPackageName = packageName,
            eventPackageName = packageName,
            eventType = 0,
            texts = features.mapNotNullTo(mutableSetOf()) { it.text },
            contentDescriptions = features.mapNotNullTo(mutableSetOf()) { it.contentDescription },
            viewIds = features.mapNotNullTo(mutableSetOf()) { it.viewId },
            classNames = features.mapNotNullTo(mutableSetOf()) { it.className },
            nodeCount = features.size,
            scrollableNodeCount = 0,
            nodeFeatures = features
        )
    }

    private fun addressFeature(
        text: String,
        focused: Boolean
    ): WindowNodeFeature {
        return WindowNodeFeature(
            text = text,
            contentDescription = null,
            viewId = "com.android.chrome:id/url_bar",
            className = "android.widget.EditText",
            isClickable = true,
            isScrollable = false,
            isVisibleToUser = true,
            boundsLeft = 0,
            boundsTop = 0,
            boundsRight = 900,
            boundsBottom = 120,
            isFocused = focused,
            isEditable = true
        )
    }

    private fun pageSearchFeature(focused: Boolean): WindowNodeFeature {
        return WindowNodeFeature(
            text = "",
            contentDescription = "Search this page",
            viewId = "page-search-input",
            className = "android.widget.EditText",
            isClickable = true,
            isScrollable = false,
            isVisibleToUser = true,
            boundsLeft = 100,
            boundsTop = 300,
            boundsRight = 980,
            boundsBottom = 420,
            isFocused = focused,
            isEditable = true
        )
    }
}
