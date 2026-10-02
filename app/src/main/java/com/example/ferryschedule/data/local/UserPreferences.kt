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

    var maxDailyCalls: Int
        get() = prefs.getInt(KEY_MAX_DAILY_CALLS, DEFAULT_MAX_DAILY_CALLS)
        set(value) = prefs.edit().putInt(KEY_MAX_DAILY_CALLS, value).apply()

    fun canMakeGoogleMapsApiCall(maxCallsPerDay: Int = maxDailyCalls): Boolean {
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

    fun resetGoogleMapsApiCallCount() {
        val today = java.time.LocalDate.now().toString()
        prefs.edit()
            .putString(KEY_API_CALL_DATE, today)
            .putInt(KEY_API_CALL_COUNT, 0)
            .apply()
    }

    var lastErrorTimestamp: Long
        get() = prefs.getLong(KEY_LAST_ERROR_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ERROR_TIMESTAMP, value).apply()

    var lastErrorCode: Int
        get() = prefs.getInt(KEY_LAST_ERROR_CODE, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_ERROR_CODE, value).apply()

    var lastErrorMessage: String
        get() = prefs.getString(KEY_LAST_ERROR_MESSAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_ERROR_MESSAGE, value).apply()

    var lastErrorCooldownMs: Long
        get() = prefs.getLong(KEY_LAST_ERROR_COOLDOWN, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ERROR_COOLDOWN, value).apply()

    fun recordError(code: Int, message: String, cooldownMs: Long) {
        prefs.edit()
            .putLong(KEY_LAST_ERROR_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_LAST_ERROR_CODE, code)
            .putString(KEY_LAST_ERROR_MESSAGE, message)
            .putLong(KEY_LAST_ERROR_COOLDOWN, cooldownMs)
            .apply()
    }

    fun clearError() {
        prefs.edit()
            .remove(KEY_LAST_ERROR_TIMESTAMP)
            .remove(KEY_LAST_ERROR_CODE)
            .remove(KEY_LAST_ERROR_MESSAGE)
            .remove(KEY_LAST_ERROR_COOLDOWN)
            .apply()
    }

    companion object {
        const val DEFAULT_MAX_DAILY_CALLS = 50
        private const val KEY_MAX_DAILY_CALLS = "google_maps_max_daily_calls"
        private const val KEY_DIRECTION = "selected_route_direction"
        private const val KEY_MAPS_API_KEY = "google_maps_api_key"
        private const val KEY_API_CALL_DATE = "google_maps_api_call_date"
        private const val KEY_API_CALL_COUNT = "google_maps_api_call_count"
        private const val KEY_LAST_ERROR_TIMESTAMP = "google_maps_last_error_ts"
        private const val KEY_LAST_ERROR_CODE = "google_maps_last_error_code"
        private const val KEY_LAST_ERROR_MESSAGE = "google_maps_last_error_msg"
        private const val KEY_LAST_ERROR_COOLDOWN = "google_maps_last_error_cooldown"

        @Volatile
        private var instance: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
