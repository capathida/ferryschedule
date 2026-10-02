package com.example.ferryschedule.data.local

import com.example.ferryschedule.domain.model.FerryDeparture
import com.example.ferryschedule.domain.model.FerryRoute
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
                statusRemarks = "Överfart ~${direction.crossingMinutes} min",
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
     * Generates a complete 24-hour departure schedule list based on the route frequency pattern.
     */
    fun getDailySchedule(direction: RouteDirection): List<LocalTime> {
        val schedule = mutableListOf<LocalTime>()
        val offsetMinutes = if (direction == direction.route.returnDirection) 5 else 0

        for (hour in 0..23) {
            val minuteIntervals: List<Int> = when (direction.route) {
                FerryRoute.HONOLEDEN -> when (hour) {
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
                FerryRoute.BJORKOLEDEN -> when (hour) {
                    in 1..4 -> listOf(0)
                    in 6..8 -> listOf(0, 15, 30, 45)
                    in 9..14 -> listOf(0, 20, 40)
                    in 15..18 -> listOf(0, 15, 30, 45)
                    else -> listOf(0, 30)
                }
                FerryRoute.SVANESUNDSLEDEN -> when (hour) {
                    in 1..4 -> listOf(10)
                    in 6..8 -> listOf(0, 15, 30, 45)
                    in 9..15 -> listOf(0, 20, 40)
                    in 16..18 -> listOf(0, 15, 30, 45)
                    else -> listOf(10, 40)
                }
                FerryRoute.GULLMARSLEDEN -> when (hour) {
                    in 1..4 -> listOf(0)
                    in 6..18 -> listOf(0, 20, 40)
                    else -> listOf(0, 30)
                }
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
