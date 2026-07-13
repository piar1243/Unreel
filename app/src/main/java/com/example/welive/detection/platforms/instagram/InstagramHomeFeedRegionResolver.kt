package com.example.welive.detection.platforms.instagram

import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import com.example.welive.intervention.ScreenRegion

class InstagramHomeFeedRegionResolver {
    fun resolve(snapshot: WindowSnapshot): ScreenRegion? {
        return resolveLayout(snapshot)?.overlayRegion
    }

    fun resolveBlockerRegion(snapshot: WindowSnapshot): ScreenRegion? {
        return resolveLayout(snapshot)?.blockerRegion
    }

    fun resolveStoryTapTargets(snapshot: WindowSnapshot): List<ScreenRegion> {
        return resolveLayout(snapshot)?.storyTapTargets.orEmpty()
    }

    private fun resolveLayout(snapshot: WindowSnapshot): HomeFeedLayout? {
        val allVisibleNodes = snapshot.nodeFeatures.filter { it.isVisibleToUser }
        if (allVisibleNodes.isEmpty()) return null

        // ViewPager keeps adjacent pages in the accessibility tree and may report
        // them as visible. Derive the physical page from nodes intersecting x=0 so
        // an off-screen page cannot double the overlay width or alter its bounds.
        val screenWidth = allVisibleNodes
            .asSequence()
            .filter { node -> node.boundsLeft <= 0 && node.boundsRight > 0 }
            .map { node -> node.boundsRight }
            .filter { right -> right in MIN_SCREEN_WIDTH..MAX_REASONABLE_SCREEN_WIDTH }
            .maxOrNull()
            ?: return null
        val screenHeight = allVisibleNodes
            .asSequence()
            .filter { node -> node.boundsLeft < screenWidth && node.boundsRight > 0 }
            .map { node -> node.boundsBottom }
            .filter { bottom -> bottom in MIN_SCREEN_HEIGHT..MAX_REASONABLE_SCREEN_HEIGHT }
            .maxOrNull()
            ?: return null
        if (screenWidth <= 0 || screenHeight <= 0) return null
        val visibleNodes = allVisibleNodes.filter { node ->
            node.boundsRight > 0 &&
                node.boundsLeft < screenWidth &&
                node.boundsBottom > 0 &&
                node.boundsTop < screenHeight
        }

        val gutter = maxOf(8, screenHeight / 100)
        val storyNodes = visibleNodes.filter { node ->
            node.boundsTop < screenHeight * 0.42f && node.matchesAnyId(
                "outer_container",
                "avatar_image_view",
                "cf_hub_recycler_view"
            )
        }
        val storyTapTargets = visibleNodes
            .filter { node -> node.isStoryTapTarget(screenWidth, screenHeight) }
            .map { node ->
                ScreenRegion(
                    left = node.boundsLeft.coerceAtLeast(0),
                    top = node.boundsTop.coerceAtLeast(0),
                    right = node.boundsRight.coerceAtMost(screenWidth),
                    bottom = node.boundsBottom.coerceAtMost(screenHeight)
                )
            }
            .distinctBy { target ->
                listOf(target.left / 8, target.top / 8, target.right / 8, target.bottom / 8)
            }
        val navNodes = visibleNodes.filter { node ->
            node.boundsTop > screenHeight * 0.55f && node.matchesAnyId(
                "feed_tab",
                "search_tab",
                "clips_tab",
                "direct_tab",
                "profile_tab"
            )
        }
        // Feed-item bounds are unstable: promoted rows, end-of-feed cards, and
        // inline Reels can all move them. The story tray and bottom navigation
        // are the two stable edges of the home-feed blocking region.
        if (navNodes.size < MIN_BOTTOM_NAV_ANCHORS) return null
        val storiesBottom = storyNodes.maxOfOrNull { it.boundsBottom }
            ?: (screenHeight * FALLBACK_STORY_TRAY_BOTTOM_RATIO).toInt()
        val storiesTop = storyTapTargets.minOfOrNull { it.top } ?: storyNodes.minOfOrNull { it.boundsTop }
        val bottomNavTop = navNodes.minOf { it.boundsTop }
        val bottomNavBottom = navNodes.maxOfOrNull { it.boundsBottom } ?: screenHeight
        val feedTop = storiesBottom + gutter
        val feedBottom = bottomNavTop - gutter

        val overlayTop = (storiesTop ?: feedTop).coerceIn(0, screenHeight)
        val blockerBottom = (feedBottom - BOTTOM_TRIM_PX).coerceIn(0, screenHeight)
        val overlayBottom = maxOf(
            blockerBottom,
            bottomNavBottom.coerceIn(0, screenHeight)
        )
        val blockerTop = (feedTop + TOP_TRIM_PX).coerceIn(0, screenHeight)
        if (blockerBottom - blockerTop < screenHeight * 0.2f) {
            val fallbackOverlayTop = (storiesTop ?: maxOf(storiesBottom + gutter, (screenHeight * 0.2f).toInt())).coerceIn(0, screenHeight)
            val fallbackBlockerTop = (maxOf(storiesBottom + gutter, (screenHeight * 0.2f).toInt()) + TOP_TRIM_PX).coerceIn(0, screenHeight)
            val fallbackBlockerBottom = (minOf(bottomNavTop - gutter, (screenHeight * 0.86f).toInt()) - BOTTOM_TRIM_PX).coerceIn(0, screenHeight)
            val fallbackOverlayBottom = maxOf(
                fallbackBlockerBottom,
                bottomNavBottom.coerceIn(0, screenHeight)
            )
            if (fallbackBlockerBottom - fallbackBlockerTop < screenHeight * 0.2f) {
                return null
            }
            return HomeFeedLayout(
                overlayRegion = ScreenRegion(
                    left = 0,
                    top = fallbackOverlayTop,
                    right = screenWidth,
                    bottom = fallbackOverlayBottom
                ),
                blockerRegion = ScreenRegion(
                    left = 0,
                    top = fallbackBlockerTop,
                    right = screenWidth,
                    bottom = fallbackBlockerBottom
                ),
                storyTapTargets = storyTapTargets
            )
        }

        return HomeFeedLayout(
            overlayRegion = ScreenRegion(
                left = 0,
                top = overlayTop,
                right = screenWidth,
                bottom = overlayBottom
            ),
            blockerRegion = ScreenRegion(
                left = 0,
                top = blockerTop,
                right = screenWidth,
                bottom = blockerBottom
            ),
            storyTapTargets = storyTapTargets
        )
    }

