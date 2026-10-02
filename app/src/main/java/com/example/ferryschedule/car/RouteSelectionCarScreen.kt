package com.example.ferryschedule.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.example.ferryschedule.domain.model.FerryRoute

/**
 * Android Auto screen that allows the driver to select between available ferry routes
 * (Hönöleden, Björköleden, Svanesundsleden, Gullmarsleden).
 */
class RouteSelectionCarScreen(
    carContext: CarContext,
    private val currentRoute: FerryRoute,
    private val onRouteSelected: (FerryRoute) -> Unit
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        FerryRoute.entries.forEach { route ->
            val isCurrent = route == currentRoute
            val titlePrefix = if (isCurrent) "✓ " else ""

            val row = Row.Builder()
                .setTitle("$titlePrefix${route.title}")
                .addText("${route.subtitle} • Överfart ca ${route.crossingMinutes} min")
                .setOnClickListener {
                    onRouteSelected(route)
                    screenManager.pop()
                }
                .build()

            listBuilder.addItem(row)
        }

        return ListTemplate.Builder()
            .setTitle("Välj Färjeled")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }
}
