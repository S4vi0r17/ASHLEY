package com.grupo2.ashley.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grupo2.ashley.home.models.Category
import com.grupo2.ashley.home.models.CategoryIcon
import com.grupo2.ashley.home.models.Product
import com.grupo2.ashley.map.SeleccionarUbicacionViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    ubicacionViewModel: SeleccionarUbicacionViewModel,
    onLocationClick: () -> Unit = {},
    innerPadding: PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)

) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val location by viewModel.location.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val direccion by ubicacionViewModel.direccionSeleccionada.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
    ) {
        // Selector de ubicación
        LocationSelector(
            location = direccion,
            onClick = onLocationClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Buscador
        SearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onClearClick = { viewModel.clearSearch() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Categorías
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
        
        // Lista de productos
        ProductsGrid(
            products = products,
            onFavoriteClick = { viewModel.toggleFavorite(it) },
            onProductClick = { /* TODO: Navegar a detalle */ }
        )
    }
}

@Composable
fun LocationSelector(
    location: String,
    onClick: () -> Unit
) {
    val mostrarTextoPorDefecto = location.isBlank() || location == "Sin dirección seleccionada"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Ubicación",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (mostrarTextoPorDefecto) "Elegir otra ubicación" else location,
                fontSize = 16.sp,
                color = if (mostrarTextoPorDefecto)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { 
                Text(
                    "Zapatillas...",
                    color = Color.Gray
                ) 
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = Color.Gray
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Botón para limpiar búsqueda
        IconButton(
            onClick = onClearClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Color(0xFFFF6B35),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Limpiar búsqueda",
                tint = Color.White
            )
        }
    }
}

@Composable
fun CategoriesRow(
    categories: List<Category>,
    selectedCategory: String,
    onCategoryClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { category ->
            CategoryItem(
                category = category,
                isSelected = selectedCategory == category.id,
                onClick = { onCategoryClick(category.id) }
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else 
                Color.Gray.copy(alpha = 0.1f),
            border = if (isSelected)
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else
                null
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = when (category.icon) {
                        CategoryIcon.ALL -> Icons.Default.Apps
                        CategoryIcon.SHOES -> Icons.Default.SportsSoccer
                        CategoryIcon.VEHICLES -> Icons.Default.DirectionsCar
                        CategoryIcon.MOBILE -> Icons.Default.PhoneAndroid
                    },
                    contentDescription = category.name,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = category.name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                Color.Gray
        )
    }
}

@Composable
fun ProductsGrid(
    products: List<Product>,
    onFavoriteClick: (String) -> Unit,
    onProductClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products) { product ->
            ProductCard(
                product = product,
                onFavoriteClick = { onFavoriteClick(product.id) },
                onClick = { onProductClick(product.id) }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Imagen del producto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                // Imagen del producto o placeholder
                if (product.imageUrl != null) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Gray.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                
                // Botón de favorito
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.9f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) 
                            Icons.Default.Favorite 
                        else 
                            Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (product.isFavorite) 
                            Color.Red 
                        else 
                            Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Título
            Text(
                text = product.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Descripción
            Text(
                text = product.description,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Ubicación
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = product.location,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Precio
            Text(
                text = "S/. ${String.format("%.2f", product.price)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
