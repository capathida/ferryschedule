package com.example.ferryschedule.domain.model

import java.time.LocalTime

data class FerryScheduleState(
    val direction: RouteDirection = RouteDirection.HONO_TO_VARHOLMEN,
    val departures: List<FerryDeparture> = emptyList(),
    val trafficStatus: TrafficStatus? = null,
    val cameras: List<TrafficCamera> = emptyList(),
    val deviations: List<String> = emptyList(),
    val hasCancellations: Boolean = false,
    val lastUpdated: LocalTime = LocalTime.now(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val nextDeparture: FerryDeparture?
        get() = departures.firstOrNull()

    val upcomingDepartures: List<FerryDeparture>
        get() = if (departures.size > 1) departures.drop(1) else emptyList()

    val currentQueueBreakdown: QueueBreakdown?
        get() = when (direction) {
            RouteDirection.HONO_TO_VARHOLMEN -> trafficStatus?.hono?.breakdown
            RouteDirection.VARHOLMEN_TO_HONO -> trafficStatus?.varholmen?.breakdown
            RouteDirection.VARHOLMEN_TO_BJORKO -> trafficStatus?.varholmen?.breakdown
            else -> null
        }
}
