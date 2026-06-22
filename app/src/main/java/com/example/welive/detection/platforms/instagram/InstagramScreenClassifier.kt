package com.example.welive.detection.platforms.instagram

import com.example.welive.detection.ContentSurface
import com.example.welive.detection.WindowSnapshot

data class InstagramScreenClassification(
    val surface: ContentSurface,
    val confidence: Float,
    val reasons: List<String>
)

class InstagramScreenClassifier {
    fun classify(snapshot: WindowSnapshot): InstagramScreenClassification {
        classifyFromVisibleNodes(snapshot)?.let { return it }

        val reasons = mutableListOf<String>()
        var reelsScore = 0f
        var negativeScore = 0f
        val hasReelsSurfaceMarker = snapshot.containsTextOrDescriptionAny("reels", "clips viewer") ||
            snapshot.containsViewIdAny("clips_viewer")
        val hasClipsResourceMarker = snapshot.containsViewIdAny(
            "clips_viewer_view_pager",
            "clips_camera",
            "clips_media",
            "clips_viewer"
        )
        val hasReelActionControls = snapshot.containsTextOrDescriptionAny(
            "like",
            "comment",
            "send",
            "share"
        )
        val hasCoreReelsSignature = hasClipsResourceMarker &&
            hasReelsSurfaceMarker &&
            hasReelActionControls

        if (hasReelsSurfaceMarker) {
            reelsScore += 0.35f
            reasons += "Reels label or clips viewer marker is visible"
        }

        if (hasClipsResourceMarker) {
            reelsScore += 0.12f
            reasons += "Instagram clips resource marker is visible"
        }

        if (hasReelActionControls) {
            reelsScore += 0.16f
            reasons += "Reel action controls are visible"
        }

        if (hasCoreReelsSignature) {
            reelsScore += 0.25f
            reasons += "Core Reels signature is visible"
            reasons += "Generic profile/direct labels ignored because clips viewer is active"
            return InstagramScreenClassification(
                surface = ContentSurface.INSTAGRAM_REELS,
                confidence = 0.94f,
                reasons = reasons
            )
        }

        if (snapshot.containsAny("audio", "original audio", "use audio", "remix")) {
            reelsScore += 0.16f
            reasons += "Audio/remix controls are visible"
        }

        if (snapshot.containsAny("more options", "not interested", "report")) {
            reelsScore += 0.08f
            reasons += "Shortform overflow actions are visible"
        }

        if (snapshot.scrollableNodeCount <= 2 && snapshot.nodeCount in 8..120) {
            reelsScore += 0.08f
            reasons += "Window structure resembles fullscreen vertical media"
        }

        val negativeSurface = classifyNegativeSurface(
            snapshot = snapshot,
            reasons = reasons,
            hasCoreReelsSignature = hasCoreReelsSignature
        )
        if (negativeSurface != ContentSurface.UNKNOWN) {
            negativeScore += when (negativeSurface) {
                ContentSurface.INSTAGRAM_DMS,
                ContentSurface.INSTAGRAM_PROFILE,
                ContentSurface.INSTAGRAM_STORIES -> 0.38f
                ContentSurface.INSTAGRAM_HOME_FEED,
                ContentSurface.INSTAGRAM_EXPLORE -> 0.25f
                else -> 0.18f
            }
        }

        val confidence = (reelsScore - negativeScore).coerceIn(0f, 0.98f)
        return if (confidence >= 0.82f) {
            InstagramScreenClassification(
                surface = ContentSurface.INSTAGRAM_REELS,
                confidence = confidence,
                reasons = reasons
            )
        } else {
            InstagramScreenClassification(
                surface = negativeSurface,
                confidence = if (negativeSurface == ContentSurface.UNKNOWN) confidence else (1f - confidence).coerceAtMost(0.75f),
                reasons = reasons.ifEmpty { listOf("No high-confidence Instagram Reels markers found") }
            )
        }
    }

