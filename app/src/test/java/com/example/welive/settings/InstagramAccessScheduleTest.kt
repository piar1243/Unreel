package com.example.welive.settings

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramAccessScheduleTest {
    @Test
    fun daytimeWindowAllowsOnlyWithinRange() {
        val windows = listOf(DailyScheduleWindow(9 * 60, 11 * 60))

        assertTrue(InstagramAccessScheduleEvaluator.isAllowedNow(windows, chicagoTime(2026, 7, 2, 9, 30)))
        assertFalse(InstagramAccessScheduleEvaluator.isAllowedNow(windows, chicagoTime(2026, 7, 2, 8, 59)))
        assertFalse(InstagramAccessScheduleEvaluator.isAllowedNow(windows, chicagoTime(2026, 7, 2, 11, 0)))
    }

    @Test
    fun overnightWindowAllowsAcrossMidnight() {
        val windows = listOf(DailyScheduleWindow(22 * 60, 2 * 60))

        assertTrue(InstagramAccessScheduleEvaluator.isAllowedNow(windows, chicagoTime(2026, 7, 2, 23, 15)))
        assertTrue(InstagramAccessScheduleEvaluator.isAllowedNow(windows, chicagoTime(2026, 7, 3, 1, 45)))
        assertFalse(InstagramAccessScheduleEvaluator.isAllowedNow(windows, chicagoTime(2026, 7, 3, 2, 0)))
    }

    @Test
    fun nextTransitionReturnsNextStartWhenCurrentlyLocked() {
        val windows = listOf(DailyScheduleWindow(12 * 60, 13 * 60))
        val status = InstagramAccessScheduleEvaluator.status(
            windows = windows,
            now = chicagoTime(2026, 7, 2, 10, 0)
        )

        assertFalse(status.isAllowedNow)
        assertEquals(chicagoTime(2026, 7, 2, 12, 0), status.nextTransitionAt)
    }

    @Test
    fun nextTransitionReturnsWindowEndWhenCurrentlyAllowed() {
        val windows = listOf(DailyScheduleWindow(12 * 60, 13 * 60))
        val status = InstagramAccessScheduleEvaluator.status(
            windows = windows,
            now = chicagoTime(2026, 7, 2, 12, 15)
        )

        assertTrue(status.isAllowedNow)
        assertEquals(chicagoTime(2026, 7, 2, 13, 0), status.nextTransitionAt)
    }

    @Test
    fun codecRoundTripsWindows() {
        val windows = listOf(
            DailyScheduleWindow(18 * 60, 19 * 60),
            DailyScheduleWindow(8 * 60, 9 * 60 + 30)
        )

        val encoded = InstagramAccessScheduleCodec.encode(windows)
        val decoded = InstagramAccessScheduleCodec.decode(encoded)

        assertEquals(
            listOf(
                DailyScheduleWindow(8 * 60, 9 * 60 + 30),
                DailyScheduleWindow(18 * 60, 19 * 60)
            ),
            decoded
        )
    }

    private fun chicagoTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): ZonedDateTime {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("America/Chicago"))
    }
}
