package com.grupo2.ashley.anuncios.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.grupo2.ashley.product.models.Product

@Composable
fun AnuncioGrid(
    products: List<Product>,
    onProductClick: (String) -> Unit,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(products) { product ->
            AnuncioCard(
                product = product,
                onClick = { onProductClick(product.productId) },
                navController = navController
            )
        }
    }
}