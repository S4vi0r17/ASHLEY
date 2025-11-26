package com.grupo2.ashley.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.grupo2.ashley.home.data.CategoryData
import com.grupo2.ashley.product.data.ProductRepository
import com.grupo2.ashley.home.models.Category
import com.grupo2.ashley.product.models.Product
import com.grupo2.ashley.favorites.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class HomeViewModel : ViewModel() {

    private val productRepository = ProductRepository()
    private val favoritesRepository = FavoritesRepository()
    private val auth = FirebaseAuth.getInstance()
    private var currentFavoriteIds: Set<String> = emptySet()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _categories = MutableStateFlow(CategoryData.categories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites = _showOnlyFavorites.asStateFlow()


    init {
        observeProductsRealtime()
        observeFavoritesRealtime()
    }

    private fun observeFavoritesRealtime() {
        viewModelScope.launch {
            favoritesRepository.observeUserFavoriteIds().collectLatest { favoriteIds ->
                currentFavoriteIds = favoriteIds
                // Actualizar _allProducts y _products según los favoritos actuales
                _allProducts.value = _allProducts.value.map { product ->
                    product.copy(isFavorite = favoriteIds.contains(product.productId))
                }
                // Reaplicar filtro para mostrar cambios en pantalla
                filterProducts()
            }
        }
    }

    private fun observeProductsRealtime() {
        viewModelScope.launch {
            productRepository.observeAllProducts().collectLatest { productsList ->
                val userid = auth.currentUser?.uid
                val filteredProducts = productsList.filter { it.userId != userid && it.isActive }
                // aplicar favoritos conocidos
                _allProducts.value = filteredProducts.map { product ->
                    product.copy(isFavorite = currentFavoriteIds.contains(product.productId))
                }
                filterProducts()
            }
        }
    }

    // loadProducts is kept for manual refresh if needed
    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            productRepository.getAllProducts()
                .onSuccess { products ->
                    val userid = auth.currentUser?.uid
                    val filteredProducts = products.filter { it.userId != userid && it.isActive }
                    val favoriteIds = currentFavoriteIds
                    _allProducts.value = filteredProducts.map { product ->
                        product.copy(isFavorite = favoriteIds.contains(product.productId))
                    }
                    filterProducts()
                }
                .onFailure { exception ->
                    _error.value = "Error al cargar productos: ${exception.message}"
                    _allProducts.value = emptyList()
                    _products.value = emptyList()
                }

            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        filterProducts()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        filterProducts()
    }

    fun onCategorySelected(categoryId: String) {
        _selectedCategory.value = categoryId
        filterProducts()
    }

    fun favoriteFilter(){
        _showOnlyFavorites.value = !_showOnlyFavorites.value
        filterProducts()
    }

    fun toggleFavorite(productId: String, dashboardViewModel: com.grupo2.ashley.dashboard.DashboardViewModel? = null) {
        viewModelScope.launch {
            val product = _allProducts.value.find { it.productId == productId } ?: return@launch
            favoritesRepository.toggleFavorite(
                productId = productId,
                productTitle = product.title,
                productImage = product.images.firstOrNull() ?: "",
                productPrice = product.price
            ).onSuccess { isFavorite ->
                _products.value = _products.value.map {
                    if (it.productId == productId) it.copy(isFavorite = isFavorite)
                    else it
                }
                _allProducts.value = _allProducts.value.map {
                    if (it.productId == productId) it.copy(isFavorite = isFavorite)
                    else it
                }
                // Notificar al dashboard que cambió favoritos
                dashboardViewModel?.onFavoritesChanged()
            }
        }
    }

    private fun filterProducts() {
        var list = productRepository.filterProducts(
            _allProducts.value,
            _selectedCategory.value,
            _searchQuery.value
        )

        // Si el filtro de favoritos está activo → filtrar solo los favoritos
        if (_showOnlyFavorites.value) {
            list = list.filter { it.isFavorite }
        }

        _products.value = list
    }

    fun refreshProducts() {
        loadProducts()
    }

    fun getProductById(productId: String): Product? {
        return _allProducts.value.find { it.productId == productId }
    }
}
