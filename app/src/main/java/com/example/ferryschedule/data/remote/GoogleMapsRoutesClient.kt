package com.example.ferryschedule.data.remote

import com.example.ferryschedule.domain.model.DrivingEta
import com.example.ferryschedule.domain.model.DrivingEtaState
import com.example.ferryschedule.domain.model.RouteDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Serializable
data class RoutesComputeRequest(
    val origin: RouteWaypoint,
    val destination: RouteWaypoint,
    val travelMode: String = "DRIVE",
    val routingPreference: String = "TRAFFIC_AWARE"
)

@Serializable
data class RouteWaypoint(
    val location: RouteLocation
)

@Serializable
data class RouteLocation(
    val latLng: RouteLatLng
)

@Serializable
data class RouteLatLng(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class RoutesComputeResponse(
    val routes: List<ComputedRoute> = emptyList()
)

@Serializable
data class ComputedRoute(
    val distanceMeters: Int? = null,
    val duration: String? = null,
    val description: String? = null
)

class GoogleMapsRoutesClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    suspend fun computeDrivingEta(
        originLat: Double,
        originLng: Double,
        direction: RouteDirection,
        apiKey: String,
        referenceTime: LocalTime = LocalTime.now()
    ): DrivingEtaState = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext DrivingEtaState.NoApiKey
        }

        val requestPayload = RoutesComputeRequest(
            origin = RouteWaypoint(RouteLocation(RouteLatLng(originLat, originLng))),
            destination = RouteWaypoint(RouteLocation(RouteLatLng(direction.departureLatitude, direction.departureLongitude)))
        )

        val jsonBody = json.encodeToString(requestPayload)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val httpRequest = Request.Builder()
            .url("https://routes.googleapis.com/directions/v2:computeRoutes")
            .header("Content-Type", "application/json")
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.description")
            .header("X-Goog-Maps-Solution-ID", "gmp_git_agentskills_v1")
            .post(jsonBody.toRequestBody(mediaType))
            .build()

        try {
            httpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorBody = response.body?.string() ?: ""
                    return@withContext when (code) {
                        400, 403 -> DrivingEtaState.Error("Google Maps: Kontrollera API-nyckel ($code)")
                        429 -> DrivingEtaState.Error("Google Maps: Kvotgräns överskriden")
                        else -> DrivingEtaState.Error("Google Maps fel ($code)")
                    }
                }

                val bodyString = response.body?.string() ?: ""
                val parsed = json.decodeFromString<RoutesComputeResponse>(bodyString)
                val route = parsed.routes.firstOrNull()
                    ?: return@withContext DrivingEtaState.Error("Ingen körrutt hittades")

                // duration string is e.g. "1260s"
                val durationSec = route.duration
                    ?.removeSuffix("s")
                    ?.toLongOrNull() ?: 0L

                val durationMinutes = ((durationSec + 30) / 60).toInt().coerceAtLeast(1)
                val distanceMeters = route.distanceMeters ?: 0
                val estimatedArrival = referenceTime.plusMinutes(durationMinutes.toLong())

                DrivingEtaState.Success(
                    DrivingEta(
                        durationMinutes = durationMinutes,
                        distanceMeters = distanceMeters,
                        estimatedArrivalTime = estimatedArrival,
                        originLat = originLat,
                        originLng = originLng,
                        destinationName = direction.departureHarborName,
                        routeSummary = route.description
                    )
                )
            }
        } catch (e: IOException) {
            DrivingEtaState.Error("Kunde inte nå Google Maps nätverk")
        } catch (e: Exception) {
            DrivingEtaState.Error("Fel vid beräkning: ${e.message}")
        }
    }
}
