package com.example.welive.settings

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class DailyScheduleWindow(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int
) {
    fun normalized(): DailyScheduleWindow {
        return DailyScheduleWindow(
            startMinuteOfDay = startMinuteOfDay.coerceIn(0, MINUTE_OF_DAY_MAX),
            endMinuteOfDay = endMinuteOfDay.coerceIn(0, MINUTE_OF_DAY_MAX)
        )
    }

    fun isNonEmpty(): Boolean = startMinuteOfDay != endMinuteOfDay

    fun containsMinute(minuteOfDay: Int): Boolean {
        val normalizedMinute = minuteOfDay.coerceIn(0, MINUTE_OF_DAY_MAX)
        if (!isNonEmpty()) return false
        return if (startMinuteOfDay < endMinuteOfDay) {
            normalizedMinute in startMinuteOfDay until endMinuteOfDay
        } else {
            normalizedMinute >= startMinuteOfDay || normalizedMinute < endMinuteOfDay
        }
    }

    companion object {
        const val MINUTE_OF_DAY_MAX = 1_439
    }
}

object InstagramAccessScheduleCodec {
    fun encode(windows: List<DailyScheduleWindow>): String {
        return windows
            .asSequence()
            .map { it.normalized() }
            .filter { it.isNonEmpty() }
            .distinctBy { "${it.startMinuteOfDay}-${it.endMinuteOfDay}" }
            .sortedBy { it.startMinuteOfDay }
            .joinToString(",") { "${it.startMinuteOfDay}-${it.endMinuteOfDay}" }
    }

    fun decode(spec: String): List<DailyScheduleWindow> {
        if (spec.isBlank()) return emptyList()
        return spec.split(",")
            .mapNotNull { token ->
                val parts = token.split("-")
                if (parts.size != 2) return@mapNotNull null
                val start = parts[0].toIntOrNull() ?: return@mapNotNull null
                val end = parts[1].toIntOrNull() ?: return@mapNotNull null
                DailyScheduleWindow(start, end).normalized().takeIf { it.isNonEmpty() }
            }
            .distinctBy { "${it.startMinuteOfDay}-${it.endMinuteOfDay}" }
            .sortedBy { it.startMinuteOfDay }
    }

    fun defaultWindows(): List<DailyScheduleWindow> {
        return listOf(DailyScheduleWindow(18 * 60, 19 * 60))
    }
}

data class InstagramScheduleStatus(
    val isAllowedNow: Boolean,
    val currentWindow: DailyScheduleWindow? = null,
    val nextWindow: DailyScheduleWindow? = null,
    val nextTransitionAt: ZonedDateTime? = null
)

object InstagramAccessScheduleEvaluator {
    fun isAllowedNow(
        windows: List<DailyScheduleWindow>,
        now: ZonedDateTime = ZonedDateTime.now()
    ): Boolean {
        return status(windows, now).isAllowedNow
    }

    fun status(
        windows: List<DailyScheduleWindow>,
        now: ZonedDateTime = ZonedDateTime.now()
    ): InstagramScheduleStatus {
        val normalizedWindows = windows.map { it.normalized() }.filter { it.isNonEmpty() }
        if (normalizedWindows.isEmpty()) {
            return InstagramScheduleStatus(isAllowedNow = false)
        }

        val intervals = expandedIntervalsAround(normalizedWindows, now)
        val activeInterval = intervals.firstOrNull { interval ->
            !now.isBefore(interval.start) && now.isBefore(interval.end)
        }
        if (activeInterval != null) {
            return InstagramScheduleStatus(
                isAllowedNow = true,
                currentWindow = activeInterval.window,
                nextTransitionAt = activeInterval.end
            )
        }

        val nextInterval = intervals
            .filter { it.start.isAfter(now) }
            .minByOrNull { it.start }

        return InstagramScheduleStatus(
            isAllowedNow = false,
            nextWindow = nextInterval?.window,
            nextTransitionAt = nextInterval?.start
        )
    }

    private fun expandedIntervalsAround(
        windows: List<DailyScheduleWindow>,
        now: ZonedDateTime
    ): List<ExpandedWindowInterval> {
        val localDate = now.toLocalDate()
        return (-1L..1L).flatMap { dayOffset ->
            val date = localDate.plusDays(dayOffset)
            windows.map { window ->
                val start = date.atStartOfDay(now.zone).plusMinutes(window.startMinuteOfDay.toLong())
                val endDate = if (window.startMinuteOfDay < window.endMinuteOfDay) {
                    date
                } else {
                    date.plusDays(1)
                }
                val end = endDate.atStartOfDay(now.zone).plusMinutes(window.endMinuteOfDay.toLong())
                ExpandedWindowInterval(window = window, start = start, end = end)
            }
        }
    }

    private data class ExpandedWindowInterval(
        val window: DailyScheduleWindow,
        val start: ZonedDateTime,
        val end: ZonedDateTime
    )
}

fun DailyScheduleWindow.startLocalTime(): LocalTime {
    return LocalTime.MIN.plusMinutes(startMinuteOfDay.toLong())
}

fun DailyScheduleWindow.endLocalTime(): LocalTime {
    return LocalTime.MIN.plusMinutes(endMinuteOfDay.toLong())
}

fun DailyScheduleWindow.crossesMidnight(): Boolean = startMinuteOfDay > endMinuteOfDay

fun ZonedDateTime.minuteOfDay(): Int = toLocalTime().hour * 60 + toLocalTime().minute

fun ZonedDateTime.localScheduleDate(): LocalDate = toLocalDate()
