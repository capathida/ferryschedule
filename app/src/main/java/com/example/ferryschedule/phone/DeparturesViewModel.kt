package com.example.ferryschedule.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ferryschedule.data.repository.FerryRepositoryImpl
import com.example.ferryschedule.domain.model.FerryScheduleState
import com.example.ferryschedule.domain.model.RouteDirection
import com.example.ferryschedule.domain.repository.FerryRepository
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

    // Allows testing specific times of day without altering device time
    var simulatedTime: LocalTime? = null
        private set

    private var tickerJob: Job? = null

    init {
        loadDepartures()
        startTicker()
    }

    fun toggleDirection() {
        val nextDirection = _uiState.value.direction.opposite()
        _uiState.update { it.copy(direction = nextDirection, isLoading = true) }
        loadDepartures()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadDepartures()
    }

    fun setSimulatedTime(time: LocalTime?) {
        simulatedTime = time
        loadDepartures()
    }

    private fun loadDepartures() {
        viewModelScope.launch {
            val referenceTime = simulatedTime ?: LocalTime.now()
            val result = repository.getNextDepartures(
                direction = _uiState.value.direction,
                fromTime = referenceTime,
                count = 3
            )
            result.onSuccess { deps ->
                _uiState.update {
                    it.copy(
                        departures = deps,
                        lastUpdated = referenceTime,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Kunde inte hämta avgångar"
                    )
                }
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000L) // Refresh countdowns every 15 seconds
                if (simulatedTime == null) {
                    loadDepartures()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
