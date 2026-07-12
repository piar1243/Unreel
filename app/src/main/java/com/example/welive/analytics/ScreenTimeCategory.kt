package com.example.welive.analytics

enum class ScreenTimeCategory(
    val displayName: String,
    val storageKey: String,
    val isWebsite: Boolean
) {
    INSTAGRAM_APP("Instagram app", "instagram_app", false),
    INSTAGRAM_WEBSITE("Instagram website", "instagram_website", true),
    YOUTUBE_APP("YouTube app", "youtube_app", false),
    YOUTUBE_WEBSITE("YouTube website", "youtube_website", true),
    TIKTOK_APP("TikTok app", "tiktok_app", false),
    TIKTOK_WEBSITE("TikTok website", "tiktok_website", true),
    SNAPCHAT_APP("Snapchat app", "snapchat_app", false),
    SNAPCHAT_WEBSITE("Snapchat website", "snapchat_website", true),
    X_APP("X app", "x_app", false),
    X_WEBSITE("X website", "x_website", true),
    THREADS_APP("Threads app", "threads_app", false),
    THREADS_WEBSITE("Threads website", "threads_website", true),
    REDDIT_APP("Reddit app", "reddit_app", false),
    REDDIT_WEBSITE("Reddit website", "reddit_website", true),
    LINKEDIN_APP("LinkedIn app", "linkedin_app", false),
    LINKEDIN_WEBSITE("LinkedIn website", "linkedin_website", true);

    companion object {
        fun fromStorageKey(key: String): ScreenTimeCategory? {
            return entries.firstOrNull { it.storageKey == key }
        }
    }
}
