package com.example.ferryschedule.data.repository

import com.example.ferryschedule.data.local.HonoTimetableEngine
import com.example.ferryschedule.data.remote.TrafikverketService
import com.example.ferryschedule.domain.model.FerryDeparture
import com.example.ferryschedule.domain.model.FerryScheduleState
import com.example.ferryschedule.domain.model.RouteDirection
import com.example.ferryschedule.domain.model.TrafficCamera
import com.example.ferryschedule.domain.model.TrafficStatus
import com.example.ferryschedule.domain.repository.FerryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalTime

class FerryRepositoryImpl(
    private val trafikverketService: TrafikverketService = TrafikverketService()
) : FerryRepository {

    private var cachedTrafficStatus: TrafficStatus? = null

    override suspend fun getNextDepartures(
        direction: RouteDirection,
        fromTime: LocalTime,
        count: Int
    ): Result<List<FerryDeparture>> {
        return runCatching {
            // 1. Try real-time official Trafikverket API first
            try {
                val liveList = trafikverketService.fetchLiveDepartures(direction)
                if (liveList.isNotEmpty()) {
                    // Attach current queue breakdown to departures
                    val queue = cachedTrafficStatus?.let {
                        if (direction == RouteDirection.HONO_TO_VARHOLMEN) it.hono.breakdown else it.varholmen.breakdown
                    }
                    return@runCatching liveList.map { dep ->
                        dep.copy(queueBreakdown = queue)
                    }.take(count)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Offline fallback: High-precision calculation engine
            HonoTimetableEngine.getNextDepartures(
                fromTime = fromTime,
                direction = direction,
                count = count
            )
        }
    }

    override suspend fun getTrafficStatus(fromTime: LocalTime): Result<TrafficStatus> {
        return runCatching {
            val status = trafikverketService.fetchTrafficStatus(fromTime)
            cachedTrafficStatus = status
            status
        }
    }

    override suspend fun getTrafficCameras(): Result<List<TrafficCamera>> {
        return runCatching {
            trafikverketService.fetchTrafficCameras()
        }
    }

    override fun observeFullState(
        direction: RouteDirection,
        refreshIntervalMillis: Long
    ): Flow<FerryScheduleState> = flow {
        while (true) {
            val traffic = getTrafficStatus().getOrNull()
            val departures = getNextDepartures(direction, LocalTime.now(), 3).getOrElse { emptyList() }
            val cameras = getTrafficCameras().getOrElse { emptyList() }

            emit(
                FerryScheduleState(
                    direction = direction,
                    departures = departures,
                    trafficStatus = traffic,
                    cameras = cameras,
                    lastUpdated = LocalTime.now(),
                    isLoading = false
                )
            )

            delay(refreshIntervalMillis)
        }
    }

    companion object {
        val instance: FerryRepository by lazy { FerryRepositoryImpl() }
    }
}
