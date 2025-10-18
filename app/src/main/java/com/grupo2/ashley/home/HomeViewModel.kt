package com.grupo2.ashley.home

import androidx.lifecycle.ViewModel
import com.grupo2.ashley.home.data.CategoryData
import com.grupo2.ashley.home.data.ProductRepository
import com.grupo2.ashley.home.models.Category
import com.grupo2.ashley.home.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {

    private val productRepository = ProductRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _categories = MutableStateFlow(CategoryData.categories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _products = MutableStateFlow(productRepository.getAllProducts())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

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
    }

    private fun filterProducts() {
        _products.value = productRepository.filterProducts(
            _selectedCategory.value,
            _searchQuery.value
        )
    }
}
