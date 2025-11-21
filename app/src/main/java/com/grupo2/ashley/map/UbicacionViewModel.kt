package com.grupo2.ashley.map

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.grupo2.ashley.R

class UbicacionViewModel : ViewModel() {

    private val _ubicacionSeleccionada = MutableStateFlow(LatLng(-12.0464, -77.0428)) // Lima
    val ubicacionSeleccionada = _ubicacionSeleccionada.asStateFlow()

    private val _direccionSeleccionada = MutableStateFlow("Sin dirección seleccionada")
    val direccionSeleccionada = _direccionSeleccionada.asStateFlow()

    private val _nombreUbicacion = MutableStateFlow("")
    val nombreUbicacion = _nombreUbicacion.asStateFlow()

    fun actualizarUbicacion(lat: Double, lng: Double, direccion: String, nombre: String = "") {
        _ubicacionSeleccionada.value = LatLng(lat, lng)
        _direccionSeleccionada.value = direccion
        _nombreUbicacion.value = nombre.ifEmpty { 
            // Extraer nombre corto de la dirección (primera parte)
            direccion.split(",").firstOrNull()?.trim() ?: "Ubicación seleccionada"
        }
    }
}