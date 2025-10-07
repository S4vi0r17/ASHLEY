package com.grupo2.ashley

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grupo2.ashley.home.HomeScreen
import com.grupo2.ashley.home.HomeViewModel
import com.grupo2.ashley.ui.theme.ASHLEYTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASHLEYTheme {
                AshleyApp()
            }
        }
    }
}

@Composable
fun AshleyApp() {
    var selectedItem by remember { mutableIntStateOf(0) }

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
                        onClick = { selectedItem = index },
                        icon = { Icon(item.second, contentDescription = item.first) },
                        label = { Text(item.first, maxLines = 1, textAlign = TextAlign.Center) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedItem) {
            0 -> {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onLocationClick = { /* TODO: Abrir diálogo para cambiar ubicación */ },
                    innerPadding = innerPadding
                )
            }
            1 -> ScreenContent("Lista de chats", innerPadding)
            2 -> ScreenContent("Publica algo en Vender", innerPadding)
            3 -> ScreenContent("Tus anuncios publicados", innerPadding)
            4 -> ScreenContent("Tu perfil", innerPadding)
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
