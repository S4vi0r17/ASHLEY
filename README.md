# 🛍️ ASHLEY — Marketplace Seguro y Conectado

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)
![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)
![License](https://img.shields.io/badge/License-Academic-red.svg)

**ASHLEY** es una aplicación móvil tipo *marketplace* inspirada en la experiencia de compra y venta de Facebook.
Permite a los usuarios **publicar productos o servicios**, **explorar categorías**, **filtrar por ubicación y precio**, y **comunicarse mediante chat integrado**, todo desde su celular y con enfoque en la **seguridad y facilidad de uso**.

[Prototipo](https://excalidraw.com/#json=SEXBKLZVZVEFFXBXLLF0W,CN93HM1RSYXFGZCVYONWEW) • [Arquitectura](docs/ARCHITECTURE.md) • [Setup](docs/SETUP.md) • [Contribuir](docs/CONTRIBUTING.md)

</div>

---

## 📋 Tabla de Contenidos

- [Características principales](#-características-principales)
- [Capturas de pantalla](#-capturas-de-pantalla)
- [Tecnologías utilizadas](#-tecnologías-utilizadas)
- [Arquitectura](#-arquitectura)
- [Instalación](#-instalación)
- [Configuración de Firebase](#-configuración-de-firebase)
- [Estructura del proyecto](#-estructura-del-proyecto)
- [Equipo de desarrollo](#-equipo-de-desarrollo)
- [Roadmap](#-roadmap)
- [Licencia](#-licencia)

---

## 🚀 Características principales

### 🔐 Autenticación
- Registro y login con **correo electrónico y contraseña**
- Inicio de sesión con **cuenta de Google**
- Recuperación de contraseña
- Configuración de perfil de usuario con foto

### 🛒 Marketplace
- **Publicación de productos** con:
  - Múltiples fotos (hasta 6 imágenes)
  - Título, descripción y precio
  - Categoría seleccionable
  - Condición del producto (Nuevo, Como nuevo, Usado - Buen estado, Usado - Aceptable)
  - Ubicación geográfica con Google Maps
- **Exploración de productos** con:
  - Vista en cuadrícula con imágenes
  - Búsqueda por nombre
  - Filtros por categoría
  - Filtros por ubicación y precio
  - Pull-to-refresh para actualizar
- **Detalle de producto** con:
  - Galería de imágenes deslizables
  - Información completa del producto
  - Perfil del vendedor
  - Ubicación en mapa
  - Botones de acción (Llamar, Chat)

### 💬 Chat en Tiempo Real
- Sistema de mensajería integrado
- Lista de conversaciones
- Chat individual con vendedores
- Envío de imágenes
- Mensajes en tiempo real con Firebase Realtime Database
- Indicadores de estado de envío

### 📊 Dashboard de Vendedor
- Estadísticas de ventas con gráficos (Vico Charts)
- Total de ingresos por período
- Productos más vendidos
- Análisis de categorías
- Gráficos interactivos de tendencias

### 🗺️ Ubicación
- Selector de ubicación con Google Maps
- Filtrado de productos por distancia
- Visualización de ubicación del producto

### 📱 UI/UX Moderna
- Interfaz con **Jetpack Compose**
- Material Design 3
- Animaciones fluidas entre pantallas
- Tema claro responsive
- Bottom navigation bar
- Sistema de navegación tipo-safe

---

## 📸 Capturas de pantalla

<img width="100%" height="auto" alt="ashley mockup" src="https://github.com/user-attachments/assets/e4ba4db9-854e-428b-b127-7555fa6e9bbc" />

---

## 🧠 Tecnologías utilizadas

### Frontend
- **Lenguaje:** [Kotlin](https://kotlinlang.org/) 1.9+
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Material Design:** [Material3](https://m3.material.io/)
- **Navigation:** [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Async:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) + Flow

### Backend & Servicios
- **Authentication:** [Firebase Authentication](https://firebase.google.com/docs/auth)
- **Database:** [Cloud Firestore](https://firebase.google.com/docs/firestore)
- **Real-time DB:** [Firebase Realtime Database](https://firebase.google.com/docs/database)
- **Storage:** [Firebase Storage](https://firebase.google.com/docs/storage)
- **Maps:** [Google Maps SDK](https://developers.google.com/maps/documentation/android-sdk)

### Librerías Principales
- **Image Loading:** [Coil](https://coil-kt.github.io/coil/) 2.5.0
- **Charts:** [Vico](https://github.com/patrykandpatrick/vico) - Charts para Compose
- **Icons:** [Material Icons Extended](https://developer.android.com/jetpack/androidx/releases/compose-material)
- **Location:** [Google Play Services Location](https://developers.google.com/android/reference/com/google/android/gms/location/package-summary)

### Arquitectura & Patrones
- **Patrón:** MVVM (Model-View-ViewModel)
- **DI:** Dependency Injection manual (próximamente Hilt)
- **State Management:** StateFlow + ViewModel
- **Repository Pattern:** Abstracción de datos

### Herramientas de Desarrollo
- **Build System:** Gradle (Kotlin DSL)
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **JVM Target:** 1.8

---

## 🏗️ Arquitectura

ASHLEY implementa **MVVM (Model-View-ViewModel)** con el **patrón Repository** para una separación clara de responsabilidades:

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  (Jetpack Compose Screens & Components)                     │
│  - HomeScreen, ProductDetailScreen, ChatScreen, etc.        │
└─────────────────┬───────────────────────────────────────────┘
                  │ observes StateFlow
                  │ emits User Events
┌─────────────────▼───────────────────────────────────────────┐
│                    ViewModel Layer                           │
│  (State Management & Business Logic)                        │
│  - HomeViewModel, ProfileViewModel, ChatViewModel, etc.     │
│  - MutableStateFlow for state                               │
│  - Coordinates repositories                                 │
└─────────────────┬───────────────────────────────────────────┘
                  │ calls repository methods
                  │ transforms data
┌─────────────────▼───────────────────────────────────────────┐
│                   Repository Layer                           │
│  (Data Access & Business Rules)                             │
│  - ProductRepository, ProfileRepository, ChatRepository     │
│  - Handles Firebase operations                             │
│  - Returns Result<T> for error handling                    │
└─────────────────┬───────────────────────────────────────────┘
                  │ Firebase SDK calls
┌─────────────────▼───────────────────────────────────────────┐
│                    Data Sources                              │
│  - Firebase Authentication                                   │
│  - Cloud Firestore                                          │
│  - Firebase Realtime Database                               │
│  - Firebase Storage                                         │
└─────────────────────────────────────────────────────────────┘
```

Para más detalles, consulta [ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## 📦 Instalación

### Prerrequisitos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 8 o superior
- SDK de Android con API 26+
- Cuenta de Firebase
- Google Maps API Key

### Pasos

1. **Clona el repositorio**
```bash
git clone https://github.com/S4vi0r17/ASHLEY.git
cd ASHLEY
```

2. **Abre el proyecto en Android Studio**
```bash
# Abre Android Studio y selecciona "Open an Existing Project"
# Navega a la carpeta ASHLEY
```

3. **Configura Firebase**
   - Ve a la [consola de Firebase](https://console.firebase.google.com/)
   - Crea un nuevo proyecto o usa uno existente
   - Descarga el archivo `google-services.json`
   - Colócalo en `app/google-services.json`
   - Consulta [docs/SETUP.md](docs/SETUP.md) para más detalles

4. **Configura Google Maps**
   - Obtén una API key de [Google Cloud Console](https://console.cloud.google.com/)
   - Agrega la key en `local.properties`:
   ```properties
   MAPS_API_KEY=tu_api_key_aqui
   ```

5. **Sincroniza el proyecto**
```bash
# En Android Studio: File > Sync Project with Gradle Files
```

6. **Ejecuta la app**
```bash
# Conecta un dispositivo Android o inicia un emulador
# Click en "Run" (▶️) en Android Studio
```

Para instrucciones detalladas, consulta [docs/SETUP.md](docs/SETUP.md)

---

## 🔥 Configuración de Firebase

### Servicios Requeridos

Habilita los siguientes servicios en tu proyecto de Firebase:

1. **Authentication**
   - Email/Password
   - Google Sign-In

2. **Cloud Firestore**
   - Modo de prueba o reglas personalizadas
   - Colecciones: `users`, `products`, `conversations`

3. **Firebase Storage**
   - Para imágenes de productos y perfiles
   - Carpetas: `product_images/`, `profile_images/`

4. **Realtime Database**
   - Para mensajes de chat en tiempo real
   - Estructura: `/conversations/{conversationId}/messages/`

### Reglas de Seguridad

Aplica las reglas de seguridad desde los archivos:
- `firestore.rules` - Reglas de Firestore
- `storage.rules` - Reglas de Storage
- `database.rules.json` - Reglas de Realtime Database

Consulta [docs/SETUP.md](docs/SETUP.md) para más detalles.

---

## 📁 Estructura del proyecto

```
app/src/main/java/com/grupo2/ashley/
├── MainActivity.kt                 # Punto de entrada
├── navigation/                     # Navegación de la app
│   └── AppNavigation.kt           # Rutas y configuración
├── ui/
│   ├── components/                # Componentes reutilizables
│   └── theme/                     # Tema Material3
├── login/                         # Feature: Autenticación
│   ├── LoginUI.kt
│   ├── LoginViewModel.kt
│   └── RegistroUI.kt
├── profile/                       # Feature: Perfil de usuario
│   ├── ProfileViewModel.kt
│   ├── data/
│   │   └── ProfileRepository.kt
│   └── models/
│       └── UserProfile.kt
├── home/                          # Feature: Exploración de productos
│   ├── HomeScreen.kt
│   ├── HomeViewModel.kt
│   ├── components/
│   ├── data/
│   │   └── ProductRepository.kt
│   └── models/
│       └── Product.kt
├── product/                       # Feature: Publicación de productos
│   ├── ProductViewModel.kt
│   ├── data/
│   │   └── ProductRepository.kt
│   └── models/
│       └── Product.kt
├── productdetail/                 # Feature: Detalle de producto
│   ├── ProductDetailScreen.kt
│   └── ProductDetailViewModel.kt
├── chat/                          # Feature: Mensajería
│   ├── ChatListScreen.kt
│   ├── ChatRealtimeScreen.kt
│   ├── ChatListViewModel.kt
│   ├── ChatRealtimeViewModel.kt
│   ├── components/
│   ├── data/
│   │   ├── ChatListRepository.kt
│   │   └── ChatRealtimeRepository.kt
│   └── models/
│       ├── Conversation.kt
│       └── Message.kt
├── dashboard/                     # Feature: Dashboard de vendedor
│   ├── DashboardScreen.kt
│   ├── DashboardViewModel.kt
│   └── components/
├── map/                           # Feature: Ubicación
│   ├── MapScreen.kt
│   └── UbicacionViewModel.kt
└── utils/                         # Utilidades
    └── IntentUtils.kt

docs/                              # Documentación
├── ARCHITECTURE.md               # Documentación de arquitectura
├── SETUP.md                      # Guía de configuración
└── CONTRIBUTING.md               # Guía de contribución
```

---

## 👥 Equipo de desarrollo

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/S4vi0r17.png" width="100px;" alt="Eder Benites"/><br />
      <sub><b>Eder Gustavo Benites Pardavé</b></sub><br />
      <sub>22200007</sub><br />
      <a href="https://github.com/EderGBeneP">GitHub</a>
    </td>
    <td align="center">
      <img src="https://i.ibb.co/ymML3trk/IMG-20251107-221618007-HDR.jpg" width="100px;" alt="Gabriel Cuba"/><br />
      <sub><b>Gabriel Isaac Cuba García</b></sub><br />
      <sub>22200014</sub><br />
      <a href="https://github.com/S4vi0r17">GitHub</a>
    </td>
    <td align="center">
      <img src="https://i.ibb.co/b5WSCsJP/download.jpg" width="100px;" alt="Diego Flores"/><br />
      <sub><b>Diego Andrés Flores Tello</b></sub><br />
      <sub>22200018</sub><br />
      <a href="https://github.com/DiegoFloresTello">GitHub</a>
    </td>
  </tr>
</table>

**Grupo 2** 🦎 — Universidad Nacional de San Agustín
**Curso:** Programación Móvil Multiplataforma
**Docente:** PhD. Elvis Supo Colquehuanca

---

## 🗺️ Roadmap

### ✅ Completado (v1.0)
- [x] Autenticación con Email y Google
- [x] CRUD de productos con imágenes
- [x] Búsqueda y filtrado de productos
- [x] Chat en tiempo real
- [x] Dashboard de estadísticas
- [x] Integración con Google Maps
- [x] UI con Jetpack Compose

### 🚧 En Progreso (v1.1)
- [ ] Implementación de Hilt para Dependency Injection
- [ ] Consolidación de modelos de datos
- [ ] Mejora del manejo de errores
- [ ] Tests unitarios
- [ ] Soporte offline con Room

### 🔮 Futuro (v2.0)
- [ ] Sistema de favoritos completo
- [ ] Notificaciones push
- [ ] Sistema de valoraciones y reseñas
- [ ] Soporte para múltiples idiomas
- [ ] Modo oscuro
- [ ] Compartir productos en redes sociales
- [ ] Sistema de reportes
- [ ] Verificación de vendedores

---

## 🤝 Contribuir

¿Quieres contribuir al proyecto? ¡Genial!

1. Fork el repositorio
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add: Amazing Feature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

Consulta [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) para más detalles sobre el proceso de contribución.

---

## 📝 Notas de Desarrollo

### Convenciones de Código
- Nombres de clases en PascalCase
- Nombres de funciones y variables en camelCase
- Composables empiezan con mayúscula
- ViewModels terminan en `ViewModel`
- Repositories terminan en `Repository`

### Git Workflow
- `main` - Rama principal con código estable
- `develop` - Rama de desarrollo
- `feature/*` - Ramas para nuevas características
- `bugfix/*` - Ramas para correcciones

---

## 📄 Licencia

Este proyecto fue desarrollado con fines **académicos** por el **Grupo 2** 🦎 de la Universidad Nacional de San Agustín.

**© 2025 — Todos los derechos reservados.**

No está permitido el uso comercial de este proyecto sin autorización expresa de los autores.

---

## 📞 Contacto

¿Tienes preguntas o sugerencias?

- **Issues:** [GitHub Issues](https://github.com/S4vi0r17/ASHLEY/issues)
- **Discussions:** [GitHub Discussions](https://github.com/S4vi0r17/ASHLEY/discussions)
- **Email:** Contacta a través de GitHub

---

<div align="center">

**Hecho con ❤️ en Arequipa, Perú**

⭐ Si te gustó el proyecto, dale una estrella en GitHub!

</div>
