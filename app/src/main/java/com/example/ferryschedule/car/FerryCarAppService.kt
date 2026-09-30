package com.example.ferryschedule.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Entry point for Android Auto and Automotive OS.
 * The system binds to this service when the device connects to a car display or DHU.
 */
class FerryCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // ALLOW_ALL_HOSTS_VALIDATOR enables testing with Desktop Head Unit (DHU)
        // and Android Auto developer mode without certificate pinning issues.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return FerrySession()
    }
}
