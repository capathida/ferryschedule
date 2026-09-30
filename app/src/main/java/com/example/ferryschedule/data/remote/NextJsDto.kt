package com.example.ferryschedule.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NextJsFerryResponse(
    @SerialName("direction")
    val direction: String,
    @SerialName("timestamp")
    val timestamp: String,
    @SerialName("departures")
    val departures: List<NextJsDepartureDto>,
    @SerialName("notice")
    val notice: String? = null
)

@Serializable
data class NextJsDepartureDto(
    @SerialName("time")
    val time: String, // e.g. "14:20"
    @SerialName("minutesUntil")
    val minutesUntil: Long,
    @SerialName("status")
    val status: String? = null,
    @SerialName("isEstimated")
    val isEstimated: Boolean = false
)
