package com.example.ferryschedule.data.repository

import com.example.ferryschedule.data.local.HonoTimetableEngine
import com.example.ferryschedule.domain.model.FerryDeparture
import com.example.ferryschedule.domain.model.RouteDirection
import com.example.ferryschedule.domain.repository.FerryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalTime

class FerryRepositoryImpl(
    private val useRemoteApi: Boolean = false
) : FerryRepository {

    override suspend fun getNextDepartures(
        direction: RouteDirection,
        fromTime: LocalTime,
        count: Int
    ): Result<List<FerryDeparture>> {
        return runCatching {
            // Future Next.js API integration hook:
            // if (useRemoteApi) {
            //     val remote = NextJsApiClient.service.getDepartures(...)
            //     return@runCatching remote.toDomain()
            // }

            // Default: High-performance offline calculation engine
            HonoTimetableEngine.getNextDepartures(
                fromTime = fromTime,
                direction = direction,
                count = count
            )
        }
    }

    override fun observeDepartures(
        direction: RouteDirection,
        refreshIntervalMillis: Long
    ): Flow<List<FerryDeparture>> = flow {
        while (true) {
            val departures = getNextDepartures(direction, LocalTime.now(), 3).getOrElse { emptyList() }
            emit(departures)
            delay(refreshIntervalMillis)
        }
    }

    companion object {
        // Singleton instance for app-wide and car-service access
        val instance: FerryRepository by lazy { FerryRepositoryImpl() }
    }
}
