package com.grupo2.ashley.navigation

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.grupo2.ashley.anuncios.AnunciosScreen
import com.grupo2.ashley.anuncios.AnunciosViewModel
import com.grupo2.ashley.anuncios.components.ModificarAnuncioScreen
import com.grupo2.ashley.anuncios.components.ModificarAnuncioViewModel
import com.grupo2.ashley.screens.CuentaScreen
import com.grupo2.ashley.screens.VenderScreen
import com.grupo2.ashley.utils.makePhoneCall
import com.grupo2.ashley.chat.ChatListScreen
import com.grupo2.ashley.chat.ChatListViewModel
import com.grupo2.ashley.chat.ChatRealtimeScreen
import com.grupo2.ashley.chat.ParticipantInfoScreen
import com.grupo2.ashley.profile.ProfileViewModel
import androidx.hilt.navigation.compose.hiltViewModel

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
    const val MODIFICAR_ANUNCIOS = "editar_anuncio/{productId}"

    fun productDetail(productId: String) = "product_detail/$productId"
    fun productMap(productId: String) = "product_map/$productId"
    fun modificarAnuncio(productId: String) = "editar_anuncio/$productId"
}

// Funciones de animación para diferentes tipos de navegación
private object NavigationAnimations {
    private const val DURATION = 400
    private const val FAST_DURATION = 300

    // Animación horizontal para navegación entre tabs del bottom bar
    fun horizontalSlideEnter(goingForward: Boolean): EnterTransition {
        return slideInHorizontally(
            animationSpec = tween(DURATION),
            initialOffsetX = { fullWidth -> if (goingForward) fullWidth else -fullWidth }
        ) + fadeIn(animationSpec = tween(DURATION))
    }

    fun horizontalSlideExit(goingForward: Boolean): ExitTransition {
        return slideOutHorizontally(
            animationSpec = tween(DURATION),
            targetOffsetX = { fullWidth -> if (goingForward) -fullWidth else fullWidth }
        ) + fadeOut(animationSpec = tween(DURATION))
    }

