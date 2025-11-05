package com.grupo2.ashley.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.grupo2.ashley.home.HomeScreen
import com.grupo2.ashley.home.HomeViewModel
import com.grupo2.ashley.map.MapScreen
import com.grupo2.ashley.map.UbicacionViewModel
import com.grupo2.ashley.screens.AnunciosScreen
import com.grupo2.ashley.screens.ChatsScreen
import com.grupo2.ashley.screens.CuentaScreen
import com.grupo2.ashley.screens.VenderScreen
import com.grupo2.ashley.ui.theme.AnimationConstants

object Routes {
    const val HOME = "home"
    const val CHATS = "chats"
    const val VENDER = "vender"
    const val ANUNCIOS = "anuncios"
    const val CUENTA = "cuenta"
    const val SELECCIONAR_UBICACION = "seleccionar_ubicacion"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    ubicacionViewModel: UbicacionViewModel,
    innerPadding: PaddingValues,
    navigationItems: List<Triple<String, Any, String>>
) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination?.route

    var previousRouteIndex by remember { mutableStateOf(0) }
    val currentRouteIndex =
        navigationItems.indexOfFirst { it.third == currentDestination }.takeIf { it >= 0 } ?: 0

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier,
        enterTransition = {
            val initialIndex =
                navigationItems.indexOfFirst { it.third == initialState.destination.route }
                    .takeIf { it >= 0 } ?: 0
            val targetIndex =
                navigationItems.indexOfFirst { it.third == targetState.destination.route }
                    .takeIf { it >= 0 } ?: 0
            val goingForward = targetIndex > initialIndex

            slideInHorizontally(
                animationSpec = tween(AnimationConstants.FLUID_DURATION),
                initialOffsetX = { fullWidth -> if (goingForward) fullWidth else -fullWidth }
            ) + fadeIn(
                animationSpec = tween(AnimationConstants.FLUID_DURATION)
            )
        },
        exitTransition = {
            val initialIndex =
                navigationItems.indexOfFirst { it.third == initialState.destination.route }
                    .takeIf { it >= 0 } ?: 0
            val targetIndex =
                navigationItems.indexOfFirst { it.third == targetState.destination.route }
                    .takeIf { it >= 0 } ?: 0
            val goingForward = targetIndex > initialIndex

            slideOutHorizontally(
                animationSpec = tween(AnimationConstants.FLUID_DURATION),
                targetOffsetX = { fullWidth -> if (goingForward) -fullWidth else fullWidth }
            ) + fadeOut(
                animationSpec = tween(AnimationConstants.FLUID_DURATION)
            )
        }) {
        composable(Routes.HOME) {
            previousRouteIndex = currentRouteIndex
            HomeScreen(
                viewModel = homeViewModel,
                ubicacionViewModel = ubicacionViewModel,
                onLocationClick = { navController.navigate(Routes.SELECCIONAR_UBICACION) },
                innerPadding = innerPadding
            )
        }

        composable(Routes.CHATS) {
            previousRouteIndex = currentRouteIndex
            ChatsScreen(innerPadding = innerPadding)
        }

        composable(Routes.VENDER) {
            previousRouteIndex = currentRouteIndex
            VenderScreen(
                navController = navController,
                viewModel = ubicacionViewModel,
                innerPadding = innerPadding
            )
        }

        composable(Routes.ANUNCIOS) {
            previousRouteIndex = currentRouteIndex
            AnunciosScreen(innerPadding = innerPadding)
        }

        composable(Routes.CUENTA) {
            previousRouteIndex = currentRouteIndex
            CuentaScreen(innerPadding = innerPadding)
        }

        composable(Routes.SELECCIONAR_UBICACION) {
            MapScreen(viewModel = ubicacionViewModel)
        }
    }
}
