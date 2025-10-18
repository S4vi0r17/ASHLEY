package com.grupo2.ashley.home.data

import com.grupo2.ashley.home.models.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProductRepository {

    private val allProducts = listOf(
        Product(
            id = "1",
            title = "Nike Air Max 270",
            description = "Zapatillas deportivas nuevas, talla 42, color negro con detalles rojos",
            price = 350.00,
            location = "Breña, Lima",
            imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400",
            category = "shoes"
        ), Product(
            id = "2",
            title = "Adidas Ultraboost",
            description = "Zapatillas running prácticamente nuevas, muy cómodas, talla 40",
            price = 420.00,
            location = "Miraflores, Lima",
            imageUrl = "https://images.unsplash.com/photo-1608231387042-66d1773070a5?w=400",
            category = "shoes"
        ), Product(
            id = "3",
            title = "Toyota Yaris 2018",
            description = "Sedan automático, 45,000 km, aire acondicionado, impecable estado",
            price = 45000.00,
            location = "San Isidro, Lima",
            imageUrl = "https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb?w=400",
            category = "vehicles"
        ), Product(
            id = "4",
            title = "Honda CRV 2020",
            description = "SUV en excelente estado, único dueño, revisiones al día",
            price = 68000.00,
            location = "Surco, Lima",
            imageUrl = "https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?w=400",
            category = "vehicles"
        ), Product(
            id = "5",
            title = "iPhone 13 Pro Max",
            description = "256GB, color azul sierra, batería al 95%, con accesorios originales",
            price = 3200.00,
            location = "San Miguel, Lima",
            imageUrl = "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?w=400",
            category = "mobile"
        ), Product(
            id = "6",
            title = "Samsung Galaxy S23",
            description = "128GB, negro, como nuevo, 3 meses de uso, con caja y cargador",
            price = 2400.00,
            location = "Jesús María, Lima",
            imageUrl = "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=400",
            category = "mobile"
        ), Product(
            id = "7",
            title = "Converse All Star",
            description = "Zapatillas clásicas negras altas, talla 39, originales",
            price = 180.00,
            location = "Breña, Lima",
            imageUrl = "https://images.unsplash.com/photo-1607522370275-f14206abe5d3?w=400",
            category = "shoes"
        ), Product(
            id = "8",
            title = "Puma RS-X",
            description = "Zapatillas urbanas blancas con azul, talla 41, edición limitada",
            price = 290.00,
            location = "Lince, Lima",
            imageUrl = "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?w=400",
            category = "shoes"
        ), Product(
            id = "9",
            title = "Hyundai Tucson 2019",
            description = "SUV full equipo, 38,000 km, transmisión automática, color plata",
            price = 52000.00,
            location = "La Molina, Lima",
            imageUrl = "https://images.unsplash.com/photo-1617469767053-d3b523a0b982?w=400",
            category = "vehicles"
        ), Product(
            id = "10",
            title = "Suzuki Swift 2020",
            description = "Hatchback mecánico, económico, 35,000 km, color rojo",
            price = 38000.00,
            location = "Ate, Lima",
            imageUrl = "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=400",
            category = "vehicles"
        ), Product(
            id = "11",
            title = "Xiaomi Redmi Note 12",
            description = "128GB, azul, excelente cámara, batería de larga duración",
            price = 850.00,
            location = "Los Olivos, Lima",
            imageUrl = "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=400",
            category = "mobile"
        ), Product(
            id = "12",
            title = "Motorola Edge 30",
            description = "256GB, gris, pantalla OLED, carga rápida, impecable",
            price = 1200.00,
            location = "San Juan de Lurigancho, Lima",
            imageUrl = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400",
            category = "mobile"
        )
    )

    private val _products = MutableStateFlow<List<Product>>(allProducts)
    val products: Flow<List<Product>> = _products.asStateFlow()

    fun getAllProducts(): List<Product> = allProducts

    fun getProductsByCategory(categoryId: String): List<Product> {
        return if (categoryId == "all") {
            allProducts
        } else {
            allProducts.filter { it.category == categoryId }
        }
    }

    fun searchProducts(query: String): List<Product> {
        return allProducts.filter { product ->
            product.title.contains(query, ignoreCase = true) || product.description.contains(
                query,
                ignoreCase = true
            )
        }
    }

    fun filterProducts(categoryId: String, searchQuery: String): List<Product> {
        var filtered = allProducts

        // Filtrar por categoría
        if (categoryId != "all") {
            filtered = filtered.filter { it.category == categoryId }
        }

        // Filtrar por búsqueda
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(
                    searchQuery,
                    ignoreCase = true
                )
            }
        }

        return filtered
    }

    fun toggleFavorite(productId: String, currentProducts: List<Product>): List<Product> {
        return currentProducts.map { product ->
            if (product.id == productId) {
                product.copy(isFavorite = !product.isFavorite)
            } else {
                product
            }
        }
    }
}
