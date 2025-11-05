package com.grupo2.ashley.profile

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import android.location.Geocoder
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ProfileLocationPickerScreen(
    viewModel: ProfileViewModel,
    onLocationSelected: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLatitude by viewModel.defaultPickupLatitude.collectAsState()
    val currentLongitude by viewModel.defaultPickupLongitude.collectAsState()
    val currentLocationName by viewModel.defaultPickupLocationName.collectAsState()

    // Ubicación inicial (Lima por defecto o la guardada)
    val initialLocation = if (currentLatitude != 0.0 && currentLongitude != 0.0) {
        LatLng(currentLatitude, currentLongitude)
    } else {
        LatLng(-12.0464, -77.0428) // Lima, Perú
    }

    var selectedLocation by remember { mutableStateOf(initialLocation) }
    var selectedAddress by remember { mutableStateOf(currentLocationName.ifBlank { "Selecciona una ubicación" }) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 15f)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Obtener ubicación actual
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    selectedLocation = currentLatLng
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f)
                    )
                    
                    // Obtener dirección
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            selectedAddress = addresses[0].getAddressLine(0) ?: "Ubicación actual"
                        }
                    } catch (e: Exception) {
                        selectedAddress = "Lat: ${it.latitude}, Lng: ${it.longitude}"
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        hasLocationPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Ubicación de Entrega") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Botón para centrar en ubicación actual
                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    val currentLatLng = LatLng(it.latitude, it.longitude)
                                    selectedLocation = currentLatLng
                                    cameraPositionState.move(
                                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f)
                                    )
                                    
                                    try {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                                        if (!addresses.isNullOrEmpty()) {
                                            selectedAddress = addresses[0].getAddressLine(0) ?: "Ubicación actual"
                                        }
                                    } catch (e: Exception) {
                                        selectedAddress = "Lat: ${it.latitude}, Lng: ${it.longitude}"
                                    }
                                }
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
                }

                // Botón para confirmar ubicación
                FloatingActionButton(
                    onClick = {
                        viewModel.setDefaultPickupLocation(
                            selectedAddress,
                            selectedLocation.latitude,
                            selectedLocation.longitude
                        )
                        onLocationSelected()
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Confirmar")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    selectedLocation = latLng
                    
                    // Obtener dirección del punto seleccionado
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            selectedAddress = addresses[0].getAddressLine(0) ?: "Ubicación seleccionada"
                        } else {
                            selectedAddress = "Lat: ${latLng.latitude}, Lng: ${latLng.longitude}"
                        }
                    } catch (e: Exception) {
                        selectedAddress = "Lat: ${latLng.latitude}, Lng: ${latLng.longitude}"
                    }
                }
            ) {
                Marker(
                    state = MarkerState(position = selectedLocation),
                    title = "Ubicación de entrega",
                    snippet = selectedAddress
                )
            }

            // Card con la información de la ubicación seleccionada
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Ubicación Seleccionada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        selectedAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Lat: ${"%.6f".format(selectedLocation.latitude)}, Lng: ${"%.6f".format(selectedLocation.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
