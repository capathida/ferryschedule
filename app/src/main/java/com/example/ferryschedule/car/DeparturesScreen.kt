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
import com.example.ferryschedule.data.repository.FerryRepositoryImpl
import com.example.ferryschedule.domain.model.FerryDeparture
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
    private val repository: FerryRepository = FerryRepositoryImpl.instance
) : Screen(carContext), DefaultLifecycleObserver {

    private var currentDirection: RouteDirection = RouteDirection.HONO_TO_VARHOLMEN
    private var departures: List<FerryDeparture> = emptyList()
    private var trafficStatus: TrafficStatus? = null
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
            val trafficResult = repository.getTrafficStatus()
            trafficStatus = trafficResult.getOrNull()

            val depResult = repository.getNextDepartures(
                direction = currentDirection,
                fromTime = LocalTime.now(),
                count = 3
            )
            departures = depResult.getOrElse { emptyList() }
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
        isLoading = true
        invalidate()
        loadData()
    }

    private fun openRoadStatus() {
        screenManager.push(RoadStatusCarScreen(carContext, repository))
    }

    private fun startNavigation() {
        try {
            val destQuery = if (currentDirection == RouteDirection.VARHOLMEN_TO_HONO) {
                "Lilla Varholmen Färjeläge"
            } else {
                "Hönö Färjeläge Pinan"
            }
            val intent = Intent(
                CarContext.ACTION_NAVIGATE,
                Uri.parse("geo:57.7088,11.7100?q=$destQuery")
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
                    .setTitle("Byt rutt")
                    .setOnClickListener { toggleDirection() }
                    .build()
            )
            .build()

        val listBuilder = ItemList.Builder()

        if (isLoading && departures.isEmpty()) {
            return ListTemplate.Builder()
                .setTitle("${currentDirection.originName} ➔ ${currentDirection.destinationName}")
                .setActionStrip(actionStrip)
                .setLoading(true)
                .build()
        }

        if (departures.isEmpty()) {
            listBuilder.setNoItemsMessage("Inga avgångar hittades just nu")
        } else {
            val queueBreakdown = if (currentDirection == RouteDirection.VARHOLMEN_TO_HONO) {
                trafficStatus?.varholmen?.breakdown
            } else {
                trafficStatus?.hono?.breakdown
            }

            departures.forEachIndexed { index, dep ->
                val rowBuilder = Row.Builder()
                when (index) {
                    0 -> {
                        val titlePrefix = if (dep.isCancelled) "INSTÄLLD: " else "Nästa: "
                        rowBuilder.setTitle("$titlePrefix${dep.formattedTime}  (${dep.countdownText})")

                        val queueAdvice = if (queueBreakdown != null && queueBreakdown.roadQueueMinutes > 0) {
                            "Bilkö: ${queueBreakdown.roadQueueMinutes} min ➔ Prognos: ${queueBreakdown.estimatedBoardingFerryTime}"
                        } else {
                            "Fri väg (0 min kö) ➔ Överfartstid ~12 min"
                        }
                        rowBuilder.addText(queueAdvice)
                    }
                    1 -> {
                        val titlePrefix = if (dep.isCancelled) "Avgång 2 [INSTÄLLD]: " else "Avgång 2: "
                        rowBuilder.setTitle("$titlePrefix${dep.formattedTime}  (${dep.countdownText})")
                    }
                    2 -> {
                        val titlePrefix = if (dep.isCancelled) "Avgång 3 [INSTÄLLD]: " else "Avgång 3: "
                        rowBuilder.setTitle("$titlePrefix${dep.formattedTime}  (${dep.countdownText})")
                    }
                    else -> {
                        rowBuilder.setTitle("${dep.formattedTime} (${dep.countdownText})")
                    }
                }
                listBuilder.addItem(rowBuilder.build())
            }
        }

        return ListTemplate.Builder()
            .setTitle("${currentDirection.originName} ➔ ${currentDirection.destinationName}")
            .setActionStrip(actionStrip)
            .setSingleList(listBuilder.build())
            .build()
    }
}
