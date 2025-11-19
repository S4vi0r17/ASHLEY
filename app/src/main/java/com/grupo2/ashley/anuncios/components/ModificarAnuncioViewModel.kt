package com.grupo2.ashley.anuncios.components

import android.content.Context
import android.net.Uri
import com.grupo2.ashley.product.models.Product
import com.grupo2.ashley.profile.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.grupo2.ashley.R
import com.grupo2.ashley.product.models.ProductCondition
import com.grupo2.ashley.product.models.ProductDeletedState
import com.grupo2.ashley.product.models.ProductUploadState
import kotlinx.coroutines.launch

class ModificarAnuncioViewModel() : ViewModel() {
    val profileRepository = ProfileRepository()
    val productRepository = com.grupo2.ashley.product.data.ProductRepository()
    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _brand = MutableStateFlow("")
    val brand: StateFlow<String> = _brand.asStateFlow()

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _condition = MutableStateFlow(ProductCondition.NUEVO)
    val condition: StateFlow<ProductCondition> = _condition.asStateFlow()

    private val _price = MutableStateFlow("")
    val price: StateFlow<String> = _price.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()
    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    private val _deliveryLocationName = MutableStateFlow("")
    val deliveryLocationName: StateFlow<String> = _deliveryLocationName.asStateFlow()

    private val _deliveryAddress = MutableStateFlow("")
    val deliveryAddress: StateFlow<String> = _deliveryAddress.asStateFlow()

    private val _deliveryLatitude = MutableStateFlow(0.0)
    val deliveryLatitude: StateFlow<Double> = _deliveryLatitude.asStateFlow()

    private val _deliveryLongitude = MutableStateFlow(0.0)
    val deliveryLongitude: StateFlow<Double> = _deliveryLongitude.asStateFlow()

    private val _useDefaultLocation = MutableStateFlow(false)
    val useDefaultLocation: StateFlow<Boolean> = _useDefaultLocation.asStateFlow()

    private val _uploadState = MutableStateFlow(ProductUploadState())
    val uploadState: StateFlow<ProductUploadState> = _uploadState.asStateFlow()

    private val _deletedState = MutableStateFlow(ProductDeletedState())
    val deletedState: StateFlow<ProductDeletedState> = _deletedState.asStateFlow()

    private val _onProductPublished = MutableStateFlow<Boolean>(false)
    val onProductPublished: StateFlow<Boolean> = _onProductPublished.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var _localURIs = MutableStateFlow<List<Uri>>(emptyList())
    var localURIs: StateFlow<List<Uri>> = _localURIs.asStateFlow()

    private val _remoteURLs = MutableStateFlow<List<String>>(emptyList())
    var remoteURLs: StateFlow<List<String>> = _remoteURLs.asStateFlow()

    fun setProduct(product: Product) {
        _product.value = product
    }

    fun loadSelectedImages(images: List<String>) {
        _remoteURLs.value = images
        val oldimages = images.map{ it.toUri() }
        _selectedImages.value = oldimages
    }

    private fun loadDefaultDeliveryLocation() {
        viewModelScope.launch {
            profileRepository.getUserProfile().onSuccess { profile ->
                if (profile != null) {
                    _deliveryLocationName.value = profile.defaultPickupLocationName
                    _deliveryAddress.value = profile.fullAddress
                    _deliveryLatitude.value = profile.defaultPickupLatitude
                    _deliveryLongitude.value = profile.defaultPickupLongitude
                }
            }
        }
    }

    fun updateDeliveryLocation(latitude: Double, longitude: Double, address: String, locationName: String) {
        _deliveryLatitude.value = latitude
        _deliveryLongitude.value = longitude
        _deliveryAddress.value = address
        _deliveryLocationName.value = locationName
    }

    fun toggleUseDefaultLocation() {
        _useDefaultLocation.value = !_useDefaultLocation.value
        if (_useDefaultLocation.value) {
            loadDefaultDeliveryLocation()
        } else loadProductLocation(_product.value)
    }
    fun removeImage(image: Uri){
        _selectedImages.value = _selectedImages.value - image
        removeLocalURI(image)
    }

    fun addImage(images: List<Uri>){
        _selectedImages.value = _selectedImages.value + images
        _localURIs.value = images
    }

    fun addLocalURI(image : Uri){
        _localURIs.value = _localURIs.value + image
    }

