package com.example.welive.settings

import java.time.LocalDate

data class AppSettings(
    val blockInstagramReels: Boolean = true,
    val blockInstagramWebsite: Boolean = true,
    val grayscaleInstagramApp: Boolean = false,
    val limitInstagramOpensPerDay: Boolean = false,
    val instagramDailyOpenLimit: Int = 5,
    val instagramOpenCountDate: String = "",
    val instagramOpenCount: Int = 0,
    val blockInstagramHomeFeed: Boolean = false,
    val blockInstagramHomeStories: Boolean = false,
    val allowInstagramStories: Boolean = true,
    val blockInstagramSearchGrid: Boolean = false,
    val allowInstagramReelsFromFriends: Boolean = false,
    val reverseFromReel: Boolean = true,
    val pulseBlockScreenOnReverse: Boolean = false,
    val temporaryAllowUntil: Long = 0L
) {
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
}
