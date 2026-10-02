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
import com.example.ferryschedule.domain.model.FerryRoute
import com.example.ferryschedule.domain.model.TrafficStatus
import com.example.ferryschedule.domain.repository.FerryRepository
import com.example.ferryschedule.util.RoadCorridorBitmapGenerator
import kotlinx.coroutines.launch

/**
 * Android Auto screen that displays the graphical road schematic with live colored lanes,
 * corridor speeds, queue breakdown, and one-tap Google Maps navigation for the active ferry route.
 */
class RoadStatusCarScreen(
    carContext: CarContext,
    private val route: FerryRoute = FerryRoute.HONOLEDEN,
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
            val query = route.defaultDirection.navQuery
            val intent = Intent(
                CarContext.ACTION_NAVIGATE,
                Uri.parse("geo:57.7088,11.7100?q=${Uri.encode(query)}")
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

        val bitmap = RoadCorridorBitmapGenerator.generateCorridorBitmap(trafficStatus, route)
        val carIcon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()

        val paneBuilder = Pane.Builder()
            .setImage(carIcon)

        val varholmen = trafficStatus?.varholmen
        val hono = trafficStatus?.hono
        val westSpeed = varholmen?.speedKmh?.toInt() ?: 42
        val vQueueMinutes = varholmen?.breakdown?.roadQueueMinutes ?: 0
        val honoSpeed = hono?.speedKmh?.toInt() ?: 45
        val hQueueMinutes = hono?.breakdown?.roadQueueMinutes ?: 0

        when (route) {
            FerryRoute.HONOLEDEN -> {
                // Row 1: Fastlandet (Väg 155) -> Färjan
                val varholmenText = if (vQueueMinutes == 0) {
                    "Hastighet: $westSpeed km/h • Fri väg (0 min kö) ➔ 1:a färjan"
                } else {
                    "Hastighet: $westSpeed km/h • Kö $vQueueMinutes min ➔ ${varholmen?.breakdown?.estimatedBoardingFerryTime ?: "2:a färjan"}"
                }
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Fastlandet: Lilla Varholmen (Väg 155)")
                        .addText(varholmenText)
                        .build()
                )

                // Row 2: Hönö (Väg 574) -> Pinan Färjeläge
                val honoText = if (hQueueMinutes == 0) {
                    "Hastighet: $honoSpeed km/h • Fri väg (0 min kö) ➔ 1:a färjan"
                } else {
                    "Hastighet: $honoSpeed km/h • Morgonkö $hQueueMinutes min i filerna ➔ ${hono?.breakdown?.estimatedBoardingFerryTime ?: "2:a färjan"}"
                }
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Hönö: Pinan Färjeläge (Väg 574)")
                        .addText(honoText)
                        .build()
                )

                // Row 3: Returriktningar
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Returvägar (Mot Stan & Ut på Hönö)")
                        .addText("Grönt flöde och fri fart i båda riktningarna från färjorna")
                        .build()
                )
            }
            FerryRoute.BJORKOLEDEN -> {
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Fastlandet: Lilla Varholmen (Väg 155)")
                        .addText("Hastighet: $westSpeed km/h • ${if (vQueueMinutes == 0) "Fri fart" else "Kö ca $vQueueMinutes min"}")
                        .build()
                )
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Björkö: Grönevik Färjeläge")
                        .addText("Fri framkomlighet vid terminalen • Överfartstid ~6 min")
                        .build()
                )
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Returvägar")
                        .addText("Normalt flöde mot Stan och ut på Björkö")
                        .build()
                )
            }
            FerryRoute.SVANESUNDSLEDEN -> {
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Fastlandet: Kolhättan (Väg 770)")
                        .addText("Fri väg mot färjeläget mot Orust • Överfart ~5 min")
                        .build()
                )
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Orust: Svanesund (Väg 160)")
                        .addText("Fri framkomlighet mot fastlandet / Stenungsund")
                        .build()
                )
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Anslutningsvägar")
                        .addText("Trafiken flyter normalt mot Stenungsund (E6) och Henån")
                        .build()
                )
            }
            FerryRoute.GULLMARSLEDEN -> {
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Lysekil: Finnsbo (Väg 161)")
                        .addText("Fri väg mot Skår • Överfartstid ~10 min")
                        .build()
                )
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Skaftö / Bokenäs: Skår")
                        .addText("Fri väg mot Finnsbo och Lysekil")
                        .build()
                )
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Väg 161")
                        .addText("Normal framkomlighet mot Torp/E6 och Lysekil")
                        .build()
                )
            }
        }

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Vägkarta: ${route.title}")
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .build()
    }
}
