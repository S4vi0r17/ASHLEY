# 🏗️ Arquitectura de ASHLEY

Este documento describe en detalle la arquitectura del proyecto ASHLEY, un marketplace móvil desarrollado con Kotlin y Jetpack Compose.

## 📑 Tabla de Contenidos

- [Visión General](#visión-general)
- [Patrón Arquitectónico](#patrón-arquitectónico)
- [Capas de la Arquitectura](#capas-de-la-arquitectura)
- [Flujo de Datos](#flujo-de-datos)
- [Módulos y Features](#módulos-y-features)
- [Gestión de Estado](#gestión-de-estado)
- [Navegación](#navegación)
- [Manejo de Errores](#manejo-de-errores)
- [Decisiones de Diseño](#decisiones-de-diseño)
- [Mejoras Futuras](#mejoras-futuras)

---

## Visión General

ASHLEY implementa el patrón **MVVM (Model-View-ViewModel)** combinado con el **patrón Repository** para proporcionar una separación clara de responsabilidades y un flujo de datos unidireccional.

### Principios Arquitectónicos

1. **Separación de Responsabilidades**: Cada capa tiene una responsabilidad única y bien definida
2. **Unidirectional Data Flow**: Los datos fluyen en una sola dirección desde la fuente hasta la UI
3. **Single Source of Truth**: Los datos tienen una única fuente de verdad
4. **Reactive Programming**: Uso de StateFlow para actualizaciones reactivas de UI
5. **Testability**: Diseño que facilita las pruebas unitarias e de integración

---

## Patrón Arquitectónico

```
┌──────────────────────────────────────────────────────────────────┐
│                         UI LAYER                                  │
│                   (Jetpack Compose)                              │
│                                                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │HomeScreen.kt│  │ChatScreen.kt │  │ProfileScreen │  ...     │
│  └──────┬──────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                │                  │                   │
│         │ observes       │ observes         │ observes          │
│         │ StateFlow      │ StateFlow        │ StateFlow         │
│         │ emits events   │ emits events     │ emits events      │
│         ▼                ▼                  ▼                   │
└─────────┼────────────────┼──────────────────┼───────────────────┘
          │                │                  │
┌─────────┼────────────────┼──────────────────┼───────────────────┐
│         │                │                  │                   │
│  ┌──────▼─────────┐ ┌───▼─────────────┐ ┌──▼──────────────┐   │
│  │HomeViewModel   │ │ChatViewModel    │ │ProfileViewModel │   │
│  │                │ │                 │ │                 │   │
│  │ • StateFlow    │ │ • StateFlow     │ │ • StateFlow     │   │
│  │ • Events       │ │ • Events        │ │ • Events        │   │
│  │ • Business     │ │ • Business      │ │ • Business      │   │
│  │   Logic        │ │   Logic         │ │   Logic         │   │
│  └───────┬────────┘ └────────┬────────┘ └─────────┬───────┘   │
│          │                   │                     │           │
│          │ calls methods     │ calls methods       │ calls     │
│          ▼                   ▼                     ▼           │
│  ┌─────────────────┐ ┌──────────────────┐ ┌────────────────┐  │
│  │ProductRepository│ │ChatRepository    │ │ProfileRepo     │  │
│  │                 │ │                  │ │                │  │
│  │ • Data access   │ │ • Data access    │ │ • Data access  │  │
│  │ • Transforms    │ │ • Transforms     │ │ • Transforms   │  │
│  │ • Caching       │ │ • Caching        │ │ • Caching      │  │
│  └────────┬────────┘ └─────────┬────────┘ └────────┬────────┘  │
└───────────┼──────────────────────┼───────────────────┼───────────┘
            │                      │                   │
            │                      │                   │
┌───────────┼──────────────────────┼───────────────────┼───────────┐
│           │                      │                   │           │
│           ▼                      ▼                   ▼           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  Cloud Firestore│  │Realtime Database│  │Firebase Storage │ │
│  │                 │  │                 │  │                 │ │
│  │  • products     │  │  • messages     │  │  • images       │ │
│  │  • users        │  │                 │  │                 │ │
│  │  • conversations│  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                  │
│                      DATA SOURCES                                │
│                      (Firebase Services)                         │
└──────────────────────────────────────────────────────────────────┘
```

---

## Capas de la Arquitectura

### 1. UI Layer (Presentation)

**Responsabilidad**: Renderizar la interfaz y manejar interacciones del usuario

**Componentes**:
- **Screens**: Composables que representan pantallas completas
- **Components**: Composables reutilizables
- **Theme**: Sistema de diseño Material3

**Características**:
- **Stateless**: No mantiene estado propio, lo recibe del ViewModel
- **Declarative**: UI declarativa con Jetpack Compose
- **Reactive**: Se recompone automáticamente cuando el estado cambia

**Ejemplo**:
```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProductClick: (String) -> Unit
) {
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LazyColumn {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product.id) }
            )
        }
    }
}
```

### 2. ViewModel Layer

**Responsabilidad**: Gestionar el estado de la UI y coordinar la lógica de negocio

**Componentes**:
- **ViewModels**: Clases que extienden de `ViewModel`
- **StateFlow**: Para exponer estado observable
- **Coroutines**: Para operaciones asíncronas

**Características**:
- **Lifecycle Aware**: Sobrevive a cambios de configuración
- **State Management**: Mantiene el estado de la UI
- **Business Logic**: Coordina repositorios y transforma datos
- **Error Handling**: Maneja errores y actualiza el estado

**Ejemplo**:
```kotlin
class HomeViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            productRepository.getAllProducts()
                .onSuccess { _products.value = it }
                .onFailure { /* handle error */ }
            _isLoading.value = false
        }
    }
}
```

### 3. Repository Layer

**Responsabilidad**: Abstraer el acceso a datos y proporcionar una API limpia

**Componentes**:
- **Repositories**: Interfaces y implementaciones
- **Data Sources**: Acceso directo a Firebase
- **Data Mapping**: Transformación de modelos Firebase a modelos de dominio

**Características**:
- **Single Source of Truth**: Centraliza el acceso a datos
- **Abstraction**: Oculta detalles de implementación de Firebase
- **Error Handling**: Usa `Result<T>` para manejar éxitos/errores
- **Caching**: (Futuro) Implementará caché local con Room

**Ejemplo**:
```kotlin
class ProductRepository {
    private val db = Firebase.firestore

    suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            val snapshot = db.collection("products").get().await()
            val products = snapshot.documents.mapNotNull {
                it.toObject(Product::class.java)
            }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 4. Data Layer (Data Sources)

**Responsabilidad**: Proporcionar acceso a fuentes de datos externas

**Componentes**:
- **Firebase Authentication**: Autenticación de usuarios
- **Cloud Firestore**: Base de datos principal
- **Firebase Realtime Database**: Chat en tiempo real
- **Firebase Storage**: Almacenamiento de imágenes

**Características**:
- **External Services**: Integración con servicios de Firebase
- **Network Operations**: Operaciones asíncronas con coroutines
- **Real-time Updates**: Listeners para actualizaciones en tiempo real

---

## Flujo de Datos

### Flujo Unidireccional

```
User Interaction
      ↓
UI emits Event
      ↓
ViewModel receives Event
      ↓
ViewModel calls Repository
      ↓
Repository fetches from Firebase
      ↓
Repository returns Result<T>
      ↓
ViewModel updates StateFlow
      ↓
UI observes StateFlow change
      ↓
UI recomposes with new state
```

### Ejemplo Completo: Publicar un Producto

```
1. Usuario llena el formulario y presiona "Publicar"
   ↓
2. VenderScreen emite evento: onPublishClick(productData)
   ↓
3. ProductViewModel.publishProduct(productData)
   ↓
4. ViewModel llama: productRepository.createProduct(product)
   ↓
5. Repository:
   - Sube imágenes a Firebase Storage
   - Obtiene URLs de las imágenes
   - Guarda producto en Firestore
   ↓
6. Repository retorna: Result.success(productId)
   ↓
7. ViewModel actualiza: _publishState.value = Success
   ↓
8. UI observa cambio y muestra mensaje de éxito
   ↓
9. Navigation navega a pantalla de inicio
```

---

## Módulos y Features

La aplicación está organizada por features, cada uno con su propia estructura MVVM:

### Feature: Authentication (login/)
```
login/
├── LoginUI.kt                    # UI de login
├── LoginViewModel.kt             # Estado y lógica de login
├── RegistroUI.kt                 # UI de registro
├── RegistroViewModel.kt          # Estado y lógica de registro
└── RecuperarUI.kt                # UI de recuperación
```

**Responsabilidades**:
- Autenticación con email/password
- Autenticación con Google
- Recuperación de contraseña
- Validación de formularios

### Feature: Profile (profile/)
```
profile/
├── ProfileViewModel.kt           # Gestión de perfil
├── ProfileSetupScreen.kt         # Configuración inicial
├── data/
│   ├── ProfileRepository.kt      # Acceso a datos de perfil
│   └── ImageStorageRepository.kt # Subida de imágenes
└── models/
    └── UserProfile.kt            # Modelo de usuario
```

**Responsabilidades**:
- CRUD de perfil de usuario
- Subida de foto de perfil
- Gestión de ubicación del usuario

### Feature: Home (home/)
```
home/
├── HomeScreen.kt                 # Pantalla principal
├── HomeViewModel.kt              # Lógica de exploración
├── components/
│   ├── ProductCard.kt            # Card de producto
│   ├── SearchBar.kt              # Barra de búsqueda
│   └── CategoryItem.kt           # Item de categoría
├── data/
│   └── ProductRepository.kt      # Acceso a productos
└── models/
    ├── Product.kt                # Modelo de producto
    └── Category.kt               # Modelo de categoría
```

**Responsabilidades**:
- Listar productos disponibles
- Búsqueda y filtrado
- Categorización de productos
- Pull-to-refresh

### Feature: Product (product/)
```
product/
├── ProductViewModel.kt           # Lógica de publicación
├── data/
│   └── ProductRepository.kt      # CRUD de productos
├── models/
│   └── Product.kt                # Modelo Firebase
└── ui/
    └── ImageComponents.kt        # Componentes de imagen
```

**Responsabilidades**:
- Crear nuevos productos
- Subir imágenes
- Validación de datos
- Integración con ubicación

### Feature: Chat (chat/)
```
chat/
├── ChatListScreen.kt             # Lista de conversaciones
├── ChatListViewModel.kt          # Estado de lista
├── ChatRealtimeScreen.kt         # Chat individual
├── ChatRealtimeViewModel.kt      # Estado de chat
├── components/
│   ├── ChatInputBar.kt           # Barra de entrada
│   ├── MessageBubble.kt          # Burbuja de mensaje
│   └── ChatListItem.kt           # Item de conversación
├── data/
│   ├── ChatListRepository.kt     # Firestore conversations
│   └── ChatRealtimeRepository.kt # Realtime DB messages
└── models/
    ├── Conversation.kt           # Modelo de conversación
    └── Message.kt                # Modelo de mensaje
```

**Responsabilidades**:
- Listar conversaciones
- Chat en tiempo real
- Envío de mensajes e imágenes
- Gestión de conversaciones

### Feature: Dashboard (dashboard/)
```
dashboard/
├── DashboardScreen.kt            # Pantalla de estadísticas
├── DashboardViewModel.kt         # Lógica de dashboard
├── components/
│   ├── DashboardComponents.kt    # Componentes UI
│   └── VicoCharts.kt             # Gráficos
├── data/
│   └── StatsRepository.kt        # Acceso a estadísticas
└── models/
    └── UserStats.kt              # Modelo de estadísticas
```

**Responsabilidades**:
- Mostrar estadísticas de ventas
- Gráficos de ingresos
- Análisis de productos
- Tendencias de categorías

---

## Gestión de Estado

### StateFlow Pattern

Todos los ViewModels usan **StateFlow** para exponer estado observable:

```kotlin
// ViewModel
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// UI
val uiState by viewModel.uiState.collectAsState()
```

### Estado por Feature

Cada ViewModel mantiene su propio estado:

**HomeViewModel**:
- `products: StateFlow<List<Product>>`
- `filteredProducts: StateFlow<List<Product>>`
- `isLoading: StateFlow<Boolean>`
- `searchQuery: StateFlow<String>`
- `selectedCategory: StateFlow<Category?>`

**ChatRealtimeViewModel**:
- `messages: StateFlow<List<Message>>`
- `isSending: StateFlow<Boolean>`
- `error: StateFlow<String?>`

### Eventos vs Estado

**Estado**: Datos que persisten (productos, mensajes, usuario)
**Eventos**: Acciones únicas (mostrar toast, navegar)

```kotlin
// Estado - StateFlow
val products: StateFlow<List<Product>>

// Evento - Single-shot
sealed class UiEvent {
    object ShowSuccess : UiEvent()
    data class ShowError(val message: String) : UiEvent()
}
```

---

## Navegación

### Navigation Component

Usa **Jetpack Navigation Compose** con navegación tipo-safe:

```kotlin
// Routes.kt
object Routes {
    const val HOME = "home"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val CHAT = "chat/{conversationId}"

    fun productDetail(id: String) = "product_detail/$id"
    fun chat(id: String) = "chat/$id"
}
```

### AppNavigation.kt

Navegación centralizada con animaciones personalizadas:

```kotlin
NavHost(navController, startDestination = Routes.HOME) {
    composable(
        route = Routes.HOME,
        enterTransition = { slideInHorizontally() },
        exitTransition = { slideOutHorizontally() }
    ) {
        HomeScreen(
            viewModel = homeViewModel,
            onProductClick = { id ->
                navController.navigate(Routes.productDetail(id))
            }
        )
    }
}
```

### Animaciones

Diferentes tipos de transiciones según el contexto:

- **Horizontal Slide**: Navegación entre tabs del bottom bar
- **Vertical Slide**: Pantallas modales (mapa, detalle)
- **Scale**: Overlays y diálogos

---

## Manejo de Errores

### Result<T> Pattern

Los Repositories retornan `Result<T>`:

```kotlin
suspend fun getProduct(id: String): Result<Product> {
    return try {
        val product = firestore.collection("products")
            .document(id)
            .get()
            .await()
            .toObject(Product::class.java)
        Result.success(product ?: throw NotFoundException())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Manejo en ViewModels

```kotlin
viewModelScope.launch {
    productRepository.getProduct(id)
        .onSuccess { product ->
            _productState.value = product
        }
        .onFailure { error ->
            _errorState.value = error.message
        }
}
```

### Tipos de Errores

- **NetworkException**: Errores de red
- **AuthException**: Errores de autenticación
- **ValidationException**: Errores de validación
- **NotFoundException**: Recurso no encontrado

---

## Decisiones de Diseño

### ¿Por qué MVVM?

1. **Separation of Concerns**: Clara separación UI/lógica/datos
2. **Testability**: ViewModels y Repositories son fáciles de testear
3. **Lifecycle Aware**: ViewModels sobreviven a cambios de configuración
4. **Android Standard**: Patrón recomendado por Google

### ¿Por qué StateFlow?

1. **Lifecycle Aware**: Se cancela automáticamente cuando la UI muere
2. **Thread Safe**: Seguro para usar en coroutines
3. **Latest Value**: Siempre tiene el último valor
4. **Compose Integration**: `collectAsState()` integra perfectamente con Compose

### ¿Por qué Repository Pattern?

1. **Abstraction**: Oculta detalles de Firebase
2. **Single Source of Truth**: Centraliza el acceso a datos
3. **Swappable**: Fácil cambiar la fuente de datos
4. **Testability**: Fácil crear mocks para testing

### ¿Por qué Feature-based Structure?

1. **Scalability**: Fácil agregar nuevos features
2. **Team Collaboration**: Equipos pueden trabajar en features separados
3. **Code Organization**: Fácil encontrar código relacionado
4. **Modularity**: Posibilidad futura de modules de Gradle

---

## Mejoras Futuras

### 1. Clean Architecture

Agregar una capa de **Domain** con Use Cases:

```
presentation/ (UI + ViewModel)
    ↓
domain/ (Use Cases + Entities)
    ↓
data/ (Repositories + Data Sources)
```

### 2. Dependency Injection (Hilt)

Implementar Hilt para:
- Inyectar ViewModels
- Inyectar Repositories
- Gestionar dependencias
- Facilitar testing

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel()
```

### 3. Offline-First con Room

Agregar base de datos local:
- Cache de productos
- Sincronización con Firebase
- Soporte offline completo

### 4. UiState Sealed Classes

Mejor gestión de estados UI:

```kotlin
sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

### 5. Domain Layer

Separar lógica de negocio en Use Cases:

```kotlin
class GetProductsUseCase(
    private val repository: ProductRepository
) {
    operator fun invoke(
        category: Category?,
        priceRange: IntRange?
    ): Flow<Result<List<Product>>> {
        // Business logic here
    }
}
```

### 6. Multi-module Architecture

Dividir en módulos de Gradle:
- `:app` - Aplicación principal
- `:core` - Código compartido
- `:feature:home` - Feature home
- `:feature:chat` - Feature chat
- `:data` - Capa de datos

---

## Diagramas de Secuencia

### Publicar un Producto

```
Usuario -> VenderScreen: Llena formulario
VenderScreen -> ProductViewModel: publishProduct()
ProductViewModel -> ImageRepository: uploadImages()
ImageRepository -> FirebaseStorage: upload()
FirebaseStorage --> ImageRepository: URLs
ImageRepository --> ProductViewModel: List<URL>
ProductViewModel -> ProductRepository: createProduct()
ProductRepository -> Firestore: save document
Firestore --> ProductRepository: success
ProductRepository --> ProductViewModel: Result.success()
ProductViewModel -> StateFlow: update state
StateFlow --> VenderScreen: Success state
VenderScreen -> Usuario: Muestra mensaje
VenderScreen -> NavController: navigate(HOME)
```

### Chat en Tiempo Real

```
Usuario -> ChatScreen: Envía mensaje
ChatScreen -> ChatViewModel: sendMessage(text)
ChatViewModel -> ChatRepository: sendMessage()
ChatRepository -> RealtimeDB: push message
RealtimeDB --> ChatRepository: success
ChatRepository --> ChatViewModel: Result.success
ChatViewModel -> StateFlow: isSending = false

[Simultaneously]
RealtimeDB -> ChatRepository: onChildAdded listener
ChatRepository -> StateFlow: emit new message
StateFlow --> ChatScreen: recompose
ChatScreen -> Usuario: Muestra mensaje
```

---

## Conclusión

La arquitectura de ASHLEY proporciona una base sólida para el desarrollo de un marketplace móvil escalable y mantenible. Aunque implementa MVVM y Repository Pattern de manera efectiva, hay oportunidades de mejora mediante la adopción de Clean Architecture, Dependency Injection con Hilt, y soporte offline con Room.

El diseño actual es apropiado para un proyecto académico y puede evolucionar hacia una arquitectura de producción a medida que el proyecto crece.

---

**Última actualización**: Noviembre 2025
**Versión**: 1.0
