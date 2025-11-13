package com.grupo2.ashley.favorites

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import com.grupo2.ashley.R

class FavoritesViewModel : ViewModel() {
    private val repository = FavoritesRepository()
    
    private val _favorites = MutableStateFlow<List<FavoriteProduct>>(emptyList())
    val favorites: StateFlow<List<FavoriteProduct>> = _favorites.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadFavorites()
    }
    
    fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getUserFavorites()
                .onSuccess { favs ->
                    _favorites.value = favs
                }
                .onFailure { e ->
                    _error.value = e.message
                }
            
            _isLoading.value = false
        }
    }
    
    fun removeFromFavorites(productId: String) {
        viewModelScope.launch {
            repository.removeFromFavorites(productId)
                .onSuccess {
                    loadFavorites()
                }
                .onFailure { e ->
                    _error.value = e.message
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBackClick: () -> Unit,
    onProductClick: (String) -> Unit,
    viewModel: FavoritesViewModel = viewModel()
) {
    val favorites by viewModel.favorites.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.favoritos),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (favorites.isNotEmpty()) {
                            Text(
                                stringResource(
                                    id = R.string.total_productos,
                                    favorites.size
                                ),
                                style = MaterialTheme.typography.bodySmall,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.error_favoritos),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadFavorites() }) {
                            Text(stringResource(R.string.reintentar))
                        }
                    }
                }
                
                favorites.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_favoritos),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.placeholder_favoritos),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favorites, key = { it.productId }) { favorite ->
                            FavoriteProductCard(
                                favorite = favorite,
                                onProductClick = { onProductClick(favorite.productId) },
                                onRemoveFavorite = { viewModel.removeFromFavorites(favorite.productId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteProductCard(
    favorite: FavoriteProduct,
    onProductClick: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProductClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagen del producto
            if (favorite.productImage.isNotEmpty()) {
                AsyncImage(
                    model = favorite.productImage,
                    contentDescription = favorite.productTitle,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            // Información
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = favorite.productTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = currencyFormat.format(favorite.productPrice),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Agregado ${formatTime(favorite.addedAt, context)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Botón de remover
            IconButton(
                onClick = { showDeleteDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.quitar_favoritos),
                    tint = Color(0xFFE91E63)
                )
            }
        }
    }
    
    // Diálogo de confirmación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.quitar_favoritos)) },
            text = { Text(stringResource(R.string.confirmar_quitar_favoritos)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveFavorite()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirmar_quitar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}

private fun formatTime(timestamp: Long, context: Context): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        minutes < 1 -> context.getString(R.string.momento)
        minutes < 60 -> context.getString(R.string.hace_minutos, minutes)
        hours < 24 -> context.getString(R.string.hace_horas, hours)
        days < 7 -> context.getString(R.string.hace_dias, days)
        else -> {
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