    private fun classifyFromVisibleNodes(snapshot: WindowSnapshot): InstagramScreenClassification? {
        if (snapshot.nodeFeatures.isEmpty()) return null

        val reasons = mutableListOf<String>()
        val visibleNodes = snapshot.nodeFeatures.filter { it.isVisibleToUser }
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

        val hasVisibleClipsViewer = hasVisibleId(
            "clips_viewer_view_pager",
            "clips_ufi_component",
            "clips_item_overlay_component",
            "clips_media_component"
        )
        val hasVisibleLike = hasVisibleId("like_button") || hasVisibleLabel("like")
        val hasVisibleComment = hasVisibleId("comment_button") || hasVisibleLabel("comment")
        val hasVisibleShare = hasVisibleId("direct_share_button") || hasVisibleLabel("share")
        val hasVisibleSave = hasVisibleId("save_button") || hasVisibleLabel("save")
        val visibleControlCount = listOf(
            hasVisibleLike,
            hasVisibleComment,
            hasVisibleShare,
            hasVisibleSave
        ).count { it }

        val hasVisibleDmSurface = hasVisibleId(
            "message_list",
            "message_composer_bar",
            "thread_fragment_container"
        )
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
        val visibleLegacyFeedMarkerCount = visibleIdNeedleCount(
            "row_feed_view_group_buttons",
            "row_feed_button_like",
            "row_feed_button_comment",
            "row_feed_button_share",
            "media_option_button"
        )
        val visibleCapturedFeedMarkerCount = visibleIdNeedleCount(
            "row_feed_photo_imageview",
            "row_feed_profile_header",
            "row_feed_photo_profile_imageview",
            "row_feed_photo_profile_name",
            "media_group",
            "end_of_feed_demarcator_container",
            "inline_follow_button",
            "row_feed_button_save"
        )
        val hasVisibleFeedBody = visibleLegacyFeedMarkerCount >= 2 ||
            visibleCapturedFeedMarkerCount >= 2 ||
            (hasVisibleId("android:id/list") && hasVisibleId("row_feed_photo_imageview")) ||
            (hasVisibleId("row_feed_view_group_buttons") && hasVisibleId("feed_tab"))
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
        val hasVisibleFollowingTitleMarker = hasVisibleLabel("following") &&
            !hasVisibleLabel("followers", "edit profile")
        val hasVisibleTitledPostsSurface = hasVisibleLabel("posts") &&
            hasVisibleId("action_bar_title", "action_bar_button_back") &&
            !hasVisibleLabel("instagram home feed")
        val hasStrongLegacyFeedControls = visibleLegacyFeedMarkerCount >= 2 &&
            hasVisibleId("feed_tab")
        val hasStrongCapturedFeedSurface = hasVisibleFeedBody &&
            hasVisibleHomeShell &&
            (
                visibleCapturedFeedMarkerCount >= 2 ||
                    (hasVisibleId("list") && hasVisibleId("row_feed_photo_imageview")) ||
                    (hasVisibleId("media_group") && hasVisibleId("row_feed_profile_header"))
                )
        val hasVisibleFeedSurface = hasVisibleFeedBody &&
            (
                hasStrongLegacyFeedControls ||
                    hasStrongCapturedFeedSurface ||
                    (hasVisibleHomeShell && (hasVisibleHomeTitleMarker || hasVisibleStoriesTray))
                ) &&
            !hasVisibleFollowingTitleMarker
        val hasVisibleSearchSurface = hasVisibleId("search_row") || hasVisibleLabel("search or ask meta ai")
        val visibleSearchGridMarkerCount = visibleIdNeedleCount(
            "recycler_view",
            "image_preview",
            "media_note_view",
            "layout_container"
        )
        val hasVisibleSearchGridShell = hasVisibleId(
            "action_bar_search_edit_text",
            "explore_action_bar_right_button_stub",
            "pill_bar_rv",
            "igds_prism_chip_label"
        ) || hasVisibleLabel(
            "search with meta ai",
            "for you",
            "search and explore"
        )
        val hasVisibleThreadContext = hasVisibleId(
            "thread_fragment_container",
            "message_list",
            "message_content_portrait_xma_container",
            "message_content",
            "message_composer_bar",
            "message_composer_reply_bar_container"
        )
        val hasVisibleThreadReelContext = hasVisibleThreadContext &&
            (
                hasVisibleId(
                    "forwarding_shortcut_button",
                    "message_riff_cutout_shortcut_button",
                    "sender_avatar",
                    "profile_attribution_picture",
                    "row_thread_composer"
                ) ||
                    hasVisibleLabel(
                        "message",
                        "message…",
                        "forward message",
                        "voice message",
                        "audio call"
                    )
                )
        val visibleFriendReplyMarkerCount = visibleIdNeedleCount(
            "reply_bar_container_scroll_view",
            "reply_bar_edittext",
            "reply_bar_reaction_sheet_button"
        )
        val visibleFriendSenderMarkerCount = visibleIdNeedleCount(
            "sender_username_or_fullname",
            "sender_profile_pic",
            "sender_timestamp"
        )
        val hasVisibleFriendReelContext = visibleFriendReplyMarkerCount >= 2 ||
            (visibleFriendReplyMarkerCount >= 1 && visibleFriendSenderMarkerCount >= 2) ||
            (visibleFriendSenderMarkerCount >= 2 && hasVisibleLabel("reply to", "reply in group to"))

        fun reelsClassification(confidence: Float, extraReason: String? = null): InstagramScreenClassification {
            if (hasVisibleFriendReelContext || hasVisibleThreadReelContext) {
                reasons += if (hasVisibleFriendReelContext) {
                    "Visible friend Reel reply or sender markers are present"
                } else {
                    "Visible message thread context is present"
                }
                extraReason?.let(reasons::add)
                return InstagramScreenClassification(
                    surface = ContentSurface.INSTAGRAM_REELS_FROM_FRIEND,
                    confidence = confidence,
                    reasons = reasons
                )
            }
            extraReason?.let(reasons::add)
            return InstagramScreenClassification(
                surface = ContentSurface.INSTAGRAM_REELS,
                confidence = confidence,
                reasons = reasons
            )
        }

        if (hasVisibleClipsViewer && hasVisibleLike && hasVisibleComment && hasVisibleShare) {
            reasons += "Visible clips viewer is active"
            reasons += "Visible Reels controls include Like, Comment, and Share"
            if (hasVisibleSave) reasons += "Visible Save control is present"
            return reelsClassification(confidence = if (hasVisibleSave) 0.99f else 0.96f)
        }

        if (hasVisibleClipsViewer && visibleControlCount >= 3 && !hasVisibleFeedSurface && !hasVisibleProfileSurface && !hasVisibleDmSurface) {
            reasons += "Visible clips viewer is active"
            reasons += "At least three visible Reels controls are present"
            return reelsClassification(confidence = 0.92f)
        }

        if (
            visibleSearchGridMarkerCount >= 3 &&
            hasVisibleSearchGridShell &&
            !hasVisibleClipsViewer &&
            !hasVisibleDmSurface &&
            !hasVisibleProfileSurface &&
            !hasVisibleFeedSurface
        ) {
            return InstagramScreenClassification(
                surface = ContentSurface.INSTAGRAM_SEARCH_REELS_GRID,
                confidence = 0.97f,
                reasons = listOf(
                    "Visible search grid media tiles are active",
                    "Visible explore search controls are active"
                )
            )
        }

        if (hasVisibleFeedBody && hasVisibleFollowingTitleMarker && !hasVisibleClipsViewer) {
            return InstagramScreenClassification(
                surface = ContentSurface.UNKNOWN,
                confidence = 0.18f,
                reasons = listOf(
                    "Visible following tab marker is active",
                    "Home feed blocker is held back until following samples are tuned"
                )
            )
        }

        if (hasVisibleFeedBody && hasVisibleTitledPostsSurface && !hasVisibleClipsViewer) {
            return InstagramScreenClassification(
                surface = ContentSurface.INSTAGRAM_PROFILE,
                confidence = 0.9f,
                reasons = listOf(
                    "Visible posts title and back button markers are active",
                    "Post detail surface is not the home feed"
                )
            )
        }

        return when {
            hasVisibleDmSurface -> InstagramScreenClassification(
                surface = ContentSurface.INSTAGRAM_DMS,
                confidence = 0.9f,
                reasons = listOf("Visible direct message surface is active")
            )
            hasVisibleProfileSurface || hasVisibleProfilePostsSurface -> InstagramScreenClassification(
                surface = ContentSurface.INSTAGRAM_PROFILE,
                confidence = 0.9f,
                reasons = listOf("Visible profile or posts surface is active")
            )
            hasVisibleFeedSurface -> InstagramScreenClassification(
                surface = ContentSurface.INSTAGRAM_HOME_FEED,
                confidence = 0.9f,
                reasons = listOf(
                    if (hasStrongCapturedFeedSurface) {
                        "Visible scrolled home feed shell is active"
                    } else {
                        "Visible home feed shell is active"
                    },
                    "Visible feed body markers are active"
                )
            )
            hasVisibleSearchSurface -> InstagramScreenClassification(
                surface = ContentSurface.INSTAGRAM_EXPLORE,
                confidence = 0.8f,
                reasons = listOf("Visible search or explore surface is active")
            )
            else -> InstagramScreenClassification(
                surface = ContentSurface.UNKNOWN,
                confidence = 0f,
                reasons = listOf("No visible Reels control cluster found")
            )
        }
    }

