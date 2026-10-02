package com.example.ferryschedule.data.repository

import android.content.Context
import com.example.ferryschedule.data.local.UserPreferences
import com.example.ferryschedule.data.location.UserLocationProvider
import com.example.ferryschedule.data.remote.GoogleMapsRoutesClient
import com.example.ferryschedule.domain.model.DrivingEtaState
import com.example.ferryschedule.domain.model.RouteDirection
import java.time.LocalTime

class DrivingEtaRepository(
    private val context: Context,
    private val locationProvider: UserLocationProvider = UserLocationProvider(context),
    private val userPrefs: UserPreferences = UserPreferences.getInstance(context),
    private val routesClient: GoogleMapsRoutesClient = GoogleMapsRoutesClient()
) {

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
        referenceTime: LocalTime = LocalTime.now()
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

        return routesClient.computeDrivingEta(
            originLat = location.latitude,
            originLng = location.longitude,
            direction = direction,
            apiKey = apiKey,
            referenceTime = referenceTime,
            packageName = context.packageName,
            sha1Cert = sha1Fingerprint
        )
    }

    companion object {
        @Volatile
        private var instance: DrivingEtaRepository? = null

        fun getInstance(context: Context): DrivingEtaRepository {
            return instance ?: synchronized(this) {
                instance ?: DrivingEtaRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
