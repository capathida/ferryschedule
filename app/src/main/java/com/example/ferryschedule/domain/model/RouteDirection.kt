package com.example.ferryschedule.domain.model

enum class FerryRoute(
    val routeId: Int,
    val title: String,
    val subtitle: String,
    val crossingMinutes: Int,
    val roadDescription: String
) {
    HONOLEDEN(
        routeId = 28,
        title = "Hönöleden",
        subtitle = "Hönö Pinan ⇄ Lilla Varholmen",
        crossingMinutes = 12,
        roadDescription = "Väg 155 (Fastlandet) & Väg 574 (Hönö)"
    ),
    BJORKOLEDEN(
        routeId = 23,
        title = "Björköleden",
        subtitle = "Björkö ⇄ Lilla Varholmen",
        crossingMinutes = 6,
        roadDescription = "Väg 155 (Lilla Varholmen)"
    ),
    SVANESUNDSLEDEN(
        routeId = 35,
        title = "Svanesundsleden",
        subtitle = "Kolhättan ⇄ Svanesund",
        crossingMinutes = 5,
        roadDescription = "Väg 770 (Stenungsund) & Orust Väg 160"
    ),
    GULLMARSLEDEN(
        routeId = 25,
        title = "Gullmarsleden",
        subtitle = "Finnsbo ⇄ Skår",
        crossingMinutes = 10,
        roadDescription = "Väg 161 (Lysekil / Skaftö)"
    );

    val directions: List<RouteDirection>
        get() = RouteDirection.entries.filter { it.routeId == this.routeId }

    val defaultDirection: RouteDirection
        get() = directions.first()

    val returnDirection: RouteDirection
        get() = directions.getOrElse(1) { directions.first() }
}

enum class RouteDirection(
    val routeId: Int,
    val originName: String,
    val destinationName: String,
    val shortCode: String,
    val fromHarborId: Int,
    val toHarborId: Int,
    val crossingMinutes: Int,
    val navQuery: String
) {
    // 1. Hönöleden (Route 28)
    HONO_TO_VARHOLMEN(
        routeId = 28,
        originName = "Hönö Pinan",
        destinationName = "Lilla Varholmen",
        shortCode = "HONO_LV",
        fromHarborId = 55,
        toHarborId = 56,
        crossingMinutes = 12,
        navQuery = "Lilla Varholmen Färjeläge"
    ),
    VARHOLMEN_TO_HONO(
        routeId = 28,
        originName = "Lilla Varholmen",
        destinationName = "Hönö Pinan",
        shortCode = "LV_HONO",
        fromHarborId = 56,
        toHarborId = 55,
        crossingMinutes = 12,
        navQuery = "Hönö Färjeläge Pinan"
    ),

    // 2. Björköleden (Route 23)
    BJORKO_TO_VARHOLMEN(
        routeId = 23,
        originName = "Björkö",
        destinationName = "Lilla Varholmen",
        shortCode = "BJORKO_LV",
        fromHarborId = 45,
        toHarborId = 46,
        crossingMinutes = 6,
        navQuery = "Lilla Varholmen Färjeläge"
    ),
    VARHOLMEN_TO_BJORKO(
        routeId = 23,
        originName = "Lilla Varholmen",
        destinationName = "Björkö",
        shortCode = "LV_BJORKO",
        fromHarborId = 46,
        toHarborId = 45,
        crossingMinutes = 6,
        navQuery = "Björkö Färjeläge Grönevik"
    ),

    // 3. Svanesundsleden (Route 35)
    KOLHATTAN_TO_SVANESUND(
        routeId = 35,
        originName = "Kolhättan",
        destinationName = "Svanesund",
        shortCode = "KOL_SVA",
        fromHarborId = 76,
        toHarborId = 75,
        crossingMinutes = 5,
        navQuery = "Svanesund Färjeläge"
    ),
    SVANESUND_TO_KOLHATTAN(
        routeId = 35,
        originName = "Svanesund",
        destinationName = "Kolhättan",
        shortCode = "SVA_KOL",
        fromHarborId = 75,
        toHarborId = 76,
        crossingMinutes = 5,
        navQuery = "Kolhättan Färjeläge"
    ),

    // 4. Gullmarsleden (Route 25)
    FINNSBO_TO_SKAR(
        routeId = 25,
        originName = "Finnsbo",
        destinationName = "Skår",
        shortCode = "FINNS_SKAR",
        fromHarborId = 49,
        toHarborId = 50,
        crossingMinutes = 10,
        navQuery = "Skår Färjeläge"
    ),
    SKAR_TO_FINNSBO(
        routeId = 25,
        originName = "Skår",
        destinationName = "Finnsbo",
        shortCode = "SKAR_FINNS",
        fromHarborId = 50,
        toHarborId = 49,
        crossingMinutes = 10,
        navQuery = "Finnsbo Färjeläge"
    );

    val route: FerryRoute
        get() = FerryRoute.entries.first { it.routeId == this.routeId }

    val displayTitle: String
        get() = "$originName ➔ $destinationName"

    fun opposite(): RouteDirection {
        return when (this) {
            HONO_TO_VARHOLMEN -> VARHOLMEN_TO_HONO
            VARHOLMEN_TO_HONO -> HONO_TO_VARHOLMEN
            BJORKO_TO_VARHOLMEN -> VARHOLMEN_TO_BJORKO
            VARHOLMEN_TO_BJORKO -> BJORKO_TO_VARHOLMEN
            KOLHATTAN_TO_SVANESUND -> SVANESUND_TO_KOLHATTAN
            SVANESUND_TO_KOLHATTAN -> KOLHATTAN_TO_SVANESUND
            FINNSBO_TO_SKAR -> SKAR_TO_FINNSBO
            SKAR_TO_FINNSBO -> FINNSBO_TO_SKAR
        }
    }
}
