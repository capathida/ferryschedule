package com.example.ferryschedule.phone

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ferryschedule.data.local.UserPreferences
import com.example.ferryschedule.data.repository.FerryRepositoryImpl
import com.example.ferryschedule.domain.model.FerryRoute
import com.example.ferryschedule.domain.model.FerryScheduleState
import com.example.ferryschedule.domain.model.RouteDirection
import com.example.ferryschedule.domain.repository.FerryRepository
import com.example.ferryschedule.util.RoadCorridorBitmapGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

class DeparturesViewModel(
    application: Application,
    private val repository: FerryRepository = FerryRepositoryImpl.instance
) : AndroidViewModel(application) {

    private val userPrefs = UserPreferences.getInstance(application)

    private val _uiState = MutableStateFlow(
        FerryScheduleState(
            direction = userPrefs.savedDirection,
            isLoading = true
        )
    )
    val uiState: StateFlow<FerryScheduleState> = _uiState.asStateFlow()

    private val _corridorBitmap = MutableStateFlow<Bitmap?>(null)
    val corridorBitmap: StateFlow<Bitmap?> = _corridorBitmap.asStateFlow()

    var simulatedTime: LocalTime? = null
        private set

    private var tickerJob: Job? = null

    init {
        loadData()
        startTicker()
    }

    fun selectRoute(route: FerryRoute) {
        val newDirection = route.defaultDirection
        userPrefs.savedDirection = newDirection
        _uiState.update { it.copy(direction = newDirection, isLoading = true) }
        loadData()
    }

    fun selectDirection(direction: RouteDirection) {
        userPrefs.savedDirection = direction
        _uiState.update { it.copy(direction = direction, isLoading = true) }
        loadData()
    }

    fun toggleDirection() {
        val nextDirection = _uiState.value.direction.opposite()
        userPrefs.savedDirection = nextDirection
        _uiState.update { it.copy(direction = nextDirection, isLoading = true) }
        loadData()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadData()
    }

    fun setSimulatedTime(time: LocalTime?) {
        simulatedTime = time
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val referenceTime = simulatedTime ?: LocalTime.now()
            val currentDirection = _uiState.value.direction

            val trafficResult = repository.getTrafficStatus(referenceTime)
            val traffic = trafficResult.getOrNull()

            val depResult = repository.getNextDepartures(
                direction = currentDirection,
                fromTime = referenceTime,
                count = 3
            )
            val camerasResult = repository.getTrafficCameras()
            val cameras = camerasResult.getOrElse { emptyList() }

            val departures = depResult.getOrElse { emptyList() }

            _uiState.update {
                it.copy(
                    departures = departures,
                    trafficStatus = traffic,
                    cameras = cameras,
                    lastUpdated = referenceTime,
                    isLoading = false,
                    errorMessage = null
                )
            }

            // Generate updated road schematic bitmap for the active route
            _corridorBitmap.value = RoadCorridorBitmapGenerator.generateCorridorBitmap(
                trafficStatus = traffic,
                route = currentDirection.route
            )
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L) // Refresh every 30 seconds
                if (simulatedTime == null) {
                    loadData()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
