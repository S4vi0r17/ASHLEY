package com.grupo2.ashley.productdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo2.ashley.favorites.FavoritesRepository
import com.grupo2.ashley.home.models.Product
import com.grupo2.ashley.profile.data.ProfileRepository
import com.grupo2.ashley.profile.models.UserProfile
import com.grupo2.ashley.tracking.ProductTrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val trackingRepository: ProductTrackingRepository = ProductTrackingRepository(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepository()
) : ViewModel() {

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _sellerProfile = MutableStateFlow<UserProfile?>(null)
    val sellerProfile: StateFlow<UserProfile?> = _sellerProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isTogglingFavorite = MutableStateFlow(false)
    val isTogglingFavorite: StateFlow<Boolean> = _isTogglingFavorite.asStateFlow()

    fun setProduct(product: Product) {
        _product.value = product
        loadSellerProfile(product.userId)
        checkIfFavorite(product.id)
        trackProductView(product.id)
    }

    private fun loadSellerProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d("ProductDetailViewModel", "Cargando perfil del vendedor: $userId")
                
                val result = profileRepository.getUserProfile(userId)
                
                result.fold(
                    onSuccess = { profile ->
                        _sellerProfile.value = profile
                        Log.d("ProductDetailViewModel", "Perfil del vendedor cargado: ${profile?.firstName}")
                    },
                    onFailure = { exception ->
                        val errorMsg = "Error al cargar perfil del vendedor: ${exception.message}"
                        Log.e("ProductDetailViewModel", errorMsg)
                        _error.value = errorMsg
                    }
                )
            } catch (e: Exception) {
                val errorMsg = "Error inesperado: ${e.message}"
                Log.e("ProductDetailViewModel", errorMsg, e)
                _error.value = errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun trackProductView(productId: String) {
        viewModelScope.launch {
            try {
                trackingRepository.trackProductView(productId)
                Log.d("ProductDetailViewModel", "Vista registrada para producto: $productId")
            } catch (e: Exception) {
                Log.e("ProductDetailViewModel", "Error al registrar vista: ${e.message}", e)
            }
        }
    }

    private fun checkIfFavorite(productId: String) {
        viewModelScope.launch {
            try {
                val result = favoritesRepository.isFavorite(productId)
                result.onSuccess { isFav ->
                    _isFavorite.value = isFav
                }
            } catch (e: Exception) {
                Log.e("ProductDetailViewModel", "Error al verificar favorito: ${e.message}", e)
            }
        }
    }

    fun toggleFavorite() {
        val currentProduct = _product.value ?: return
        
        viewModelScope.launch {
            _isTogglingFavorite.value = true
            try {
                val result = favoritesRepository.toggleFavorite(
                    productId = currentProduct.id,
                    productTitle = currentProduct.title,
                    productImage = currentProduct.imageUrl ?: "",
                    productPrice = currentProduct.price
                )
                
                result.onSuccess { newState ->
                    _isFavorite.value = newState
                    Log.d("ProductDetailViewModel", "Favorito actualizado: $newState")
                }
            } catch (e: Exception) {
                Log.e("ProductDetailViewModel", "Error al alternar favorito: ${e.message}", e)
                _error.value = "Error al actualizar favorito"
            } finally {
                _isTogglingFavorite.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
