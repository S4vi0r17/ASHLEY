package com.grupo2.ashley.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grupo2.ashley.home.components.*
import com.grupo2.ashley.map.UbicacionViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    ubicacionViewModel: UbicacionViewModel,
    onLocationClick: () -> Unit = {},
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val direccion by ubicacionViewModel.direccionSeleccionada.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
    ) {
        LocationSelector(
            location = direccion,
            onClick = onLocationClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        SearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onClearClick = { viewModel.clearSearch() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Categorías",
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

        Box(modifier = Modifier.weight(1f)) {
            ProductsGrid(
                products = products,
                onFavoriteClick = { viewModel.toggleFavorite(it) },
                onProductClick = { /* TODO: Navegar a detalle */ },
                modifier = Modifier.fillMaxSize(),
                bottomPadding = innerPadding.calculateBottomPadding()
            )
        }
    }
}
