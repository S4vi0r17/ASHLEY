package com.grupo2.ashley.dashboard.models

data class UserStats(
    val totalProductsPublished: Int = 0,
    val activeProducts: Int = 0,
    val inactiveProducts: Int = 0,
    val totalViews: Int = 0,
    val totalFavorites: Int = 0,
    val totalMessages: Int = 0,
    val categoriesUsed: Int = 0,
    val averagePrice: Double = 0.0,
    val mostViewedProduct: ProductSummary? = null,
    val mostFavoritedProduct: ProductSummary? = null,
    val recentProducts: List<ProductSummary> = emptyList(),
    val productsByCategory: Map<String, Int> = emptyMap(),
    val productsByCondition: Map<String, Int> = emptyMap(),
    val viewsLast7Days: List<DailyStats> = emptyList(),
    val memberSince: Long = 0L
)

data class ProductSummary(
    val productId: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val views: Int = 0,
    val favorites: Int = 0,
    val category: String = "",
    val createdAt: Long = 0L,
    val isActive: Boolean = true
)

data class DailyStats(
    val date: String = "", // formato: "2025-11-01"
    val views: Int = 0,
    val favorites: Int = 0,
    val messages: Int = 0
)

data class CategoryStats(
    val categoryName: String = "",
    val productCount: Int = 0,
    val totalViews: Int = 0,
    val totalFavorites: Int = 0,
    val averagePrice: Double = 0.0
)

data class DashboardState(
    val isLoading: Boolean = true,
    val stats: UserStats = UserStats(),
    val error: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
