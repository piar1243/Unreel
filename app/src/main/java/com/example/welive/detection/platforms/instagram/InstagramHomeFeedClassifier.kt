package com.example.welive.detection.platforms.instagram

import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot

enum class InstagramHomeFeedState {
    HOME_FEED,
    FOLLOWING_TAB,
    OTHER_SURFACE,
    UNKNOWN
}

data class InstagramHomeFeedClassification(
    val state: InstagramHomeFeedState,
    val confidence: Float,
    val reasons: List<String>
)

class InstagramHomeFeedClassifier {
    fun classify(snapshot: WindowSnapshot): InstagramHomeFeedClassification {
        val rawVisibleNodes = snapshot.nodeFeatures.filter { it.isVisibleToUser }
        val visibleNodes = rawVisibleNodes.primaryScreenNodes().ifEmpty { rawVisibleNodes }
        if (visibleNodes.isEmpty()) {
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.UNKNOWN,
                confidence = 0f,
                reasons = listOf("No visible nodes available for home feed classification")
            )
        }

        val visibleIds = visibleNodes.mapNotNull { it.viewId?.lowercase() }
        val visibleIdNames = visibleIds.map { it.substringAfterLast('/') }
        val visibleDescriptions = visibleNodes.mapNotNull { it.contentDescription?.lowercase() }
        val visibleTexts = visibleNodes.mapNotNull { it.text?.lowercase() }

        fun hasVisibleId(vararg needles: String): Boolean {
            return needles.any { needle ->
                visibleIdNames.any { id -> id.contains(needle) }
            }
        }

        fun visibleIdNeedleCount(vararg needles: String): Int {
            return needles.count { needle ->
                visibleIdNames.any { id -> id.contains(needle) }
            }
        }

        fun hasVisibleIdExact(vararg needles: String): Boolean {
            return needles.any { needle ->
                visibleIdNames.any { id -> id == needle }
            }
        }

        fun hasVisibleLabel(vararg needles: String): Boolean {
            return needles.any { needle ->
                visibleDescriptions.any { it == needle || it.contains(needle) } ||
                    visibleTexts.any { it == needle || it.contains(needle) }
            }
        }

        fun selectedTabId(): String? {
            val selectedTabIds = visibleNodes
                .filter { node -> node.isSelected }
                .mapNotNull { node ->
                    node.viewId
                        ?.lowercase()
                        ?.substringAfterLast('/')
                        ?.takeIf { id -> id in BOTTOM_NAV_TAB_IDS }
                }
                .distinct()
            return selectedTabIds.firstOrNull { id -> id != "feed_tab" }
                ?: selectedTabIds.firstOrNull()
        }

