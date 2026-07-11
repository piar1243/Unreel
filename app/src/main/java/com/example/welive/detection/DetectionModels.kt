package com.example.welive.detection

enum class Platform {
    INSTAGRAM,
    INSTAGRAM_WEB,
    YOUTUBE,
    YOUTUBE_WEB,
    TIKTOK,
    PROTECTED_WEB,
    SETTINGS,
    UNKNOWN
}

enum class ContentSurface {
    INSTAGRAM_REELS,
    INSTAGRAM_REELS_FROM_FRIEND,
    INSTAGRAM_SEARCH_REELS_GRID,
    INSTAGRAM_STORIES,
    INSTAGRAM_HOME_FEED,
    INSTAGRAM_EXPLORE,
    INSTAGRAM_PROFILE,
    INSTAGRAM_DMS,
    INSTAGRAM_WEB,
    YOUTUBE_APP,
    YOUTUBE_SHORTS_WEB,
    TIKTOK_SHORTFORM,
    TIKTOK_MESSAGES,
    PROTECTED_WEBSITE_YOUTUBE,
    PROTECTED_WEBSITE_TIKTOK,
    PROTECTED_WEBSITE_SNAPCHAT,
    PROTECTED_WEBSITE_X,
    PROTECTED_WEBSITE_THREADS,
    PROTECTED_WEBSITE_REDDIT,
    PROTECTED_WEBSITE_LINKEDIN,
    SETTINGS_UNINSTALL_UNREEL,
    SETTINGS_ACCESSIBILITY_BLOCKER,
    SETTINGS_SAFE,
    UNKNOWN
}

enum class InterventionAction {
    NONE,
    BLOCK,
    BLOCK_AND_RETURN
}

data class DetectionResult(
    val platform: Platform,
    val surface: ContentSurface,
    val confidence: Float,
    val packageName: String,
    val reasons: List<String>,
    val recommendedAction: InterventionAction,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val shouldBlock: Boolean
        get() = recommendedAction != InterventionAction.NONE
}
