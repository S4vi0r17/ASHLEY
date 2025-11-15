package com.grupo2.ashley.anuncios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grupo2.ashley.anuncios.components.*
import com.grupo2.ashley.home.components.*
import com.grupo2.ashley.map.UbicacionViewModel
import com.grupo2.ashley.R
import com.grupo2.ashley.anuncios.AnunciosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnunciosScreen(
    viewModel: AnunciosViewModel,
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

            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                onClearClick = { viewModel.clearSearch() }
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
                                    text = stringResource(R.string.aqui_mostraran_tus_anuncios),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        // Mostrar grid de productos
                       AnuncioGrid(
                            products = products,
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