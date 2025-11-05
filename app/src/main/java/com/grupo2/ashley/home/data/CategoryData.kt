package com.grupo2.ashley.home.data

import com.grupo2.ashley.home.models.Category
import com.grupo2.ashley.home.models.CategoryIcon

object CategoryData {
    val categories = listOf(
        Category("all", "Todos", CategoryIcon.ALL),
        Category("shoes", "Zapatillas", CategoryIcon.SHOES),
        Category("vehicles", "Vehículos", CategoryIcon.VEHICLES),
        Category("mobile", "Móviles", CategoryIcon.MOBILE)
    )
}
