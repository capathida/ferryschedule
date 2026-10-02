package com.example.ferryschedule.car

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.ferryschedule.data.repository.FerryRepositoryImpl
import com.example.ferryschedule.domain.model.TrafficStatus
import com.example.ferryschedule.domain.repository.FerryRepository
import com.example.ferryschedule.util.RoadCorridorBitmapGenerator
import kotlinx.coroutines.launch

/**
 * Android Auto screen that displays the graphical road schematic with live colored lanes,
 * current corridor speeds, queue breakdown, and one-tap Google Maps navigation.
 */
class RoadStatusCarScreen(
    carContext: CarContext,
    private val repository: FerryRepository = FerryRepositoryImpl.instance
) : Screen(carContext), DefaultLifecycleObserver {

    private var trafficStatus: TrafficStatus? = null
    private var isLoading: Boolean = true

    init {
        lifecycle.addObserver(this)
        loadTraffic()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        loadTraffic()
    }

    private fun loadTraffic() {
        lifecycleScope.launch {
            val result = repository.getTrafficStatus()
            trafficStatus = result.getOrNull()
            isLoading = false
            invalidate()
        }
    }

    private fun startNavigation() {
        try {
            val intent = Intent(
                CarContext.ACTION_NAVIGATE,
                Uri.parse("geo:57.7088,11.7100?q=Lilla+Varholmen")
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
                    .setTitle("Navigera")
                    .setOnClickListener { startNavigation() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Uppdatera")
                    .setOnClickListener {
                        isLoading = true
                        invalidate()
                        loadTraffic()
                    }
                    .build()
            )
            .build()

        val bitmap = RoadCorridorBitmapGenerator.generateCorridorBitmap(trafficStatus)
        val carIcon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()

        val paneBuilder = Pane.Builder()
            .setImage(carIcon)

        val varholmen = trafficStatus?.varholmen
        val westSpeed = varholmen?.speedKmh?.toInt() ?: 42
        val queueMinutes = varholmen?.breakdown?.roadQueueMinutes ?: 0

        // Row 1: Westbound towards ferry
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Mot Färjan (Västerut)")
                .addText("Hastighet vid rampen: $westSpeed km/h • ${varholmen?.statusText ?: "Fri fart"}")
                .build()
        )

        // Row 2: Queue & Boarding forecast
        val queueText = if (queueMinutes == 0) {
            "Ingen bilkö (0 min) ➔ Du hinner med 1:a färjan!"
        } else {
            "Bilkö ca $queueMinutes min ➔ Beräknas ombord på 2:a färjan"
        }
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Bilkötid & Färjeprognos")
                .addText(queueText)
                .build()
        )

        // Row 3: Eastbound towards city
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Mot Göteborg (Österut)")
                .addText("Trafiken flyter på normalt mot Stan")
                .build()
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Vägstatus Väg 155 & Hönöleden")
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .build()
    }
}
