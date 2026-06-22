package com.example.welive.detection

enum class Platform {
    INSTAGRAM,
    INSTAGRAM_WEB,
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
