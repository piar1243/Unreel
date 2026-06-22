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
        val GrayscaleInstagramApp = booleanPreferencesKey("grayscale_instagram_app")
        val LimitInstagramOpensPerDay = booleanPreferencesKey("limit_instagram_opens_per_day_v2")
        val InstagramDailyOpenLimit = intPreferencesKey("instagram_daily_open_limit_v2")
        val InstagramOpenCountDate = stringPreferencesKey("instagram_open_count_date_v2")
        val InstagramOpenCount = intPreferencesKey("instagram_open_count_v2")
        val BlockInstagramHomeFeed = booleanPreferencesKey("block_instagram_home_feed")
        val BlockInstagramHomeStories = booleanPreferencesKey("block_instagram_home_stories")
        val AllowInstagramStories = booleanPreferencesKey("allow_instagram_stories")
        val BlockInstagramSearchGrid = booleanPreferencesKey("block_instagram_search_grid")
        val AllowInstagramReelsFromFriends = booleanPreferencesKey("allow_instagram_reels_from_friends")
        val ReverseFromReel = booleanPreferencesKey("reverse_from_reel")
        val PulseBlockScreenOnReverse = booleanPreferencesKey("pulse_block_screen_on_reverse")
        val TemporaryAllowUntil = longPreferencesKey("temporary_allow_until")
    }

    val settings: Flow<AppSettings> = context.weLiveDataStore.data.map { preferences ->
        AppSettings(
            blockInstagramReels = preferences[Keys.BlockInstagramReels] ?: true,
            blockInstagramWebsite = preferences[Keys.BlockInstagramWebsite] ?: true,
            grayscaleInstagramApp = preferences[Keys.GrayscaleInstagramApp] ?: false,
            limitInstagramOpensPerDay = preferences[Keys.LimitInstagramOpensPerDay] ?: false,
            instagramDailyOpenLimit = preferences[Keys.InstagramDailyOpenLimit] ?: 5,
            instagramOpenCountDate = preferences[Keys.InstagramOpenCountDate] ?: "",
            instagramOpenCount = preferences[Keys.InstagramOpenCount] ?: 0,
            blockInstagramHomeFeed = preferences[Keys.BlockInstagramHomeFeed] ?: false,
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

    suspend fun setPulseBlockScreenOnReverse(enabled: Boolean) {
        context.weLiveDataStore.edit { it[Keys.PulseBlockScreenOnReverse] = enabled }
    }

    suspend fun allowTemporarily(durationMillis: Long) {
        context.weLiveDataStore.edit {
            it[Keys.TemporaryAllowUntil] = System.currentTimeMillis() + durationMillis
        }
    }
}
