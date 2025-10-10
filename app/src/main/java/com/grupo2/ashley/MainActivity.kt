package com.grupo2.ashley

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.places.api.Places
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.grupo2.ashley.map.SeleccionarUbicacionScreen
import com.grupo2.ashley.map.SeleccionarUbicacionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import com.grupo2.ashley.home.HomeScreen
import com.grupo2.ashley.home.HomeViewModel
import com.grupo2.ashley.ui.theme.ASHLEYTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyD-htAcCn275_30Bvi7EuErkxd4tS8BumE")
        }
        setContent {
            ASHLEYTheme {
                AshleyApp()
            }
        }
    }
}

@Composable
fun AshleyApp() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val ubicacionViewModel: SeleccionarUbicacionViewModel = viewModel()

    // Define rutas e íconos
    val items = listOf(
        Triple("Inicio", Icons.Default.Home, "home"),
        Triple("Chats", Icons.AutoMirrored.Filled.Message, "chats"),
        Triple("Vender", Icons.Default.AddCircle, "vender"),
        Triple("Anuncios", Icons.AutoMirrored.Filled.List, "anuncios"),
        Triple("Cuenta", Icons.Default.Person, "cuenta")
    )

    // Estado actual de la ruta
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEach { (label, icon, route) ->
                    NavigationBarItem(
                        selected = currentDestination == route,
                        onClick = {
                            // Evita re-navegar si ya estás en esa ruta
                            if (currentDestination != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, maxLines = 1, textAlign = TextAlign.Center) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = homeViewModel,
                    ubicacionViewModel = ubicacionViewModel,
                    onLocationClick = { navController.navigate("seleccionar_ubicacion") },
                    innerPadding = innerPadding
                )
            }

            composable("chats") { ScreenContent("Lista de chats", innerPadding) }

            composable("vender") { VenderScreen(navController, ubicacionViewModel) }

            composable("anuncios") { ScreenContent("Tus anuncios publicados", innerPadding) }

            composable("cuenta") { ScreenContent("Tu perfil", innerPadding) }

            composable("seleccionar_ubicacion") {
                SeleccionarUbicacionScreen(viewModel = ubicacionViewModel)
            }
        }
    }
}

@Composable
fun VenderScreen(
    navController: NavHostController,
    viewModel: SeleccionarUbicacionViewModel
) {
    val ubicacion by viewModel.ubicacionSeleccionada.collectAsState()
    val direccion by viewModel.direccionSeleccionada.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Publica algo en Vender")

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Ubicación guardada:",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Lat: ${ubicacion.latitude}, Lng: ${ubicacion.longitude}",
            color = Color.Gray
        )

        Text(
            text = "Dirección: ${direccion.toString()}",
            color = Color.Gray
        )

        Spacer(Modifier.height(20.dp))

        Button(onClick = { navController.navigate("seleccionar_ubicacion") }) {
            Text("Seleccionar ubicación")
        }
    }
}

@Composable
fun ScreenContent(text: String, innerPadding: PaddingValues) {
    Text(
        text = text,
        modifier = Modifier.padding(innerPadding),
        textAlign = TextAlign.Center
    )
}


@Preview(showBackground = true)
@Composable
fun AshleyPreview() {
    ASHLEYTheme {
        AshleyApp()
    }
}
