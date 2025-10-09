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
    var selectedItem by remember { mutableIntStateOf(0) }

    val ubicacionViewModel: SeleccionarUbicacionViewModel = viewModel()

    val items = listOf(
        "Inicio" to Icons.Default.Home,
        "Chats" to Icons.AutoMirrored.Filled.Message,
        "Vender" to Icons.Default.AddCircle,
        "Anuncios" to Icons.AutoMirrored.Filled.List,
        "Cuenta" to Icons.Default.Person
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            when (index) {
                                0 -> navController.navigate("inicio")
                                1 -> navController.navigate("chats")
                                2 -> navController.navigate("vender")
                                3 -> navController.navigate("anuncios")
                                4 -> navController.navigate("cuenta")
                            }
                        },
                        icon = { Icon(item.second, contentDescription = item.first) },
                        label = { Text(item.first, maxLines = 1, textAlign = TextAlign.Center) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "inicio",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("inicio") { ScreenContent("Bienvenido a ASHLEY", innerPadding) }
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
            text = "Direccion: ${direccion.toString()}",
            color = Color.Gray
        )

        Spacer(Modifier.height(20.dp))

        Button(onClick = {
            navController.navigate("seleccionar_ubicacion")
        }) {
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
