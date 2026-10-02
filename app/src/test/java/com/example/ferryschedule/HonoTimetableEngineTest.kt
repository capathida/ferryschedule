package com.example.ferryschedule

import com.example.ferryschedule.data.local.HonoTimetableEngine
import com.example.ferryschedule.domain.model.FerryDeparture
import com.example.ferryschedule.domain.model.RouteDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class HonoTimetableEngineTest {

    @Test
    fun testNextDeparturesReturnsExactlyThreeItems() {
        val testTime = LocalTime.of(14, 0)
        val departures = HonoTimetableEngine.getNextDepartures(
            fromTime = testTime,
            direction = RouteDirection.HONO_TO_VARHOLMEN,
            count = 3
        )

        assertEquals(3, departures.size)
        // Ensure each departure is after or equal to 14:00
        departures.forEach { dep ->
            assertTrue(dep.minutesUntilDeparture >= 0)
        }
    }

    @Test
    fun testChronologicalOrdering() {
        val testTime = LocalTime.of(8, 0)
        val departures = HonoTimetableEngine.getNextDepartures(
            fromTime = testTime,
            direction = RouteDirection.HONO_TO_VARHOLMEN,
            count = 3
        )

        assertEquals(3, departures.size)
        assertTrue(departures[0].minutesUntilDeparture <= departures[1].minutesUntilDeparture)
        assertTrue(departures[1].minutesUntilDeparture <= departures[2].minutesUntilDeparture)
    }

    @Test
    fun testMidnightRolloverCalculation() {
        // Late at night (23:55) - the next departures will cross into the next day (00:00, 00:30, 01:00)
        val lateNightTime = LocalTime.of(23, 55)
        val departures = HonoTimetableEngine.getNextDepartures(
            fromTime = lateNightTime,
            direction = RouteDirection.HONO_TO_VARHOLMEN,
            count = 3
        )

        assertEquals(3, departures.size)

        // The first departure after 23:55 is at 00:00
        val firstDep = departures[0]
        assertEquals(LocalTime.of(0, 0), firstDep.departureTime)
        assertEquals(5L, firstDep.minutesUntilDeparture)
        assertEquals("om 5 min", firstDep.countdownText)

        // The second departure is at 00:30
        val secondDep = departures[1]
        assertEquals(LocalTime.of(0, 30), secondDep.departureTime)
        assertEquals(35L, secondDep.minutesUntilDeparture)

        // The third departure is at 01:00
        val thirdDep = departures[2]
        assertEquals(LocalTime.of(1, 0), thirdDep.departureTime)
        assertEquals(65L, thirdDep.minutesUntilDeparture)
        assertEquals("om 1 tim 5 min", thirdDep.countdownText)
    }

    @Test
    fun testMinutesCalculationDirect() {
        val t1 = LocalTime.of(10, 0)
        val t2 = LocalTime.of(10, 15)
        assertEquals(15L, HonoTimetableEngine.calculateMinutesUntil(t1, t2))

        // Across midnight
        val late = LocalTime.of(23, 50)
        val early = LocalTime.of(0, 10)
        assertEquals(20L, HonoTimetableEngine.calculateMinutesUntil(late, early))
    }

    @Test
    fun testDepartureFormattingAndImminent() {
        val depNow = FerryDeparture(
            departureTime = LocalTime.of(12, 0),
            minutesUntilDeparture = 0
        )
        assertTrue(depNow.isDepartingNow)
        assertEquals("Avgår nu!", depNow.countdownText)

        val depSoon = FerryDeparture(
            departureTime = LocalTime.of(12, 3),
            minutesUntilDeparture = 3
        )
        assertTrue(depSoon.isImminent)
        assertEquals("om 3 min", depSoon.countdownText)

        val depFuture = FerryDeparture(
            departureTime = LocalTime.of(12, 45),
            minutesUntilDeparture = 45
        )
        assertEquals("om 45 min", depFuture.countdownText)
    }

    @Test
    fun testBothDirectionsProduceValidSchedules() {
        val time = LocalTime.of(12, 0)
        val honoDepartures = HonoTimetableEngine.getNextDepartures(time, RouteDirection.HONO_TO_VARHOLMEN)
        val varholmenDepartures = HonoTimetableEngine.getNextDepartures(time, RouteDirection.VARHOLMEN_TO_HONO)

        assertEquals(3, honoDepartures.size)
        assertEquals(3, varholmenDepartures.size)
    }

    @Test
    fun testBjorkoledenSchedule() {
        val time = LocalTime.of(10, 0)
        val deps = HonoTimetableEngine.getNextDepartures(time, RouteDirection.BJORKO_TO_VARHOLMEN)

        assertEquals(3, deps.size)
        assertTrue(deps[0].statusRemarks?.contains("6 min") == true)
        assertEquals(RouteDirection.VARHOLMEN_TO_BJORKO, RouteDirection.BJORKO_TO_VARHOLMEN.opposite())
    }

    @Test
    fun testSvanesundsledenSchedule() {
        val time = LocalTime.of(10, 0)
        val deps = HonoTimetableEngine.getNextDepartures(time, RouteDirection.KOLHATTAN_TO_SVANESUND)

        assertEquals(3, deps.size)
        assertTrue(deps[0].statusRemarks?.contains("5 min") == true)
        assertEquals(RouteDirection.SVANESUND_TO_KOLHATTAN, RouteDirection.KOLHATTAN_TO_SVANESUND.opposite())
    }

    @Test
    fun testGullmarsledenSchedule() {
        val time = LocalTime.of(10, 0)
        val deps = HonoTimetableEngine.getNextDepartures(time, RouteDirection.FINNSBO_TO_SKAR)

        assertEquals(3, deps.size)
        assertTrue(deps[0].statusRemarks?.contains("10 min") == true)
        assertEquals(RouteDirection.SKAR_TO_FINNSBO, RouteDirection.FINNSBO_TO_SKAR.opposite())
    }
}
