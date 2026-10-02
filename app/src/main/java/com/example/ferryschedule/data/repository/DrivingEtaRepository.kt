package com.example.ferryschedule.data.repository

import android.content.Context
import android.location.Location
import com.example.ferryschedule.data.local.UserPreferences
import com.example.ferryschedule.data.location.UserLocationProvider
import com.example.ferryschedule.data.remote.GoogleMapsRoutesClient
import com.example.ferryschedule.domain.model.DrivingEta
import com.example.ferryschedule.domain.model.DrivingEtaState
import com.example.ferryschedule.domain.model.RouteDirection
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

data class CachedDrivingEta(
    val eta: DrivingEta,
    val timestampMs: Long,
    val originLat: Double,
    val originLng: Double,
    val durationMinutes: Int,
    val distanceMeters: Int
)

class DrivingEtaRepository(
    private val context: Context,
    private val locationProvider: UserLocationProvider = UserLocationProvider(context),
    private val userPrefs: UserPreferences = UserPreferences.getInstance(context),
    private val routesClient: GoogleMapsRoutesClient = GoogleMapsRoutesClient()
) {

    private val cacheMap = ConcurrentHashMap<RouteDirection, CachedDrivingEta>()

    private val sha1Fingerprint: String? by lazy {
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
            }
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val cert = signatures?.firstOrNull()?.toByteArray() ?: return@lazy null
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val digest = md.digest(cert)
            digest.joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getDrivingEta(
        direction: RouteDirection,
        referenceTime: LocalTime = LocalTime.now(),
        forceRefresh: Boolean = false
    ): DrivingEtaState {
        if (!locationProvider.hasLocationPermission()) {
            return DrivingEtaState.NoLocationPermission
        }

        val apiKey = userPrefs.googleMapsApiKey
        if (apiKey.isBlank()) {
            return DrivingEtaState.NoApiKey
        }

        val location = locationProvider.getLastKnownLocation()
            ?: return DrivingEtaState.LocationUnavailable

        val nowMs = System.currentTimeMillis()
        val cached = cacheMap[direction]

        // --- LAGER 1 & LAGER 2: Cooldown, Rörelsetröskel & Caching ---
        if (cached != null && !forceRefresh) {
            val ageMs = nowMs - cached.timestampMs
            val distanceMovedMeters = calculateDistanceMeters(
                cached.originLat, cached.originLng,
                location.latitude, location.longitude
            )

            val isWithinCooldown = ageMs < COOLDOWN_MS
            val isStationaryAndFresh = distanceMovedMeters < MOVEMENT_THRESHOLD_METERS && ageMs < MAX_CACHE_AGE_MS

            if (isWithinCooldown || isStationaryAndFresh) {
                // Återanvänd beräknad körtid utan API-anrop! Uppdatera ankomsttid till aktuell tid + körtid
                val updatedArrival = referenceTime.plusMinutes(cached.durationMinutes.toLong())
                val updatedEta = cached.eta.copy(
                    estimatedArrivalTime = updatedArrival,
                    originLat = location.latitude,
                    originLng = location.longitude
                )
                return DrivingEtaState.Success(updatedEta)
            }
        }

        // --- LAGER 3: Daglig kvotspärr (max 80 anrop/dygn för att hålla sig under Googles 100-tak) ---
        if (!userPrefs.canMakeGoogleMapsApiCall(maxCallsPerDay = MAX_DAILY_CALLS)) {
            if (cached != null) {
                val updatedArrival = referenceTime.plusMinutes(cached.durationMinutes.toLong())
                return DrivingEtaState.Success(
                    cached.eta.copy(
                        estimatedArrivalTime = updatedArrival,
                        originLat = location.latitude,
                        originLng = location.longitude
                    )
                )
            }
            return DrivingEtaState.Error("Dagsbudget för Google Maps uppnådd (${userPrefs.getTodayGoogleMapsApiCallCount()} anrop idag)")
        }

        // Gör nätverksanrop mot Google Maps Routes API
        val result = routesClient.computeDrivingEta(
            originLat = location.latitude,
            originLng = location.longitude,
            direction = direction,
            apiKey = apiKey,
            referenceTime = referenceTime,
            packageName = context.packageName,
            sha1Cert = sha1Fingerprint
        )

        if (result is DrivingEtaState.Success) {
            userPrefs.recordGoogleMapsApiCall()
            val eta = result.eta
            cacheMap[direction] = CachedDrivingEta(
                eta = eta,
                timestampMs = nowMs,
                originLat = location.latitude,
                originLng = location.longitude,
                durationMinutes = eta.durationMinutes,
                distanceMeters = eta.distanceMeters
            )
        }

        return result
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    companion object {
        const val COOLDOWN_MS = 90_000L // Minst 90 sekunder mellan Google Maps-anrop
        const val MAX_CACHE_AGE_MS = 300_000L // 5 minuter max ålder om bilen står stilla (< 250m)
        const val MOVEMENT_THRESHOLD_METERS = 250f // Bilen måste ha rört sig minst 250m
        const val MAX_DAILY_CALLS = 80 // Håller god säkerhetsmarginal till 100 req/dag-taket

        @Volatile
        private var instance: DrivingEtaRepository? = null

        fun getInstance(context: Context): DrivingEtaRepository {
            return instance ?: synchronized(this) {
                instance ?: DrivingEtaRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
