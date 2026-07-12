package com.example.welive.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.welive.analytics.ProtectionEvent
import com.example.welive.protection.ProtectedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weLiveDataStore by preferencesDataStore(name = "welive_user_rules")

class UserRulesRepository(private val context: Context) {
    private object Keys {
        val BlockInstagramReels = booleanPreferencesKey("block_instagram_reels")
        val BlockInstagramAppCompletely = booleanPreferencesKey("block_instagram_app_completely")
        val BlockInstagramWebsite = booleanPreferencesKey("block_instagram_website")
        val BlockYouTubeApp = booleanPreferencesKey("block_youtube_app")
        val BlockYouTubeWebsiteCompletely = booleanPreferencesKey("block_youtube_website_completely")
        val BlockYouTubeShortsWebsite = booleanPreferencesKey("block_youtube_shorts_website")
        val AllowYouTubeFriendShorts = booleanPreferencesKey("allow_youtube_friend_shorts")
        val BlockYouTubeShortsInApp = booleanPreferencesKey("block_youtube_shorts_in_app")
        val BlockTikTokAppCompletely = booleanPreferencesKey("block_tiktok_app_completely")
        val BlockTikTokWebsiteCompletely = booleanPreferencesKey("block_tiktok_website_completely")
        val BlockTikTokShortForm = booleanPreferencesKey("block_tiktok_short_form")
        val BlockSnapchatAppCompletely = booleanPreferencesKey("block_snapchat_app_completely")
        val BlockSnapchatWebsiteCompletely = booleanPreferencesKey("block_snapchat_website_completely")
        val BlockXAppCompletely = booleanPreferencesKey("block_x_app_completely")
        val BlockXWebsiteCompletely = booleanPreferencesKey("block_x_website_completely")
        val BlockThreadsAppCompletely = booleanPreferencesKey("block_threads_app_completely")
        val BlockThreadsWebsiteCompletely = booleanPreferencesKey("block_threads_website_completely")
        val BlockRedditAppCompletely = booleanPreferencesKey("block_reddit_app_completely")
        val BlockRedditWebsiteCompletely = booleanPreferencesKey("block_reddit_website_completely")
        val BlockLinkedInAppCompletely = booleanPreferencesKey("block_linkedin_app_completely")
        val BlockLinkedInWebsiteCompletely = booleanPreferencesKey("block_linkedin_website_completely")
        val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val AverageWeeklyShortFormMinutes = intPreferencesKey("average_weekly_short_form_minutes")
        val OnboardingCompletedAtMillis = longPreferencesKey("onboarding_completed_at_millis")
        val InstagramReelsBlockedCount = intPreferencesKey("instagram_reels_blocked_count")
        val InstagramSearchGridBlockedCount = intPreferencesKey("instagram_search_grid_blocked_count")
        val YouTubeAppBlockedCount = intPreferencesKey("youtube_app_blocked_count")
        val YouTubeShortsBlockedCount = intPreferencesKey("youtube_shorts_blocked_count")
        val ObservedShortFormMillis = longPreferencesKey("observed_short_form_millis")
        val AppSecurityEnabled = booleanPreferencesKey("app_security_enabled")
        val AppAccessPinHash = stringPreferencesKey("app_access_pin_hash")
        val AppAccessPinSalt = stringPreferencesKey("app_access_pin_salt")
        val AppLockDurationHours = intPreferencesKey("app_lock_duration_hours")
        val AppLockedUntilMillis = longPreferencesKey("app_locked_until_millis")
        val ProtectAppUninstall = booleanPreferencesKey("protect_app_uninstall")
        val UninstallBypassUntilMillis = longPreferencesKey("uninstall_bypass_until_millis")
        val HideLauncherIcon = booleanPreferencesKey("hide_launcher_icon")
        val GrayscaleInstagramApp = booleanPreferencesKey("grayscale_instagram_app")
        val LimitInstagramOpensPerDay = booleanPreferencesKey("limit_instagram_opens_per_day_v2")
        val InstagramDailyOpenLimit = intPreferencesKey("instagram_daily_open_limit_v2")
        val InstagramOpenCountDate = stringPreferencesKey("instagram_open_count_date_v2")
        val InstagramOpenCount = intPreferencesKey("instagram_open_count_v2")
        val LimitInstagramToSchedule = booleanPreferencesKey("limit_instagram_to_schedule")
        val InstagramAccessSchedule = stringPreferencesKey("instagram_access_schedule")
        val BlockInstagramHomeFeed = booleanPreferencesKey("block_instagram_home_feed")
        val BlockInstagramHomeStories = booleanPreferencesKey("block_instagram_home_stories")
        val AllowInstagramStories = booleanPreferencesKey("allow_instagram_stories")
        val BlockInstagramSearchGrid = booleanPreferencesKey("block_instagram_search_grid")
        val AllowInstagramReelsFromFriends = booleanPreferencesKey("allow_instagram_reels_from_friends")
        val ReverseFromReel = booleanPreferencesKey("reverse_from_reel")
        val TemporaryAllowUntil = longPreferencesKey("temporary_allow_until")
    }

