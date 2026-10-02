package com.example.ferryschedule

import com.example.ferryschedule.data.remote.TrafikverketService
import com.example.ferryschedule.domain.model.CongestionLevel
import com.example.ferryschedule.domain.model.RouteDirection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TrafficStatusTest {

    private val service = TrafikverketService()

    @Test
    fun testHonoRoadSegmentsIncluded() = runBlocking {
        val status = service.fetchTrafficStatus(LocalTime.of(12, 0))

        assertNotNull(status.hono)
        assertNotNull(status.varholmen)

        // Verify Hönö road segments exist
        val honoSegments = status.segments.filter { it.side == "hono" }
        assertTrue("Should include at least 2 Hönö road segments", honoSegments.size >= 2)

        val approachSeg = status.segments.find { it.id == "hono_approaches" }
        assertNotNull("Should find hono_approaches segment", approachSeg)
        assertEquals("hono", approachSeg?.side)

        val pinanSeg = status.segments.find { it.id == "hono_pinan" }
        assertNotNull("Should find hono_pinan segment", pinanSeg)
        assertEquals("hono", pinanSeg?.side)
    }

    @Test
    fun testMorningRushOnHonoSide() = runBlocking {
        // At 07:30, morning commute rush should produce yellow/queue on Hönö side
        val status = service.fetchTrafficStatus(LocalTime.of(7, 30))

        assertEquals(CongestionLevel.YELLOW, status.hono.level)
        assertTrue(status.hono.breakdown.roadQueueMinutes > 0)
        assertEquals("2:a färjan", status.hono.breakdown.estimatedBoardingFerryTime)
        assertTrue(status.hono.statusText.contains("Morgonkö"))

        val pinanSeg = status.segments.find { it.id == "hono_pinan" }
        assertEquals(CongestionLevel.YELLOW, pinanSeg?.level)
    }

    @Test
    fun testFreeFlowOutsideMorningRushOnHonoSide() = runBlocking {
        // At 14:00, traffic should be free flowing on Hönö side
        val status = service.fetchTrafficStatus(LocalTime.of(14, 0))

        assertEquals(CongestionLevel.GREEN, status.hono.level)
        assertEquals(0, status.hono.breakdown.roadQueueMinutes)
        assertEquals("Nästa färja", status.hono.breakdown.estimatedBoardingFerryTime)

        val pinanSeg = status.segments.find { it.id == "hono_pinan" }
        assertEquals(CongestionLevel.GREEN, pinanSeg?.level)
    }

    @Test
    fun testLiveDeparturesForBjorkoAndSvanesund() = runBlocking {
        val bjorkoDeps = service.fetchLiveDepartures(RouteDirection.BJORKO_TO_VARHOLMEN)
        if (bjorkoDeps.isNotEmpty()) {
            assertTrue(bjorkoDeps.size > 0)
            assertTrue(bjorkoDeps[0].statusRemarks?.contains("6 min") == true)
        }

        val svanesundDeps = service.fetchLiveDepartures(RouteDirection.KOLHATTAN_TO_SVANESUND)
        if (svanesundDeps.isNotEmpty()) {
            assertTrue(svanesundDeps.size > 0)
            assertTrue(svanesundDeps[0].statusRemarks?.contains("5 min") == true)
        }
    }
}
