package com.grupo2.ashley.home.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.grupo2.ashley.home.models.Product
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Obtiene todos los productos activos EXCEPTO los del usuario actual
     * Usa índice compuesto (isActive + createdAt) para mejor rendimiento
     */
    suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: ""
            android.util.Log.d("ProductRepository", "========== INICIO CARGA PRODUCTOS ==========")
            android.util.Log.d("ProductRepository", "Usuario actual: $currentUserId")
            
            // PRIMERO: Obtener TODOS los documentos sin filtro para ver qué hay
            android.util.Log.d("ProductRepository", "PASO 1: Obteniendo TODOS los documentos sin filtro...")
            val allSnapshot = firestore.collection("products")
                .get()
                .await()
            
            android.util.Log.d("ProductRepository", "Total documentos en colección: ${allSnapshot.documents.size}")
            
            // Mostrar los datos RAW de cada documento
            allSnapshot.documents.forEach { doc ->
                android.util.Log.d("ProductRepository", "═══════════════════════════════════════")
                android.util.Log.d("ProductRepository", "Documento ID: ${doc.id}")
                android.util.Log.d("ProductRepository", "Datos RAW completos:")
                doc.data?.forEach { (key, value) ->
                    android.util.Log.d("ProductRepository", "  $key = $value (${value?.javaClass?.simpleName})")
                }
                android.util.Log.d("ProductRepository", "═══════════════════════════════════════")
            }
            
            // PASO 2: Intentar consulta con filtro active
            android.util.Log.d("ProductRepository", "PASO 2: Intentando consulta CON filtro active=true...")
            val snapshot = try {
                firestore.collection("products")
                    .whereEqualTo("active", true)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (indexError: Exception) {
                android.util.Log.e("ProductRepository", "Error con índice, usando consulta simple", indexError)
                firestore.collection("products")
                    .whereEqualTo("active", true)
                    .get()
                    .await()
            }

            android.util.Log.d("ProductRepository", "Documentos obtenidos CON filtro active: ${snapshot.documents.size}")
            
            if (snapshot.documents.isEmpty()) {
                android.util.Log.w("ProductRepository", "¡No hay documentos con active=true!")
                android.util.Log.w("ProductRepository", "Usando TODOS los documentos (${allSnapshot.documents.size}) como fallback...")
                // Usar todos los documentos como fallback para debugging
                return procesarYFiltrarProductos(allSnapshot, currentUserId)
            }

            // Usar documentos filtrados
            procesarYFiltrarProductos(snapshot, currentUserId)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "ERROR FATAL al cargar productos: ${e.message}", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun procesarYFiltrarProductos(
        snapshot: com.google.firebase.firestore.QuerySnapshot,
        currentUserId: String
    ): Result<List<Product>> {
        return try {
            val allProducts = snapshot.documents.mapNotNull { doc ->
                try {
                    android.util.Log.d("ProductRepository", "--- Procesando documento: ${doc.id} ---")
                    
                    // Intentar leer campos directamente primero
                    val docData = doc.data
                    android.util.Log.d("ProductRepository", "Datos del documento: $docData")
                    
                    val firebaseProduct = doc.toObject(com.grupo2.ashley.product.models.Product::class.java)
                    
                    if (firebaseProduct == null) {
                        android.util.Log.e("ProductRepository", "Error: No se pudo convertir documento ${doc.id} a Product")
                        return@mapNotNull null
                    }
                    
                    android.util.Log.d("ProductRepository", "✓ Producto convertido: ${firebaseProduct.title}")
                    android.util.Log.d("ProductRepository", "  - UserId: ${firebaseProduct.userId}")
                    android.util.Log.d("ProductRepository", "  - IsActive: ${firebaseProduct.isActive}")
                    android.util.Log.d("ProductRepository", "  - Imágenes: ${firebaseProduct.images.size}")
                    
                    Product(
                        id = firebaseProduct.productId,
                        title = firebaseProduct.title,
                        description = firebaseProduct.description,
                        price = firebaseProduct.price,
                        location = firebaseProduct.deliveryAddress,
                        imageUrl = firebaseProduct.images.firstOrNull(),
                        category = firebaseProduct.category,
                        isFavorite = false,
                        brand = firebaseProduct.brand,
                        condition = firebaseProduct.condition.displayName,
                        allImages = firebaseProduct.images,
                        userId = firebaseProduct.userId,
                        userEmail = firebaseProduct.userEmail,
                        createdAt = firebaseProduct.createdAt,
                        deliveryLatitude = firebaseProduct.deliveryLatitude,
                        deliveryLongitude = firebaseProduct.deliveryLongitude
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ProductRepository", "ERROR al convertir documento ${doc.id}: ${e.message}", e)
                    null
                }
            }

            android.util.Log.d("ProductRepository", "Total productos convertidos: ${allProducts.size}")
            
            // Filtrar productos del usuario actual
            val filteredProducts = allProducts.filter { product ->
                val isNotCurrentUser = product.userId != currentUserId
                android.util.Log.d("ProductRepository", "Producto '${product.title}': userId=${product.userId}, isNotCurrentUser=$isNotCurrentUser")
                isNotCurrentUser
            }
            
            android.util.Log.d("ProductRepository", "Productos después de filtrar: ${filteredProducts.size}")
            android.util.Log.d("ProductRepository", "========== FIN CARGA PRODUCTOS ==========")

            // Ordenar por fecha si no se ordenó en la consulta
            val sortedProducts = filteredProducts.sortedByDescending { it.createdAt }

            Result.success(sortedProducts)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "Error en procesarYFiltrarProductos", e)
            Result.failure(e)
        }
    }

    /**
     * Filtra productos por categoría y búsqueda
     */
    fun filterProducts(
        products: List<Product>,
        categoryId: String,
        searchQuery: String
    ): List<Product> {
        var filtered = products

        // Filtrar por categoría
        if (categoryId != "all") {
            filtered = filtered.filter { it.category == categoryId }
        }

        // Filtrar por búsqueda
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.brand.contains(searchQuery, ignoreCase = true)
            }
        }

        return filtered
    }

    /**
     * Alterna el estado de favorito de un producto
     */
    fun toggleFavorite(productId: String, currentProducts: List<Product>): List<Product> {
        return currentProducts.map { product ->
            if (product.id == productId) {
                product.copy(isFavorite = !product.isFavorite)
            } else {
                product
            }
        }
    }

    suspend fun getAllProductsAnuncio(): Result<List<Product>> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: ""
            android.util.Log.d("ProductRepository", "========== INICIO CARGA PRODUCTOS ==========")
            android.util.Log.d("ProductRepository", "Usuario actual: $currentUserId")

            // PRIMERO: Obtener TODOS los documentos sin filtro para ver qué hay
            android.util.Log.d("ProductRepository", "PASO 1: Obteniendo TODOS los documentos sin filtro...")
            val allSnapshot = firestore.collection("products")
                .get()
                .await()

            android.util.Log.d("ProductRepository", "Total documentos en colección: ${allSnapshot.documents.size}")

            // Mostrar los datos RAW de cada documento
            allSnapshot.documents.forEach { doc ->
                android.util.Log.d("ProductRepository", "═══════════════════════════════════════")
                android.util.Log.d("ProductRepository", "Documento ID: ${doc.id}")
                android.util.Log.d("ProductRepository", "Datos RAW completos:")
                doc.data?.forEach { (key, value) ->
                    android.util.Log.d("ProductRepository", "  $key = $value (${value?.javaClass?.simpleName})")
                }
                android.util.Log.d("ProductRepository", "═══════════════════════════════════════")
            }

            // PASO 2: Intentar consulta con filtro active
            android.util.Log.d("ProductRepository", "PASO 2: Intentando consulta CON filtro active=true...")
            val snapshot = try {
                firestore.collection("products")
                    .whereEqualTo("active", true)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (indexError: Exception) {
                android.util.Log.e("ProductRepository", "Error con índice, usando consulta simple", indexError)
                firestore.collection("products")
                    .whereEqualTo("active", true)
                    .get()
                    .await()
            }

            android.util.Log.d("ProductRepository", "Documentos obtenidos CON filtro active: ${snapshot.documents.size}")

            if (snapshot.documents.isEmpty()) {
                android.util.Log.w("ProductRepository", "¡No hay documentos con active=true!")
                android.util.Log.w("ProductRepository", "Usando TODOS los documentos (${allSnapshot.documents.size}) como fallback...")
                // Usar todos los documentos como fallback para debugging
                return procesarYFiltrarProductos(allSnapshot, currentUserId)
            }

            // Usar documentos filtrados
            procesarYFiltrarProductosAnuncio(snapshot, currentUserId)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "ERROR FATAL al cargar productos: ${e.message}", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun procesarYFiltrarProductosAnuncio(
        snapshot: com.google.firebase.firestore.QuerySnapshot,
        currentUserId: String
    ): Result<List<Product>> {
        return try {
            val allProducts = snapshot.documents.mapNotNull { doc ->
                try {
                    android.util.Log.d("ProductRepository", "--- Procesando documento: ${doc.id} ---")

                    // Intentar leer campos directamente primero
                    val docData = doc.data
                    android.util.Log.d("ProductRepository", "Datos del documento: $docData")

                    val firebaseProduct = doc.toObject(com.grupo2.ashley.product.models.Product::class.java)

                    if (firebaseProduct == null) {
                        android.util.Log.e("ProductRepository", "Error: No se pudo convertir documento ${doc.id} a Product")
                        return@mapNotNull null
                    }

                    android.util.Log.d("ProductRepository", "✓ Producto convertido: ${firebaseProduct.title}")
                    android.util.Log.d("ProductRepository", "  - UserId: ${firebaseProduct.userId}")
                    android.util.Log.d("ProductRepository", "  - IsActive: ${firebaseProduct.isActive}")
                    android.util.Log.d("ProductRepository", "  - Imágenes: ${firebaseProduct.images.size}")

                    Product(
                        id = firebaseProduct.productId,
                        title = firebaseProduct.title,
                        description = firebaseProduct.description,
                        price = firebaseProduct.price,
                        location = firebaseProduct.deliveryAddress,
                        imageUrl = firebaseProduct.images.firstOrNull(),
                        category = firebaseProduct.category,
                        isFavorite = false,
                        brand = firebaseProduct.brand,
                        condition = firebaseProduct.condition.displayName,
                        allImages = firebaseProduct.images,
                        userId = firebaseProduct.userId,
                        userEmail = firebaseProduct.userEmail,
                        createdAt = firebaseProduct.createdAt,
                        deliveryLatitude = firebaseProduct.deliveryLatitude,
                        deliveryLongitude = firebaseProduct.deliveryLongitude
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ProductRepository", "ERROR al convertir documento ${doc.id}: ${e.message}", e)
                    null
                }
            }

            android.util.Log.d("ProductRepository", "Total productos convertidos: ${allProducts.size}")

            // Filtrar productos del usuario actual
            val filteredProducts = allProducts.filter { product ->
                val isNotCurrentUser = product.userId == currentUserId
                android.util.Log.d("ProductRepository", "Producto '${product.title}': userId=${product.userId}, isNotCurrentUser=$isNotCurrentUser")
                isNotCurrentUser
            }

            android.util.Log.d("ProductRepository", "Productos después de filtrar: ${filteredProducts.size}")
            android.util.Log.d("ProductRepository", "========== FIN CARGA PRODUCTOS ==========")

            // Ordenar por fecha si no se ordenó en la consulta
            val sortedProducts = filteredProducts.sortedByDescending { it.createdAt }

            Result.success(sortedProducts)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "Error en procesarYFiltrarProductos", e)
            Result.failure(e)
        }
    }
}