    val settings: Flow<AppSettings> = context.weLiveDataStore.data.map { preferences ->
        val scheduleSpec = preferences[Keys.InstagramAccessSchedule] ?: "480-1380"
        AppSettings(
            blockInstagramReels = preferences[Keys.BlockInstagramReels] ?: true,
            blockInstagramAppCompletely = preferences[Keys.BlockInstagramAppCompletely] ?: false,
            blockInstagramWebsite = preferences[Keys.BlockInstagramWebsite] ?: true,
            blockYouTubeApp = preferences[Keys.BlockYouTubeApp] ?: true,
            blockYouTubeWebsiteCompletely = preferences[Keys.BlockYouTubeWebsiteCompletely] ?: false,
            blockYouTubeShortsWebsite = preferences[Keys.BlockYouTubeShortsWebsite] ?: true,
            allowYouTubeFriendShorts = preferences[Keys.AllowYouTubeFriendShorts] ?: false,
            blockYouTubeShortsInApp = preferences[Keys.BlockYouTubeShortsInApp] ?: false,
            blockTikTokAppCompletely = preferences[Keys.BlockTikTokAppCompletely] ?: false,
            blockTikTokWebsiteCompletely = preferences[Keys.BlockTikTokWebsiteCompletely] ?: false,
            blockTikTokShortForm = preferences[Keys.BlockTikTokShortForm] ?: true,
            blockSnapchatAppCompletely = preferences[Keys.BlockSnapchatAppCompletely] ?: false,
            blockSnapchatWebsiteCompletely = preferences[Keys.BlockSnapchatWebsiteCompletely] ?: false,
            blockXAppCompletely = preferences[Keys.BlockXAppCompletely] ?: false,
            blockXWebsiteCompletely = preferences[Keys.BlockXWebsiteCompletely] ?: false,
            blockThreadsAppCompletely = preferences[Keys.BlockThreadsAppCompletely] ?: false,
            blockThreadsWebsiteCompletely = preferences[Keys.BlockThreadsWebsiteCompletely] ?: false,
            blockRedditAppCompletely = preferences[Keys.BlockRedditAppCompletely] ?: false,
            blockRedditWebsiteCompletely = preferences[Keys.BlockRedditWebsiteCompletely] ?: false,
            blockLinkedInAppCompletely = preferences[Keys.BlockLinkedInAppCompletely] ?: false,
            blockLinkedInWebsiteCompletely = preferences[Keys.BlockLinkedInWebsiteCompletely] ?: false,
            onboardingCompleted = preferences[Keys.OnboardingCompleted] ?: false,
            averageWeeklyShortFormMinutes = preferences[Keys.AverageWeeklyShortFormMinutes] ?: 0,
            onboardingCompletedAtMillis = preferences[Keys.OnboardingCompletedAtMillis] ?: 0L,
            instagramReelsBlockedCount = preferences[Keys.InstagramReelsBlockedCount] ?: 0,
            instagramSearchGridBlockedCount = preferences[Keys.InstagramSearchGridBlockedCount] ?: 0,
            youtubeAppBlockedCount = preferences[Keys.YouTubeAppBlockedCount] ?: 0,
            youtubeShortsBlockedCount = preferences[Keys.YouTubeShortsBlockedCount] ?: 0,
            observedShortFormMillis = preferences[Keys.ObservedShortFormMillis] ?: 0L,
            appSecurityEnabled = preferences[Keys.AppSecurityEnabled] ?: false,
            appAccessPinHash = preferences[Keys.AppAccessPinHash] ?: "",
            appAccessPinSalt = preferences[Keys.AppAccessPinSalt] ?: "",
            appLockDurationHours = preferences[Keys.AppLockDurationHours] ?: 24,
            appLockedUntilMillis = preferences[Keys.AppLockedUntilMillis] ?: 0L,
            protectAppUninstall = preferences[Keys.ProtectAppUninstall] ?: true,
            uninstallBypassUntilMillis = preferences[Keys.UninstallBypassUntilMillis] ?: 0L,
            hideLauncherIcon = preferences[Keys.HideLauncherIcon] ?: false,
            grayscaleInstagramApp = preferences[Keys.GrayscaleInstagramApp] ?: false,
            limitInstagramOpensPerDay = preferences[Keys.LimitInstagramOpensPerDay] ?: false,
            instagramDailyOpenLimit = preferences[Keys.InstagramDailyOpenLimit] ?: 5,
            instagramOpenCountDate = preferences[Keys.InstagramOpenCountDate] ?: "",
            instagramOpenCount = preferences[Keys.InstagramOpenCount] ?: 0,
            limitInstagramToSchedule = preferences[Keys.LimitInstagramToSchedule] ?: false,
            instagramAccessScheduleSpec = scheduleSpec,
            instagramAccessSchedule = InstagramAccessScheduleCodec.decode(scheduleSpec),
            blockInstagramHomeFeed = preferences[Keys.BlockInstagramHomeFeed] ?: false,
            blockInstagramHomeStories = preferences[Keys.BlockInstagramHomeStories] ?: false,
            allowInstagramStories = preferences[Keys.AllowInstagramStories] ?: true,
            blockInstagramSearchGrid = preferences[Keys.BlockInstagramSearchGrid] ?: false,
            allowInstagramReelsFromFriends = preferences[Keys.AllowInstagramReelsFromFriends] ?: false,
            reverseFromReel = preferences[Keys.ReverseFromReel] ?: true,
            temporaryAllowUntil = preferences[Keys.TemporaryAllowUntil] ?: 0L
        )
    }

