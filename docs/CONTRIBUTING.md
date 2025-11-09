# 🤝 Guía de Contribución a ASHLEY

¡Gracias por tu interés en contribuir a ASHLEY! Esta guía te ayudará a entender cómo puedes colaborar con el proyecto.

## 📋 Tabla de Contenidos

- [Código de Conducta](#código-de-conducta)
- [Cómo Puedo Contribuir](#cómo-puedo-contribuir)
- [Proceso de Desarrollo](#proceso-de-desarrollo)
- [Estándares de Código](#estándares-de-código)
- [Guía de Git](#guía-de-git)
- [Pull Requests](#pull-requests)
- [Reportar Bugs](#reportar-bugs)
- [Sugerir Features](#sugerir-features)

---

## Código de Conducta

Este proyecto se adhiere a un código de conducta. Al participar, se espera que mantengas este código. Por favor reporta comportamiento inaceptable.

### Nuestros Estándares

**Comportamiento Aceptable**:
- Ser respetuoso con diferentes opiniones
- Aceptar críticas constructivas
- Enfocarse en lo mejor para la comunidad
- Mostrar empatía hacia otros miembros

**Comportamiento Inaceptable**:
- Uso de lenguaje o imágenes sexualizadas
- Trolling, insultos o comentarios despectivos
- Acoso público o privado
- Publicar información privada de otros sin permiso

---

## Cómo Puedo Contribuir

Hay muchas formas de contribuir a ASHLEY:

### 1. Reportar Bugs

¿Encontraste un bug? Ayúdanos a mejorarlo reportándolo:

1. Verifica que no esté ya reportado en [Issues](https://github.com/S4vi0r17/ASHLEY/issues)
2. Si no existe, [crea un nuevo issue](https://github.com/S4vi0r17/ASHLEY/issues/new)
3. Usa la plantilla de bug report
4. Incluye toda la información relevante

### 2. Sugerir Mejoras

¿Tienes una idea para mejorar ASHLEY?

1. Verifica que no esté ya sugerida
2. Crea un nuevo issue con la etiqueta `enhancement`
3. Describe claramente la mejora propuesta
4. Explica por qué sería útil

### 3. Contribuir con Código

¿Quieres contribuir código?

1. Revisa los [issues abiertos](https://github.com/S4vi0r17/ASHLEY/issues)
2. Busca issues etiquetados como `good first issue` si eres nuevo
3. Comenta en el issue que quieres trabajar en él
4. Fork el repositorio y crea una rama
5. Implementa tu solución
6. Crea un Pull Request

### 4. Mejorar Documentación

La documentación siempre puede mejorar:

- Corregir errores ortográficos
- Clarificar explicaciones confusas
- Agregar ejemplos
- Traducir a otros idiomas

### 5. Diseño y UI/UX

¿Eres diseñador?

- Mejorar la interfaz de usuario
- Crear iconos y recursos gráficos
- Proponer mejoras de usabilidad
- Diseñar mockups para nuevas features

---

## Proceso de Desarrollo

### Configuración del Ambiente

Antes de empezar, configura tu ambiente de desarrollo:

1. **Fork el repositorio**:
   - Click en "Fork" en GitHub
   - Clone tu fork localmente

2. **Configura el proyecto**:
   ```bash
   git clone https://github.com/TU_USUARIO/ASHLEY.git
   cd ASHLEY
   ```

3. **Configura el upstream**:
   ```bash
   git remote add upstream https://github.com/S4vi0r17/ASHLEY.git
   ```

4. **Configura Firebase** según [SETUP.md](SETUP.md)

### Workflow de Desarrollo

1. **Sincroniza con upstream**:
   ```bash
   git checkout main
   git pull upstream main
   ```

2. **Crea una rama**:
   ```bash
   git checkout -b feature/nombre-feature
   # o
   git checkout -b bugfix/nombre-bug
   ```

3. **Haz tus cambios**:
   - Implementa tu feature/fix
   - Sigue los estándares de código
   - Escribe tests si es aplicable

4. **Commit tus cambios**:
   ```bash
   git add .
   git commit -m "tipo: descripción breve"
   ```

5. **Push a tu fork**:
   ```bash
   git push origin feature/nombre-feature
   ```

6. **Crea un Pull Request**:
   - Ve a GitHub
   - Click en "New Pull Request"
   - Describe tus cambios

---

## Estándares de Código

### Convenciones de Nomenclatura

#### Kotlin

```kotlin
// Clases: PascalCase
class ProductRepository { }
class HomeViewModel : ViewModel() { }

// Funciones y variables: camelCase
fun loadProducts() { }
val productList = listOf<Product>()

// Constantes: UPPER_SNAKE_CASE
const val MAX_PRODUCTS = 100
const val API_KEY = "..."

// Composables: PascalCase (como clases)
@Composable
fun ProductCard() { }

// Variables privadas: camelCase con _
private val _products = MutableStateFlow<List<Product>>(emptyList())
val products: StateFlow<List<Product>> = _products.asStateFlow()
```

#### Archivos

```
// Screens: Descriptive + Screen
HomeScreen.kt
ProductDetailScreen.kt

// ViewModels: Descriptive + ViewModel
HomeViewModel.kt
ProfileViewModel.kt

// Repositories: Descriptive + Repository
ProductRepository.kt
ChatRepository.kt

// Models: Descriptive noun
Product.kt
User.kt
Message.kt
```

### Estructura de Archivos

```kotlin
// 1. Package declaration
package com.grupo2.ashley.home

// 2. Imports (agrupados y ordenados)
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

// 3. Constants (opcional)
private const val MAX_ITEMS = 10

// 4. Class/Interface definition
class HomeViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    // 5. Properties (público primero, privado después)
    val products: StateFlow<List<Product>> = _products.asStateFlow()
    private val _products = MutableStateFlow<List<Product>>(emptyList())

    // 6. Init block (si es necesario)
    init {
        loadProducts()
    }

    // 7. Public functions
    fun loadProducts() { }
    fun searchProducts(query: String) { }

    // 8. Private functions
    private fun filterProducts() { }
    private fun sortProducts() { }
}
```

### Formato de Código

Usa el formateador de Android Studio:

```bash
# Reformatear código
Ctrl + Alt + L (Windows/Linux)
Cmd + Option + L (Mac)

# Optimizar imports
Ctrl + Alt + O (Windows/Linux)
Cmd + Option + O (Mac)
```

**Configuración recomendada**:
- **Indentación**: 4 espacios (NO tabs)
- **Longitud de línea**: 100 caracteres máximo
- **Trailing commas**: Sí (para listas multilinea)

### Composables

```kotlin
// Buena práctica
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        // UI components
    }
}

// Evitar
@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    // Código complejo mezclado con UI
    val viewModel: ProductViewModel = viewModel()
    val data = viewModel.loadData() // NO!

    Card { /* ... */ }
}
```

### ViewModels

```kotlin
// Buena práctica
class HomeViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadProducts -> loadProducts()
            is HomeEvent.SearchProducts -> searchProducts(event.query)
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getProducts()
                .onSuccess { products ->
                    _uiState.update { it.copy(products = products, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }
}
```

### Repositories

```kotlin
// Buena práctica
class ProductRepository {
    private val firestore = Firebase.firestore

    suspend fun getProducts(): Result<List<Product>> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            val products = snapshot.toObjects(Product::class.java)
            Result.success(products)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching products", e)
            Result.failure(e)
        }
    }
}
```

---

## Guía de Git

### Nombres de Ramas

Usa nombres descriptivos según el tipo de cambio:

```bash
# Features
feature/add-product-favorites
feature/implement-dark-mode
feature/improve-search-performance

# Bugfixes
bugfix/fix-chat-crash
bugfix/correct-image-upload

# Hotfixes
hotfix/critical-auth-bug

# Documentación
docs/update-readme
docs/add-api-documentation

# Refactoring
refactor/reorganize-viewmodels
refactor/extract-components
```

### Mensajes de Commit

Usa [Conventional Commits](https://www.conventionalcommits.org/):

```bash
# Formato
tipo(scope opcional): descripción breve

cuerpo opcional

footer opcional
```

**Tipos**:
- `feat`: Nueva funcionalidad
- `fix`: Corrección de bug
- `docs`: Cambios en documentación
- `style`: Formato, punto y coma, etc (no afecta el código)
- `refactor`: Refactorización de código
- `perf`: Mejoras de rendimiento
- `test`: Agregar o corregir tests
- `chore`: Cambios en build, dependencias, etc

**Ejemplos**:

```bash
# Feature
feat(home): add pull-to-refresh functionality

# Bugfix
fix(chat): resolve message duplication issue

# Documentación
docs(readme): update installation instructions

# Refactor
refactor(viewmodels): extract common state management logic

# Style
style(product): format code according to conventions

# Performance
perf(home): optimize product list rendering
```

### Commits Atómicos

Haz commits pequeños y enfocados:

```bash
# ✅ Bueno
git commit -m "feat(auth): add Google Sign-In button"
git commit -m "feat(auth): implement Google Sign-In logic"
git commit -m "test(auth): add Google Sign-In tests"

# ❌ Malo
git commit -m "Added Google Sign-In, fixed bugs, updated docs"
```

---

## Pull Requests

### Antes de Crear un PR

1. **Sincroniza con main**:
   ```bash
   git checkout main
   git pull upstream main
   git checkout feature/tu-feature
   git rebase main
   ```

2. **Verifica que compile**:
   ```bash
   ./gradlew build
   ```

3. **Ejecuta tests** (si existen):
   ```bash
   ./gradlew test
   ```

4. **Revisa tus cambios**:
   ```bash
   git diff main
   ```

### Crear un Pull Request

1. **Push a tu fork**:
   ```bash
   git push origin feature/tu-feature
   ```

2. **Abre un PR en GitHub**:
   - Ve a tu fork en GitHub
   - Click en "Pull Request"
   - Selecciona tu rama
   - Completa la plantilla del PR

### Plantilla de PR

```markdown
## Descripción
Descripción clara y concisa de los cambios.

## Tipo de Cambio
- [ ] Bug fix (no breaking change)
- [ ] New feature (no breaking change)
- [ ] Breaking change
- [ ] Documentation update

## Cambios Realizados
- Cambio 1
- Cambio 2
- Cambio 3

## Screenshots (si aplica)
[Agregar capturas de pantalla]

## Checklist
- [ ] Mi código sigue los estándares del proyecto
- [ ] He comentado código complejo
- [ ] He actualizado la documentación
- [ ] Mis cambios no generan nuevos warnings
- [ ] He agregado tests
- [ ] Todos los tests pasan
- [ ] He probado en un dispositivo físico
```

### Revisión de Código

Tu PR será revisado por otros contributors:

- **Responde a los comentarios** de forma constructiva
- **Haz los cambios** solicitados
- **Pide clarificación** si algo no está claro
- **Actualiza el PR** cuando hagas cambios

---

## Reportar Bugs

### Plantilla de Bug Report

```markdown
## Descripción del Bug
Descripción clara y concisa del bug.

## Pasos para Reproducir
1. Ve a '...'
2. Click en '...'
3. Scroll hasta '...'
4. Ver error

## Comportamiento Esperado
Qué esperabas que sucediera.

## Comportamiento Actual
Qué sucedió en realidad.

## Screenshots
[Agregar capturas si es relevante]

## Ambiente
- Dispositivo: [e.g. Pixel 6]
- OS: [e.g. Android 13]
- Versión de la App: [e.g. 1.0]

## Logs
```
[Pegar logs relevantes]
```

## Información Adicional
Cualquier otra información relevante.
```

---

## Sugerir Features

### Plantilla de Feature Request

```markdown
## Problema a Resolver
Descripción clara del problema que esta feature resolvería.

## Solución Propuesta
Descripción clara de cómo funcionaría la feature.

## Alternativas Consideradas
Otras soluciones que consideraste.

## Beneficios
- Beneficio 1
- Beneficio 2

## Mockups (opcional)
[Agregar diseños o wireframes]

## Información Adicional
Cualquier contexto adicional.
```

---

## Preguntas Frecuentes

### ¿Puedo trabajar en múltiples issues a la vez?

Sí, pero crea una rama separada para cada uno.

### ¿Cómo mantengo mi fork actualizado?

```bash
git checkout main
git pull upstream main
git push origin main
```

### ¿Qué hago si mi PR tiene conflictos?

```bash
git checkout tu-rama
git rebase main
# Resuelve conflictos
git add .
git rebase --continue
git push -f origin tu-rama
```

### ¿Cuánto tiempo tarda en revisarse un PR?

Normalmente 1-3 días. Ten paciencia.

### ¿Puedo contribuir sin saber programar?

¡Sí! Puedes:
- Mejorar documentación
- Reportar bugs
- Sugerir mejoras
- Ayudar con traducciones
- Diseñar UI/UX

---

## Recursos Útiles

### Documentación

- [ARCHITECTURE.md](ARCHITECTURE.md) - Arquitectura del proyecto
- [SETUP.md](SETUP.md) - Configuración del proyecto
- [README.md](../README.md) - Información general

### Enlaces Externos

- [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- [Jetpack Compose Guidelines](https://developer.android.com/jetpack/compose/mental-model)
- [Git Best Practices](https://git-scm.com/book/en/v2)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

## Reconocimientos

Todos los contributors serán reconocidos en el README.md del proyecto.

¡Gracias por contribuir a ASHLEY! 🎉

---

**¿Preguntas?** Abre un issue o contacta al equipo.

**Última actualización**: Noviembre 2025
**Versión**: 1.0
