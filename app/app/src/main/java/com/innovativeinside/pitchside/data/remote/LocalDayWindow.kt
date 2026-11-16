package com.innovativeinside.pitchside.data.remote

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * One calendar day on the device clock, as the API needs it: [startMs] until [endMs] (exclusive) in epoch millis, and
 * the UTC dates [utcFrom]..[utcTo] that the day overlaps (one or two days; the API is queried with timezone=UTC).
 * Handles DST days (23 or 25 hours) because both ends come from the zone's own start-of-day.
 */
data class LocalDayWindow(val day: LocalDate, val startMs: Long, val endMs: Long, val utcFrom: LocalDate, val utcTo: LocalDate) {
    fun contains(ms: Long): Boolean = ms in startMs until endMs

    companion object {
        fun of(day: LocalDate, zone: ZoneId): LocalDayWindow {
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return LocalDayWindow(
                day, start, end,
                utcFrom = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate(),
                utcTo = Instant.ofEpochMilli(end - 1).atZone(ZoneOffset.UTC).toLocalDate(),
            )
        }
    }
}
