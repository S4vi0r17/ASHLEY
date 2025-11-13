package com.grupo2.ashley.home.data

import com.grupo2.ashley.home.models.Category
import com.grupo2.ashley.home.models.CategoryIcon
import com.grupo2.ashley.R

object CategoryData {
    val categories = listOf(
        Category("all", R.string.cat_all, CategoryIcon.ALL),
        Category("electronics", R.string.cat_electronics, CategoryIcon.ELECTRONICS),
        Category("fashion", R.string.cat_fashion, CategoryIcon.FASHION),
        Category("home", R.string.cat_home, CategoryIcon.HOME),
        Category("sports", R.string.cat_sports, CategoryIcon.SPORTS),
        Category("books", R.string.cat_books, CategoryIcon.BOOKS),
        Category("toys", R.string.cat_toys, CategoryIcon.TOYS),
        Category("vehicles", R.string.cat_vehicles, CategoryIcon.VEHICLES),
        Category("others", R.string.cat_others, CategoryIcon.OTHERS)
    )
}
