package com.example.ferryschedule

import com.example.ferryschedule.domain.model.DrivingEta
import com.example.ferryschedule.domain.model.DrivingEtaState
import com.example.ferryschedule.domain.model.FerryDeparture
import com.example.ferryschedule.domain.model.RouteDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class DrivingEtaTest {

    @Test
    fun testDrivingEtaFormatting() {
        val etaShort = DrivingEta(
            durationMinutes = 18,
            distanceMeters = 14200,
            estimatedArrivalTime = LocalTime.of(14, 28),
            originLat = 57.70,
            originLng = 11.95,
            destinationName = "Lilla Varholmen",
            routeSummary = "Väg 155"
        )

        assertEquals("18 min", etaShort.formattedDuration)
        assertEquals("14.2 km", etaShort.formattedDistance)

        val etaLong = DrivingEta(
            durationMinutes = 75,
            distanceMeters = 65000,
            estimatedArrivalTime = LocalTime.of(15, 25),
            originLat = 57.70,
            originLng = 11.95,
            destinationName = "Lilla Varholmen"
        )
        assertEquals("1 tim 15 min", etaLong.formattedDuration)
        assertEquals("65.0 km", etaLong.formattedDistance)
    }

    @Test
    fun testEtaDepartureMatchingLogic() {
        val now = LocalTime.of(14, 10)
        val drivingMinutes = 18
        val arrivalTime = now.plusMinutes(drivingMinutes.toLong()) // 14:28

        val departures = listOf(
            FerryDeparture(departureTime = LocalTime.of(14, 15), minutesUntilDeparture = 5),
            FerryDeparture(departureTime = LocalTime.of(14, 25), minutesUntilDeparture = 15),
            FerryDeparture(departureTime = LocalTime.of(14, 35), minutesUntilDeparture = 25),
            FerryDeparture(departureTime = LocalTime.of(14, 50), minutesUntilDeparture = 40)
        )

        var recommendedAssigned = false
        val matched = departures.map { dep ->
            val isMissed = dep.departureTime.isBefore(arrivalTime)
            val isRecommended = !isMissed && !dep.isCancelled && !recommendedAssigned
            if (isRecommended) recommendedAssigned = true

            val buffer = if (isRecommended) {
                ChronoUnit.MINUTES.between(arrivalTime, dep.departureTime).toInt()
            } else null

            dep.copy(
                isMissedByEta = isMissed,
                isRecommendedForEta = isRecommended,
                etaBufferMinutes = buffer
            )
        }

        // 14:15 is missed
        assertTrue(matched[0].isMissedByEta)
        assertFalse(matched[0].isRecommendedForEta)

        // 14:25 is missed (arrival is 14:28)
        assertTrue(matched[1].isMissedByEta)
        assertFalse(matched[1].isRecommendedForEta)

        // 14:35 is the recommended departure (7 minutes buffer)
        assertFalse(matched[2].isMissedByEta)
        assertTrue(matched[2].isRecommendedForEta)
        assertEquals(7, matched[2].etaBufferMinutes)

        // 14:50 is a later departure (not recommended, not missed)
        assertFalse(matched[3].isMissedByEta)
        assertFalse(matched[3].isRecommendedForEta)
    }

    @Test
    fun testDepartureCoordinatesAssignedToRouteDirection() {
        assertEquals("Lilla Varholmen", RouteDirection.VARHOLMEN_TO_HONO.departureHarborName)
        assertEquals(57.7088, RouteDirection.VARHOLMEN_TO_HONO.departureLatitude, 0.0001)
        assertEquals(11.7100, RouteDirection.VARHOLMEN_TO_HONO.departureLongitude, 0.0001)

        assertEquals("Hönö Pinan", RouteDirection.HONO_TO_VARHOLMEN.departureHarborName)
        assertEquals(57.7005, RouteDirection.HONO_TO_VARHOLMEN.departureLatitude, 0.0001)
        assertEquals(11.6565, RouteDirection.HONO_TO_VARHOLMEN.departureLongitude, 0.0001)
    }

    @Test
    fun testDeparturesViewModelHasApplicationConstructor() {
        val constructor = com.example.ferryschedule.phone.DeparturesViewModel::class.java
            .getConstructor(android.app.Application::class.java)
        org.junit.Assert.assertNotNull(constructor)
    }

    @Test
    fun testRepositoryAntiSpamConstants() {
        assertEquals(90_000L, com.example.ferryschedule.data.repository.DrivingEtaRepository.COOLDOWN_MS)
        assertEquals(300_000L, com.example.ferryschedule.data.repository.DrivingEtaRepository.MAX_CACHE_AGE_MS)
        assertEquals(250f, com.example.ferryschedule.data.repository.DrivingEtaRepository.MOVEMENT_THRESHOLD_METERS, 0.01f)
        assertEquals(80, com.example.ferryschedule.data.repository.DrivingEtaRepository.MAX_DAILY_CALLS)
    }
}
