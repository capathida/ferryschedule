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

data class CachedError(
    val error: DrivingEtaState.Error,
    val timestampMs: Long,
    val cooldownMs: Long
)

class DrivingEtaRepository(
    private val context: Context,
    private val locationProvider: UserLocationProvider = UserLocationProvider(context),
    private val userPrefs: UserPreferences = UserPreferences.getInstance(context),
    private val routesClient: GoogleMapsRoutesClient = GoogleMapsRoutesClient()
) {

    private val cacheMap = ConcurrentHashMap<RouteDirection, CachedDrivingEta>()
    private val errorCacheMap = ConcurrentHashMap<RouteDirection, CachedError>()

    val todayCallCount: Int
        get() = userPrefs.getTodayGoogleMapsApiCallCount()

    val maxDailyCalls: Int
        get() = userPrefs.maxDailyCalls

    fun resetCallCount() {
        userPrefs.resetGoogleMapsApiCallCount()
        clearCache()
    }

    fun clearCache() {
        cacheMap.clear()
        errorCacheMap.clear()
        userPrefs.clearError()
    }

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

        val nowMs = System.currentTimeMillis()

        // -------------------------------------------------------------------------
        // LAGER 1: Fel-cooldown & Backoff (Skyddar mot loopande anrop vid API-fel)
        // Om Google returnerat fel (t.ex. 403 eller 429), pausa anrop i 5-30 min
        // Sparas även på disk så att cooldownen gäller efter app-omstart!
        // -------------------------------------------------------------------------
        if (!forceRefresh) {
            val cachedErr = errorCacheMap[direction]
            val errTimestamp = cachedErr?.timestampMs ?: userPrefs.lastErrorTimestamp
            val errCooldown = cachedErr?.cooldownMs ?: userPrefs.lastErrorCooldownMs
            val errMsg = cachedErr?.error?.message ?: userPrefs.lastErrorMessage
            val errCode = cachedErr?.error?.httpCode ?: userPrefs.lastErrorCode

            if (errTimestamp > 0L && errCooldown > 0L) {
                val age = nowMs - errTimestamp
                if (age < errCooldown) {
                    val remainingMins = ((errCooldown - age) / 60_000L).coerceAtLeast(1)
                    return DrivingEtaState.Error(
                        message = "$errMsg (Pausad i $remainingMins min för att skydda kvoten)",
                        httpCode = errCode
                    )
                } else {
                    errorCacheMap.remove(direction)
                    userPrefs.clearError()
                }
            }
        }

        val location = locationProvider.getLastKnownLocation()
            ?: return DrivingEtaState.LocationUnavailable

        val cached = cacheMap[direction]

        // -------------------------------------------------------------------------
        // LAGER 2: Smart Körtids-cache & Lokal Extrapolering
        // Återanvänder beräknad körtid utan API-anrop!
        // 10 min normal cooldown, 30 min om bilen inte förflyttat sig > 1.5 km.
        // -------------------------------------------------------------------------
        if (cached != null && !forceRefresh) {
            val ageMs = nowMs - cached.timestampMs
            val distanceMovedMeters = calculateDistanceMeters(
                cached.originLat, cached.originLng,
                location.latitude, location.longitude
            )

            val isWithinStandardCooldown = ageMs < SUCCESS_CACHE_COOLDOWN_MS
            val isStationaryAndFresh = distanceMovedMeters < MOVEMENT_THRESHOLD_METERS && ageMs < STATIONARY_CACHE_MAX_AGE_MS

            if (isWithinStandardCooldown || isStationaryAndFresh) {
                // Lokal extrapolering: Uppdatera ankomsttid med aktuell tid + sparad körtid (0 API-anrop!)
                val updatedArrival = referenceTime.plusMinutes(cached.durationMinutes.toLong())
                val updatedEta = cached.eta.copy(
                    estimatedArrivalTime = updatedArrival,
                    originLat = location.latitude,
                    originLng = location.longitude
                )
                return DrivingEtaState.Success(updatedEta)
            }
        }

        // -------------------------------------------------------------------------
        // LAGER 3: Strikt Daglig Budgetspärr (max 50 anrop/dygn mot Googles 100-tak)
        // -------------------------------------------------------------------------
        val maxCalls = userPrefs.maxDailyCalls
        if (!userPrefs.canMakeGoogleMapsApiCall(maxCallsPerDay = maxCalls)) {
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
            return DrivingEtaState.Error(
                "Dagsbudget för Google Maps uppnådd (${userPrefs.getTodayGoogleMapsApiCallCount()}/$maxCalls anrop idag). Skyddar din kvot.",
                httpCode = 429
            )
        }

        // -------------------------------------------------------------------------
        // LAGER 4: Räkna anropet INNAN vi skickar (Google räknar alla HTTP-anrop i kvoten)
        // -------------------------------------------------------------------------
        userPrefs.recordGoogleMapsApiCall()

        // -------------------------------------------------------------------------
        // LAGER 5: Nätverksanrop mot Google Maps Routes API
        // -------------------------------------------------------------------------
        val result = routesClient.computeDrivingEta(
            originLat = location.latitude,
            originLng = location.longitude,
            direction = direction,
            apiKey = apiKey,
            referenceTime = referenceTime,
            packageName = context.packageName,
            sha1Cert = sha1Fingerprint
        )

        when (result) {
            is DrivingEtaState.Success -> {
                errorCacheMap.remove(direction)
                userPrefs.clearError()
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
            is DrivingEtaState.Error -> {
                // Dynamisk backoff baserat på felkod
                val cooldown = when (result.httpCode) {
                    429 -> ERROR_QUOTA_COOLDOWN_MS // 30 minuter vid kvotfel
                    in 400..403 -> ERROR_AUTH_COOLDOWN_MS // 15 minuter vid behörighetsfel
                    else -> ERROR_NETWORK_COOLDOWN_MS // 5 minuter vid andra fel
                }
                errorCacheMap[direction] = CachedError(
                    error = result,
                    timestampMs = nowMs,
                    cooldownMs = cooldown
                )
                userPrefs.recordError(result.httpCode ?: -1, result.message, cooldown)
            }
            else -> {}
        }

        return result
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    companion object {
        const val SUCCESS_CACHE_COOLDOWN_MS = 600_000L // 10 minuter standardcache för lyckat anrop
        const val STATIONARY_CACHE_MAX_AGE_MS = 1_800_000L // 30 minuter om man står stilla (< 1.5 km)
        const val MOVEMENT_THRESHOLD_METERS = 1_500f // 1.5 km förflyttning krävs för att bryta stationärcachen
        const val ERROR_QUOTA_COOLDOWN_MS = 1_800_000L // 30 minuter cooldown om Google returnerar 429
        const val ERROR_AUTH_COOLDOWN_MS = 900_000L // 15 minuter cooldown om Google returnerar 400-403
        const val ERROR_NETWORK_COOLDOWN_MS = 300_000L // 5 minuter cooldown vid nätverksfel

        const val COOLDOWN_MS = SUCCESS_CACHE_COOLDOWN_MS
        const val MAX_CACHE_AGE_MS = STATIONARY_CACHE_MAX_AGE_MS
        const val MAX_DAILY_CALLS = UserPreferences.DEFAULT_MAX_DAILY_CALLS

        @Volatile
        private var instance: DrivingEtaRepository? = null

        fun getInstance(context: Context): DrivingEtaRepository {
            return instance ?: synchronized(this) {
                instance ?: DrivingEtaRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
