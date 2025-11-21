package com.grupo2.ashley.home.models

import androidx.annotation.StringRes

data class Category(
    val id: String, @StringRes val labelResId: Int, val icon: CategoryIcon
)

enum class CategoryIcon {
    ALL,           // Todos
    ELECTRONICS,   // Electrónica
    FASHION,       // Moda
    HOME,          // Hogar
    SPORTS,        // Deportes
    BOOKS,         // Libros
    TOYS,          // Juguetes
    VEHICLES,      // Vehículos
    OTHERS         // Otros
}
