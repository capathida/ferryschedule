package com.example.ferryschedule.domain.model

enum class CongestionLevel(val hexColor: Long, val displayName: String) {
    GREEN(0xFF10B981, "Fri fart (Grönt)"),
    YELLOW(0xFFF59E0B, "Trögt flöde (Gult)"),
    RED(0xFFF43F5E, "Köbildning (Rött)"),
    DARK_RED(0xFFDC2626, "Mycket lång kö (Mörkrött)");

    companion object {
        fun fromString(str: String?): CongestionLevel {
            return when (str?.lowercase()) {
                "yellow" -> YELLOW
                "red" -> RED
                "darkred", "dark_red" -> DARK_RED
                else -> GREEN
            }
        }
    }
}

data class RoadSegment(
    val id: String,
    val name: String,
    val description: String,
    val speedKmh: Double,
    val normalSpeedKmh: Double,
    val travelTimeSeconds: Int,
    val freeFlowSeconds: Int,
    val delaySeconds: Int,
    val lengthMeters: Int,
    val level: CongestionLevel,
    val side: String, // "varholmen", "hono", "mainland_approach"
    val direction: String? = null // "towards_ferry", "towards_city"
)

data class QueueBreakdown(
    val roadQueueMinutes: Int = 0,
    val nextFerryMinutes: Int = 0,
    val ferriesWaitingCount: Int = 0,
    val estimatedBoardingFerryTime: String = "--:--",
    val totalWaitMinutes: Int = 0,
    val queueExplanation: String = "Trafiken flyter normalt."
)

data class SideQueueStatus(
    val side: String,
    val name: String,
    val destination: String,
    val level: CongestionLevel,
    val statusText: String,
    val queueMetersEstimate: Int,
    val queueDescription: String,
    val breakdown: QueueBreakdown,
    val speedKmh: Double,
    val greenStartPoint: String
)

data class TrafficStatus(
    val varholmen: SideQueueStatus,
    val hono: SideQueueStatus,
    val overallLevel: CongestionLevel,
    val overallStatusText: String,
    val segments: List<RoadSegment>,
    val greenZoneDescription: String,
    val lastUpdated: String
)

data class TrafficCamera(
    val id: String,
    val name: String,
    val description: String,
    val photoUrl: String,
    val photoTime: String,
    val direction: Int? = null
)