    suspend fun setBlockInstagramReels(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.BlockInstagramReels] = enabled }
    }

    suspend fun setTotalAppBlock(app: ProtectedApp, enabled: Boolean) {
        context.weLiveDataStore.edit { preferences ->
            preferences[when (app) {
                ProtectedApp.INSTAGRAM -> Keys.BlockInstagramAppCompletely
                ProtectedApp.YOUTUBE -> Keys.BlockYouTubeApp
                ProtectedApp.TIKTOK -> Keys.BlockTikTokAppCompletely
                ProtectedApp.SNAPCHAT -> Keys.BlockSnapchatAppCompletely
                ProtectedApp.X -> Keys.BlockXAppCompletely
                ProtectedApp.THREADS -> Keys.BlockThreadsAppCompletely
                ProtectedApp.REDDIT -> Keys.BlockRedditAppCompletely
                ProtectedApp.LINKEDIN -> Keys.BlockLinkedInAppCompletely
            }] = enabled
            if (app == ProtectedApp.TIKTOK && enabled) {
                preferences[Keys.BlockTikTokShortForm] = false
            }
        }
    }

    suspend fun setTotalWebsiteBlock(app: ProtectedApp, enabled: Boolean) {
        context.weLiveDataStore.edit { preferences ->
            preferences[when (app) {
                ProtectedApp.INSTAGRAM -> Keys.BlockInstagramWebsite
                ProtectedApp.YOUTUBE -> Keys.BlockYouTubeWebsiteCompletely
                ProtectedApp.TIKTOK -> Keys.BlockTikTokWebsiteCompletely
                ProtectedApp.SNAPCHAT -> Keys.BlockSnapchatWebsiteCompletely
                ProtectedApp.X -> Keys.BlockXWebsiteCompletely
                ProtectedApp.THREADS -> Keys.BlockThreadsWebsiteCompletely
                ProtectedApp.REDDIT -> Keys.BlockRedditWebsiteCompletely
                ProtectedApp.LINKEDIN -> Keys.BlockLinkedInWebsiteCompletely
            }] = enabled
        }
    }

    suspend fun setBlockTikTokShortForm(enabled: Boolean) {
        context.weLiveDataStore.edit {
            it[Keys.BlockTikTokShortForm] = enabled
            if (enabled) {
                it[Keys.BlockTikTokAppCompletely] = false
            }
        }
    }

    suspend fun setBlockInstagramWebsite(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.BlockInstagramWebsite] = enabled }
    }

    suspend fun setBlockYouTubeApp(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.BlockYouTubeApp] = enabled }
    }

    suspend fun setBlockYouTubeShortsWebsite(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.BlockYouTubeShortsWebsite] = enabled }
    }

    suspend fun setAllowYouTubeFriendShorts(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.AllowYouTubeFriendShorts] = enabled }
    }

    suspend fun setBlockYouTubeShortsInApp(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.BlockYouTubeShortsInApp] = enabled }
    }

    suspend fun completeOnboarding(averageWeeklyMinutes: Int) {
        context.weLiveDataStore.edit {
            it[Keys.AverageWeeklyShortFormMinutes] = averageWeeklyMinutes.coerceIn(0, 10_080)
            it[Keys.OnboardingCompletedAtMillis] = System.currentTimeMillis()
            it[Keys.OnboardingCompleted] = true
        }
    }

    suspend fun recordProtectionEvent(event: ProtectionEvent) {
        context.weLiveDataStore.edit { preferences ->
            val key = when (event) {
                ProtectionEvent.INSTAGRAM_REEL -> Keys.InstagramReelsBlockedCount
                ProtectionEvent.INSTAGRAM_SEARCH_GRID -> Keys.InstagramSearchGridBlockedCount
                ProtectionEvent.YOUTUBE_APP -> Keys.YouTubeAppBlockedCount
                ProtectionEvent.YOUTUBE_SHORT -> Keys.YouTubeShortsBlockedCount
            }
            preferences[key] = (preferences[key] ?: 0) + 1
        }
    }

    suspend fun addObservedShortFormTime(durationMillis: Long) {
        if (durationMillis <= 0L) return
        context.weLiveDataStore.edit {
            val safeDuration = durationMillis.coerceAtMost(MAX_EXPOSURE_SESSION_MILLIS)
            it[Keys.ObservedShortFormMillis] =
                (it[Keys.ObservedShortFormMillis] ?: 0L) + safeDuration
        }
    }

    suspend fun setAppLockDurationHours(hours: Int) {
        context.weLiveDataStore.edit {
            it[Keys.AppLockDurationHours] = hours.coerceIn(1, 720)
        }
    }

    suspend fun enableAppSecurity(pin: String, durationHours: Int) {
        val normalizedPin = pin.trim()
        val salt = AppSecurity.generateSalt()
        val hash = AppSecurity.hashPin(normalizedPin, salt)
        val safeDurationHours = durationHours.coerceIn(1, 720)
        context.weLiveDataStore.edit {
            it[Keys.AppSecurityEnabled] = true
            it[Keys.AppAccessPinSalt] = salt
            it[Keys.AppAccessPinHash] = hash
            it[Keys.AppLockDurationHours] = safeDurationHours
            it[Keys.AppLockedUntilMillis] = System.currentTimeMillis() + safeDurationHours * HOUR_IN_MILLIS
        }
    }

    suspend fun updateAppSecurityPin(pin: String) {
        val normalizedPin = pin.trim()
        val salt = AppSecurity.generateSalt()
        val hash = AppSecurity.hashPin(normalizedPin, salt)
        context.weLiveDataStore.edit {
            it[Keys.AppAccessPinSalt] = salt
            it[Keys.AppAccessPinHash] = hash
        }
    }

    suspend fun armAppSecurityLock() {
        context.weLiveDataStore.edit {
            val enabled = it[Keys.AppSecurityEnabled] ?: false
            val hasPin = !(it[Keys.AppAccessPinHash].isNullOrBlank() || it[Keys.AppAccessPinSalt].isNullOrBlank())
            if (enabled && hasPin) {
                val durationHours = (it[Keys.AppLockDurationHours] ?: 24).coerceIn(1, 720)
                it[Keys.AppLockedUntilMillis] = System.currentTimeMillis() + durationHours * HOUR_IN_MILLIS
            }
        }
    }

    suspend fun setProtectAppUninstall(enabled: Boolean) {
        context.weLiveDataStore.edit {
            it[Keys.ProtectAppUninstall] = enabled
            if (!enabled) {
                it[Keys.UninstallBypassUntilMillis] = 0L
            }
        }
    }

    suspend fun setHideLauncherIcon(enabled: Boolean) {
        context.weLiveDataStore.edit {
            it[Keys.HideLauncherIcon] = enabled
        }
    }

    suspend fun allowAppUninstallTemporarily(durationMillis: Long) {
        context.weLiveDataStore.edit {
            val safeDurationMillis = durationMillis.coerceIn(60_000L, 30L * 60L * 1000L)
            it[Keys.UninstallBypassUntilMillis] = System.currentTimeMillis() + safeDurationMillis
        }
    }

    suspend fun clearAppUninstallBypass() {
        context.weLiveDataStore.edit {
            it[Keys.UninstallBypassUntilMillis] = 0L
        }
    }

    suspend fun disableAppSecurity() {
        context.weLiveDataStore.edit {
            it[Keys.AppSecurityEnabled] = false
            it[Keys.AppAccessPinSalt] = ""
            it[Keys.AppAccessPinHash] = ""
            it[Keys.AppLockedUntilMillis] = 0L
        }
    }

    suspend fun setGrayscaleInstagramApp(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.GrayscaleInstagramApp] = enabled }
    }

    suspend fun setLimitInstagramOpensPerDay(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.LimitInstagramOpensPerDay] = enabled }
    }

    suspend fun setInstagramDailyOpenLimit(limit: Int) {
        context.weLiveDataStore.edit {
            it[Keys.InstagramDailyOpenLimit] = limit.coerceIn(1, 99)
        }
    }

    suspend fun recordInstagramAppOpen(dateKey: String) {
        context.weLiveDataStore.edit {
            val currentDate = it[Keys.InstagramOpenCountDate]
            val currentCount = if (currentDate == dateKey) {
                it[Keys.InstagramOpenCount] ?: 0
            } else {
                0
            }
            it[Keys.InstagramOpenCountDate] = dateKey
            it[Keys.InstagramOpenCount] = currentCount + 1
        }
    }

    suspend fun setLimitInstagramToSchedule(enabled: Boolean) {
        context.weLiveDataStore.edit {
            it[Keys.LimitInstagramToSchedule] = enabled
            if (enabled && (it[Keys.InstagramAccessSchedule].isNullOrBlank())) {
                it[Keys.InstagramAccessSchedule] = InstagramAccessScheduleCodec.encode(
                    InstagramAccessScheduleCodec.defaultWindows()
                )
            }
        }
    }

    suspend fun setInstagramAccessSchedule(windows: List<DailyScheduleWindow>) {
        context.weLiveDataStore.edit {
            it[Keys.InstagramAccessSchedule] = InstagramAccessScheduleCodec.encode(windows)
        }
    }

    suspend fun setBlockInstagramHomeFeed(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.BlockInstagramHomeFeed] = enabled }
    }

    suspend fun setBlockInstagramHomeStories(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.BlockInstagramHomeStories] = enabled }
    }

    suspend fun setAllowInstagramStories(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.AllowInstagramStories] = enabled }
    }

    suspend fun setBlockInstagramSearchGrid(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.BlockInstagramSearchGrid] = enabled }
    }

    suspend fun setAllowInstagramReelsFromFriends(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.AllowInstagramReelsFromFriends] = enabled }
    }

    suspend fun setReverseFromReel(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.ReverseFromReel] = enabled }
    }

    suspend fun allowTemporarily(durationMillis: Long) {
        context.weLiveDataStore.edit {
            it[Keys.TemporaryAllowUntil] = System.currentTimeMillis() + durationMillis
        }
    }

    private companion object {
        const val HOUR_IN_MILLIS = 60L * 60L * 1000L
        const val MAX_EXPOSURE_SESSION_MILLIS = 10L * 60L * 1000L
    }
}
