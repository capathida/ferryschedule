package com.example.ferryschedule.data.local

import android.content.Context
import com.example.ferryschedule.domain.model.FerryRoute
import com.example.ferryschedule.domain.model.RouteDirection

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("ferry_preferences", Context.MODE_PRIVATE)

    var savedDirection: RouteDirection
        get() {
            val name = prefs.getString(KEY_DIRECTION, RouteDirection.HONO_TO_VARHOLMEN.name)
            return try {
                RouteDirection.valueOf(name ?: RouteDirection.HONO_TO_VARHOLMEN.name)
            } catch (e: Exception) {
                RouteDirection.HONO_TO_VARHOLMEN
            }
        }
        set(value) {
            prefs.edit().putString(KEY_DIRECTION, value.name).apply()
        }

    var savedRoute: FerryRoute
        get() = savedDirection.route
        set(value) {
            savedDirection = value.defaultDirection
        }

    var googleMapsApiKey: String
        get() {
            val saved = prefs.getString(KEY_MAPS_API_KEY, "") ?: ""
            return if (saved.isNotBlank()) saved else com.example.ferryschedule.BuildConfig.GOOGLE_MAPS_API_KEY
        }
        set(value) {
            prefs.edit().putString(KEY_MAPS_API_KEY, value.trim()).apply()
        }

    fun canMakeGoogleMapsApiCall(maxCallsPerDay: Int = 80): Boolean {
        val today = java.time.LocalDate.now().toString()
        val lastDate = prefs.getString(KEY_API_CALL_DATE, "") ?: ""
        if (lastDate != today) {
            return true
        }
        val currentCount = prefs.getInt(KEY_API_CALL_COUNT, 0)
        return currentCount < maxCallsPerDay
    }

    fun recordGoogleMapsApiCall(): Int {
        val today = java.time.LocalDate.now().toString()
        val lastDate = prefs.getString(KEY_API_CALL_DATE, "") ?: ""
        val count = if (lastDate == today) {
            prefs.getInt(KEY_API_CALL_COUNT, 0) + 1
        } else {
            1
        }
        prefs.edit()
            .putString(KEY_API_CALL_DATE, today)
            .putInt(KEY_API_CALL_COUNT, count)
            .apply()
        return count
    }

    fun getTodayGoogleMapsApiCallCount(): Int {
        val today = java.time.LocalDate.now().toString()
        val lastDate = prefs.getString(KEY_API_CALL_DATE, "") ?: ""
        return if (lastDate == today) prefs.getInt(KEY_API_CALL_COUNT, 0) else 0
    }

    companion object {
        private const val KEY_DIRECTION = "selected_route_direction"
        private const val KEY_MAPS_API_KEY = "google_maps_api_key"
        private const val KEY_API_CALL_DATE = "google_maps_api_call_date"
        private const val KEY_API_CALL_COUNT = "google_maps_api_call_count"

        @Volatile
        private var instance: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
