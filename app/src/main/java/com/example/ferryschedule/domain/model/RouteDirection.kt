package com.example.ferryschedule.domain.model

enum class RouteDirection(
    val originName: String,
    val destinationName: String,
    val shortCode: String
) {
    HONO_TO_VARHOLMEN(
        originName = "Hönö Pinan",
        destinationName = "Lilla Varholmen",
        shortCode = "HONO_LV"
    ),
    VARHOLMEN_TO_HONO(
        originName = "Lilla Varholmen",
        destinationName = "Hönö Pinan",
        shortCode = "LV_HONO"
    );

    val displayTitle: String
        get() = "$originName ➔ $destinationName"

    fun opposite(): RouteDirection {
        return when (this) {
            HONO_TO_VARHOLMEN -> VARHOLMEN_TO_HONO
            VARHOLMEN_TO_HONO -> HONO_TO_VARHOLMEN
        }
    }
}
