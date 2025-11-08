package com.grupo2.ashley.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.grupo2.ashley.home.HomeScreen
import com.grupo2.ashley.home.HomeViewModel
import com.grupo2.ashley.map.MapScreen
import com.grupo2.ashley.map.UbicacionViewModel
import com.grupo2.ashley.product.ProductViewModel
import com.grupo2.ashley.productdetail.ProductDetailScreen
import com.grupo2.ashley.productdetail.ProductDetailViewModel
import com.grupo2.ashley.productdetail.ProductMapScreen
import com.grupo2.ashley.dashboard.DashboardScreen
import com.grupo2.ashley.screens.AnunciosScreen
import com.grupo2.ashley.screens.ChatsScreen
import com.grupo2.ashley.screens.CuentaScreen
import com.grupo2.ashley.screens.VenderScreen
import com.grupo2.ashley.ui.theme.AnimationConstants
import com.grupo2.ashley.utils.makePhoneCall
import com.grupo2.ashley.utils.openMapRoute

object Routes {
    const val HOME = "home"
    const val CHATS = "chats"
    const val VENDER = "vender"
    const val ANUNCIOS = "anuncios"
    const val CUENTA = "cuenta"
    const val SELECCIONAR_UBICACION = "seleccionar_ubicacion"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val PRODUCT_MAP = "product_map/{productId}"
    const val DASHBOARD = "dashboard"
    
    fun productDetail(productId: String) = "product_detail/$productId"
    fun productMap(productId: String) = "product_map/$productId"
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
                onProductClick = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                },
                innerPadding = innerPadding
            )
        }

        composable(Routes.CHATS) {
            previousRouteIndex = currentRouteIndex
            ChatsScreen(innerPadding = innerPadding)
        }

        composable(Routes.VENDER) {
            previousRouteIndex = currentRouteIndex
            val productViewModel: ProductViewModel = viewModel()
            VenderScreen(
                navController = navController,
                viewModel = ubicacionViewModel,
                innerPadding = innerPadding,
                productViewModel = productViewModel,
                homeViewModel = homeViewModel
            )
        }

        composable(Routes.ANUNCIOS) {
            previousRouteIndex = currentRouteIndex
            AnunciosScreen(innerPadding = innerPadding)
        }

        composable(Routes.CUENTA) {
            previousRouteIndex = currentRouteIndex
            CuentaScreen(
                innerPadding = innerPadding,
                ubicacionViewModel = ubicacionViewModel,
                onNavigateToMap = {
                    navController.navigate(Routes.SELECCIONAR_UBICACION)
                },
                onNavigateToDashboard = {
                    navController.navigate(Routes.DASHBOARD)
                }
            )
        }

        composable(Routes.SELECCIONAR_UBICACION) {
            MapScreen(
                viewModel = ubicacionViewModel,
                onLocationConfirmed = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val context = LocalContext.current
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            val product = homeViewModel.getProductById(productId)
            
            if (product != null) {
                val productDetailViewModel: ProductDetailViewModel = viewModel()
                productDetailViewModel.setProduct(product)
                
                val sellerProfile by productDetailViewModel.sellerProfile.collectAsState()
                
                ProductDetailScreen(
                    product = product,
                    sellerProfile = sellerProfile,
                    onBackClick = { navController.popBackStack() },
                    onMapClick = {
                        navController.navigate(Routes.productMap(productId))
                    },
                    onCallClick = {
                        sellerProfile?.phoneNumber?.let { phoneNumber ->
                            makePhoneCall(context, phoneNumber)
                        }
                    },
                    onChatClick = {
                        // TODO: Implementar navegación al chat
                    }
                )
            } else {
                // Producto no encontrado, volver atrás
                navController.popBackStack()
            }
        }

        composable(
            route = Routes.PRODUCT_MAP,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            val product = homeViewModel.getProductById(productId)
            
            if (product != null) {
                ProductMapScreen(
                    product = product,
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                // Producto no encontrado, volver atrás
                navController.popBackStack()
            }
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
