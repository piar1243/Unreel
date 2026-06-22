package com.example.welive.detection.platforms.instagram

import com.example.welive.detection.ContentSurface
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramDetectorTest {
    private val detector = InstagramDetector()

    @Test
    fun detectsReelsWhenMultipleHighConfidenceSignalsArePresent() {
        val result = detector.detect(
            snapshot(
                texts = setOf("Reels", "Like", "Comment", "Send", "Original audio", "Remix"),
                descriptions = setOf("More options", "Share"),
                viewIds = setOf("com.instagram.android:id/clips_viewer_view_pager"),
                nodeCount = 42,
                scrollableNodeCount = 1
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_REELS, result.surface)
        assertEquals(result.toString(), InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
        assertTrue(result.toString(), result.confidence >= 0.82f)
    }

    @Test
    fun avoidsBlockingHomeFeedSignals() {
        val result = detector.detect(
            snapshot(
                texts = setOf("Instagram", "Home", "Feed", "Reels", "Like", "Comment", "Share"),
                descriptions = setOf("Search", "Profile"),
                nodeCount = 180,
                scrollableNodeCount = 6
            )
        )

        assertEquals(InterventionAction.NONE, result.recommendedAction)
        assertTrue(result.surface != ContentSurface.INSTAGRAM_REELS || result.confidence < 0.82f)
    }

    @Test
    fun reelsSignatureWinsOverShareMessageText() {
        val result = detector.detect(
            snapshot(
                texts = setOf("Reels", "Like", "Comment", "Send", "Share"),
                descriptions = setOf("Send message", "Direct"),
                viewIds = setOf("com.instagram.android:id/clips_viewer_view_pager"),
                nodeCount = 58,
                scrollableNodeCount = 1
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_REELS, result.surface)
        assertEquals(result.toString(), InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun reelsSignatureWinsOverBottomNavigationProfileText() {
        val result = detector.detect(
            snapshot(
                texts = setOf("Reels", "Like", "Comment", "Share", "Profile"),
                descriptions = setOf("Profile", "Send"),
                viewIds = setOf("com.instagram.android:id/clips_viewer_view_pager"),
                nodeCount = 52,
                scrollableNodeCount = 1
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_REELS, result.surface)
        assertEquals(result.toString(), InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun actualInboxMarkersRemainDirectMessages() {
        val result = detector.detect(
            snapshot(
                texts = setOf("Direct", "Inbox", "Message requests"),
                descriptions = setOf("Search"),
                nodeCount = 80,
                scrollableNodeCount = 2
            )
        )

        assertEquals(ContentSurface.INSTAGRAM_DMS, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun plainProfileBottomNavLabelDoesNotClassifyAsProfile() {
        val result = detector.detect(
            snapshot(
                texts = setOf("Profile"),
                descriptions = setOf("Profile"),
                nodeCount = 140,
                scrollableNodeCount = 4
            )
        )

        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun activeAudioWithoutReelControlsDoesNotClassifyAsReels() {
        val result = detector.detect(
            snapshot(
                texts = setOf("Reels"),
                viewIds = setOf("com.instagram.android:id/clips_viewer_view_pager"),
                nodeCount = 120,
                scrollableNodeCount = 3,
                isMusicActive = true
            )
        )

        assertEquals(ContentSurface.UNKNOWN, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun visibleReelsControlClusterBlocks() {
        val result = detector.detect(
            snapshot(
                texts = emptySet(),
                descriptions = emptySet(),
                viewIds = emptySet(),
                nodeCount = 205,
                scrollableNodeCount = 5,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/clips_viewer_view_pager"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_ufi_component"),
                    visibleFeature(viewId = "com.instagram.android:id/like_button", description = "like"),
                    visibleFeature(viewId = "com.instagram.android:id/comment_button", description = "comment"),
                    visibleFeature(viewId = "com.instagram.android:id/direct_share_button", description = "share"),
                    visibleFeature(viewId = "com.instagram.android:id/save_button", description = "save")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_REELS, result.surface)
        assertEquals(result.toString(), InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun visibleReelsInsideMessageThreadClassifiesAsFriendReels() {
        val result = detector.detect(
            snapshot(
                texts = emptySet(),
                descriptions = emptySet(),
                viewIds = emptySet(),
                nodeCount = 205,
                scrollableNodeCount = 5,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/thread_fragment_container"),
                    visibleFeature(viewId = "com.instagram.android:id/message_list"),
                    visibleFeature(viewId = "com.instagram.android:id/message_content_portrait_xma_container"),
                    visibleFeature(viewId = "com.instagram.android:id/forwarding_shortcut_button", description = "forward message"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_viewer_view_pager"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_ufi_component"),
                    visibleFeature(viewId = "com.instagram.android:id/like_button", description = "like"),
                    visibleFeature(viewId = "com.instagram.android:id/comment_button", description = "comment"),
                    visibleFeature(viewId = "com.instagram.android:id/direct_share_button", description = "share"),
                    visibleFeature(viewId = "com.instagram.android:id/save_button", description = "save")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_REELS_FROM_FRIEND, result.surface)
        assertEquals(result.toString(), InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun visibleReelsWithFriendReplyBarClassifiesAsFriendReels() {
        val result = detector.detect(
            snapshot(
                texts = emptySet(),
                descriptions = emptySet(),
                viewIds = emptySet(),
                nodeCount = 190,
                scrollableNodeCount = 2,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/clips_viewer_view_pager"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_ufi_component"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_item_overlay_component"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_media_component"),
                    visibleFeature(viewId = "com.instagram.android:id/like_button", description = "like"),
                    visibleFeature(viewId = "com.instagram.android:id/comment_button", description = "comment"),
                    visibleFeature(viewId = "com.instagram.android:id/direct_share_button", description = "share"),
                    visibleFeature(viewId = "com.instagram.android:id/save_button", description = "save"),
                    visibleFeature(viewId = "com.instagram.android:id/reply_bar_container_scroll_view"),
                    visibleFeature(viewId = "com.instagram.android:id/reply_bar_edittext", description = "reply to yourself"),
                    visibleFeature(viewId = "com.instagram.android:id/reply_bar_reaction_sheet_button"),
                    visibleFeature(viewId = "com.instagram.android:id/sender_username_or_fullname"),
                    visibleFeature(viewId = "com.instagram.android:id/sender_profile_pic"),
                    visibleFeature(viewId = "com.instagram.android:id/sender_timestamp")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_REELS_FROM_FRIEND, result.surface)
        assertEquals(result.toString(), InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun normalReelsWithBottomNavigationMessageLabelStaysPlainReels() {
        val result = detector.detect(
            snapshot(
                texts = emptySet(),
                descriptions = emptySet(),
                viewIds = emptySet(),
                nodeCount = 205,
                scrollableNodeCount = 5,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/clips_viewer_view_pager"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_ufi_component"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_item_overlay_component"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_media_component"),
                    visibleFeature(viewId = "com.instagram.android:id/like_button", description = "like"),
                    visibleFeature(viewId = "com.instagram.android:id/comment_button", description = "comment"),
                    visibleFeature(viewId = "com.instagram.android:id/direct_share_button", description = "share"),
                    visibleFeature(viewId = "com.instagram.android:id/save_button", description = "save"),
                    visibleFeature(viewId = "com.instagram.android:id/direct_tab", description = "message"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_tab", description = "reels"),
                    visibleFeature(viewId = "com.instagram.android:id/feed_tab", description = "home")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_REELS, result.surface)
        assertEquals(result.toString(), InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun normalReelsWithSenderMetadataButNoReplyBarStaysPlainReels() {
        val result = detector.detect(
            snapshot(
                texts = emptySet(),
                descriptions = emptySet(),
                viewIds = emptySet(),
                nodeCount = 70,
                scrollableNodeCount = 2,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/clips_viewer_view_pager"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_ufi_component"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_item_overlay_component"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_media_component"),
                    visibleFeature(viewId = "com.instagram.android:id/like_button", description = "like"),
                    visibleFeature(viewId = "com.instagram.android:id/comment_button", description = "comment"),
                    visibleFeature(viewId = "com.instagram.android:id/direct_share_button", description = "share"),
                    visibleFeature(viewId = "com.instagram.android:id/save_button", description = "save"),
                    visibleFeature(viewId = "com.instagram.android:id/sender_username_or_fullname"),
                    visibleFeature(viewId = "com.instagram.android:id/sender_profile_pic"),
                    visibleFeature(viewId = "com.instagram.android:id/sender_timestamp")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_REELS, result.surface)
        assertEquals(result.toString(), InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
    }

    @Test
    fun visibleSearchReelsGridClassifiesAsBlockedSearchGrid() {
        val result = detector.detect(
            snapshot(
                texts = emptySet(),
                descriptions = emptySet(),
                viewIds = emptySet(),
                nodeCount = 160,
                scrollableNodeCount = 2,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/search_tab", description = "search and explore"),
                    visibleFeature(viewId = "com.instagram.android:id/recycler_view"),
                    visibleFeature(viewId = "com.instagram.android:id/image_preview"),
                    visibleFeature(viewId = "com.instagram.android:id/media_note_view"),
                    visibleFeature(viewId = "com.instagram.android:id/layout_container"),
                    visibleFeature(viewId = "com.instagram.android:id/action_bar_search_edit_text", description = "search with meta ai"),
                    visibleFeature(viewId = "com.instagram.android:id/pill_bar_rv"),
                    visibleFeature(viewId = "com.instagram.android:id/igds_prism_chip_label", description = "for you")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_SEARCH_REELS_GRID, result.surface)
        assertEquals(result.toString(), InterventionAction.BLOCK, result.recommendedAction)
    }

    @Test
    fun plainSearchSurfaceWithoutGridRemainsExploreOnly() {
        val result = detector.detect(
            snapshot(
                texts = emptySet(),
                descriptions = emptySet(),
                viewIds = emptySet(),
                nodeCount = 90,
                scrollableNodeCount = 2,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "search_row"),
                    visibleFeature(viewId = "com.instagram.android:id/search_tab", description = "search and explore"),
                    visibleFeature(viewId = "ig_text", description = "search or ask meta ai")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_EXPLORE, result.surface)
        assertEquals(result.toString(), InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun visibleHomeFeedStaysObserveOnly() {
        val result = detector.detect(
            snapshot(
                texts = emptySet(),
                descriptions = emptySet(),
                viewIds = emptySet(),
                nodeCount = 170,
                scrollableNodeCount = 3,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_view_group_buttons"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_like"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_comment"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_share"),
                    visibleFeature(viewId = "com.instagram.android:id/media_option_button"),
                    visibleFeature(viewId = "com.instagram.android:id/feed_tab", description = "home"),
                    visibleFeature(viewId = "com.instagram.android:id/search_tab", description = "search and explore"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_tab", description = "reels")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_HOME_FEED, result.surface)
        assertEquals(result.toString(), InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun capturedStyleHomeFeedShellStaysObserveOnly() {
        val result = detector.detect(
            snapshot(
                texts = setOf("3 new messages", "reels"),
                descriptions = setOf(
                    "instagram home feed",
                    "reels tray container",
                    "home",
                    "search and explore",
                    "message",
                    "profile"
                ),
                viewIds = emptySet(),
                nodeCount = 220,
                scrollableNodeCount = 6,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/feed_tab", description = "home"),
                    visibleFeature(viewId = "com.instagram.android:id/search_tab", description = "search and explore"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_tab", description = "reels"),
                    visibleFeature(viewId = "com.instagram.android:id/direct_tab", description = "message"),
                    visibleFeature(viewId = "com.instagram.android:id/profile_tab", description = "profile"),
                    visibleFeature(viewId = "com.instagram.android:id/swipeable_tab_view_pager"),
                    visibleFeature(viewId = "com.instagram.android:id/title_logo", description = "instagram home feed"),
                    visibleFeature(viewId = "android:id/list"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_photo_imageview"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_profile_header"),
                    visibleFeature(viewId = "com.instagram.android:id/media_group"),
                    visibleFeature(viewId = "com.instagram.android:id/media_option_button"),
                    visibleFeature(viewId = "com.instagram.android:id/outer_container"),
                    visibleFeature(viewId = "com.instagram.android:id/avatar_image_view")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_HOME_FEED, result.surface)
        assertEquals(result.toString(), InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun scrolledHomeFeedStaysObserveOnlyWithoutTopHeaderMarkers() {
        val result = detector.detect(
            snapshot(
                texts = emptySet(),
                descriptions = setOf("home", "search and explore", "reels", "message", "profile"),
                viewIds = emptySet(),
                nodeCount = 210,
                scrollableNodeCount = 5,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/feed_tab", description = "home"),
                    visibleFeature(viewId = "com.instagram.android:id/search_tab", description = "search and explore"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_tab", description = "reels"),
                    visibleFeature(viewId = "com.instagram.android:id/direct_tab", description = "message"),
                    visibleFeature(viewId = "com.instagram.android:id/profile_tab", description = "profile"),
                    visibleFeature(viewId = "com.instagram.android:id/swipeable_tab_view_pager"),
                    visibleFeature(viewId = "android:id/list"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_photo_imageview"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_profile_header"),
                    visibleFeature(viewId = "com.instagram.android:id/media_group"),
                    visibleFeature(viewId = "com.instagram.android:id/media_option_button")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_HOME_FEED, result.surface)
        assertEquals(result.toString(), InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun hiddenReelsControlClusterDoesNotBlock() {
        val result = detector.detect(
            snapshot(
                texts = setOf("Reels", "Like", "Comment", "Share"),
                descriptions = setOf("Reels", "Like", "Comment", "Share"),
                viewIds = setOf(
                    "com.instagram.android:id/clips_viewer_view_pager",
                    "com.instagram.android:id/like_button",
                    "com.instagram.android:id/comment_button",
                    "com.instagram.android:id/direct_share_button"
                ),
                nodeCount = 238,
                scrollableNodeCount = 6,
                nodeFeatures = listOf(
                    hiddenFeature(viewId = "com.instagram.android:id/clips_viewer_view_pager"),
                    hiddenFeature(viewId = "com.instagram.android:id/clips_ufi_component"),
                    hiddenFeature(viewId = "com.instagram.android:id/like_button", description = "like"),
                    hiddenFeature(viewId = "com.instagram.android:id/comment_button", description = "comment"),
                    hiddenFeature(viewId = "com.instagram.android:id/direct_share_button", description = "share"),
                    visibleFeature(viewId = "com.instagram.android:id/profile_header")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_PROFILE, result.surface)
        assertEquals(result.toString(), InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun profilePostsGridDoesNotClassifyAsHomeFeed() {
        val result = detector.detect(
            snapshot(
                texts = emptySet(),
                descriptions = setOf("grid view", "posts"),
                viewIds = emptySet(),
                nodeCount = 180,
                scrollableNodeCount = 4,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/profile_viewpager"),
                    visibleFeature(viewId = "com.instagram.android:id/profile_tab_layout", description = "grid view"),
                    visibleFeature(viewId = "com.instagram.android:id/profile_tab_icon_view"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_grid_recyclerview"),
                    visibleFeature(viewId = "com.instagram.android:id/feed_tab", description = "home"),
                    visibleFeature(viewId = "com.instagram.android:id/search_tab", description = "search and explore"),
                    visibleFeature(viewId = "com.instagram.android:id/clips_tab", description = "reels"),
                    visibleFeature(viewId = "com.instagram.android:id/direct_tab", description = "message"),
                    visibleFeature(viewId = "com.instagram.android:id/profile_tab", description = "profile"),
                    visibleFeature(viewId = "android:id/list"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_photo_imageview"),
                    visibleFeature(viewId = "com.instagram.android:id/media_group")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_PROFILE, result.surface)
        assertEquals(result.toString(), InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun titledPostsDetailDoesNotClassifyAsHomeFeed() {
        val result = detector.detect(
            snapshot(
                texts = setOf("posts"),
                descriptions = setOf("posts", "like", "comment", "send post", "add to saved"),
                viewIds = emptySet(),
                nodeCount = 170,
                scrollableNodeCount = 3,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/action_bar_title", description = "posts"),
                    visibleFeature(viewId = "com.instagram.android:id/action_bar_button_back"),
                    visibleFeature(viewId = "com.instagram.android:id/swipeable_tab_view_pager"),
                    visibleFeature(viewId = "android:id/list"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_profile_header"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_like", description = "like"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_comment", description = "comment"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_share", description = "send post"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_save", description = "add to saved")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.INSTAGRAM_PROFILE, result.surface)
        assertEquals(result.toString(), InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun titledFollowingDetailDoesNotClassifyAsHomeFeed() {
        val result = detector.detect(
            snapshot(
                texts = setOf("following"),
                descriptions = setOf("following", "like", "comment", "send post", "add to saved"),
                viewIds = emptySet(),
                nodeCount = 170,
                scrollableNodeCount = 3,
                nodeFeatures = listOf(
                    visibleFeature(viewId = "com.instagram.android:id/action_bar_title", description = "following"),
                    visibleFeature(viewId = "com.instagram.android:id/action_bar_button_back"),
                    visibleFeature(viewId = "com.instagram.android:id/swipeable_tab_view_pager"),
                    visibleFeature(viewId = "android:id/list"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_profile_header"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_like", description = "like"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_comment", description = "comment"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_share", description = "send post"),
                    visibleFeature(viewId = "com.instagram.android:id/row_feed_button_save", description = "add to saved")
                )
            )
        )

        assertEquals(result.toString(), ContentSurface.UNKNOWN, result.surface)
        assertEquals(result.toString(), InterventionAction.NONE, result.recommendedAction)
    }

    private fun snapshot(
        texts: Set<String>,
        descriptions: Set<String> = emptySet(),
        viewIds: Set<String> = emptySet(),
        nodeCount: Int,
        scrollableNodeCount: Int,
        nodeFeatures: List<WindowNodeFeature> = emptyList(),
        isMusicActive: Boolean = false
    ): WindowSnapshot {
        return WindowSnapshot(
            packageName = InstagramPackageConfig.PACKAGE_NAME,
            rootPackageName = InstagramPackageConfig.PACKAGE_NAME,
            eventPackageName = InstagramPackageConfig.PACKAGE_NAME,
            eventType = 0,
            texts = texts,
            contentDescriptions = descriptions,
            viewIds = viewIds,
            classNames = emptySet(),
            nodeCount = nodeCount,
            scrollableNodeCount = scrollableNodeCount,
            nodeFeatures = nodeFeatures,
            isMusicActive = isMusicActive
        )
    }

    private fun visibleFeature(
        viewId: String,
        description: String? = null
    ): WindowNodeFeature {
        return feature(viewId = viewId, description = description, visible = true)
    }

    private fun hiddenFeature(
        viewId: String,
        description: String? = null
    ): WindowNodeFeature {
        return feature(viewId = viewId, description = description, visible = false)
    }

    private fun feature(
        viewId: String,
        description: String?,
        visible: Boolean
    ): WindowNodeFeature {
        return WindowNodeFeature(
            text = null,
            contentDescription = description,
            viewId = viewId,
            className = "android.widget.ImageView",
            isClickable = true,
            isScrollable = false,
            isVisibleToUser = visible,
            boundsLeft = 0,
            boundsTop = 0,
            boundsRight = 100,
            boundsBottom = 100
        )
    }
}
