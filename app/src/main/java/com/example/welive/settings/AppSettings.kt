package com.example.welive.settings

import java.time.LocalDate
import java.time.ZonedDateTime

data class AppSettings(
    val blockInstagramReels: Boolean = true,
    val blockInstagramWebsite: Boolean = true,
    val blockYouTubeApp: Boolean = true,
    val blockYouTubeShortsWebsite: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val averageWeeklyShortFormMinutes: Int = 0,
    val onboardingCompletedAtMillis: Long = 0L,
    val instagramReelsBlockedCount: Int = 0,
    val instagramSearchGridBlockedCount: Int = 0,
    val youtubeAppBlockedCount: Int = 0,
    val youtubeShortsBlockedCount: Int = 0,
    val observedShortFormMillis: Long = 0L,
    val appSecurityEnabled: Boolean = false,
    val appAccessPinHash: String = "",
    val appAccessPinSalt: String = "",
    val appLockDurationHours: Int = 24,
    val appLockedUntilMillis: Long = 0L,
    val protectAppUninstall: Boolean = true,
    val uninstallBypassUntilMillis: Long = 0L,
    val hideLauncherIcon: Boolean = false,
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

    fun isUninstallBypassActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return uninstallBypassUntilMillis > nowMillis
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

    fun totalProtectionCount(): Int {
        return instagramReelsBlockedCount +
            instagramSearchGridBlockedCount +
            youtubeAppBlockedCount +
            youtubeShortsBlockedCount
    }

    fun estimatedTimeSavedMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        if (!onboardingCompleted || onboardingCompletedAtMillis <= 0L) return 0L
        val elapsedMillis = (nowMillis - onboardingCompletedAtMillis).coerceAtLeast(0L)
        val baselineMillis = averageWeeklyShortFormMinutes.toLong() *
            60_000L *
            elapsedMillis /
            WEEK_IN_MILLIS
        return (baselineMillis - observedShortFormMillis).coerceAtLeast(0L)
    }

    private companion object {
        const val WEEK_IN_MILLIS = 7L * 24L * 60L * 60L * 1000L
    }
}
