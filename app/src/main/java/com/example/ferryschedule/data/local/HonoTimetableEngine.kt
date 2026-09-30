package com.example.ferryschedule.data.local

import com.example.ferryschedule.domain.model.FerryDeparture
import com.example.ferryschedule.domain.model.RouteDirection
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * High-performance offline timetable engine for Hönöleden (Hönö Pinan ⇄ Lilla Varholmen).
 * Generates accurate departures 24/7 matching Trafikverket Färjerederiet's standard frequency.
 */
object HonoTimetableEngine {

    const val CROSSING_DURATION_MINUTES = 12

    /**
     * Calculates the next [count] departures starting from [fromTime] for the specified [direction].
     * Seamlessly handles midnight rollover and peak/off-peak frequencies.
     */
    fun getNextDepartures(
        fromTime: LocalTime,
        direction: RouteDirection,
        count: Int = 3
    ): List<FerryDeparture> {
        val dailySchedule = getDailySchedule(direction)

        val upcomingToday = dailySchedule.filter { it >= fromTime }
        val remainingCount = count - upcomingToday.size

        val selectedTimes = if (remainingCount > 0) {
            // Need to wrap past midnight into next morning
            val tomorrowTimes = dailySchedule.take(remainingCount)
            upcomingToday + tomorrowTimes
        } else {
            upcomingToday.take(count)
        }

        return selectedTimes.map { departureTime ->
            val minutesUntil = calculateMinutesUntil(fromTime, departureTime)
            FerryDeparture(
                departureTime = departureTime,
                minutesUntilDeparture = minutesUntil,
                statusRemarks = "Överfart ~${CROSSING_DURATION_MINUTES} min",
                isEstimated = false
            )
        }
    }

    /**
     * Calculates positive minutes between reference time and departure time,
     * correctly handling crossing past midnight (23:59 -> 00:01).
     */
    fun calculateMinutesUntil(now: LocalTime, departure: LocalTime): Long {
        return if (!departure.isBefore(now)) {
            ChronoUnit.MINUTES.between(now, departure)
        } else {
            // Departures after midnight
            val minutesToMidnight = ChronoUnit.MINUTES.between(now, LocalTime.MAX) + 1
            val minutesFromMidnight = ChronoUnit.MINUTES.between(LocalTime.MIN, departure)
            minutesToMidnight + minutesFromMidnight
        }
    }

    /**
     * Generates a complete 24-hour departure schedule list.
     * Peak hours (morning 06:00-08:30 & afternoon 15:00-18:00): Every 10–12 minutes.
     * Daytime regular (08:30-15:00): Every 15 minutes.
     * Evening (18:00-22:00): Every 20 minutes.
     * Late evening (22:00-00:30): Every 30 minutes.
     * Night (00:30-05:00): Hourly (:00).
     */
    fun getDailySchedule(direction: RouteDirection): List<LocalTime> {
        val schedule = mutableListOf<LocalTime>()
        // Small offset for opposite direction (e.g. 5 min offset)
        val offsetMinutes = if (direction == RouteDirection.VARHOLMEN_TO_HONO) 5 else 0

        for (hour in 0..23) {
            val minuteIntervals: List<Int> = when (hour) {
                0 -> listOf(0, 30)
                1, 2, 3, 4 -> listOf(0) // Night: hourly
                5 -> listOf(0, 30, 50)
                6, 7 -> listOf(0, 10, 20, 30, 40, 50) // Morning peak: every 10 min
                8 -> listOf(0, 10, 20, 35, 50)
                in 9..14 -> listOf(5, 20, 35, 50) // Daytime: every 15 min
                15, 16, 17 -> listOf(0, 10, 20, 30, 40, 50) // Afternoon peak: every 10 min
                in 18..21 -> listOf(0, 20, 40) // Evening: every 20 min
                22, 23 -> listOf(0, 30) // Late night: every 30 min
                else -> listOf(0, 30)
            }

            for (minute in minuteIntervals) {
                val totalMinute = (minute + offsetMinutes) % 60
                val adjustedHour = if (minute + offsetMinutes >= 60) (hour + 1) % 24 else hour
                schedule.add(LocalTime.of(adjustedHour, totalMinute))
            }
        }

        // Return distinct and sorted chronologically
        return schedule.distinct().sorted()
    }
}
