# 📊 Dashboard Module - Documentación Completa

## 📑 Índice
1. [Descripción General](#descripción-general)
2. [Arquitectura del Módulo](#arquitectura-del-módulo)
3. [Librerías Utilizadas](#librerías-utilizadas)
4. [Estructura de Archivos](#estructura-de-archivos)
5. [Modelos de Datos](#modelos-de-datos)
6. [Repositorio y Fuente de Datos](#repositorio-y-fuente-de-datos)
7. [ViewModel](#viewmodel)
8. [Componentes UI](#componentes-ui)
9. [Gráficos y Visualizaciones](#gráficos-y-visualizaciones)
10. [Flujo de Datos](#flujo-de-datos)
11. [Integración con Firebase](#integración-con-firebase)
12. [Animaciones](#animaciones)
13. [Temas y Estilos](#temas-y-estilos)

---

## 📖 Descripción General

El **módulo Dashboard** es una pantalla de análisis y estadísticas que muestra métricas detalladas sobre los productos publicados por el usuario en la aplicación Ashley. Proporciona visualizaciones interactivas, gráficos y resúmenes de rendimiento.

### Funcionalidades Principales:
- ✅ Estadísticas generales de productos publicados
- ✅ Análisis de vistas y favoritos
- ✅ Productos destacados (más visto y más favorito)
- ✅ Distribución por categoría y condición
- ✅ Gráficos de tendencias (últimos 7 días)
- ✅ Productos recientes
- ✅ Actualización en tiempo real

---

## 🏗️ Arquitectura del Módulo

El módulo sigue la arquitectura **MVVM (Model-View-ViewModel)** con separación clara de responsabilidades:

```
dashboard/
├── DashboardScreen.kt          # Vista principal (UI)
├── DashboardViewModel.kt       # Lógica de presentación
├── components/
│   ├── DashboardComponents.kt  # Componentes reutilizables (cards, stats)
│   └── VicoCharts.kt          # Gráficos avanzados con Vico
├── data/
│   └── StatsRepository.kt     # Capa de datos (Firebase)
└── models/
    └── UserStats.kt           # Modelos de datos
```

### Capas:

1. **UI Layer (View)**: 
   - `DashboardScreen.kt`: Composable principal
   - `DashboardComponents.kt` y `VicoCharts.kt`: Componentes visuales

2. **Presentation Layer (ViewModel)**:
   - `DashboardViewModel.kt`: Manejo de estado y lógica de negocio

3. **Data Layer (Repository)**:
   - `StatsRepository.kt`: Acceso a Firebase Firestore

4. **Domain Layer (Models)**:
   - `UserStats.kt`: Entidades de dominio

---

## 📚 Librerías Utilizadas

### Dependencias Principales

#### 1. **Vico Charts** (Visualización de Datos)
```kotlin
// build.gradle.kts
implementation(libs.vico.compose)      // v2.x
implementation(libs.vico.compose.m3)   // Material 3 support
implementation(libs.vico.core)         // Core functionality
```
- **Propósito**: Gráficos profesionales e interactivos
- **Uso en Dashboard**:
  - `VicoBarChart`: Distribución por categorías
  - `VicoPieChart`: Productos por condición
  - `VicoLineChart`: Tendencias temporales
  - `VicoMultiLineChart`: Comparación vistas vs favoritos

#### 2. **Coil** (Carga de Imágenes)
```kotlin
implementation(libs.coil.compose)
```
- **Propósito**: Carga asíncrona de imágenes
- **Uso**: Visualización de imágenes de productos en cards

#### 3. **Firebase**
```kotlin
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.auth)
implementation(libs.firebase.firestore)
```
- **Firestore**: Base de datos para productos y estadísticas
- **Auth**: Autenticación del usuario actual

#### 4. **Jetpack Compose**
```kotlin
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.material3)
implementation(libs.androidx.material.icons.extended)
```
- **Material 3**: Sistema de diseño moderno
- **Icons**: Iconografía extendida para el UI

#### 5. **Kotlin Coroutines**
```kotlin
implementation(libs.coroutines.android)
```
- **Propósito**: Manejo asíncrono de datos

---

## 📁 Estructura de Archivos

### 1. `DashboardScreen.kt`
**Responsabilidad**: Pantalla principal del dashboard

**Composables principales**:
- `DashboardScreen()`: Scaffold con TopBar y manejo de estados
- `LoadingView()`: Indicador de carga
- `ErrorView()`: Pantalla de error con retry
- `DashboardContent()`: Contenido principal scrolleable
- `WelcomeCard()`: Card de bienvenida
- `TopProductCard()`: Card de producto destacado
- `RecentProductCard()`: Card de producto reciente

### 2. `DashboardViewModel.kt`
**Responsabilidad**: Lógica de presentación y estado

**Propiedades**:
```kotlin
private val _dashboardState = MutableStateFlow(DashboardState())
val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()
```

**Métodos**:
- `loadStats()`: Carga inicial de estadísticas
- `refreshStats()`: Recarga de datos

### 3. `components/DashboardComponents.kt`
**Componentes reutilizables**:

- `StatCard()`: Card grande con gradiente para estadística principal
  - Parámetros: título, valor, icono, gradiente, subtítulo opcional, tendencia
  
- `MiniStatCard()`: Card pequeña para grid de métricas
  - Parámetros: título, valor, icono, color
  
- `CategoryPieChart()`: Gráfico de barras simple para categorías
  - Usa animaciones personalizadas

- `SimpleLineChart()`: Gráfico de líneas básico
  - Fallback para datos temporales

### 4. `components/VicoCharts.kt`
**Gráficos avanzados con Vico**:

- `VicoLineChart()`: Gráfico de línea simple
- `VicoBarChart()`: Gráfico de barras con leyenda
- `VicoPieChart()`: Gráfico tipo pie (implementado como barras horizontales)
- `VicoMultiLineChart()`: Gráfico multi-línea para comparar métricas

### 5. `data/StatsRepository.kt`
**Repositorio de datos**:

**Métodos principales**:
```kotlin
suspend fun getUserStats(): Result<UserStats>
suspend fun getCategoryStats(): Result<List<CategoryStats>>
suspend fun getUserDailyStats(userId: String, days: Int): Result<List<DailyStats>>
```

### 6. `models/UserStats.kt`
**Modelos de datos**:

```kotlin
data class UserStats(...)
data class ProductSummary(...)
data class DailyStats(...)
data class CategoryStats(...)
data class DashboardState(...)
```

---

## 🗂️ Modelos de Datos

### UserStats
**Estadísticas principales del usuario**

```kotlin
data class UserStats(
    val totalProductsPublished: Int = 0,      // Total de productos
    val activeProducts: Int = 0,               // Productos activos
    val inactiveProducts: Int = 0,             // Productos inactivos
    val totalViews: Int = 0,                   // Vistas totales
    val totalFavorites: Int = 0,               // Favoritos totales
    val totalMessages: Int = 0,                // Mensajes (futuro)
    val categoriesUsed: Int = 0,               // Categorías únicas
    val averagePrice: Double = 0.0,            // Precio promedio
    val mostViewedProduct: ProductSummary?,    // Producto más visto
    val mostFavoritedProduct: ProductSummary?, // Producto más favorito
    val recentProducts: List<ProductSummary>,  // Últimos 5 productos
    val productsByCategory: Map<String, Int>,  // Distribución por categoría
    val productsByCondition: Map<String, Int>, // Distribución por condición
    val viewsLast7Days: List<DailyStats>,      // Estadísticas diarias
    val memberSince: Long = 0L                 // Fecha de registro
)
```

### ProductSummary
**Resumen de información de producto**

```kotlin
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
```

### DailyStats
**Estadísticas por día**

```kotlin
data class DailyStats(
    val date: String = "",        // formato: "2025-11-21"
    val views: Int = 0,
    val favorites: Int = 0,
    val messages: Int = 0
)
```

### CategoryStats
**Estadísticas por categoría**

```kotlin
data class CategoryStats(
    val categoryName: String = "",
    val productCount: Int = 0,
    val totalViews: Int = 0,
    val totalFavorites: Int = 0,
    val averagePrice: Double = 0.0
)
```

### DashboardState
**Estado UI del Dashboard**

```kotlin
data class DashboardState(
    val isLoading: Boolean = true,
    val stats: UserStats = UserStats(),
    val error: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
```

---

## 💾 Repositorio y Fuente de Datos

### StatsRepository

**Ubicación**: `dashboard/data/StatsRepository.kt`

#### Inicialización
```kotlin
class StatsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
}
```

#### Métodos Principales

##### 1. `getUserStats(): Result<UserStats>`
**Propósito**: Obtiene todas las estadísticas del usuario actual

**Proceso**:
1. Verifica autenticación del usuario
2. Consulta colección `products` filtrada por `userId`
3. Mapea documentos a objetos `Product`
4. Calcula estadísticas:
   - Cuenta productos activos/inactivos
   - Suma vistas y favoritos totales
   - Agrupa por categoría y condición
   - Calcula precio promedio
   - Identifica productos destacados
   - Ordena productos recientes
5. Obtiene fecha de registro del usuario desde colección `users`
6. Consulta estadísticas diarias (últimos 7 días)
7. Retorna `Result.success(stats)` o `Result.failure(exception)`

**Query Firebase**:
```kotlin
firestore.collection("products")
    .whereEqualTo("userId", userId)
    .get()
    .await()
```

##### 2. `getCategoryStats(): Result<List<CategoryStats>>`
**Propósito**: Obtiene estadísticas agrupadas por categoría

**Proceso**:
1. Consulta productos del usuario
2. Agrupa por categoría
3. Calcula métricas por categoría
4. Ordena por cantidad de productos descendente

##### 3. `getUserDailyStats(userId: String, days: Int): Result<List<DailyStats>>`
**Propósito**: Obtiene estadísticas diarias de los últimos N días

**Proceso**:
1. Inicializa mapa con últimos N días (valores en 0)
2. Para cada producto del usuario:
   - Consulta subcollection `product_stats`
   - Suma vistas, favoritos y mensajes por fecha
3. Retorna lista ordenada por fecha

**Estructura Firebase**:
```
products/{productId}/product_stats/{date}
    - date: "2025-11-21"
    - views: 5
    - favorites: 2
    - messages: 0
```

#### Helper Methods

##### `toProductSummary(product: Product): ProductSummary`
Convierte un `Product` completo a un `ProductSummary` para el dashboard.

##### `calculateLast7DaysStats(products: List<Product>): List<DailyStats>`
**⚠️ DEPRECATED**: Generaba datos mock. Reemplazado por `getUserDailyStats()`.

---

## 🎨 ViewModel

### DashboardViewModel

**Ubicación**: `dashboard/DashboardViewModel.kt`

#### Propiedades

```kotlin
private val repository = StatsRepository()

private val _dashboardState = MutableStateFlow(DashboardState())
val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()
```

#### Ciclo de Vida

```kotlin
init {
    loadStats()  // Carga automática al crear ViewModel
}
```

#### Métodos

##### `loadStats()`
```kotlin
fun loadStats() {
    viewModelScope.launch {
        // 1. Establecer estado loading
        _dashboardState.update { it.copy(isLoading = true, error = null) }
        
        // 2. Consultar repository
        repository.getUserStats()
            .onSuccess { stats ->
                // 3. Actualizar estado con datos
                _dashboardState.update { 
                    it.copy(
                        isLoading = false,
                        stats = stats,
                        error = null,
                        lastUpdated = System.currentTimeMillis()
                    ) 
                }
            }
            .onFailure { exception ->
                // 4. Manejar error
                _dashboardState.update { 
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Error desconocido"
                    ) 
                }
            }
    }
}
```

##### `refreshStats()`
```kotlin
fun refreshStats() {
    loadStats()  // Simplemente recarga
}
```

#### Manejo de Estados

El ViewModel usa `StateFlow` para emitir estados reactivos que la UI observa:

1. **Loading**: `isLoading = true`
2. **Success**: `stats` poblado, `error = null`
3. **Error**: `error` con mensaje, `isLoading = false`

---

## 🎯 Componentes UI

### DashboardScreen

**Estructura de la pantalla**:

```
Scaffold
├── TopAppBar
│   ├── NavigationIcon (back)
│   ├── Title + Last Updated
│   └── Actions (refresh)
└── Content
    ├── LoadingView (si isLoading)
    ├── ErrorView (si error)
    └── DashboardContent (si success)
```

#### TopAppBar Features:
- **Título**: "Mi Dashboard"
- **Subtítulo**: Última actualización formateada
- **Botón atrás**: Navega hacia atrás
- **Botón refresh**: Recarga estadísticas

#### Estados de la Pantalla:

##### 1. LoadingView
```kotlin
@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), 
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text("Cargando estadísticas...")
        }
    }
}
```

##### 2. ErrorView
```kotlin
@Composable
private fun ErrorView(error: String, onRetry: () -> Unit) {
    Box(...) {
        Column(...) {
            Icon(Icons.Default.ErrorOutline)
            Text("Error al cargar")
            Text(error)
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}
```

##### 3. DashboardContent
**Contenido principal scrolleable**:

```kotlin
@Composable
private fun DashboardContent(
    stats: UserStats,
    scrollState: ScrollState,
    bottomPadding: Dp = 0.dp
) {
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 1. Welcome Card
        WelcomeCard(stats)
        
        // 2. Resumen General
        Text("Resumen General")
        StatCard(totalProductsPublished)
        
        // 3. Grid de Mini Stats
        Row {
            MiniStatCard(totalViews)
            MiniStatCard(totalFavorites)
        }
        Row {
            MiniStatCard(categoriesUsed)
            MiniStatCard(averagePrice)
        }
        
        // 4. Productos Destacados
        Text("Productos Destacados")
        TopProductCard(mostViewedProduct)
        TopProductCard(mostFavoritedProduct)
        
        // 5. Análisis por Categoría
        Text("Análisis por Categoría")
        VicoBarChart(productsByCategory)
        
        // 6. Estado de Productos
        Text("Estado de Productos")
        VicoPieChart(productsByCondition)
        
        // 7. Publicaciones Recientes
        Text("Publicaciones Recientes")
        recentProducts.forEach { RecentProductCard(it) }
        
        // 8. Tendencias de Engagement
        Text("Tendencias de Engagement")
        VicoMultiLineChart(viewsData, favoritesData)
    }
}
```

---

## 📊 Gráficos y Visualizaciones

### Vico Charts Integration

#### VicoBarChart
**Uso**: Distribución de productos por categoría

```kotlin
@Composable
fun VicoBarChart(
    categories: Map<String, Int>,
    title: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = [...]
)
```

**Características**:
- Muestra máximo 8 categorías (las más populares)
- Ordenadas por cantidad descendente
- Leyenda con porcentajes
- Nombres truncados a 10 caracteres
- Animaciones suaves

**Implementación**:
```kotlin
val modelProducer = remember { CartesianChartModelProducer() }

LaunchedEffect(categories) {
    modelProducer.runTransaction {
        columnSeries { series(sortedCategories.map { it.value }) }
    }
}

CartesianChartHost(
    chart = rememberCartesianChart(
        rememberColumnCartesianLayer(),
        startAxis = rememberStartAxis(...),
        bottomAxis = rememberBottomAxis(...)
    ),
    modelProducer = modelProducer
)
```

#### VicoPieChart
**Uso**: Distribución de productos por condición (Nuevo, Usado, etc.)

**Nota**: Implementado como barras de progreso horizontales, no como gráfico circular real.

```kotlin
@Composable
fun VicoPieChart(
    data: Map<String, Int>,
    title: String,
    colors: List<Color> = [...]
)
```

**Características**:
- Máximo 5 categorías
- Indicadores de color
- Porcentajes calculados dinámicamente
- LinearProgressIndicator para cada categoría

#### VicoLineChart
**Uso**: Tendencias simples de una métrica

```kotlin
@Composable
fun VicoLineChart(
    data: List<Pair<String, Int>>,
    title: String,
    color: Color = Color(0xFF6200EE)
)
```

#### VicoMultiLineChart
**Uso**: Comparación vistas vs favoritos en los últimos 7 días

```kotlin
@Composable
fun VicoMultiLineChart(
    viewsData: List<Pair<String, Int>>,
    favoritesData: List<Pair<String, Int>>,
    title: String
)
```

**Características**:
- Dos líneas superpuestas (vistas en azul, favoritos en rosa)
- Leyenda de colores
- Eje X con fechas (últimos 5 caracteres: "11-21")
- Mensaje informativo sobre datos históricos

**Configuración**:
```kotlin
LaunchedEffect(viewsData, favoritesData) {
    modelProducer.runTransaction {
        lineSeries {
            series(viewsData.map { it.second })
            series(favoritesData.map { it.second })
        }
    }
}
```

---

## 🔄 Flujo de Datos

### Diagrama de Flujo

```
Usuario → DashboardScreen
            ↓
      DashboardViewModel.init()
            ↓
      loadStats()
            ↓
      StatsRepository.getUserStats()
            ↓
      Firebase Firestore
            ├── products collection
            ├── users collection
            └── product_stats subcollections
            ↓
      Procesamiento y agregación
            ↓
      Result.success(UserStats)
            ↓
      _dashboardState.update()
            ↓
      dashboardState (StateFlow)
            ↓
      DashboardScreen observa cambios
            ↓
      Recomposición UI
```

### Secuencia de Consultas Firebase

```kotlin
// 1. Consulta de productos
val products = firestore.collection("products")
    .whereEqualTo("userId", currentUserId)
    .get()
    .await()

// 2. Consulta de usuario (para memberSince)
val user = firestore.collection("users")
    .document(currentUserId)
    .get()
    .await()

// 3. Consulta de estadísticas diarias (para cada producto)
for (product in products) {
    val dailyStats = firestore.collection("products")
        .document(product.id)
        .collection("product_stats")
        .get()
        .await()
}
```

### Estados Reactivos con Flow

```kotlin
// ViewModel
private val _dashboardState = MutableStateFlow(DashboardState())
val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

// UI observa el estado
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    
    when {
        dashboardState.isLoading -> LoadingView()
        dashboardState.error != null -> ErrorView(...)
        else -> DashboardContent(dashboardState.stats)
    }
}
```

---

## 🔥 Integración con Firebase

### Colecciones Utilizadas

#### 1. Collection: `products`
**Estructura del documento**:
```javascript
{
  productId: "auto-generated",
  userId: "userId_del_dueño",
  title: "Laptop HP",
  description: "...",
  price: 1500.0,
  category: "Electrónica",
  condition: {
    value: "LIKE_NEW",
    displayName: "Como Nuevo"
  },
  images: ["url1", "url2"],
  views: 25,
  favorites: 5,
  isActive: true,
  createdAt: 1700000000000,
  updatedAt: 1700000000000,
  location: {...}
}
```

**Query del Dashboard**:
```kotlin
firestore.collection("products")
    .whereEqualTo("userId", currentUserId)
    .get()
```

#### 2. Collection: `users`
**Estructura del documento**:
```javascript
{
  userId: "auto-generated",
  email: "user@example.com",
  displayName: "Usuario",
  createdAt: 1700000000000,
  ...
}
```

**Query del Dashboard**:
```kotlin
firestore.collection("users")
    .document(currentUserId)
    .get()
```

#### 3. SubCollection: `products/{productId}/product_stats`
**Estructura del documento** (por fecha):
```javascript
{
  date: "2025-11-21",
  views: 5,
  favorites: 2,
  messages: 0
}
```

**Query del Dashboard**:
```kotlin
firestore.collection("products")
    .document(productId)
    .collection("product_stats")
    .get()
```

### Índices Requeridos en Firebase

Para optimizar las consultas, asegúrate de tener estos índices:

1. **products**: `userId` (ascending)
2. **product_stats**: `date` (ascending)

---

## ✨ Animaciones

### AnimationConstants

**Ubicación**: `ui/theme/AnimationConstants.kt`

```kotlin
object AnimationConstants {
    const val FLUID_DURATION = 400      // Animaciones suaves
    const val QUICK_DURATION = 200      // Animaciones rápidas
    const val SLOW_DURATION = 600       // Animaciones lentas
}
```

### Animaciones en el Dashboard

#### 1. Fade In de Cards
```kotlin
@Composable
fun StatCard(...) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = AnimationConstants.FLUID_DURATION)
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
}
```

#### 2. Animación de Barras de Progreso
```kotlin
@Composable
private fun CategoryBar(...) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) percentage / 100f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth(animatedProgress)
            .background(color)
    )
}
```

#### 3. Animación de Gráficos de Línea
Los gráficos de Vico incluyen animaciones integradas al cargar datos.

---

## 🎨 Temas y Estilos

### AppGradients

**Ubicación**: `ui/theme/Gradient.kt`

#### Gradientes Usados en Dashboard:

##### 1. PrimaryGradient
```kotlin
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(Purple60, Purple50)
)
```
**Uso**: StatCard de productos publicados

##### 2. Gradientes Personalizados
```kotlin
// Más Visto (azul)
Brush.linearGradient(
    colors = listOf(Color(0xFF2196F3), Color(0xFF1976D2))
)

// Más Favorito (rosa)
Brush.linearGradient(
    colors = listOf(Color(0xFFE91E63), Color(0xFFC2185B))
)
```

### Paleta de Colores para Stats

```kotlin
// Vistas Totales
Color(0xFF2196F3)  // Azul

// Favoritos
Color(0xFFE91E63)  // Rosa

// Categorías
Color(0xFF9C27B0)  // Púrpura

// Precio Promedio
Color(0xFF4CAF50)  // Verde

// Categorías (gráficos)
listOf(
    Color(0xFF6200EE),  // Púrpura
    Color(0xFF03DAC5),  // Cyan
    Color(0xFFFF5722),  // Naranja
    Color(0xFF4CAF50),  // Verde
    Color(0xFFFFC107),  // Amarillo
    Color(0xFF9C27B0),  // Magenta
    Color(0xFF00BCD4),  // Cyan claro
    Color(0xFFFF9800)   // Naranja oscuro
)
```

### Tipografía

```kotlin
// Títulos de secciones
MaterialTheme.typography.titleLarge

// Valores de estadísticas
MaterialTheme.typography.headlineLarge
fontSize = 32.sp
fontWeight = FontWeight.Bold

// Subtítulos
MaterialTheme.typography.bodySmall

// Labels
MaterialTheme.typography.labelSmall
```

---

## 🚀 Cómo Usar el Dashboard

### Integración en Navegación

```kotlin
// AppNavigation.kt
composable("dashboard") {
    DashboardScreen(
        onBackClick = { navController.popBackStack() },
        bottomPadding = bottomNavHeight
    )
}
```

### Navegación desde Cuenta

```kotlin
// CuentaScreen.kt
Row(
    onClick = { navController.navigate("dashboard") }
) {
    Icon(Icons.Default.Dashboard)
    Text("Mi Dashboard")
}
```

### Inyección del ViewModel

El ViewModel se crea automáticamente usando `viewModel()`:

```kotlin
@Composable
fun DashboardScreen(
    onBackClick: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    // ViewModel se crea y mantiene automáticamente
}
```

**No se requiere inyección de dependencias manual** (no usa Hilt en este módulo).

---

## 📝 Recursos de Strings

**Ubicación**: `res/values/strings.xml`

```xml
<!-- Dashboard -->
<string name="mi_dashboard">Mi Dashboard</string>
<string name="ultima_actualizacion">Última actualización: %s</string>
<string name="volver">Volver</string>
<string name="actualizar">Actualizar</string>
<string name="cargando_estadisticas">Cargando estadísticas…</string>
<string name="error_desconocido">Error desconocido</string>
<string name="reintentar">Reintentar</string>
<string name="resumen_general">Resumen General</string>
<string name="productos_publicados">Productos Publicados</string>
<string name="vistas_totales">Vistas Totales</string>
<string name="favoritos">Favoritos</string>
<string name="categorias">Categorías</string>
<string name="precio_promedio">Precio Promedio</string>
<string name="activo">Activo</string>
<string name="inactivo">Inactivo</string>
<string name="vistas">Vistas</string>
<string name="no_hay_datos">No hay datos disponibles</string>
<string name="no_hay_dcategorias">No hay categorías para mostrar</string>
<string name="productos_categorias">Productos por Categoría</string>
<string name="momento">Hace un momento</string>
<string name="hace_minutos">Hace %d minutos</string>
<string name="hace_horas">Hace %d horas</string>
<string name="hace_dias">Hace %d días</string>
```

---

## 🔍 Testing (Futuro)

### Unit Tests
```kotlin
// DashboardViewModelTest.kt
@Test
fun `loadStats should update state on success`() = runTest {
    // Arrange
    val mockRepository = MockStatsRepository()
    val viewModel = DashboardViewModel(mockRepository)
    
    // Act
    viewModel.loadStats()
    
    // Assert
    assertEquals(false, viewModel.dashboardState.value.isLoading)
    assertNotNull(viewModel.dashboardState.value.stats)
}
```

### Integration Tests
```kotlin
// StatsRepositoryTest.kt
@Test
fun `getUserStats should return valid data`() = runTest {
    val repository = StatsRepository()
    val result = repository.getUserStats()
    
    assertTrue(result.isSuccess)
}
```

---

## 🐛 Troubleshooting

### Problemas Comunes

#### 1. "Error al cargar estadísticas"
**Causa**: Usuario no autenticado o sin conexión a Firebase

**Solución**:
```kotlin
// Verificar autenticación
val user = FirebaseAuth.getInstance().currentUser
if (user == null) {
    // Redirigir a login
}
```

#### 2. Gráficos no se muestran
**Causa**: Dependencias de Vico no configuradas correctamente

**Solución**:
```kotlin
// Verificar en build.gradle.kts
implementation("com.patrykandpatrick.vico:compose:2.x.x")
implementation("com.patrykandpatrick.vico:compose-m3:2.x.x")
```

#### 3. Imágenes no cargan
**Causa**: Coil no configurado o URLs inválidas

**Solución**:
```kotlin
// Verificar AsyncImage
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .build(),
    contentDescription = null
)
```

#### 4. Datos desactualizados
**Causa**: Caché del StateFlow

**Solución**:
```kotlin
// Botón de refresh en TopBar
IconButton(onClick = { viewModel.refreshStats() }) {
    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
}
```

---

## 📌 Mejoras Futuras

### 1. Caché Local con Room
```kotlin
// Guardar estadísticas en Room para acceso offline
@Entity
data class CachedUserStats(...)

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats WHERE userId = :userId")
    fun getStats(userId: String): Flow<CachedUserStats>
}
```

### 2. Notificaciones Push
```kotlin
// Notificar cuando un producto alcanza X vistas
if (product.views % 100 == 0) {
    sendNotification("¡${product.title} alcanzó ${product.views} vistas!")
}
```

### 3. Exportar Estadísticas
```kotlin
// Generar PDF o CSV con estadísticas
fun exportStatsToCSV(stats: UserStats): File {
    // Implementación
}
```

### 4. Comparación Temporal
```kotlin
// Comparar estadísticas entre periodos
data class StatsComparison(
    val current: UserStats,
    val previous: UserStats,
    val growth: Map<String, Double>
)
```

### 5. Filtros Avanzados
```kotlin
// Filtrar por rango de fechas, categoría, etc.
fun getStatsByDateRange(startDate: Long, endDate: Long): UserStats
fun getStatsByCategory(category: String): CategoryStats
```

---

## 📚 Referencias

### Documentación Oficial
- [Vico Charts](https://github.com/patrykandpatrick/vico)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Firebase Firestore](https://firebase.google.com/docs/firestore)
- [Coil](https://coil-kt.github.io/coil/compose/)

### Arquitectura
- [MVVM con Jetpack Compose](https://developer.android.com/topic/architecture)
- [StateFlow y SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)

---

## ✅ Checklist de Implementación

- [x] Modelos de datos definidos
- [x] Repositorio con consultas Firebase
- [x] ViewModel con manejo de estados
- [x] UI principal (DashboardScreen)
- [x] Componentes reutilizables (cards, stats)
- [x] Gráficos con Vico Charts
- [x] Animaciones suaves
- [x] Manejo de errores
- [x] Carga de imágenes con Coil
- [x] Internacionalización (strings)
- [ ] Tests unitarios
- [ ] Tests de integración
- [ ] Caché local
- [ ] Exportar datos

---

## 👥 Contribuidores

**Equipo**: Grupo 2
**Módulo**: Dashboard
**Fecha**: Noviembre 2025

---

## 📄 Licencia

Este módulo es parte del proyecto **Ashley** - Marketplace de artículos usados.

---

**¿Necesitas ayuda adicional?** Revisa los comentarios en el código o contacta al equipo de desarrollo.