        val reasons = mutableListOf<String>()
        val selectedTabId = selectedTabId()
        val hasVisibleClipsViewer = hasVisibleId(
            "clips_viewer_view_pager",
            "clips_ufi_component",
            "clips_item_overlay_component",
            "clips_media_component"
        )
        val hasVisibleSearchGridShell = hasVisibleId(
            "action_bar_search_edit_text",
            "explore_action_bar_right_button_stub",
            "pill_bar_rv",
            "igds_prism_chip_label",
            "search_row"
        ) || hasVisibleLabel(
            "search with meta ai",
            "search or ask meta ai"
        )
        val hasVisibleDmSurface = hasVisibleId(
            "message_list",
            "message_composer_bar",
            "message_composer_container",
            "thread_fragment_container",
            "layout_container_parent",
            "row_thread_composer_edittext",
            "inbox_refreshable_thread_list_recyclerview"
        ) || hasVisibleLabel(
            "voice message, press and hold to record",
            "forward message",
            "message?"
        )
        val visibleStoryViewerMarkerCount = visibleIdNeedleCount(
            "reel_viewer_root",
            "reel_viewer_header",
            "reel_viewer_title",
            "reel_viewer_timestamp",
            "reel_viewer_media_container",
            "reel_viewer_text_container",
            "reel_viewer_comments_button",
            "toolbar_like_button",
            "message_composer_container",
            "composer_text"
        )
        val hasVisibleStoryActionLabel = hasVisibleLabel(
            "send message",
            "send message or reaction",
            "like story",
            "liked story",
            "story,"
        )
        val hasVisibleStorySurface = visibleStoryViewerMarkerCount >= 2 ||
            (visibleStoryViewerMarkerCount >= 1 && hasVisibleStoryActionLabel)
        val hasVisibleProfileSurface = hasVisibleIdExact(
            "profile_header",
            "row_profile_header",
            "profile_tab_layout",
            "profile_viewpager"
        ) || hasVisibleId(
            "profile_header_familiar_followers_label",
            "profile_header_familiar_following_label",
            "profile_header_familiar_post_count_label",
            "profile_user_info_compose_view",
            "row_profile_header_imageview"
        ) || hasVisibleLabel("edit profile", "followers", "view profile on threads", "share profile")
        val hasVisibleProfilePostsSurface = hasVisibleId(
            "profile_tab_layout",
            "profile_viewpager",
            "profile_tab_icon_view"
        ) && (
            hasVisibleId("clips_grid_recyclerview") ||
                hasVisibleLabel("grid view", "posts", "reposted")
            )
        val hasVisibleFollowingTitleMarker = hasVisibleLabel("following") &&
            hasVisibleId("action_bar_title", "action_bar_button_back") &&
            !hasVisibleLabel("followers", "edit profile")
        val hasVisiblePostsTitleMarker = hasVisibleLabel("posts") &&
            hasVisibleId("action_bar_title", "action_bar_button_back") &&
            !hasVisibleLabel("instagram home feed")
        val hasVisibleHomeShell = hasVisibleId(
            "feed_tab",
            "search_tab",
            "clips_tab",
            "direct_tab",
            "profile_tab",
            "swipeable_tab_view_pager"
        )
        val hasVisibleHomeTitleMarker = hasVisibleId("title_logo", "action_bar_title_view") ||
            hasVisibleLabel("instagram home feed")
        val hasVisibleStoriesTray = hasVisibleId(
            "outer_container",
            "avatar_image_view",
            "cf_hub_recycler_view"
        ) || hasVisibleLabel(
            "reels tray container",
            "open story"
        )
        val visibleLegacyFeedMarkerCount = visibleIdNeedleCount(
            "row_feed_view_group_buttons",
            "row_feed_button_like",
            "row_feed_button_comment",
            "row_feed_button_share",
            "row_feed_button_save",
            "media_option_button"
        )
        val visibleCapturedFeedMarkerCount = visibleIdNeedleCount(
            "row_feed_photo_imageview",
            "row_feed_profile_header",
            "row_feed_photo_profile_imageview",
            "row_feed_photo_profile_name",
            "media_group",
            "inline_follow_button",
            "end_of_feed_demarcator_container"
        )
        val hasVisibleFeedBody = visibleLegacyFeedMarkerCount >= 2 ||
            visibleCapturedFeedMarkerCount >= 2 ||
            (hasVisibleId("android:id/list") && (
                hasVisibleId("row_feed_photo_imageview") ||
                    hasVisibleId("row_feed_view_group_buttons")
                ))
        val hasStrongFeedBody = visibleCapturedFeedMarkerCount >= 2 ||
            visibleLegacyFeedMarkerCount >= 2
        val hasVisibleFeedSurface = hasVisibleFeedBody &&
            hasVisibleHomeShell &&
            (hasStrongFeedBody || hasVisibleStoriesTray || hasVisibleHomeTitleMarker)

