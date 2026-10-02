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

    companion object {
        private const val KEY_DIRECTION = "selected_route_direction"

        @Volatile
        private var instance: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
