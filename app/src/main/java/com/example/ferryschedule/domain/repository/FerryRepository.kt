package com.example.ferryschedule.domain.repository

import com.example.ferryschedule.domain.model.FerryDeparture
import com.example.ferryschedule.domain.model.RouteDirection
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface FerryRepository {
    /**
     * Retrieves the upcoming ferry departures for a given direction starting from [fromTime].
     * @param count Number of departures to return (default: 3).
     */
    suspend fun getNextDepartures(
        direction: RouteDirection,
        fromTime: LocalTime = LocalTime.now(),
        count: Int = 3
    ): Result<List<FerryDeparture>>

    /**
     * Observes departures with periodic refreshes.
     */
    fun observeDepartures(
        direction: RouteDirection,
        refreshIntervalMillis: Long = 30_000L
    ): Flow<List<FerryDeparture>>
}
