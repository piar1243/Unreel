package com.example.welive.detection.platforms.instagram

import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class InstagramHomeFeedRegionResolverTest {
    private val resolver = InstagramHomeFeedRegionResolver()

    @Test
    fun adjacentViewPagerPageDoesNotChangePhysicalScreenBounds() {
        val snapshot = snapshot(
            feature("com.instagram.android:id/title_logo", bottom = 150),
            feature("com.instagram.android:id/cf_hub_recycler_view", top = 150, bottom = 430),
            feature("com.instagram.android:id/outer_container", top = 170, bottom = 420),
            feature("com.instagram.android:id/row_feed_photo_imageview", top = 450, bottom = 1700),
            feature("com.instagram.android:id/feed_tab", top = 2200, bottom = 2400),
            feature(
                viewId = "com.instagram.android:id/offscreen_page",
                left = 1080,
                right = 2160,
                top = 0,
                bottom = 2400
            )
        )

        val overlayRegion = resolver.resolve(snapshot)

        assertNotNull(overlayRegion)
        assertEquals(1080, overlayRegion?.right)
        assertEquals(2400, overlayRegion?.bottom)
    }

    private fun snapshot(vararg features: WindowNodeFeature): WindowSnapshot {
        return WindowSnapshot(
            packageName = InstagramPackageConfig.PACKAGE_NAME,
            rootPackageName = InstagramPackageConfig.PACKAGE_NAME,
            eventPackageName = InstagramPackageConfig.PACKAGE_NAME,
            eventType = 0,
            texts = emptySet(),
            contentDescriptions = emptySet(),
            viewIds = features.mapNotNull { it.viewId }.toSet(),
            classNames = emptySet(),
            nodeCount = features.size,
            scrollableNodeCount = 1,
            nodeFeatures = features.toList(),
            isMusicActive = false
        )
    }

    private fun feature(
        viewId: String,
        left: Int = 0,
        top: Int = 0,
        right: Int = 1080,
        bottom: Int
    ): WindowNodeFeature {
        return WindowNodeFeature(
            text = null,
            contentDescription = null,
            viewId = viewId,
            className = "android.view.View",
            isClickable = true,
            isScrollable = false,
            isVisibleToUser = true,
            boundsLeft = left,
            boundsTop = top,
            boundsRight = right,
            boundsBottom = bottom
        )
    }
}
