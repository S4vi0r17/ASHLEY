package com.grupo2.ashley.dashboard

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.grupo2.ashley.dashboard.components.*
import com.grupo2.ashley.ui.theme.AnimationConstants
import com.grupo2.ashley.ui.theme.AppGradients
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.grupo2.ashley.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    viewModel: DashboardViewModel = viewModel()
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.mi_dashboard),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (!dashboardState.isLoading) {
                            Text(
                                text = stringResource(
                                    R.string.ultima_actualizacion,
                                    formatTime(dashboardState.lastUpdated, context)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.volver)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStats() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.actualizar)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                dashboardState.isLoading -> {
                    LoadingView()
                }
                dashboardState.error != null -> {
                    ErrorView(
                        error = dashboardState.error ?: stringResource(R.string.error_desconocido),
                        onRetry = { viewModel.refreshStats() }
                    )
                }
                else -> {
                    DashboardContent(
                        stats = dashboardState.stats,
                        scrollState = scrollState,
                        bottomPadding = bottomPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = stringResource(R.string.cargando_estadisticas),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorView(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.cargando_estadisticas),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reintentar))
            }
        }
    }
}

@Composable
private fun DashboardContent(
    stats: com.grupo2.ashley.dashboard.models.UserStats,
    scrollState: androidx.compose.foundation.ScrollState,
    bottomPadding: Dp = 0.dp
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mensaje de bienvenida
        WelcomeCard(stats = stats)

        // Estadísticas principales
        Text(
            text = stringResource(R.string.resumen_general),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Productos publicados
        StatCard(
            title = stringResource(R.string.productos_publicados),
            value = stats.totalProductsPublished.toString(),
            icon = Icons.Default.ShoppingBag,
            gradient = AppGradients.PrimaryGradient,
            subtitle = "${stats.activeProducts} activos • ${stats.inactiveProducts} inactivos"
        )

        // Grid de mini stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStatCard(
                title = stringResource(R.string.vistas_totales),
                value = stats.totalViews.toString(),
                icon = Icons.Default.Visibility,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            MiniStatCard(
                title = stringResource(R.string.favoritos),
                value = stats.totalFavorites.toString(),
                icon = Icons.Default.Favorite,
                color = Color(0xFFE91E63),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStatCard(
                title = stringResource(R.string.categorias),
                value = stats.categoriesUsed.toString(),
                icon = Icons.Default.Category,
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
            MiniStatCard(
                title = stringResource(R.string.precio_promedio),
                value = if (stats.averagePrice > 0) {
                    currencyFormat.format(stats.averagePrice)
                } else {
                    "S/ 0"
                },
                icon = Icons.Default.AttachMoney,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }

        // Productos destacados
        if (stats.mostViewedProduct != null || stats.mostFavoritedProduct != null) {
            Text(
                text = stringResource(R.string.productos_destacados),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            stats.mostViewedProduct?.let { product ->
                TopProductCard(
                    title = stringResource(R.string.mas_visto),
                    product = product,
                    icon = Icons.Default.Visibility,
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFF2196F3), Color(0xFF1976D2))
                    )
                )
            }

            stats.mostFavoritedProduct?.let { product ->
                TopProductCard(
                    title = stringResource(R.string.mas_favorito),
                    product = product,
                    icon = Icons.Default.Favorite,
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFFE91E63), Color(0xFFC2185B))
                    )
                )
            }
        }

        // Análisis por categoría
        if (stats.productsByCategory.isNotEmpty()) {
            Text(
                text = stringResource(R.string.analisis_por_categoria),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            VicoBarChart(
                categories = stats.productsByCategory,
                title = stringResource(R.string.distribucion_de_productos)
            )
        }

        // Productos por condición
        if (stats.productsByCondition.isNotEmpty()) {
            Text(
                text = stringResource(R.string.estado_de_productos),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            VicoPieChart(
                data = stats.productsByCondition,
                title = stringResource(R.string.productos_por_condicion)
            )
        }

        // Productos recientes
        if (stats.recentProducts.isNotEmpty()) {
            Text(
                text = stringResource(R.string.publicaciones_recientes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            stats.recentProducts.forEach { product ->
                RecentProductCard(product = product)
            }
        }

        // Gráfico de tendencias de engagement
        // COMENTADO: Deshabilitado temporalmente
        /*
        if (stats.viewsLast7Days.isNotEmpty()) {
            Text(
                text = "Tendencias de Engagement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Mostrar TODOS los días (incluso si tienen 0 vistas/favoritos)
            val viewsData = stats.viewsLast7Days.map { daily ->
                val label = daily.date.takeLast(5) // "MM-dd"
                android.util.Log.d("DashboardScreen", "Día: ${daily.date} -> Label: $label | Vistas: ${daily.views} | Favoritos: ${daily.favorites}")
                label to daily.views
            }
            val favoritesData = stats.viewsLast7Days.map { daily ->
                val label = daily.date.takeLast(5)
                label to daily.favorites
            }

            VicoMultiLineChart(
                viewsData = viewsData,
                favoritesData = favoritesData,
                title = "Últimos 7 Días"
            )
        }
        */

        // Gráfico de pie - Distribución de vistas por producto
        if (stats.recentProducts.isNotEmpty() && stats.totalViews > 0) {
            Text(
                text = stringResource(R.string.distribucion_de_vistas),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Preparar datos: Top 5 productos por vistas
            val productViewsData = stats.recentProducts
                .sortedByDescending { it.views }
                .take(5)
                .associate {
                    val truncatedTitle = if (it.title.length > 25) {
                        it.title.take(22) + "..."
                    } else {
                        it.title
                    }
                    truncatedTitle to it.views
                }

            VicoPieChart(
                data = productViewsData,
                title = stringResource(R.string.top_5_productos_mas_vistos)
            )
        }

        // Espaciado final para que el último elemento no quede tapado por la barra de navegación
        Spacer(modifier = Modifier.height(16.dp + bottomPadding))
    }
}

@Composable
private fun WelcomeCard(stats: com.grupo2.ashley.dashboard.models.UserStats) {
    val dateFormat = remember { SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES")) }
    val memberSinceDate = remember(stats.memberSince) {
        dateFormat.format(Date(stats.memberSince))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.bienvenido_a_tu_dashboard),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.miembro_desde_variable, memberSinceDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun TopProductCard(
    title: String,
    product: com.grupo2.ashley.dashboard.models.ProductSummary,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Imagen del producto
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.title,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Información del producto
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2
                    )

                    Text(
                        text = currencyFormat.format(product.price),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = product.views.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = product.favorites.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentProductCard(product: com.grupo2.ashley.dashboard.models.ProductSummary) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del producto
            if (product.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Información
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = currencyFormat.format(product.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateFormat.format(Date(product.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Estado
            Surface(
                color = if (product.isActive) {
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                } else {
                    Color(0xFFFF5722).copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (product.isActive) stringResource(R.string.activo) else stringResource(R.string.inactivo),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.isActive) Color(0xFF4CAF50) else Color(0xFFFF5722),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long, context: Context): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60000

    return when {
        minutes < 1 -> context.getString(R.string.momento)
        minutes < 60 -> context.getString(R.string.hace_minutos)
        else -> {
            val hours = minutes / 60
            if (hours < 24) context.getString(R.string.hace_horas) else context.getString(R.string.hace_dias)
        }
    }
}
