package com.example.ferryschedule.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

/**
 * Manages the lifecycle of the in-car connection.
 * Supplies the initial Screen rendered on the vehicle head unit.
 */
class FerrySession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return DeparturesScreen(carContext)
    }
}
