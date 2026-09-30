package com.example.ferryschedule.domain.model

import java.time.LocalTime

data class FerryScheduleState(
    val direction: RouteDirection = RouteDirection.HONO_TO_VARHOLMEN,
    val departures: List<FerryDeparture> = emptyList(),
    val lastUpdated: LocalTime = LocalTime.now(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val nextDeparture: FerryDeparture?
        get() = departures.firstOrNull()

    val upcomingDepartures: List<FerryDeparture>
        get() = if (departures.size > 1) departures.drop(1) else emptyList()
}
