package com.grupo2.ashley.map

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UbicacionViewModel : ViewModel() {

    private val _ubicacionSeleccionada = MutableStateFlow(LatLng(-12.0464, -77.0428)) // Lima
    val ubicacionSeleccionada = _ubicacionSeleccionada.asStateFlow()

    private val _direccionSeleccionada = MutableStateFlow("Sin dirección seleccionada")
    val direccionSeleccionada = _direccionSeleccionada.asStateFlow()

    fun actualizarUbicacion(lat: Double, lng: Double, direccion: String) {
        _ubicacionSeleccionada.value = LatLng(lat, lng)
        _direccionSeleccionada.value = direccion
    }
}