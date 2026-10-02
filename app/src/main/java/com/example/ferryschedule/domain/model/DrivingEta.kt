package com.example.ferryschedule.domain.model

import java.time.LocalTime
import java.util.Locale

data class DrivingEta(
    val durationMinutes: Int,
    val distanceMeters: Int,
    val estimatedArrivalTime: LocalTime,
    val originLat: Double,
    val originLng: Double,
    val destinationName: String,
    val routeSummary: String? = null
) {
    val formattedDuration: String
        get() = when {
            durationMinutes < 1 -> "< 1 min"
            durationMinutes < 60 -> "$durationMinutes min"
            else -> {
                val hours = durationMinutes / 60
                val mins = durationMinutes % 60
                if (mins == 0) "$hours tim" else "$hours tim $mins min"
            }
        }

    val formattedDistance: String
        get() {
            val km = distanceMeters / 1000.0
            return String.format(Locale.US, "%.1f km", km)
        }
}

sealed interface DrivingEtaState {
    data object Idle : DrivingEtaState
    data object Loading : DrivingEtaState
    data object NoApiKey : DrivingEtaState
    data object NoLocationPermission : DrivingEtaState
    data object LocationUnavailable : DrivingEtaState
    data class Success(val eta: DrivingEta) : DrivingEtaState
    data class Error(val message: String, val httpCode: Int? = null) : DrivingEtaState
}
