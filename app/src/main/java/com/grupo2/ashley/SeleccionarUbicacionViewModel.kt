package com.grupo2.ashley

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SeleccionarUbicacionViewModel : ViewModel() {
    private val _ubicacionSeleccionada = MutableStateFlow(LatLng(-12.0464, -77.0428)) // Lima por defecto
    val ubicacionSeleccionada = _ubicacionSeleccionada.asStateFlow()

    fun actualizarUbicacion(lat: Double, lng: Double) {
        _ubicacionSeleccionada.value = LatLng(lat, lng)
    }
}