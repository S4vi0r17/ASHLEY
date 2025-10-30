package com.grupo2.ashley.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.grupo2.ashley.map.UbicacionViewModel
import com.grupo2.ashley.navigation.Routes

@Composable
fun VenderScreen(
    navController: NavHostController,
    viewModel: UbicacionViewModel,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val ubicacion by viewModel.ubicacionSeleccionada.collectAsState()
    val direccion by viewModel.direccionSeleccionada.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
    ) {
        Text(
            text = "Publica algo en Vender",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Ubicación guardada:",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Lat: ${ubicacion.latitude}, Lng: ${ubicacion.longitude}",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Dirección: $direccion",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(20.dp))

        Button(onClick = { navController.navigate(Routes.SELECCIONAR_UBICACION) }) {
            Text("Seleccionar ubicación")
        }
    }
}
