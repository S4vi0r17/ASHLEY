package com.grupo2.ashley.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grupo2.ashley.home.components.*
import com.grupo2.ashley.map.UbicacionViewModel
import com.grupo2.ashley.R
import com.grupo2.ashley.ui.components.GradientButton
import com.grupo2.ashley.ui.components.GradientIconButton
import com.grupo2.ashley.ui.theme.AppGradients
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    ubicacionViewModel: UbicacionViewModel,
    onLocationClick: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val direccion by ubicacionViewModel.direccionSeleccionada.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var favoritesSelected by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(modifier = Modifier.weight(1f)) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        onClearClick = { viewModel.clearSearch() }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                GradientIconButton(
                    onClick = {
                        viewModel.favoriteFilter()
                        favoritesSelected = !favoritesSelected
                    },
                    modifier = Modifier.size(48.dp),
                    icon = if (favoritesSelected) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    gradient = AppGradients.ErrorGradient,
                    contentDescription = stringResource(R.string.favorito)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.categorias),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            CategoriesRow(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategoryClick = { viewModel.onCategorySelected(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refreshProducts() },
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && products.isEmpty() -> {
                        // Mostrar indicador de carga solo si no hay productos cargados aún
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(stringResource(R.string.cargando_productos))
                            }
                        }
                    }
                    error != null -> {
                        // Mostrar mensaje de error
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = error ?: stringResource(R.string.error_desconocido),
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.refreshProducts() }) {
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
                    products.isEmpty() -> {
                        // Mostrar mensaje cuando no hay productos
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = if (searchQuery.isNotEmpty() || selectedCategory != "all") {
                                        stringResource(R.string.no_se_encontraron_productos)
                                    } else {
                                        stringResource(R.string.aun_no_hay_productos)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.se_el_primero_en_publicar),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        // Mostrar grid de productos
                        ProductsGrid(
                            products = products,
                            onFavoriteClick = { viewModel.toggleFavorite(it) },
                            onProductClick = onProductClick,
                            modifier = Modifier.fillMaxSize(),
                            bottomPadding = innerPadding.calculateBottomPadding() + 16.dp
                        )
                    }
                }
            }
        }
    }
}
