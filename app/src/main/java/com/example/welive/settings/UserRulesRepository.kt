package com.example.welive.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weLiveDataStore by preferencesDataStore(name = "welive_user_rules")

class UserRulesRepository(private val context: Context) {
    private object Keys {
        val BlockInstagramReels = booleanPreferencesKey("block_instagram_reels")
        val BlockInstagramWebsite = booleanPreferencesKey("block_instagram_website")
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
        val PreloadHomeFeedBlockOnInstagramOpen = booleanPreferencesKey("preload_home_feed_block_on_instagram_open")
        val BlockInstagramHomeStories = booleanPreferencesKey("block_instagram_home_stories")
        val AllowInstagramStories = booleanPreferencesKey("allow_instagram_stories")
        val BlockInstagramSearchGrid = booleanPreferencesKey("block_instagram_search_grid")
        val AllowInstagramReelsFromFriends = booleanPreferencesKey("allow_instagram_reels_from_friends")
        val ReverseFromReel = booleanPreferencesKey("reverse_from_reel")
        val PulseBlockScreenOnReverse = booleanPreferencesKey("pulse_block_screen_on_reverse")
        val TemporaryAllowUntil = longPreferencesKey("temporary_allow_until")
    }

    val settings: Flow<AppSettings> = context.weLiveDataStore.data.map { preferences ->
        val scheduleSpec = preferences[Keys.InstagramAccessSchedule] ?: ""
        AppSettings(
            blockInstagramReels = preferences[Keys.BlockInstagramReels] ?: true,
            blockInstagramWebsite = preferences[Keys.BlockInstagramWebsite] ?: true,
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
            preloadHomeFeedBlockOnInstagramOpen = preferences[Keys.PreloadHomeFeedBlockOnInstagramOpen] ?: false,
            blockInstagramHomeStories = preferences[Keys.BlockInstagramHomeStories] ?: false,
            allowInstagramStories = preferences[Keys.AllowInstagramStories] ?: true,
            blockInstagramSearchGrid = preferences[Keys.BlockInstagramSearchGrid] ?: false,
            allowInstagramReelsFromFriends = preferences[Keys.AllowInstagramReelsFromFriends] ?: false,
            reverseFromReel = preferences[Keys.ReverseFromReel] ?: true,
            pulseBlockScreenOnReverse = preferences[Keys.PulseBlockScreenOnReverse] ?: false,
            temporaryAllowUntil = preferences[Keys.TemporaryAllowUntil] ?: 0L
        )
    }

    suspend fun setBlockInstagramReels(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.BlockInstagramReels] = enabled }
    }

    suspend fun setBlockInstagramWebsite(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.BlockInstagramWebsite] = enabled }
    }

    suspend fun setAppLockDurationHours(hours: Int) {
        context.weLiveDataStore.edit {
            it[Keys.AppLockDurationHours] = hours.coerceIn(1, 168)
        }
    }

    suspend fun enableAppSecurity(pin: String, durationHours: Int) {
        val normalizedPin = pin.trim()
        val salt = AppSecurity.generateSalt()
        val hash = AppSecurity.hashPin(normalizedPin, salt)
        val safeDurationHours = durationHours.coerceIn(1, 168)
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
                val durationHours = (it[Keys.AppLockDurationHours] ?: 24).coerceIn(1, 168)
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

    suspend fun setPreloadHomeFeedBlockOnInstagramOpen(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.PreloadHomeFeedBlockOnInstagramOpen] = enabled }
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

    suspend fun setPulseBlockScreenOnReverse(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.PulseBlockScreenOnReverse] = enabled }
    }

    suspend fun allowTemporarily(durationMillis: Long) {
        context.weLiveDataStore.edit {
            it[Keys.TemporaryAllowUntil] = System.currentTimeMillis() + durationMillis
        }
    }

    private companion object {
        const val HOUR_IN_MILLIS = 60L * 60L * 1000L
    }
}
