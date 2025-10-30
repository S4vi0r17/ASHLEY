package com.grupo2.ashley.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grupo2.ashley.home.models.Product

@Composable
fun ProductsGrid(
    products: List<Product>,
    onFavoriteClick: (String) -> Unit,
    onProductClick: (String) -> Unit,
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
            ProductoCard(
                product = product,
                onFavoriteClick = { onFavoriteClick(product.id) },
                onClick = { onProductClick(product.id) })
        }
    }
}
