package com.grupo2.ashley.productdetail

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.maps.android.compose.*
import com.grupo2.ashley.home.models.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductMapScreen(
    product: Product,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    var routeInfo by remember { mutableStateOf<RouteInfo?>(null) }
    
    val destinationLocation = LatLng(product.deliveryLatitude, product.deliveryLongitude)
    
    // Obtener API Key desde AndroidManifest
    val apiKey = remember {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    // Camera state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(destinationLocation, 12f)
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Obtener ubicación actual
            scope.launch {
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    val location = fusedLocationClient.lastLocation.await()
                    location?.let {
                        currentLocation = LatLng(it.latitude, it.longitude)
                        
                        // Obtener la ruta
                        isLoadingRoute = true
                        val result = getDirectionsRoute(
                            origin = LatLng(it.latitude, it.longitude),
                            destination = destinationLocation,
                            apiKey = apiKey
                        )
                        routePoints = result.points
                        routeInfo = result.info
                        isLoadingRoute = false
                        
                        // Ajustar cámara para mostrar ambos puntos
                        val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
                            .include(LatLng(it.latitude, it.longitude))
                            .include(destinationLocation)
                            .build()
                        
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 150)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    isLoadingRoute = false
                }
            }
        }
    }
    
    // Verificar permisos al iniciar
    LaunchedEffect(Unit) {
        hasLocationPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Obtener ubicación actual
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                    
                    // Obtener la ruta
                    isLoadingRoute = true
                    val result = getDirectionsRoute(
                        origin = LatLng(it.latitude, it.longitude),
                        destination = destinationLocation,
                        apiKey = apiKey
                    )
                    routePoints = result.points
                    routeInfo = result.info
                    isLoadingRoute = false
                    
                    // Ajustar cámara para mostrar ambos puntos
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
                        .include(LatLng(it.latitude, it.longitude))
                        .include(destinationLocation)
                        .build()
                    
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 150)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isLoadingRoute = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubicación de entrega") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentLocation != null) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            currentLocation?.let { current ->
                                val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
                                    .include(current)
                                    .include(destinationLocation)
                                    .build()
                                
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngBounds(bounds, 100)
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Centrar en mi ubicación"
                    )
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
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                // Marcador de destino (producto)
                Marker(
                    state = MarkerState(position = destinationLocation),
                    title = product.title,
                    snippet = product.location
                )
                
                // NO agregamos marcador de "Mi ubicación" - solo se muestra el punto azul nativo del GPS
                
                // Dibujar la ruta con Polyline
                if (routePoints.isNotEmpty()) {
                    Polyline(
                        points = routePoints,
                        color = MaterialTheme.colorScheme.primary,
                        width = 12f
                    )
                }
            }
            
            // Card con información del producto y ruta
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Información de la ruta
                    if (currentLocation != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Distancia
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Distancia",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = routeInfo?.distance ?: run {
                                            val distance = calculateDistance(
                                                currentLocation!!.latitude,
                                                currentLocation!!.longitude,
                                                destinationLocation.latitude,
                                                destinationLocation.longitude
                                            )
                                            formatDistance(distance)
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                            
                            // Tiempo estimado
                            routeInfo?.duration?.let { duration ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = "Tiempo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = duration,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (isLoadingRoute) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Calculando ruta...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (routeInfo == null && routePoints.isEmpty() && !isLoadingRoute) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No se pudo calcular la ruta. Verifica tu conexión.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Activa la ubicación para ver la distancia y el tiempo estimado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clase para almacenar información de la ruta
 */
data class RouteInfo(
    val distance: String,
    val duration: String,
    val distanceMeters: Int,
    val durationSeconds: Int
)

/**
 * Resultado de la API de Directions
 */
data class DirectionsResult(
    val points: List<LatLng>,
    val info: RouteInfo?
)

/**
 * Calcula la distancia entre dos puntos usando la fórmula de Haversine
 */
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371 // Radio de la Tierra en kilómetros
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

/**
 * Formatea la distancia en un texto legible
 */
private fun formatDistance(distanceKm: Double): String {
    return if (distanceKm < 1) {
        "${(distanceKm * 1000).toInt()} metros"
    } else if (distanceKm < 10) {
        String.format("%.1f km", distanceKm)
    } else {
        "${distanceKm.toInt()} km"
    }
}

/**
 * Obtiene la ruta desde Google Directions API
 */
private suspend fun getDirectionsRoute(origin: LatLng, destination: LatLng, apiKey: String): DirectionsResult {
    return withContext(Dispatchers.IO) {
        try {
            val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=driving" +
                    "&key=$apiKey"
            
            val response = URL(urlString).readText()
            val json = JSONObject(response)
            
            if (json.getString("status") == "OK") {
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    
                    // Obtener la polilínea
                    val overviewPolyline = route.getJSONObject("overview_polyline")
                    val encodedString = overviewPolyline.getString("points")
                    val points = decodePolyline(encodedString)
                    
                    // Obtener información de distancia y duración
                    val legs = route.getJSONArray("legs")
                    if (legs.length() > 0) {
                        val leg = legs.getJSONObject(0)
                        
                        val distance = leg.getJSONObject("distance")
                        val distanceText = distance.getString("text")
                        val distanceValue = distance.getInt("value")
                        
                        val duration = leg.getJSONObject("duration")
                        val durationText = duration.getString("text")
                        val durationValue = duration.getInt("value")
                        
                        val routeInfo = RouteInfo(
                            distance = distanceText,
                            duration = durationText,
                            distanceMeters = distanceValue,
                            durationSeconds = durationValue
                        )
                        
                        return@withContext DirectionsResult(points, routeInfo)
                    }
                    
                    return@withContext DirectionsResult(points, null)
                }
            }
            
            Log.e("DirectionsAPI", "Error: ${json.getString("status")}")
            DirectionsResult(emptyList(), null)
        } catch (e: Exception) {
            Log.e("DirectionsAPI", "Exception: ${e.message}", e)
            DirectionsResult(emptyList(), null)
        }
    }
}

/**
 * Decodifica una polilínea codificada de Google Maps
 */
private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
        poly.add(p)
    }

    return poly
}
