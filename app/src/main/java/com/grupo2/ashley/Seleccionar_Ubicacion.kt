package com.grupo2.ashley

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.findFragment
import androidx.fragment.app.commit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarUbicacionScreen(
    viewModel: SeleccionarUbicacionViewModel = viewModel()
) {
    val context = LocalContext.current
    val ubicacion by viewModel.ubicacionSeleccionada.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ubicacion, 15f)
    }

    var isPlacesReady by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            Places.initialize(context.applicationContext, "AIzaSyD-htAcCn275_30Bvi7EuErkxd4tS8BumE")
        }
        isPlacesReady = true
    }

    if (!isPlacesReady) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val placesClient = remember { Places.createClient(context) }

    var query by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf(emptyList<String>()) }

    Column(modifier = Modifier.fillMaxSize()) {


        OutlinedTextField(
            value = query,
            onValueChange = { newValue ->
                query = newValue
                if (newValue.length > 2) {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(newValue)
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            predictions = response.autocompletePredictions.map { it.getFullText(null).toString() }
                        }
                        .addOnFailureListener {
                            predictions = emptyList()
                        }
                } else {
                    predictions = emptyList()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            label = { Text("Buscar dirección...") },
            singleLine = true
        )


        LazyColumn {
            items(predictions.size) { index ->
                Text(
                    text = predictions[index],
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable() {
                            query = predictions[index]
                            predictions = emptyList()


                            val request = FindAutocompletePredictionsRequest.builder()
                                .setQuery(query)
                                .build()

                            placesClient.findAutocompletePredictions(request)
                                .addOnSuccessListener { response ->
                                    val first = response.autocompletePredictions.firstOrNull()
                                    if (first != null) {
                                        val placeId = first.placeId
                                        val fetchRequest =
                                            com.google.android.libraries.places.api.net.FetchPlaceRequest.builder(
                                                placeId,
                                                listOf(
                                                    Place.Field.LAT_LNG,
                                                    Place.Field.NAME
                                                )
                                            ).build()

                                        placesClient.fetchPlace(fetchRequest)
                                            .addOnSuccessListener { placeResponse ->
                                                placeResponse.place.latLng?.let { latLng ->
                                                    viewModel.actualizarUbicacion(latLng.latitude, latLng.longitude)
                                                    cameraPositionState.move(
                                                        CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                                                    )
                                                }
                                            }
                                    }
                                }
                        }
                        .padding(12.dp)
                )
                Divider()
            }
        }


        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = ubicacion),
                    title = "Ubicación seleccionada"
                )
            }

            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "Ubicación guardada: ${ubicacion.latitude}, ${ubicacion.longitude}",
                        Toast.LENGTH_LONG
                    ).show()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Confirmar ubicación")
            }
        }
    }
}