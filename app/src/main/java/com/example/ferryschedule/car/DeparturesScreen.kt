package com.example.ferryschedule.car

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
import com.example.ferryschedule.domain.repository.FerryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Driver-friendly Android Auto screen displaying the next 3 ferry departures.
 * Follows Google's Android for Cars Design Guidelines (glanceable, distraction-free).
 */
class DeparturesScreen(
    carContext: CarContext,
    private val repository: FerryRepository = FerryRepositoryImpl.instance
) : Screen(carContext), DefaultLifecycleObserver {

    private var currentDirection: RouteDirection = RouteDirection.HONO_TO_VARHOLMEN
    private var departures: List<FerryDeparture> = emptyList()
    private var isLoading: Boolean = true
    private var refreshJob: Job? = null

    init {
        lifecycle.addObserver(this)
        loadDepartures()
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

    private fun loadDepartures() {
        lifecycleScope.launch {
            val result = repository.getNextDepartures(
                direction = currentDirection,
                fromTime = LocalTime.now(),
                count = 3
            )
            departures = result.getOrElse { emptyList() }
            isLoading = false
            invalidate()
        }
    }

    private fun startAutoRefreshTicker() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(30_000L) // Refresh every 30 seconds
                val result = repository.getNextDepartures(
                    direction = currentDirection,
                    fromTime = LocalTime.now(),
                    count = 3
                )
                departures = result.getOrElse { emptyList() }
                invalidate()
            }
        }
    }

    private fun toggleDirection() {
        currentDirection = currentDirection.opposite()
        isLoading = true
        invalidate()
        loadDepartures()
    }

    override fun onGetTemplate(): Template {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Byt riktning")
                    .setOnClickListener { toggleDirection() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Uppdatera")
                    .setOnClickListener {
                        isLoading = true
                        invalidate()
                        loadDepartures()
                    }
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
            departures.forEachIndexed { index, dep ->
                val rowBuilder = Row.Builder()
                when (index) {
                    0 -> {
                        rowBuilder.setTitle("Nästa: ${dep.formattedTime}  (${dep.countdownText})")
                        rowBuilder.addText("Överfartstid ~12 min • Trafikverket Färjerederiet")
                    }
                    1 -> {
                        rowBuilder.setTitle("Avgång 2: ${dep.formattedTime}  (${dep.countdownText})")
                    }
                    2 -> {
                        rowBuilder.setTitle("Avgång 3: ${dep.formattedTime}  (${dep.countdownText})")
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
