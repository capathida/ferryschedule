package com.example.ferryschedule.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class UserLocationProvider(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun getLastKnownLocation(): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission() || locationManager == null) return@withContext null

        try {
            var bestLocation: Location? = null

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            for (provider in providers) {
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null) {
                            if (bestLocation == null || loc.time > bestLocation.time) {
                                bestLocation = loc
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Provider might not be available
                }
            }

            // On API 30+, if bestLocation is null, try getCurrentLocation
            if (bestLocation == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bestLocation = getCurrentLocationApi30()
            }

            bestLocation
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getCurrentLocationApi30(): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || locationManager == null) return null
        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            val executor = Executors.newSingleThreadExecutor()

            continuation.invokeOnCancellation {
                cancellationSignal.cancel()
                executor.shutdown()
            }

            try {
                val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationManager.GPS_PROVIDER
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else {
                    LocationManager.PASSIVE_PROVIDER
                }

                locationManager.getCurrentLocation(
                    provider,
                    cancellationSignal,
                    executor
                ) { location ->
                    executor.shutdown()
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
            } catch (e: SecurityException) {
                executor.shutdown()
                if (continuation.isActive) continuation.resume(null)
            } catch (e: Exception) {
                executor.shutdown()
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
}
