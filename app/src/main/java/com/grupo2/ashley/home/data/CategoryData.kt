package com.grupo2.ashley.home.data

import com.grupo2.ashley.home.models.Category
import com.grupo2.ashley.home.models.CategoryIcon

object CategoryData {
    val categories = listOf(
        Category("all", "Todos", CategoryIcon.ALL),
        Category("electronics", "Electrónica", CategoryIcon.ELECTRONICS),
        Category("fashion", "Moda", CategoryIcon.FASHION),
        Category("home", "Hogar", CategoryIcon.HOME),
        Category("sports", "Deportes", CategoryIcon.SPORTS),
        Category("books", "Libros", CategoryIcon.BOOKS),
        Category("toys", "Juguetes", CategoryIcon.TOYS),
        Category("vehicles", "Vehículos", CategoryIcon.VEHICLES),
        Category("others", "Otros", CategoryIcon.OTHERS)
    )
}
