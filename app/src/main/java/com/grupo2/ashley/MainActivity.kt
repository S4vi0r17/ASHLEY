package com.grupo2.ashley

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.places.api.Places
import com.grupo2.ashley.anuncios.AnunciosViewModel
import com.grupo2.ashley.chat.UnreadMessagesViewModel
import com.grupo2.ashley.home.HomeViewModel
import com.grupo2.ashley.login.updateLocale
import com.grupo2.ashley.map.UbicacionViewModel
import com.grupo2.ashley.navigation.AppNavigation
import com.grupo2.ashley.navigation.Routes
import com.grupo2.ashley.product.ProductViewModel
import com.grupo2.ashley.profile.ProfileViewModel
import com.grupo2.ashley.ui.theme.ASHLEYTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyD-htAcCn275_30Bvi7EuErkxd4tS8BumE")
        }

        runBlocking {
            val savedLanguage = LanguagePreferences.languageFlow(this@MainActivity).firstOrNull()
            val languageToUse = savedLanguage ?: Locale.getDefault().language

            // Guardar el idioma del dispositivo si es la primera vez
            if (savedLanguage == null) {
                LanguagePreferences.saveLanguage(this@MainActivity, languageToUse)
            }

            updateLocale(languageToUse)
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
    val ubicacionViewModel: UbicacionViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val anunciosViewModel: AnunciosViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val unreadMessagesViewModel: UnreadMessagesViewModel = hiltViewModel()
    val context = LocalContext.current

    val unreadCount by unreadMessagesViewModel.unreadCount.collectAsState()

    val navigationItems = listOf(
        Triple(context.getString(R.string.inicio), Icons.Default.Home, Routes.HOME),
        Triple(context.getString(R.string.chats), Icons.AutoMirrored.Filled.Message, Routes.CHATS),
        Triple(context.getString(R.string.vender), Icons.Default.AddCircle, Routes.VENDER),
        Triple(context.getString(R.string.anuncios), Icons.AutoMirrored.Filled.List, Routes.ANUNCIOS),
        Triple(context.getString(R.string.cuenta), Icons.Default.Person, Routes.CUENTA)
    )

    // Estado actual de la ruta
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination?.route

    // Refrescar contador cuando se navega a chats
    LaunchedEffect(currentDestination) {
        if (currentDestination == Routes.CHATS) {
            unreadMessagesViewModel.refreshUnreadCount()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Solo mostrar la barra de navegación si no estamos en la pantalla de seleccionar ubicación
            if (currentDestination != Routes.SELECCIONAR_UBICACION &&
                !currentDestination.orEmpty().startsWith("chat/") &&
                !currentDestination.orEmpty().startsWith("participant_info/") &&
                !currentDestination.orEmpty().startsWith("product_map/")) {
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
                                BadgedBox(
                                    badge = {
                                        // Solo mostrar badge en el ícono de Chats
                                        if (route == Routes.CHATS && unreadCount > 0) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ) {
                                                Text(
                                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = label,
                                        tint = if (currentDestination == route)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
            profileViewModel = profileViewModel,
            anunciosViewModel = anunciosViewModel,
            innerPadding = innerPadding,
            productViewModel = productViewModel,
            navigationItems = navigationItems
        )
    }
}
