package com.grupo2.ashley.product.models

import androidx.annotation.StringRes
import com.google.firebase.firestore.PropertyName
import com.grupo2.ashley.R

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
    val messagesReceived: Int = 0,
    val isFavorite: Boolean = false //Failsafe, actual favoritos no incluido
)

enum class ProductCondition(val displayName: String, @StringRes val labelResId: Int) {
    NUEVO("Nuevo", R.string.condicion_nuevo),
    COMO_NUEVO("Como nuevo", R.string.condicion_como_nuevo),
    BUEN_ESTADO("Buen estado", R.string.condicion_buen_estado),
    USADO("Usado", R.string.condicion_usado),
    PARA_REPARAR("Para reparar", R.string.condicion_para_reparar);

    companion object {
        fun fromdisplayName(value: String): ProductCondition? {
            return entries.find { it.displayName.equals(value, ignoreCase = true) }
        }
    }
}

data class ProductCategory(
    val id: String,
    @StringRes val labelResId: Int
) {
    companion object {
        val categories = listOf(
            ProductCategory("electronics", R.string.cat_electronics),
            ProductCategory("fashion", R.string.cat_fashion),
            ProductCategory("home", R.string.cat_home),
            ProductCategory("sports", R.string.cat_sports),
            ProductCategory("books", R.string.cat_books),
            ProductCategory("toys", R.string.cat_toys),
            ProductCategory("vehicles", R.string.cat_vehicles),
            ProductCategory("others", R.string.cat_others)
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

data class ProductDeletedState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)
