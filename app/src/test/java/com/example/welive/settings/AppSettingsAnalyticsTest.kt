package com.example.welive.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsAnalyticsTest {
    @Test
    fun estimatedTimeSavedAccruesFromBaselineAndSubtractsExposure() {
        val startedAt = 1_000_000L
        val settings = AppSettings(
            onboardingCompleted = true,
            averageWeeklyShortFormMinutes = 7 * 24 * 60,
            onboardingCompletedAtMillis = startedAt,
            observedShortFormMillis = 15L * 60L * 1000L
        )

        val result = settings.estimatedTimeSavedMillis(
            nowMillis = startedAt + 60L * 60L * 1000L
        )

        assertEquals(45L * 60L * 1000L, result)
    }

    @Test
    fun estimatedTimeSavedNeverGoesNegative() {
        val settings = AppSettings(
            onboardingCompleted = true,
            averageWeeklyShortFormMinutes = 60,
            onboardingCompletedAtMillis = 1_000L,
            observedShortFormMillis = 10L * 60L * 1000L
        )

        assertEquals(0L, settings.estimatedTimeSavedMillis(nowMillis = 2_000L))
    }

    @Test
    fun totalProtectionCountCombinesEveryProtectedSurface() {
        val settings = AppSettings(
            instagramReelsBlockedCount = 4,
            instagramSearchGridBlockedCount = 3,
            youtubeAppBlockedCount = 2,
            youtubeShortsBlockedCount = 1
        )

        assertEquals(10, settings.totalProtectionCount())
    }
}
