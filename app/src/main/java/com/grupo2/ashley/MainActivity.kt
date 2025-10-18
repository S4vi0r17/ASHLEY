package com.grupo2.ashley

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.places.api.Places
import com.grupo2.ashley.home.HomeViewModel
import com.grupo2.ashley.map.SeleccionarUbicacionViewModel
import com.grupo2.ashley.navigation.AppNavigation
import com.grupo2.ashley.navigation.Routes
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
    val navigationItems = listOf(
        Triple("Inicio", Icons.Default.Home, Routes.HOME),
        Triple("Chats", Icons.AutoMirrored.Filled.Message, Routes.CHATS),
        Triple("Vender", Icons.Default.AddCircle, Routes.VENDER),
        Triple("Anuncios", Icons.AutoMirrored.Filled.List, Routes.ANUNCIOS),
        Triple("Cuenta", Icons.Default.Person, Routes.CUENTA)
    )

    // Estado actual de la ruta
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Solo mostrar la barra de navegación si no estamos en la pantalla de seleccionar ubicación
            if (currentDestination != Routes.SELECCIONAR_UBICACION) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    navigationItems.forEach { (label, icon, route) ->
                        NavigationBarItem(
                            selected = currentDestination == route,
                            onClick = {
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
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (currentDestination == route)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    label,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentDestination == route)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            homeViewModel = homeViewModel,
            ubicacionViewModel = ubicacionViewModel,
            innerPadding = innerPadding,
            navigationItems = navigationItems
        )
    }
}
