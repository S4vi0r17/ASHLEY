package com.grupo2.ashley.productdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo2.ashley.home.models.Product
import com.grupo2.ashley.profile.data.ProfileRepository
import com.grupo2.ashley.profile.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _sellerProfile = MutableStateFlow<UserProfile?>(null)
    val sellerProfile: StateFlow<UserProfile?> = _sellerProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setProduct(product: Product) {
        _product.value = product
        loadSellerProfile(product.userId)
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

    fun clearError() {
        _error.value = null
    }
}
