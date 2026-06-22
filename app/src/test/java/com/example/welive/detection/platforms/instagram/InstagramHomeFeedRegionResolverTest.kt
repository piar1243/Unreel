package com.example.welive.detection.platforms.instagram

import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramHomeFeedRegionResolverTest {
    private val resolver = InstagramHomeFeedRegionResolver()

    @Test
    fun resolvesCenterFeedBetweenStoriesAndBottomNav() {
        val snapshot = snapshot(
            visibleFeature("com.instagram.android:id/outer_container", top = 60, bottom = 220, left = 24, right = 156),
            visibleFeature("com.instagram.android:id/avatar_image_view", top = 80, bottom = 210, left = 36, right = 144),
            visibleFeature("android:id/list", top = 250, bottom = 2080),
            visibleFeature("com.instagram.android:id/row_feed_photo_imageview", top = 320, bottom = 1520),
            visibleFeature("com.instagram.android:id/row_feed_view_group_buttons", top = 1540, bottom = 1680),
            visibleFeature("com.instagram.android:id/feed_tab", top = 2240, bottom = 2360),
            visibleFeature("com.instagram.android:id/search_tab", top = 2240, bottom = 2360),
            visibleFeature("com.instagram.android:id/clips_tab", top = 2240, bottom = 2360)
        )
        val region = resolver.resolve(snapshot)
        val blockerRegion = resolver.resolveBlockerRegion(snapshot)
        val storyTargets = resolver.resolveStoryTapTargets(snapshot)

        assertNotNull(region)
        assertNotNull(blockerRegion)
        assertTrue(region!!.top <= 80)
        assertTrue(blockerRegion!!.top > 220)
        assertTrue(region.bottom >= 2360)
        assertTrue(blockerRegion.bottom < 2240)
        assertTrue(region.height > 1000)
        assertTrue(storyTargets.isNotEmpty())
    }

    @Test
    fun fallsBackToStableBandWhenFeedNodesAreSparse() {
        val region = resolver.resolve(
            snapshot(
                visibleFeature("com.instagram.android:id/outer_container", top = 40, bottom = 230, left = 24, right = 156),
                visibleFeature("com.instagram.android:id/avatar_image_view", top = 65, bottom = 220, left = 36, right = 144),
                visibleFeature("com.instagram.android:id/feed_tab", top = 2245, bottom = 2360),
                visibleFeature("com.instagram.android:id/search_tab", top = 2245, bottom = 2360),
                visibleFeature("com.instagram.android:id/profile_tab", top = 2245, bottom = 2360)
            )
        )

        assertNotNull(region)
        assertTrue(region!!.top <= 65)
        assertTrue(region.bottom > region.top)
        assertTrue(region.height >= 1200)
    }

    private fun snapshot(vararg nodeFeatures: WindowNodeFeature): WindowSnapshot {
        return WindowSnapshot(
            packageName = InstagramPackageConfig.PACKAGE_NAME,
            rootPackageName = InstagramPackageConfig.PACKAGE_NAME,
            eventPackageName = InstagramPackageConfig.PACKAGE_NAME,
            eventType = 0,
            texts = emptySet(),
            contentDescriptions = emptySet(),
            viewIds = emptySet(),
            classNames = emptySet(),
            nodeCount = nodeFeatures.size,
            scrollableNodeCount = 1,
            nodeFeatures = nodeFeatures.toList(),
            isMusicActive = false
        )
    }

    private fun visibleFeature(
        viewId: String,
        top: Int,
        bottom: Int,
        left: Int = 0,
        right: Int = 1080
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
