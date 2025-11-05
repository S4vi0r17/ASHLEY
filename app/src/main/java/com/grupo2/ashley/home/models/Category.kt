package com.grupo2.ashley.home.models

data class Category(
    val id: String, val name: String, val icon: CategoryIcon
)

enum class CategoryIcon {
    ALL, SHOES, VEHICLES, MOBILE
}
