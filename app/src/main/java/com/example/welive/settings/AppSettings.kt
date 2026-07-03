package com.example.welive.settings

import java.time.LocalDate
import java.time.ZonedDateTime

data class AppSettings(
    val blockInstagramReels: Boolean = true,
    val blockInstagramWebsite: Boolean = true,
    val appSecurityEnabled: Boolean = false,
    val appAccessPinHash: String = "",
    val appAccessPinSalt: String = "",
    val appLockDurationHours: Int = 24,
    val appLockedUntilMillis: Long = 0L,
    val grayscaleInstagramApp: Boolean = false,
    val limitInstagramOpensPerDay: Boolean = false,
    val instagramDailyOpenLimit: Int = 5,
    val instagramOpenCountDate: String = "",
    val instagramOpenCount: Int = 0,
    val limitInstagramToSchedule: Boolean = false,
    val instagramAccessScheduleSpec: String = "",
    val instagramAccessSchedule: List<DailyScheduleWindow> = emptyList(),
    val blockInstagramHomeFeed: Boolean = false,
    val preloadHomeFeedBlockOnInstagramOpen: Boolean = false,
    val blockInstagramHomeStories: Boolean = false,
    val allowInstagramStories: Boolean = true,
    val blockInstagramSearchGrid: Boolean = false,
    val allowInstagramReelsFromFriends: Boolean = false,
    val reverseFromReel: Boolean = true,
    val pulseBlockScreenOnReverse: Boolean = false,
    val temporaryAllowUntil: Long = 0L
) {
    fun hasAppPinConfigured(): Boolean {
        return appAccessPinHash.isNotBlank() && appAccessPinSalt.isNotBlank()
    }

    fun isAppLocked(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return appSecurityEnabled && hasAppPinConfigured() && appLockedUntilMillis > nowMillis
    }

    fun verifyAppPin(pin: String): Boolean {
        return hasAppPinConfigured() && AppSecurity.verifyPin(pin, appAccessPinSalt, appAccessPinHash)
    }

    fun isTemporarilyAllowed(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return temporaryAllowUntil > nowMillis
    }

    fun instagramOpensToday(todayKey: String = LocalDate.now().toString()): Int {
        return if (instagramOpenCountDate == todayKey) instagramOpenCount else 0
    }

    fun hasInstagramOpensRemaining(todayKey: String = LocalDate.now().toString()): Boolean {
        return !limitInstagramOpensPerDay ||
            instagramDailyOpenLimit <= 0 ||
            instagramOpensToday(todayKey) < instagramDailyOpenLimit
    }

    fun isWithinInstagramSchedule(now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        return !limitInstagramToSchedule ||
            InstagramAccessScheduleEvaluator.isAllowedNow(instagramAccessSchedule, now)
    }

    fun instagramScheduleStatus(now: ZonedDateTime = ZonedDateTime.now()): InstagramScheduleStatus {
        return InstagramAccessScheduleEvaluator.status(instagramAccessSchedule, now)
    }
}