    // Animación vertical para pantallas de detalle/modales (de abajo hacia arriba)
    fun verticalSlideEnter(): EnterTransition {
        return slideInVertically(
            animationSpec = tween(FAST_DURATION),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(animationSpec = tween(FAST_DURATION))
    }

    fun verticalSlideExit(): ExitTransition {
        return slideOutVertically(
            animationSpec = tween(FAST_DURATION),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(animationSpec = tween(FAST_DURATION))
    }

    // Animación para pantallas que se apilan (como detalles sobre otras pantallas)
    fun scaleEnter(): EnterTransition {
        return slideInVertically(
            animationSpec = tween(FAST_DURATION),
            initialOffsetY = { fullHeight -> fullHeight / 4 }
        ) + fadeIn(animationSpec = tween(FAST_DURATION))
    }

    fun scaleExit(): ExitTransition {
        return slideOutVertically(
            animationSpec = tween(FAST_DURATION),
            targetOffsetY = { fullHeight -> fullHeight / 4 }
        ) + fadeOut(animationSpec = tween(FAST_DURATION))
    }

    // Sin animación para ciertas pantallas
    fun noAnimation(): EnterTransition = EnterTransition.None
    fun noAnimationExit(): ExitTransition = ExitTransition.None
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    ubicacionViewModel: UbicacionViewModel,
    profileViewModel: ProfileViewModel,
    anunciosViewModel : AnunciosViewModel,
    innerPadding: PaddingValues,
    navigationItems: List<Triple<String, Any, String>>,
    productViewModel: ProductViewModel
) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination?.route

    var previousRouteIndex by remember { mutableIntStateOf(0) }
    val currentRouteIndex =
        navigationItems.indexOfFirst { it.third == currentDestination }.let { index ->
            if (index != -1) index else previousRouteIndex
        }

    val userProfile by profileViewModel.userProfile.collectAsState()
    val currentUserId = userProfile?.userId

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier
    ) {
        composable(
            route = Routes.HOME,
            enterTransition = {
                val targetIndex = navigationItems.indexOfFirst { it.third == Routes.HOME }
                val goingForward = targetIndex > previousRouteIndex
                NavigationAnimations.horizontalSlideEnter(goingForward)
            },
            exitTransition = {
                val initialIndex = navigationItems.indexOfFirst { it.third == Routes.HOME }
                val targetIndex = navigationItems.indexOfFirst { it.third == targetState.destination.route }
                val goingForward = targetIndex > initialIndex
                NavigationAnimations.horizontalSlideExit(goingForward)
            },
            popEnterTransition = {
                val targetIndex = navigationItems.indexOfFirst { it.third == Routes.HOME }
                val goingForward = targetIndex > previousRouteIndex
                NavigationAnimations.horizontalSlideEnter(goingForward)
            },
            popExitTransition = {
                NavigationAnimations.horizontalSlideExit(false)
            }
        ) {
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

        composable(
            route = Routes.CHATS,
            enterTransition = {
                val targetIndex = navigationItems.indexOfFirst { it.third == Routes.CHATS }
                val goingForward = targetIndex > previousRouteIndex
                NavigationAnimations.horizontalSlideEnter(goingForward)
            },
            exitTransition = {
                val initialIndex = navigationItems.indexOfFirst { it.third == Routes.CHATS }
                val targetIndex = navigationItems.indexOfFirst { it.third == targetState.destination.route }
                val goingForward = targetIndex > initialIndex
                NavigationAnimations.horizontalSlideExit(goingForward)
            },
            popEnterTransition = {
                val targetIndex = navigationItems.indexOfFirst { it.third == Routes.CHATS }
                val goingForward = targetIndex > previousRouteIndex
                NavigationAnimations.horizontalSlideEnter(goingForward)
            },
            popExitTransition = {
                NavigationAnimations.horizontalSlideExit(false)
            }
        ) {
            previousRouteIndex = currentRouteIndex
            ChatListScreen(
                navController = navController,
                currentUserId = currentUserId
            )
        }

        composable(
            route = "chat/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""

            ChatRealtimeScreen(
                conversationId = conversationId,
                currentUserId = currentUserId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToParticipantInfo = {
                    navController.navigate("participant_info/$conversationId")
                }
            )
        }

        composable(
            route = "participant_info/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""

            ParticipantInfoScreen(
                conversationId = conversationId,
                currentUserId = currentUserId,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Routes.VENDER,
            enterTransition = {
                val targetIndex = navigationItems.indexOfFirst { it.third == Routes.VENDER }
                val goingForward = targetIndex > previousRouteIndex
                NavigationAnimations.horizontalSlideEnter(goingForward)
            },
            exitTransition = {
                val initialIndex = navigationItems.indexOfFirst { it.third == Routes.VENDER }
                val targetIndex = navigationItems.indexOfFirst { it.third == targetState.destination.route }
                val goingForward = targetIndex > initialIndex
                NavigationAnimations.horizontalSlideExit(goingForward)
            },
            popEnterTransition = {
                val targetIndex = navigationItems.indexOfFirst { it.third == Routes.VENDER }
                val goingForward = targetIndex > previousRouteIndex
                NavigationAnimations.horizontalSlideEnter(goingForward)
            },
            popExitTransition = {
                NavigationAnimations.horizontalSlideExit(false)
            }
        ) {
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

        composable(
            route = Routes.ANUNCIOS,
            enterTransition = {
                val targetIndex = navigationItems.indexOfFirst { it.third == Routes.ANUNCIOS }
                val goingForward = targetIndex > previousRouteIndex
                NavigationAnimations.horizontalSlideEnter(goingForward)
            },
            exitTransition = {
                val initialIndex = navigationItems.indexOfFirst { it.third == Routes.ANUNCIOS }
                val targetIndex = navigationItems.indexOfFirst { it.third == targetState.destination.route }
                val goingForward = targetIndex > initialIndex
                NavigationAnimations.horizontalSlideExit(goingForward)
            },
            popEnterTransition = {
                val targetIndex = navigationItems.indexOfFirst { it.third == Routes.ANUNCIOS }
                val goingForward = targetIndex > previousRouteIndex
                NavigationAnimations.horizontalSlideEnter(goingForward)
            },
            popExitTransition = {
                NavigationAnimations.horizontalSlideExit(false)
            }
        ) {
            previousRouteIndex = currentRouteIndex
            AnunciosScreen(
                navController = navController,
                viewModel = anunciosViewModel,
                ubicacionViewModel = ubicacionViewModel,
                onProductClick = { productId ->
                    navController.navigate(Routes.modificarAnuncio(productId))
                },
                innerPadding = innerPadding)
        }

        composable(
            route = Routes.CUENTA,
            enterTransition = {
                val targetIndex = navigationItems.indexOfFirst { it.third == Routes.CUENTA }
                val goingForward = targetIndex > previousRouteIndex
                NavigationAnimations.horizontalSlideEnter(goingForward)
            },
            exitTransition = {
                val initialIndex = navigationItems.indexOfFirst { it.third == Routes.CUENTA }
                val targetIndex = navigationItems.indexOfFirst { it.third == targetState.destination.route }
                val goingForward = targetIndex > initialIndex
                NavigationAnimations.horizontalSlideExit(goingForward)
            },
            popEnterTransition = {
                val targetIndex = navigationItems.indexOfFirst { it.third == Routes.CUENTA }
                val goingForward = targetIndex > previousRouteIndex
                NavigationAnimations.horizontalSlideEnter(goingForward)
            },
            popExitTransition = {
                NavigationAnimations.horizontalSlideExit(false)
            }
        ) {
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

        composable(
            route = Routes.SELECCIONAR_UBICACION,
            enterTransition = { NavigationAnimations.verticalSlideEnter() },
            exitTransition = { NavigationAnimations.noAnimationExit() },
            popEnterTransition = { NavigationAnimations.noAnimation() },
            popExitTransition = { NavigationAnimations.verticalSlideExit() }
        ) {
            MapScreen(
                viewModel = ubicacionViewModel,
                onLocationConfirmed = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.StringType }),
            enterTransition = { NavigationAnimations.verticalSlideEnter() },
            exitTransition = { NavigationAnimations.noAnimationExit() },
            popEnterTransition = { NavigationAnimations.noAnimation() },
            popExitTransition = { NavigationAnimations.verticalSlideExit() }
        ) { backStackEntry ->
            val context = LocalContext.current
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            val product = homeViewModel.getProductById(productId)

            if (product != null) {
                val productDetailViewModel: ProductDetailViewModel = viewModel()
                val chatListViewModel: ChatListViewModel = hiltViewModel()

                // Llamar a setProduct solo una vez cuando se carga la pantalla
                LaunchedEffect(productId) {
                    productDetailViewModel.setProduct(product)
                }

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
                        val sellerId = sellerProfile?.userId

                        if (currentUserId != null && sellerId != null) {
                            chatListViewModel.createConversation(currentUserId, sellerId) { conversationId ->
                                if (conversationId != null) {
                                    navController.navigate("chat/$conversationId") 
                                 }
                            }
                        }
                    },
                    bottomPadding = innerPadding.calculateBottomPadding()
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(
            route = Routes.PRODUCT_MAP,
            arguments = listOf(navArgument("productId") { type = NavType.StringType }),
            enterTransition = { NavigationAnimations.scaleEnter() },
            exitTransition = { NavigationAnimations.noAnimationExit() },
            popEnterTransition = { NavigationAnimations.noAnimation() },
            popExitTransition = { NavigationAnimations.scaleExit() }
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

        composable(
            route = Routes.DASHBOARD,
            enterTransition = { NavigationAnimations.verticalSlideEnter() },
            exitTransition = { NavigationAnimations.noAnimationExit() },
            popEnterTransition = { NavigationAnimations.noAnimation() },
            popExitTransition = { NavigationAnimations.verticalSlideExit() }
        ) {
            DashboardScreen(
                onBackClick = { navController.popBackStack() },
                bottomPadding = innerPadding.calculateBottomPadding()
            )
        }

        composable(
            route = Routes.MODIFICAR_ANUNCIOS,
            arguments = listOf(navArgument("productId") { type = NavType.StringType }),
            enterTransition = { NavigationAnimations.verticalSlideEnter() },
            exitTransition = { NavigationAnimations.noAnimationExit() },
            popEnterTransition = { NavigationAnimations.noAnimation() },
            popExitTransition = { NavigationAnimations.verticalSlideExit() }
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val product = productViewModel.getProductById(productId)

            if (product != null) {
                val modificarAnuncioViewModel: ModificarAnuncioViewModel = viewModel()

                // Llamar a setProduct solo una vez cuando se carga la pantalla
                LaunchedEffect(productId) {
                    modificarAnuncioViewModel.setProduct(product)
                }

                ModificarAnuncioScreen(
                    product = product,
                    onBackClick = { navController.popBackStack() },
                    ubicacionViewModel = ubicacionViewModel,
                    anunciosViewModel = anunciosViewModel,
                    innerPadding = innerPadding,
                    navController = navController
                )
            } else {
                Log.e("HOLA","ERROR")
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}