    private fun classifyNegativeSurface(
        snapshot: WindowSnapshot,
        reasons: MutableList<String>,
        hasCoreReelsSignature: Boolean
    ): ContentSurface {
        val hasDmInboxMarker = snapshot.containsAny("inbox", "requests", "message requests")
        val hasDirectThreadMarker = snapshot.containsAny("direct") &&
            snapshot.containsAny("message") &&
            !hasCoreReelsSignature

        return when {
            hasDmInboxMarker || hasDirectThreadMarker -> {
                reasons += "Direct messages markers are visible"
                ContentSurface.INSTAGRAM_DMS
            }
            snapshot.containsAny("story", "stories", "close friends") &&
                snapshot.containsAny("reply", "send message") -> {
                reasons += "Story viewer markers are visible"
                ContentSurface.INSTAGRAM_STORIES
            }
            snapshot.containsTextOrDescriptionAny("followers", "following", "edit profile") ||
                snapshot.containsViewIdAny("profile_header", "profile_tab", "profile_grid") -> {
                reasons += "Profile markers are visible"
                ContentSurface.INSTAGRAM_PROFILE
            }
            snapshot.containsAny("search", "explore", "recent searches") -> {
                reasons += "Search or explore markers are visible"
                ContentSurface.INSTAGRAM_EXPLORE
            }
            snapshot.containsAny("home", "feed") -> {
                reasons += "Home feed markers are visible"
                ContentSurface.INSTAGRAM_HOME_FEED
            }
            else -> ContentSurface.UNKNOWN
        }
    }
}