        if (hasVisibleClipsViewer) {
            reasons += "Visible clips viewer markers are active"
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.OTHER_SURFACE,
                confidence = 0.98f,
                reasons = reasons
            )
        }

        if (hasVisibleFollowingTitleMarker) {
            reasons += "Visible following title and back button markers are active"
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.FOLLOWING_TAB,
                confidence = 0.98f,
                reasons = reasons
            )
        }

        if (hasVisiblePostsTitleMarker) {
            reasons += "Visible posts title and back button markers are active"
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.OTHER_SURFACE,
                confidence = 0.98f,
                reasons = reasons
            )
        }

        if (hasVisibleStorySurface) {
            reasons += "Visible stories viewer markers are active"
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.OTHER_SURFACE,
                confidence = 0.97f,
                reasons = reasons
            )
        }

        if (hasVisibleDmSurface) {
            reasons += "Visible direct message markers are active"
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.OTHER_SURFACE,
                confidence = 0.95f,
                reasons = reasons
            )
        }

        if (hasVisibleProfileSurface || hasVisibleProfilePostsSurface) {
            reasons += "Visible profile markers are active"
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.OTHER_SURFACE,
                confidence = 0.95f,
                reasons = reasons
            )
        }

        if (hasVisibleSearchGridShell) {
            reasons += "Visible search or explore markers are active"
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.OTHER_SURFACE,
                confidence = 0.95f,
                reasons = reasons
            )
        }

        if (selectedTabId != null && selectedTabId != "feed_tab") {
            reasons += "Selected bottom navigation tab is $selectedTabId"
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.OTHER_SURFACE,
                confidence = 0.99f,
                reasons = reasons
            )
        }

        if (selectedTabId == "feed_tab") {
            reasons += "Selected bottom navigation tab is feed_tab"
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.HOME_FEED,
                confidence = 0.99f,
                reasons = reasons
            )
        }

        if (hasVisibleFeedSurface) {
            reasons += if (hasStrongFeedBody) {
                "Visible home feed body markers are active"
            } else {
                "Visible home shell and stories tray markers are active"
            }
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.HOME_FEED,
                confidence = if (hasStrongFeedBody) 0.96f else 0.88f,
                reasons = reasons
            )
        }

        if (hasVisibleHomeShell && (hasVisibleStoriesTray || hasVisibleHomeTitleMarker)) {
            reasons += "Visible home shell is active"
            return InstagramHomeFeedClassification(
                state = InstagramHomeFeedState.HOME_FEED,
                confidence = 0.76f,
                reasons = reasons
            )
        }

        return InstagramHomeFeedClassification(
            state = InstagramHomeFeedState.UNKNOWN,
            confidence = 0.2f,
            reasons = listOf("Home feed markers are currently inconclusive")
        )
    }

    private fun List<WindowNodeFeature>.primaryScreenNodes(): List<WindowNodeFeature> {
        val primaryWidth = asSequence()
            .filter { node -> node.boundsLeft <= 0 && node.boundsRight > 0 }
            .map { node -> node.boundsRight }
            .filter { right -> right in MIN_SCREEN_WIDTH..MAX_REASONABLE_SCREEN_WIDTH }
            .maxOrNull()
            ?: return emptyList()
        val primaryHeight = asSequence()
            .filter { node -> node.boundsTop <= 0 || node.boundsLeft < primaryWidth }
            .map { node -> node.boundsBottom }
            .filter { bottom -> bottom in MIN_SCREEN_HEIGHT..MAX_REASONABLE_SCREEN_HEIGHT }
            .maxOrNull()
            ?: MAX_REASONABLE_SCREEN_HEIGHT

        return filter { node ->
            node.boundsRight > node.boundsLeft &&
                node.boundsBottom > node.boundsTop &&
                node.boundsRight > 0 &&
                node.boundsLeft < primaryWidth &&
                node.boundsBottom > 0 &&
                node.boundsTop < primaryHeight
        }
    }

    private companion object {
        const val MIN_SCREEN_WIDTH = 320
        const val MIN_SCREEN_HEIGHT = 480
        const val MAX_REASONABLE_SCREEN_WIDTH = 1600
        const val MAX_REASONABLE_SCREEN_HEIGHT = 3200
        val BOTTOM_NAV_TAB_IDS = setOf(
            "feed_tab",
            "search_tab",
            "clips_tab",
            "direct_tab",
            "profile_tab"
        )
    }
}
