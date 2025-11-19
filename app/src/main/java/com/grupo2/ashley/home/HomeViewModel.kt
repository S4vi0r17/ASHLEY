package com.grupo2.ashley.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.grupo2.ashley.home.data.CategoryData
import com.grupo2.ashley.product.data.ProductRepository
import com.grupo2.ashley.home.models.Category
import com.grupo2.ashley.product.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val productRepository = ProductRepository()
    private val auth = FirebaseAuth.getInstance()

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

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            productRepository.getAllProducts()
                .onSuccess { products ->
                    val userid = auth.currentUser?.uid
                    _allProducts.value = products.filter { it.userId != userid && it.isActive }
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

    fun toggleFavorite(productId: String) {
        _products.value = productRepository.toggleFavorite(productId, _products.value)
        _allProducts.value = productRepository.toggleFavorite(productId, _allProducts.value)
    }

    private fun filterProducts() {
        _products.value = productRepository.filterProducts(
            _allProducts.value,
            _selectedCategory.value,
            _searchQuery.value
        )
    }

    fun refreshProducts() {
        loadProducts()
    }

    fun getProductById(productId: String): Product? {
        return _allProducts.value.find { it.productId == productId }
    }
}
