package com.example.ferryschedule.car

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.ferryschedule.data.local.UserPreferences
import com.example.ferryschedule.data.repository.FerryRepositoryImpl
import com.example.ferryschedule.domain.model.FerryDeparture
import com.example.ferryschedule.domain.model.FerryRoute
import com.example.ferryschedule.domain.model.RouteDirection
import com.example.ferryschedule.domain.model.TrafficStatus
import com.example.ferryschedule.domain.repository.FerryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Driver-friendly Android Auto screen displaying the next 3 ferry departures,
 * real-time queue forecast, and quick shortcuts to the visual road corridor & navigation.
 */
class DeparturesScreen(
    carContext: CarContext,
    private val repository: FerryRepository = FerryRepositoryImpl.instance,
    private val etaRepository: com.example.ferryschedule.data.repository.DrivingEtaRepository = com.example.ferryschedule.data.repository.DrivingEtaRepository.getInstance(carContext)
) : Screen(carContext), DefaultLifecycleObserver {

    private val userPrefs = UserPreferences.getInstance(carContext)
    private var currentDirection: RouteDirection = userPrefs.savedDirection
    private var departures: List<FerryDeparture> = emptyList()
    private var trafficStatus: TrafficStatus? = null
    private var drivingEtaState: com.example.ferryschedule.domain.model.DrivingEtaState = com.example.ferryschedule.domain.model.DrivingEtaState.Idle
    private var isLoading: Boolean = true
    private var refreshJob: Job? = null

    init {
        lifecycle.addObserver(this)
        loadData()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        startAutoRefreshTicker()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        refreshJob?.cancel()
        refreshJob = null
    }

    private fun loadData() {
        lifecycleScope.launch {
            val now = LocalTime.now()
            val trafficResult = repository.getTrafficStatus(now)
            trafficStatus = trafficResult.getOrNull()

            val etaState = etaRepository.getDrivingEta(currentDirection, now)
            drivingEtaState = etaState

            if (etaState is com.example.ferryschedule.domain.model.DrivingEtaState.Success) {
                val arrivalTime = etaState.eta.estimatedArrivalTime
                val nowDepartures = repository.getNextDepartures(currentDirection, now, 2).getOrElse { emptyList() }
                val targetDepartures = repository.getNextDepartures(currentDirection, arrivalTime, 3).getOrElse { emptyList() }

                val combined = (nowDepartures + targetDepartures)
                    .distinctBy { it.departureTime }
                    .sortedBy { it.departureTime }

                var recommendedAssigned = false
                departures = combined.map { dep ->
                    val isMissed = dep.departureTime.isBefore(arrivalTime)
                    val isRecommended = !isMissed && !dep.isCancelled && !recommendedAssigned
                    if (isRecommended) recommendedAssigned = true

                    val bufferMinutes = if (isRecommended) {
                        java.time.temporal.ChronoUnit.MINUTES.between(arrivalTime, dep.departureTime).toInt()
                    } else null

                    dep.copy(
                        isMissedByEta = isMissed,
                        isRecommendedForEta = isRecommended,
                        etaBufferMinutes = bufferMinutes
                    )
                }
            } else {
                val depResult = repository.getNextDepartures(
                    direction = currentDirection,
                    fromTime = now,
                    count = 3
                )
                departures = depResult.getOrElse { emptyList() }
            }

            isLoading = false
            invalidate()
        }
    }

    private fun startAutoRefreshTicker() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(30_000L) // Refresh every 30 seconds
                loadData()
            }
        }
    }

    private fun toggleDirection() {
        currentDirection = currentDirection.opposite()
        userPrefs.savedDirection = currentDirection
        isLoading = true
        invalidate()
        loadData()
    }

    private fun openRouteSelection() {
        screenManager.push(
            RouteSelectionCarScreen(carContext, currentDirection.route) { selectedRoute ->
                currentDirection = selectedRoute.defaultDirection
                userPrefs.savedDirection = currentDirection
                isLoading = true
                invalidate()
                loadData()
            }
        )
    }

    private fun openRoadStatus() {
        screenManager.push(RoadStatusCarScreen(carContext, currentDirection.route, repository))
    }

    private fun startNavigation() {
        try {
            val destQuery = currentDirection.navQuery
            val lat = currentDirection.departureLatitude
            val lng = currentDirection.departureLongitude
            val intent = Intent(
                CarContext.ACTION_NAVIGATE,
                Uri.parse("geo:$lat,$lng?q=${Uri.encode(destQuery)}")
            )
            carContext.startCarApp(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onGetTemplate(): Template {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Vägkarta")
                    .setOnClickListener { openRoadStatus() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Navigera")
                    .setOnClickListener { startNavigation() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Välj led")
                    .setOnClickListener { openRouteSelection() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Byt rutt")
                    .setOnClickListener { toggleDirection() }
                    .build()
            )
            .build()

        val listBuilder = ItemList.Builder()

        if (isLoading && departures.isEmpty()) {
            return ListTemplate.Builder()
                .setTitle("${currentDirection.route.title}: ${currentDirection.originName} ➔ ${currentDirection.destinationName}")
                .setActionStrip(actionStrip)
                .setLoading(true)
                .build()
        }

        // Add driving ETA banner row if available
        val currentEta = (drivingEtaState as? com.example.ferryschedule.domain.model.DrivingEtaState.Success)?.eta
        if (currentEta != null) {
            val rec = departures.firstOrNull { it.isRecommendedForEta }
            val recSub = if (rec != null) {
                "Hinner ${rec.formattedTime} (+${rec.etaBufferMinutes ?: 0}m marginal)"
            } else {
                "Mål: ${currentEta.destinationName}"
            }
            val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🚗 Körtid: ${currentEta.formattedDuration} (${currentEta.formattedDistance})")
                    .addText("Ankomst ${currentEta.estimatedArrivalTime.format(timeFormatter)} ➔ $recSub")
                    .build()
            )
        }

        if (departures.isEmpty()) {
            listBuilder.setNoItemsMessage("Inga avgångar hittades just nu för ${currentDirection.route.title}")
        } else {
            val queueBreakdown = when (currentDirection) {
                RouteDirection.HONO_TO_VARHOLMEN -> trafficStatus?.hono?.breakdown
                RouteDirection.VARHOLMEN_TO_HONO -> trafficStatus?.varholmen?.breakdown
                RouteDirection.VARHOLMEN_TO_BJORKO -> trafficStatus?.varholmen?.breakdown
                else -> null
            }

            departures.take(5).forEachIndexed { index, dep ->
                val rowBuilder = Row.Builder()
                val prefix = when {
                    dep.isCancelled -> "INSTÄLLD: "
                    dep.isRecommendedForEta -> "⭐ REKOMMENDERAD: "
                    dep.isMissedByEta -> "⏳ Missas: "
                    index == 0 -> "Nästa: "
                    else -> "Avgång: "
                }

                rowBuilder.setTitle("$prefix${dep.formattedTime}  (${dep.countdownText})")

                if (dep.isRecommendedForEta) {
                    val buffer = dep.etaBufferMinutes ?: 0
                    rowBuilder.addText("Beräknad ankomst ger $buffer min marginal vid kajen")
                } else if (index == 0) {
                    val queueAdvice = if (queueBreakdown != null && queueBreakdown.roadQueueMinutes > 0) {
                        "Bilkö: ${queueBreakdown.roadQueueMinutes} min ➔ Prognos: ${queueBreakdown.estimatedBoardingFerryTime}"
                    } else {
                        "Fri väg (0 min kö) ➔ Överfartstid ~${currentDirection.crossingMinutes} min"
                    }
                    rowBuilder.addText(queueAdvice)
                }

                listBuilder.addItem(rowBuilder.build())
            }
        }

        return ListTemplate.Builder()
            .setTitle("${currentDirection.route.title}: ${currentDirection.originName} ➔ ${currentDirection.destinationName}")
            .setActionStrip(actionStrip)
            .setSingleList(listBuilder.build())
            .build()
    }
}