    private fun WindowNodeFeature.matchesAnyId(vararg needles: String): Boolean {
        val normalizedId = viewId?.lowercase() ?: return false
        return needles.any { needle ->
            normalizedId.contains(needle.lowercase())
        }
    }

    private fun WindowNodeFeature.isStoryTapTarget(screenWidth: Int, screenHeight: Int): Boolean {
        if (!isVisibleToUser || !isClickable) return false
        if (boundsTop > screenHeight * 0.42f) return false
        val width = (boundsRight - boundsLeft).coerceAtLeast(0)
        val height = (boundsBottom - boundsTop).coerceAtLeast(0)
        if (width <= 0 || height <= 0) return false
        if (width > screenWidth * 0.35f || height > screenHeight * 0.18f) return false

        val label = listOfNotNull(contentDescription, text).joinToString(" ").lowercase()
        if (label.contains("reels tray container")) return false

        return matchesAnyId("outer_container", "avatar_image_view", "reel_empty_badge") ||
            label.contains("story")
    }

    private data class HomeFeedLayout(
        val overlayRegion: ScreenRegion,
        val blockerRegion: ScreenRegion,
        val storyTapTargets: List<ScreenRegion>
    )

    companion object {
        // Increase these to trim more off the home blocker from the top or bottom.
        const val TOP_TRIM_PX = 0
        const val BOTTOM_TRIM_PX = -25
        private const val MIN_SCREEN_WIDTH = 320
        private const val MIN_SCREEN_HEIGHT = 480
        private const val MAX_REASONABLE_SCREEN_WIDTH = 1600
        private const val MAX_REASONABLE_SCREEN_HEIGHT = 3200
        private const val MIN_BOTTOM_NAV_ANCHORS = 3
        private const val FALLBACK_STORY_TRAY_BOTTOM_RATIO = 0.18f
    }
}
