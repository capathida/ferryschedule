package com.example.ferryschedule.domain.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class FerryDeparture(
    val departureTime: LocalTime,
    val minutesUntilDeparture: Long,
    val statusRemarks: String? = null,
    val isEstimated: Boolean = false
) {
    val formattedTime: String
        get() = departureTime.format(TIME_FORMATTER)

    val isDepartingNow: Boolean
        get() = minutesUntilDeparture <= 0

    val isImminent: Boolean
        get() = minutesUntilDeparture in 1..4

    val countdownText: String
        get() = when {
            minutesUntilDeparture <= 0 -> "Avgår nu!"
            minutesUntilDeparture == 1L -> "om 1 min"
            minutesUntilDeparture < 60L -> "om $minutesUntilDeparture min"
            else -> {
                val hours = minutesUntilDeparture / 60
                val remainingMins = minutesUntilDeparture % 60
                if (remainingMins == 0L) {
                    "om $hours tim"
                } else {
                    "om $hours tim $remainingMins min"
                }
            }
        }

    val inCarSummary: String
        get() = "$formattedTime ($countdownText)"

    companion object {
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
