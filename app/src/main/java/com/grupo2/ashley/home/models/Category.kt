package com.grupo2.ashley.home.models

data class Category(
    val id: String, val name: String, val icon: CategoryIcon
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
