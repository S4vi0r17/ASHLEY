package com.grupo2.ashley.product

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo2.ashley.product.data.ProductRepository
import com.grupo2.ashley.product.models.Product
import com.grupo2.ashley.product.models.ProductCondition
import com.grupo2.ashley.product.models.ProductUploadState
import com.grupo2.ashley.profile.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.grupo2.ashley.R
import kotlinx.coroutines.flow.collectLatest

class ProductViewModel : ViewModel() {
    private val productRepository = ProductRepository()
    private val profileRepository = ProfileRepository()

    // Estados del formulario
    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
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

    // Ubicación de entrega
    private val _deliveryLocationName = MutableStateFlow("")
    val deliveryLocationName: StateFlow<String> = _deliveryLocationName.asStateFlow()

    private val _deliveryAddress = MutableStateFlow("")
    val deliveryAddress: StateFlow<String> = _deliveryAddress.asStateFlow()

    private val _deliveryLatitude = MutableStateFlow(0.0)
    val deliveryLatitude: StateFlow<Double> = _deliveryLatitude.asStateFlow()

    private val _deliveryLongitude = MutableStateFlow(0.0)
    val deliveryLongitude: StateFlow<Double> = _deliveryLongitude.asStateFlow()

    private val _useDefaultLocation = MutableStateFlow(true)
    val useDefaultLocation: StateFlow<Boolean> = _useDefaultLocation.asStateFlow()

    // Estado de carga
    private val _uploadState = MutableStateFlow(ProductUploadState())
    val uploadState: StateFlow<ProductUploadState> = _uploadState.asStateFlow()

    // Callback para notificar cuando se publica un producto
    private val _onProductPublished = MutableStateFlow<Boolean>(false)
    val onProductPublished: StateFlow<Boolean> = _onProductPublished.asStateFlow()

    init {
        loadDefaultDeliveryLocation()
        loadProducts()
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

    fun addImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value + uri
    }

    fun removeImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value - uri
    }

    fun setSelectedImages(images: List<Uri>) {
        _selectedImages.value = images
    }

    fun updateDeliveryLocationName(value: String) {
        _deliveryLocationName.value = value
    }

    fun updateDeliveryAddress(value: String) {
        _deliveryAddress.value = value
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
        }
        // No limpiar la ubicación cuando se desactiva, solo cuando se seleccione una nueva
    }

    fun publishProduct(context: Context) {
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

                if (_deliveryLatitude.value == 0.0 || _deliveryLongitude.value == 0.0) {
                    _uploadState.value = ProductUploadState(error = "Debes seleccionar una ubicación de entrega")
                    return@launch
                }

                _uploadState.value = ProductUploadState(isLoading = true, isUploadingImages = true)

                // Subir imágenes
                val imageResult = productRepository.uploadProductImages(_selectedImages.value)
                if (imageResult.isFailure) {
                    _uploadState.value = ProductUploadState(
                        error = "Error al subir las imágenes: ${imageResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val imageUrls = imageResult.getOrNull() ?: emptyList()

                _uploadState.value = ProductUploadState(isLoading = true, uploadProgress = 0.5f)

                // Crear producto
                val product = Product(
                    title = _title.value,
                    brand = _brand.value,
                    category = _category.value,
                    condition = _condition.value,
                    price = _price.value.toDouble(),
                    description = _description.value,
                    images = imageUrls,
                    deliveryLocationName = _deliveryLocationName.value,
                    deliveryAddress = _deliveryAddress.value,
                    deliveryLatitude = _deliveryLatitude.value,
                    deliveryLongitude = _deliveryLongitude.value
                )

                val createResult = productRepository.createProduct(product)
                if (createResult.isFailure) {
                    _uploadState.value = ProductUploadState(
                        error = "Error al crear el producto: ${createResult.exceptionOrNull()?.message}"
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

                // Limpiar formulario
                clearForm()
            } catch (e: Exception) {
                _uploadState.value = ProductUploadState(error = "Error inesperado: ${e.message}")
            }
        }
    }

    private fun clearForm() {
        _title.value = ""
        _brand.value = ""
        _category.value = ""
        _condition.value = ProductCondition.NUEVO
        _price.value = ""
        _description.value = ""
        _selectedImages.value = emptyList()
        _useDefaultLocation.value = true
        loadDefaultDeliveryLocation()
    }

    fun resetUploadState() {
        _uploadState.value = ProductUploadState()
        _onProductPublished.value = false
    }

    fun loadProducts() {
        viewModelScope.launch {
            productRepository.observeAllProducts().collectLatest { productsList ->
                _allProducts.value = productsList
            }
        }
    }

    fun getProductById(productId: String): Product? {
        return _allProducts.value.find { it.productId == productId }
    }
}
