package com.example.ferryschedule.domain.repository

import com.example.ferryschedule.domain.model.FerryDeparture
import com.example.ferryschedule.domain.model.FerryScheduleState
import com.example.ferryschedule.domain.model.RouteDirection
import com.example.ferryschedule.domain.model.TrafficCamera
import com.example.ferryschedule.domain.model.TrafficStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface FerryRepository {
    /**
     * Retrieves the upcoming ferry departures for a given direction.
     */
    suspend fun getNextDepartures(
        direction: RouteDirection,
        fromTime: LocalTime = LocalTime.now(),
        count: Int = 3
    ): Result<List<FerryDeparture>>

    /**
     * Retrieves live road corridor traffic status, speeds, and queues.
     */
    suspend fun getTrafficStatus(fromTime: LocalTime = LocalTime.now()): Result<TrafficStatus>

    /**
     * Retrieves live Trafikverket camera photos for Route 155 approach.
     */
    suspend fun getTrafficCameras(): Result<List<TrafficCamera>>

    /**
     * Continuously observes departures, queues, and cameras.
     */
    fun observeFullState(
        direction: RouteDirection,
        refreshIntervalMillis: Long = 30_000L
    ): Flow<FerryScheduleState>
}
