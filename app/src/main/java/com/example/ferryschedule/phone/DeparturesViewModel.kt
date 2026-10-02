package com.example.ferryschedule.phone

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ferryschedule.data.local.UserPreferences
import com.example.ferryschedule.data.repository.DrivingEtaRepository
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

class DeparturesViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: FerryRepository = FerryRepositoryImpl.instance,
    private val drivingEtaRepository: DrivingEtaRepository = DrivingEtaRepository.getInstance(application)
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

    val currentGoogleMapsApiKey: String
        get() = userPrefs.googleMapsApiKey

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

    fun onLocationPermissionGranted() {
        loadData()
    }

    fun saveGoogleMapsApiKey(apiKey: String) {
        userPrefs.googleMapsApiKey = apiKey
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

            val etaState = drivingEtaRepository.getDrivingEta(currentDirection, referenceTime)

            val departures = if (etaState is com.example.ferryschedule.domain.model.DrivingEtaState.Success) {
                val arrivalTime = etaState.eta.estimatedArrivalTime
                val nowDepartures = repository.getNextDepartures(currentDirection, referenceTime, 2).getOrElse { emptyList() }
                val targetDepartures = repository.getNextDepartures(currentDirection, arrivalTime, 3).getOrElse { emptyList() }

                val combined = (nowDepartures + targetDepartures)
                    .distinctBy { it.departureTime }
                    .sortedBy { it.departureTime }

                var recommendedAssigned = false
                combined.map { dep ->
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
                repository.getNextDepartures(currentDirection, referenceTime, 3).getOrElse { emptyList() }
            }

            val camerasResult = repository.getTrafficCameras()
            val cameras = camerasResult.getOrElse { emptyList() }

            _uiState.update {
                it.copy(
                    departures = departures,
                    trafficStatus = traffic,
                    cameras = cameras,
                    lastUpdated = referenceTime,
                    isLoading = false,
                    errorMessage = null,
                    drivingEtaState = etaState
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
