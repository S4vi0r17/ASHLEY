package com.grupo2.ashley.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    viewModel: UbicacionViewModel,
    onLocationConfirmed: () -> Unit = {}
) {
    val context = LocalContext.current
    val ubicacion by viewModel.ubicacionSeleccionada.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ubicacion, 15f)
    }

    var isPlacesReady by remember { mutableStateOf(false) }
    var hasInitializedLocation by remember { mutableStateOf(false) }

    // Obtener ubicación actual al iniciar
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            Places.initialize(context.applicationContext, "AIzaSyD-htAcCn275_30Bvi7EuErkxd4tS8BumE")
        }
        isPlacesReady = true
        
        // Obtener ubicación actual automáticamente
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    if (!hasInitializedLocation) {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        
                        // Obtener dirección de la ubicación actual
                        val geocoder = Geocoder(context, Locale.getDefault())
                        try {
                            val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                            val direccion = if (!addresses.isNullOrEmpty()) {
                                addresses[0].getAddressLine(0) ?: "Ubicación actual"
                            } else {
                                "Ubicación actual"
                            }
                            
                            viewModel.actualizarUbicacion(
                                it.latitude,
                                it.longitude,
                                direccion,
                                nombre = ""  // Se extraerá automáticamente
                            )
                            
                            cameraPositionState.move(
                                CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f)
                            )
                            hasInitializedLocation = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    if (!isPlacesReady) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val placesClient = remember { Places.createClient(context) }

    var query by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf(emptyList<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar ubicación de entrega") },
                navigationIcon = {
                    IconButton(onClick = onLocationConfirmed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

        OutlinedTextField(
            value = query,
            onValueChange = { newValue ->
                query = newValue
                if (newValue.length > 2) {
                    val request =
                        FindAutocompletePredictionsRequest.builder().setQuery(newValue).build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            predictions = response.autocompletePredictions.map {
                                it.getFullText(null).toString()
                            }
                        }.addOnFailureListener {
                            predictions = emptyList()
                        }
                } else {
                    predictions = emptyList()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            label = { Text("Buscar dirección...") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Gray,
                cursorColor = Color(0xFF90CAF9),
                focusedLabelColor = Color(0xFF90CAF9),
                unfocusedLabelColor = Color(0xFFB0BEC5),
                focusedBorderColor = Color(0xFF64B5F6),
                unfocusedBorderColor = Color.Gray
            )
        )


        LazyColumn {
            items(predictions.size) { index ->
                Text(text = predictions[index], modifier = Modifier
                    .fillMaxWidth()
                    .clickable() {
                        query = predictions[index]
                        predictions = emptyList()


                        val request =
                            FindAutocompletePredictionsRequest.builder().setQuery(query).build()

                        placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener { response ->
                                val first = response.autocompletePredictions.firstOrNull()
                                if (first != null) {
                                    val placeId = first.placeId
                                    val fetchRequest = FetchPlaceRequest.builder(
                                        placeId, listOf(
                                            Place.Field.LAT_LNG, Place.Field.NAME
                                        )
                                    ).build()

                                    placesClient.fetchPlace(fetchRequest)
                                        .addOnSuccessListener { placeResponse ->
                                            placeResponse.place.latLng?.let { latLng ->
                                                // Obtener la dirección completa
                                                val geocoder = Geocoder(context, Locale.getDefault())
                                                val direccion = try {
                                                    val addresses = geocoder.getFromLocation(
                                                        latLng.latitude,
                                                        latLng.longitude,
                                                        1
                                                    )
                                                    addresses?.firstOrNull()?.getAddressLine(0) ?: query
                                                } catch (e: Exception) {
                                                    query
                                                }
                                                
                                                viewModel.actualizarUbicacion(
                                                    latLng.latitude,
                                                    latLng.longitude,
                                                    direccion,
                                                    nombre = ""  // Se extraerá automáticamente de la dirección
                                                )
                                                cameraPositionState.move(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        latLng, 16f
                                                    )
                                                )
                                            }
                                        }
                                }
                            }
                    }
                    .padding(12.dp))
                HorizontalDivider()
            }
        }


        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(), 
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    // Al hacer clic en el mapa, actualizar la ubicación
                    val geocoder = Geocoder(context, Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                        val direccion = if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            address.getAddressLine(0) ?: "Dirección desconocida"
                        } else {
                            "Dirección desconocida"
                        }
                        
                        viewModel.actualizarUbicacion(
                            latLng.latitude,
                            latLng.longitude,
                            direccion,
                            nombre = ""  // Se extraerá automáticamente
                        )
                        
                        Toast.makeText(
                            context,
                            "Ubicación actualizada",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        viewModel.actualizarUbicacion(
                            latLng.latitude,
                            latLng.longitude,
                            "Ubicación personalizada",
                            nombre = "Ubicación personalizada"
                        )
                    }
                }
            ) {
                Marker(
                    state = MarkerState(position = ubicacion), 
                    title = "Ubicación de entrega",
                    snippet = "Toca el mapa para cambiar la ubicación"
                )
            }

            val context = LocalContext.current


            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(context, "Permiso de ubicación concedido", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT)
                        .show()
                }
            }


            Button(
                onClick = {
                    val fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(context)
                    if (ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            location?.let {
                                val currentLatLng = LatLng(it.latitude, it.longitude)

                                // 🔹 Usa Geocoder para convertir coordenadas → dirección
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val addresses =
                                    geocoder.getFromLocation(it.latitude, it.longitude, 1)
                                val direccion = if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    address.getAddressLine(0) ?: "Dirección desconocida"
                                } else {
                                    "Dirección desconocida"
                                }


                                viewModel.actualizarUbicacion(
                                    it.latitude,
                                    it.longitude,
                                    direccion,
                                    nombre = ""  // Se extraerá automáticamente
                                )


                                cameraPositionState.move(
                                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f)
                                )

                                Toast.makeText(
                                    context, "Ubicación actual detectada", Toast.LENGTH_SHORT
                                ).show()
                            } ?: Toast.makeText(
                                context, "No se pudo obtener ubicación", Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {

                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }, modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(100.dp)
            ) {
                Text("Mi ubicación")
            }

            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "Ubicación guardada",
                        Toast.LENGTH_SHORT
                    ).show()
                    onLocationConfirmed()
                }, modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(50.dp)
            ) {
                Text("Confirmar ubicación")
            }
        }
        }
    }
}