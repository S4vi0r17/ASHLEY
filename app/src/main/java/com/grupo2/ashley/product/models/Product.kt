package com.grupo2.ashley.product.models

import com.google.firebase.firestore.PropertyName

data class Product(
    val productId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val title: String = "",
    val brand: String = "",
    val category: String = "",
    val condition: ProductCondition = ProductCondition.NUEVO,
    val price: Double = 0.0,
    val description: String = "",
    val images: List<String> = emptyList(), // URLs de las imágenes en Firebase Storage
    // Ubicación de entrega
    val deliveryLocationName: String = "",
    val deliveryAddress: String = "",
    val deliveryLatitude: Double = 0.0,
    val deliveryLongitude: Double = 0.0,
    @PropertyName("active")
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Campos para tracking y estadísticas
    val views: Int = 0,
    val favorites: Int = 0,
    val messagesReceived: Int = 0
)

enum class ProductCondition(val displayName: String) {
    NUEVO("Nuevo"),
    COMO_NUEVO("Como nuevo"),
    BUEN_ESTADO("Buen estado"),
    USADO("Usado"),
    PARA_REPARAR("Para reparar")
}

data class ProductCategory(
    val id: String,
    val name: String
) {
    companion object {
        val categories = listOf(
            ProductCategory("electronics", "Electrónica"),
            ProductCategory("fashion", "Moda y Accesorios"),
            ProductCategory("home", "Hogar y Jardín"),
            ProductCategory("sports", "Deportes"),
            ProductCategory("books", "Libros y Música"),
            ProductCategory("toys", "Juguetes"),
            ProductCategory("vehicles", "Vehículos"),
            ProductCategory("others", "Otros")
        )
    }
}

data class ProductUploadState(
    val isLoading: Boolean = false,
    val isUploadingImages: Boolean = false,
    val uploadProgress: Float = 0f,
    val error: String? = null,
    val success: Boolean = false,
    val productId: String? = null
)
