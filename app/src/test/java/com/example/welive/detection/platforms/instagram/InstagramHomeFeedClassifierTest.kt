package com.example.welive.detection.platforms.instagram

import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramHomeFeedClassifierTest {
    private val classifier = InstagramHomeFeedClassifier()

    @Test
    fun visibleHomeFeedClassifiesAsHomeFeed() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/feed_tab", description = "home"),
                visibleFeature("com.instagram.android:id/search_tab", description = "search and explore"),
                visibleFeature("com.instagram.android:id/clips_tab", description = "reels"),
                visibleFeature("com.instagram.android:id/direct_tab", description = "message"),
                visibleFeature("com.instagram.android:id/profile_tab", description = "profile"),
                visibleFeature("com.instagram.android:id/swipeable_tab_view_pager"),
                visibleFeature("android:id/list"),
                visibleFeature("com.instagram.android:id/row_feed_photo_imageview"),
                visibleFeature("com.instagram.android:id/row_feed_profile_header"),
                visibleFeature("com.instagram.android:id/row_feed_view_group_buttons"),
                visibleFeature("com.instagram.android:id/media_option_button"),
                visibleFeature("com.instagram.android:id/outer_container"),
                visibleFeature("com.instagram.android:id/avatar_image_view")
            )
        )

        assertEquals(InstagramHomeFeedState.HOME_FEED, result.state)
        assertTrue(result.confidence >= 0.88f)
    }

    @Test
    fun followingTabDoesNotClassifyAsHomeFeed() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/action_bar_title", text = "following", description = "following"),
                visibleFeature("com.instagram.android:id/action_bar_button_back"),
                visibleFeature("com.instagram.android:id/feed_tab", description = "home"),
                visibleFeature("com.instagram.android:id/search_tab", description = "search and explore"),
                visibleFeature("com.instagram.android:id/swipeable_tab_view_pager"),
                visibleFeature("android:id/list"),
                visibleFeature("com.instagram.android:id/row_feed_photo_imageview")
            )
        )

        assertEquals(InstagramHomeFeedState.FOLLOWING_TAB, result.state)
    }

    @Test
    fun reelsSurfaceDoesNotClassifyAsHomeFeed() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/clips_viewer_view_pager"),
                visibleFeature("com.instagram.android:id/clips_ufi_component"),
                visibleFeature("com.instagram.android:id/like_button", description = "like"),
                visibleFeature("com.instagram.android:id/comment_button", description = "comment"),
                visibleFeature("com.instagram.android:id/direct_share_button", description = "share")
            )
        )

        assertEquals(InstagramHomeFeedState.OTHER_SURFACE, result.state)
    }

    @Test
    fun directMessagesWinOverOffscreenHomeFeedMarkers() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/thread_fragment_container"),
                visibleFeature("com.instagram.android:id/message_composer_bar"),
                visibleFeature("com.instagram.android:id/row_thread_composer_edittext", text = "message?"),
                visibleFeature("com.instagram.android:id/message_list"),
                offscreenRightFeature("android:id/list"),
                offscreenRightFeature("com.instagram.android:id/row_feed_photo_imageview"),
                offscreenRightFeature("com.instagram.android:id/row_feed_profile_header"),
                offscreenRightFeature("com.instagram.android:id/feed_tab", description = "home"),
                offscreenRightFeature("com.instagram.android:id/outer_container")
            )
        )

        assertEquals(InstagramHomeFeedState.OTHER_SURFACE, result.state)
    }

    @Test
    fun storiesWinOverOffscreenHomeFeedMarkers() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/reel_viewer_root"),
                visibleFeature("com.instagram.android:id/reel_viewer_header"),
                visibleFeature("com.instagram.android:id/reel_viewer_title", description = "michiliang's story, 30 minutes ago"),
                visibleFeature("com.instagram.android:id/message_composer_container"),
                visibleFeature("com.instagram.android:id/composer_text", text = "send message"),
                visibleFeature("com.instagram.android:id/toolbar_like_button", description = "like story"),
                offscreenRightFeature("android:id/list"),
                offscreenRightFeature("com.instagram.android:id/row_feed_photo_imageview"),
                offscreenRightFeature("com.instagram.android:id/row_feed_profile_header"),
                offscreenRightFeature("com.instagram.android:id/feed_tab", description = "home")
            )
        )

        assertEquals(InstagramHomeFeedState.OTHER_SURFACE, result.state)
    }

    @Test
    fun profileWinsOverOffscreenHomeFeedMarkers() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/profile_viewpager"),
                visibleFeature("com.instagram.android:id/profile_tab_layout"),
                visibleFeature("com.instagram.android:id/profile_header_familiar_followers_label", text = "followers"),
                visibleFeature("com.instagram.android:id/button_container", text = "edit profile"),
                visibleFeature("com.instagram.android:id/button_container", text = "share profile"),
                offscreenRightFeature("android:id/list"),
                offscreenRightFeature("com.instagram.android:id/row_feed_photo_imageview"),
                offscreenRightFeature("com.instagram.android:id/row_feed_profile_header"),
                offscreenRightFeature("com.instagram.android:id/feed_tab", description = "home")
            )
        )

        assertEquals(InstagramHomeFeedState.OTHER_SURFACE, result.state)
    }

    @Test
    fun suggestedForYouHomeFeedStillClassifiesAsHomeFeed() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/feed_tab", description = "home"),
                visibleFeature("com.instagram.android:id/search_tab", description = "search and explore"),
                visibleFeature("com.instagram.android:id/clips_tab", description = "reels"),
                visibleFeature("com.instagram.android:id/direct_tab", description = "message"),
                visibleFeature("com.instagram.android:id/profile_tab", description = "profile"),
                visibleFeature("com.instagram.android:id/swipeable_tab_view_pager"),
                visibleFeature("android:id/list"),
                visibleFeature("com.instagram.android:id/row_feed_photo_imageview"),
                visibleFeature("com.instagram.android:id/row_feed_profile_header"),
                visibleFeature("com.instagram.android:id/row_feed_photo_profile_name"),
                visibleFeature("com.instagram.android:id/media_group"),
                visibleFeature("com.instagram.android:id/secondary_label", text = "suggested for you")
            )
        )

        assertEquals(InstagramHomeFeedState.HOME_FEED, result.state)
    }

    @Test
    fun stableTopChromeAndStoryTrayClassifyHomeBeforeFeedBodySettles() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/feed_tab", description = "home"),
                visibleFeature("com.instagram.android:id/search_tab", description = "search and explore"),
                visibleFeature("com.instagram.android:id/clips_tab", description = "reels"),
                visibleFeature("com.instagram.android:id/direct_tab", description = "message"),
                visibleFeature("com.instagram.android:id/profile_tab", description = "profile"),
                visibleFeature("com.instagram.android:id/swipeable_tab_view_pager"),
                visibleFeature("com.instagram.android:id/title_logo", description = "instagram home feed"),
                visibleFeature("com.instagram.android:id/action_bar_title_view"),
                visibleFeature("com.instagram.android:id/notification"),
                visibleFeature("com.instagram.android:id/cf_hub_recycler_view", description = "reels tray container"),
                visibleFeature("com.instagram.android:id/outer_container"),
                visibleFeature("com.instagram.android:id/avatar_image_view", description = "maria's story, 1 of 18, unseen."),
                visibleFeature("com.instagram.android:id/reel_empty_badge", description = "add to story")
            )
        )

        assertEquals(InstagramHomeFeedState.HOME_FEED, result.state)
        assertTrue(result.confidence >= 0.93f)
    }

    @Test
    fun dmSurfaceStillWinsOverTopChromeAndStoryTrayMarkers() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/message_list"),
                visibleFeature("com.instagram.android:id/message_composer_bar"),
                visibleFeature("com.instagram.android:id/row_thread_composer_edittext", text = "message?"),
                visibleFeature("com.instagram.android:id/title_logo", description = "instagram home feed"),
                visibleFeature("com.instagram.android:id/notification"),
                visibleFeature("com.instagram.android:id/cf_hub_recycler_view", description = "reels tray container"),
                visibleFeature("com.instagram.android:id/outer_container"),
                visibleFeature("com.instagram.android:id/avatar_image_view", description = "maria's story, 1 of 18, unseen."),
                visibleFeature("com.instagram.android:id/feed_tab", description = "home")
            )
        )

        assertEquals(InstagramHomeFeedState.OTHER_SURFACE, result.state)
    }

    @Test
    fun profilePostsGridDoesNotClassifyAsHomeFeed() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/profile_viewpager"),
                visibleFeature("com.instagram.android:id/profile_tab_layout", description = "grid view"),
                visibleFeature("com.instagram.android:id/profile_tab_icon_view"),
                visibleFeature("com.instagram.android:id/clips_grid_recyclerview"),
                visibleFeature("com.instagram.android:id/feed_tab", description = "home"),
                visibleFeature("com.instagram.android:id/search_tab", description = "search and explore"),
                visibleFeature("com.instagram.android:id/clips_tab", description = "reels"),
                visibleFeature("com.instagram.android:id/direct_tab", description = "message"),
                visibleFeature("com.instagram.android:id/profile_tab", description = "profile"),
                visibleFeature("android:id/list"),
                visibleFeature("com.instagram.android:id/row_feed_photo_imageview"),
                visibleFeature("com.instagram.android:id/media_group")
            )
        )

        assertEquals(InstagramHomeFeedState.OTHER_SURFACE, result.state)
    }

    @Test
    fun titledPostsDetailDoesNotClassifyAsHomeFeed() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/action_bar_title", text = "posts", description = "posts"),
                visibleFeature("com.instagram.android:id/action_bar_button_back"),
                visibleFeature("com.instagram.android:id/swipeable_tab_view_pager"),
                visibleFeature("android:id/list"),
                visibleFeature("com.instagram.android:id/row_feed_profile_header"),
                visibleFeature("com.instagram.android:id/row_feed_button_like", description = "like"),
                visibleFeature("com.instagram.android:id/row_feed_button_comment", description = "comment"),
                visibleFeature("com.instagram.android:id/row_feed_button_share", description = "send post"),
                visibleFeature("com.instagram.android:id/row_feed_button_save", description = "add to saved")
            )
        )

        assertEquals(InstagramHomeFeedState.OTHER_SURFACE, result.state)
    }

    @Test
    fun titledFollowingPostsDetailDoesNotClassifyAsHomeFeed() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/action_bar_title", text = "following", description = "following"),
                visibleFeature("com.instagram.android:id/action_bar_button_back"),
                visibleFeature("com.instagram.android:id/swipeable_tab_view_pager"),
                visibleFeature("android:id/list"),
                visibleFeature("com.instagram.android:id/row_feed_profile_header"),
                visibleFeature("com.instagram.android:id/row_feed_button_like", description = "like"),
                visibleFeature("com.instagram.android:id/row_feed_button_comment", description = "comment"),
                visibleFeature("com.instagram.android:id/row_feed_button_share", description = "send post"),
                visibleFeature("com.instagram.android:id/row_feed_button_save", description = "add to saved")
            )
        )

        assertEquals(InstagramHomeFeedState.FOLLOWING_TAB, result.state)
    }

    @Test
    fun postCreatorDoesNotClassifyAsHomeFeed() {
        val result = classifier.classify(
            snapshot(
                visibleFeature("com.instagram.android:id/cam_dest_feed", text = "post"),
                visibleFeature("com.instagram.android:id/cam_dest_story", text = "story"),
                visibleFeature("com.instagram.android:id/cam_dest_clips", text = "reel", isSelected = true),
                visibleFeature("com.instagram.android:id/cam_dest_live", text = "live"),
                visibleFeature("com.instagram.android:id/gallery_container_coordinator", bottom = 2180),
                visibleFeature("com.instagram.android:id/gallery_recycler_view", top = 564, bottom = 2180),
                visibleFeature(
                    "com.instagram.android:id/gallery_grid_item_thumbnail",
                    description = "unselected photo thumbnail created on july 2, 2026 4:45 pm",
                    left = 5,
                    top = 1196,
                    right = 358,
                    bottom = 1823
                ),
                visibleFeature(
                    "com.instagram.android:id/gallery_grid_item_thumbnail",
                    description = "unselected photo thumbnail created on july 2, 2026 4:46 pm",
                    left = 364,
                    top = 1196,
                    right = 716,
                    bottom = 1823
                ),
                visibleFeature(
                    "com.instagram.android:id/gallery_grid_item_thumbnail",
                    description = "unselected photo thumbnail created on july 2, 2026 4:56 pm",
                    left = 722,
                    top = 1196,
                    right = 1075,
                    bottom = 1823
                ),
                offscreenRightFeature("com.instagram.android:id/feed_tab", description = "home"),
                offscreenRightFeature("com.instagram.android:id/title_logo", description = "instagram home feed"),
                offscreenRightFeature("com.instagram.android:id/outer_container")
            )
        )

        assertEquals(InstagramHomeFeedState.OTHER_SURFACE, result.state)
        assertTrue(result.confidence >= 0.97f)
    }

    private fun snapshot(vararg features: WindowNodeFeature): WindowSnapshot {
        return WindowSnapshot(
            packageName = InstagramPackageConfig.PACKAGE_NAME,
            rootPackageName = InstagramPackageConfig.PACKAGE_NAME,
            eventPackageName = InstagramPackageConfig.PACKAGE_NAME,
            eventType = 0,
            texts = features.mapNotNull { it.text }.toSet(),
            contentDescriptions = features.mapNotNull { it.contentDescription }.toSet(),
            viewIds = features.mapNotNull { it.viewId }.toSet(),
            classNames = features.mapNotNull { it.className }.toSet(),
            nodeCount = features.size,
            scrollableNodeCount = 2,
            nodeFeatures = features.toList(),
            isMusicActive = false
        )
    }

    private fun visibleFeature(
        viewId: String,
        text: String? = null,
        description: String? = null,
        left: Int = 0,
        top: Int = 0,
        right: Int = 1080,
        bottom: Int = 200,
        isSelected: Boolean = false
    ): WindowNodeFeature {
        return WindowNodeFeature(
            text = text,
            contentDescription = description,
            viewId = viewId,
            className = "android.view.View",
            isClickable = true,
            isScrollable = false,
            isVisibleToUser = true,
            boundsLeft = left,
            boundsTop = top,
            boundsRight = right,
            boundsBottom = bottom,
            isSelected = isSelected
        )
    }

    private fun offscreenRightFeature(
        viewId: String,
        text: String? = null,
        description: String? = null
    ): WindowNodeFeature {
        return visibleFeature(
            viewId = viewId,
            text = text,
            description = description,
            left = 1080,
            right = 2160
        )
    }
}
