package com.grupo2.ashley.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo2.ashley.dashboard.data.StatsRepository
import com.grupo2.ashley.dashboard.models.DashboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val repository = StatsRepository()
    
    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()
    
    companion object {
        private const val TAG = "DashboardViewModel"
    }
    
    init {
        loadStats()
    }
    
    fun loadStats() {
        viewModelScope.launch {
            _dashboardState.update { it.copy(isLoading = true, error = null) }
            
            repository.getUserStats()
                .onSuccess { stats ->
                    Log.d(TAG, "Estadísticas cargadas exitosamente")
                    _dashboardState.update { 
                        it.copy(
                            isLoading = false,
                            stats = stats,
                            error = null,
                            lastUpdated = System.currentTimeMillis()
                        ) 
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "Error al cargar estadísticas: ${exception.message}", exception)
                    _dashboardState.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error desconocido"
                        ) 
                    }
                }
        }
    }
    
    fun refreshStats() {
        loadStats()
    }
}