    fun removeLocalURI(image: Uri){
        _localURIs.value = _localURIs.value - image
    }

    fun updateTitle(value: String) {
        _title.value = value
    }

    fun updateBrand(value: String) {
        _brand.value = value
    }

    fun updateCategory(value: String) {
        _category.value = value
    }

    fun updateCondition(value: ProductCondition) {
        _condition.value = value
    }

    fun updatePrice(value: String) {
        _price.value = value
    }

    fun updateDescription(value: String) {
        _description.value = value
    }

    fun loadProductValues(product: Product){
        _title.value = product.title
        _brand.value = product.brand
        _category.value = product.category
        _condition.value = product.condition
        _price.value = product.price.toString()
        _description.value = product.description
    }

    fun loadProductLocation(product: Product?){
        if(product != null) {
            _deliveryLocationName.value = product.deliveryLocationName
            _deliveryAddress.value = product.deliveryAddress
            _deliveryLatitude.value = product.deliveryLatitude
            _deliveryLongitude.value = product.deliveryLongitude
        } else loadDefaultDeliveryLocation()
    }

    fun modifyProduct(context: Context){
        viewModelScope.launch {
            try {
                // Validaciones
                if (_title.value.isBlank()) {
                    _uploadState.value = ProductUploadState(error = context.getString(R.string.el_titulo_es_requerido))
                    return@launch
                }

                if (_category.value.isBlank()) {
                    _uploadState.value = ProductUploadState(error = context.getString(R.string.la_categoria_es_requerida))
                    return@launch
                }

                if (_price.value.isBlank() || _price.value.toDoubleOrNull() == null) {
                    _uploadState.value =
                        ProductUploadState(error = context.getString(R.string.error_numero_invalido))
                    return@launch
                }

                if (_selectedImages.value.isEmpty()) {
                    _uploadState.value =
                        ProductUploadState(error = context.getString(R.string.debes_seleccionar_al_menos_una_imagen))
                    return@launch
                }

                _uploadState.value = ProductUploadState(isLoading = true, isUploadingImages = true)

                // Subir imágenes
                val imageResult = productRepository.uploadProductImages(_localURIs.value)
                if (imageResult.isFailure) {
                    _uploadState.value = ProductUploadState(
                        error = "Error al subir las imágenes: ${imageResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val imageUrls = imageResult.getOrNull() ?: emptyList()
                val allImages = _remoteURLs.value + imageUrls

                _uploadState.value = ProductUploadState(isLoading = true, uploadProgress = 0.5f)

                // Crear producto
                val product = Product(
                    productId = _product.value?.productId ?: "",
                    title = _title.value,
                    brand = _brand.value,
                    category = _category.value,
                    condition = _condition.value,
                    price = _price.value.toDouble(),
                    description = _description.value,
                    images = allImages,
                    deliveryLocationName = _deliveryLocationName.value,
                    deliveryAddress = _deliveryAddress.value,
                    deliveryLatitude = _deliveryLatitude.value,
                    deliveryLongitude = _deliveryLongitude.value
                )

                val createResult = productRepository.updateProduct(product)
                if (createResult.isFailure) {
                    _uploadState.value = ProductUploadState(
                        error = context.getString(
                            R.string.error_al_crear_el_producto,
                            createResult.exceptionOrNull()?.message
                        )
                    )
                    return@launch
                }

                val productId = createResult.getOrNull()
                _uploadState.value = ProductUploadState(
                    success = true,
                    uploadProgress = 1f,
                    productId = productId
                )

                // Notificar que se publicó un producto
                _onProductPublished.value = true
            } catch (e: Exception) {
                _uploadState.value = ProductUploadState(error = context.getString(R.string.error_inesperado,{e.message}))
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = ProductUploadState()
        _onProductPublished.value = false
    }

    fun deleteProductbyID(productId: String, context: Context){
        _deletedState.value = ProductDeletedState(isLoading = true)
        viewModelScope.launch {
            val result = productRepository.deleteProduct(productId)
            if (result.isFailure) {
                _deletedState.value = ProductDeletedState(
                    error = context.getString(
                        R.string.error_al_eliminar,
                        result.exceptionOrNull()?.message
                    )
                )
                return@launch
            }
            _deletedState.value = ProductDeletedState(isLoading = false, success = true)
        } 
    }

    fun resetDeletedState() {
        _deletedState.value = ProductDeletedState()
    }
}