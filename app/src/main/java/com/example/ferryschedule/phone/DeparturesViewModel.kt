package com.example.ferryschedule.phone

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ferryschedule.data.repository.FerryRepositoryImpl
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
    private val repository: FerryRepository = FerryRepositoryImpl.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(FerryScheduleState(isLoading = true))
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

    fun toggleDirection() {
        val nextDirection = _uiState.value.direction.opposite()
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

            val trafficResult = repository.getTrafficStatus()
            val traffic = trafficResult.getOrNull()

            val depResult = repository.getNextDepartures(
                direction = _uiState.value.direction,
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

            // Generate updated road schematic bitmap
            _corridorBitmap.value = RoadCorridorBitmapGenerator.generateCorridorBitmap(traffic)
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
