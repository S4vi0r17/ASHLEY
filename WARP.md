# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

ASHLEY is an Android marketplace application built with Kotlin and Jetpack Compose. It's a feature-rich platform for buying and selling products with integrated real-time chat, Firebase backend, Google Maps integration, and AI-powered message enhancement using Gemini AI.

**Tech Stack**: Kotlin, Jetpack Compose, Firebase (Auth/Firestore/Storage/Realtime DB), Google Maps, Hilt DI, Room, Vico Charts, Gemini AI

## Common Commands

### Build and Run

```powershell
# Windows - Build the project
.\gradlew.bat build

# Windows - Clean build
.\gradlew.bat clean build

# Windows - Install debug APK
.\gradlew.bat installDebug

# Windows - Run tests
.\gradlew.bat test

# Windows - Run instrumented tests
.\gradlew.bat connectedAndroidTest
```

```bash
# Unix/Mac - Build the project
./gradlew build

# Unix/Mac - Clean build
./gradlew clean build

# Unix/Mac - Install debug APK
./gradlew installDebug

# Unix/Mac - Run tests
./gradlew test

# Unix/Mac - Run instrumented tests
./gradlew connectedAndroidTest
```

### Development Workflow

**Running in Android Studio**:
- Use the Run button (▶️) to build and deploy
- Select device from dropdown (emulator or physical device)
- Use Debug (🐛) for debugging with breakpoints

**Sync Gradle**: When dependencies change, sync with `File > Sync Project with Gradle Files` in Android Studio

## Architecture

### MVVM with Repository Pattern

The app follows **MVVM (Model-View-ViewModel)** with the **Repository Pattern** organized by features:

**Data Flow**: UI (Compose) → ViewModel (StateFlow) → Repository → Firebase Services

**Key Architectural Layers**:
1. **UI Layer**: Jetpack Compose screens/components (stateless, observes ViewModels)
2. **ViewModel Layer**: State management with StateFlow, business logic coordination
3. **Repository Layer**: Data access abstraction, Firebase operations, Result<T> error handling
4. **Data Layer**: Firebase services (Auth, Firestore, Realtime DB, Storage)

### Feature-Based Structure

Each feature has its own directory with MVVM components:

```
feature/
├── [Feature]Screen.kt          # UI with Composables
├── [Feature]ViewModel.kt       # State management with @HiltViewModel
├── components/                 # Feature-specific reusable components
├── data/                      # Repositories with @Inject
│   └── [Feature]Repository.kt
└── models/                    # Data classes
    └── [Feature]Model.kt
```

**Core Features**:
- `login/` - Authentication (Email/Password, Google Sign-In)
- `home/` - Product browsing, search, filtering
- `product/` - Product creation and publishing
- `productdetail/` - Product details with map integration
- `chat/` - Real-time messaging (Firestore + Realtime DB)
- `dashboard/` - Seller statistics with Vico Charts
- `profile/` - User profile management
- `map/` - Google Maps location selection

### Dependency Injection (Hilt)

The app uses **Hilt** for dependency injection:

- Application class: `AshleyApplication.kt` with `@HiltAndroidApp`
- ViewModels: Use `@HiltViewModel` with `@Inject constructor`
- Repositories: Use `@Inject constructor` 
- Modules in `di/`:
  - `DatabaseModule.kt` - Room database and DAOs
  - `NetworkModule.kt` - Firebase instances (Auth, Firestore, etc.)
  - `RepositoryModule.kt` - Repository bindings

**When creating new ViewModels**: Always annotate with `@HiltViewModel` and inject dependencies via constructor.

**When creating new Repositories**: Use `@Inject constructor` for automatic injection.

### State Management

All ViewModels expose state via **StateFlow**:

```kotlin
// ViewModel pattern
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// UI consumption
val uiState by viewModel.uiState.collectAsState()
```

### Navigation

Navigation uses **Jetpack Navigation Compose** with type-safe routes defined in `navigation/AppNavigation.kt`. The app uses bottom navigation bar for main sections (Home, Sell, Chat, Dashboard, Profile).

## Firebase Integration

### Required Configuration Files

**Critical**: These files must exist but are NOT in version control:
- `app/google-services.json` - Firebase config (download from Firebase Console)
- `local.properties` - Contains `MAPS_API_KEY=your_key_here`

### Firebase Services

1. **Authentication**: Email/Password + Google Sign-In (requires SHA-1 in Firebase Console)
2. **Cloud Firestore**: Collections: `users`, `products`, `conversations`
3. **Firebase Storage**: Folders: `product_images/`, `profile_images/`
4. **Realtime Database**: Chat messages at `/conversations/{id}/messages/`

### Error Handling Pattern

Repositories return `Result<T>`:

```kotlin
suspend fun getData(): Result<Data> {
    return try {
        val data = firestore.collection("items").get().await()
        Result.success(data)
    } catch (e: Exception) {
        Log.e(TAG, "Error", e)
        Result.failure(e)
    }
}

// ViewModel handles it
repository.getData()
    .onSuccess { data -> _state.update { it.copy(data = data) } }
    .onFailure { error -> _state.update { it.copy(error = error.message) } }
```

## Code Conventions

### Naming

- **Classes**: PascalCase (`ProductRepository`, `HomeViewModel`)
- **Functions/Variables**: camelCase (`loadProducts`, `productList`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_PRODUCTS`, `API_KEY`)
- **Composables**: PascalCase like classes (`ProductCard`, `ChatBubble`)
- **Private StateFlow**: Prefix with underscore (`_products`, `_isLoading`)

### File Naming

- Screens: `[Feature]Screen.kt` (e.g., `HomeScreen.kt`)
- ViewModels: `[Feature]ViewModel.kt` (e.g., `HomeViewModel.kt`)
- Repositories: `[Feature]Repository.kt` (e.g., `ProductRepository.kt`)
- Models: Descriptive noun (e.g., `Product.kt`, `Message.kt`)

### Composable Best Practices

- Keep Composables stateless - pass data as parameters
- Accept `modifier: Modifier = Modifier` as last parameter
- Avoid ViewModels inside reusable components
- Use `remember` for computed values within a Composable
- Use `LaunchedEffect` for side effects triggered by key changes

### Git Commit Format

Use **Conventional Commits**:
- `feat(scope): description` - New features
- `fix(scope): description` - Bug fixes
- `refactor(scope): description` - Code refactoring
- `docs(scope): description` - Documentation changes
- `style(scope): description` - Formatting changes
- `test(scope): description` - Test additions/changes
- `chore(scope): description` - Build/dependency changes

Examples:
```bash
feat(chat): add message deletion functionality
fix(home): resolve product grid crash on refresh
refactor(profile): extract image upload to repository
```

## Important Implementation Details

### Gemini AI Integration

The chat feature includes AI-powered message enhancement via Gemini AI. Configuration in `chat/ai/GeminiAIService.kt` requires an API key (see `GEMINI_AI_SETUP.md`). The AI improves message grammar, spelling, and tone.

### Image Handling

- Maximum 6 images per product
- 5MB max per image
- Compression via `ImageCompressor` utility (injected via Hilt)
- Upload to Firebase Storage, URLs stored in Firestore

### Real-time Chat Architecture

Chat uses a **hybrid approach**:
- **Firestore**: Stores conversation metadata (`conversations` collection)
- **Realtime Database**: Stores actual messages for real-time sync
- Messages sync bidirectionally between both databases

### Location Features

- Google Maps integration for product location
- Requires `MAPS_API_KEY` in `local.properties`
- Distance filtering available in product search
- Location selection uses Maps Compose library

### Offline Support (Planned)

Room database is configured in `DatabaseModule.kt` for future offline-first capabilities. Currently, the app requires network connectivity for most features.

## Testing

### Test Structure

```
app/src/
├── test/                      # Unit tests (local JVM)
├── androidTest/              # Instrumented tests (require device/emulator)
```

**When adding tests**:
- Unit tests for ViewModels and Repositories
- UI tests for critical user flows
- Mock Firebase dependencies using Hilt test modules

## Project-Specific Gotchas

### SDK Versions
- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 36 (Android 15)
- **compileSdk**: 36
- **JVM Target**: 11

### Build Configurations

- Uses Kotlin DSL for Gradle files
- Secrets Gradle Plugin manages `MAPS_API_KEY` from `local.properties`
- Firebase plugin applies `google-services.json` configuration

### Common Issues and Solutions

**"google-services.json not found"**: Download from Firebase Console → Project Settings → Your apps, place in `app/` directory.

**"MAPS_API_KEY not found"**: Add `MAPS_API_KEY=your_key` to `local.properties` file in project root.

**Google Sign-In fails**: Ensure SHA-1 fingerprint is added to Firebase Console. Generate with:
```bash
# Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

# Unix/Mac
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Hilt injection errors**: Ensure Application class `AshleyApplication` is declared in `AndroidManifest.xml` with `android:name=".AshleyApplication"`.

## Development Workflow

### Setting Up New Features

1. Create feature directory under `com.grupo2.ashley.[feature]`
2. Add ViewModel with `@HiltViewModel`
3. Create Repository interface and implementation
4. Add Repository binding in `di/RepositoryModule.kt` if using interfaces
5. Build UI with Composables in `[Feature]Screen.kt`
6. Add navigation route in `navigation/AppNavigation.kt`

### Working with Firebase

- Always use coroutines with `.await()` for Firebase operations
- Wrap Firebase calls in try-catch blocks
- Return `Result<T>` from repositories
- Log errors with appropriate tags

### Code Review Checklist

- Uses Hilt for dependency injection
- ViewModels expose StateFlow, not LiveData
- Repositories return Result<T> for error handling
- Composables are stateless and reusable
- Firebase operations are async with proper error handling
- Navigation uses type-safe routes
- Follows naming conventions
- Includes appropriate logging

## Additional Documentation

- **Architecture Deep Dive**: `docs/ARCHITECTURE.md`
- **Setup Instructions**: `docs/SETUP.md`
- **Contributing Guide**: `docs/CONTRIBUTING.md`
- **Gemini AI Setup**: `GEMINI_AI_SETUP.md`

## Package Structure

```
com.grupo2.ashley/
├── AshleyApplication.kt       # App entry with @HiltAndroidApp
├── MainActivity.kt            # Single activity, hosts Compose navigation
├── chat/                      # Chat feature
├── core/                      # Shared utilities (network, image compression)
├── dashboard/                 # Seller dashboard
├── di/                        # Hilt modules
├── favorites/                 # Product favorites (future feature)
├── home/                      # Product browsing
├── login/                     # Authentication
├── map/                       # Location selection
├── navigation/                # Navigation routes and setup
├── product/                   # Product creation
├── productdetail/             # Product details
├── profile/                   # User profile
├── screens/                   # Miscellaneous screens
├── tracking/                  # Order tracking (future feature)
├── ui/                        # Shared UI components and theme
└── utils/                     # General utilities
```

## Notes for AI Agents

- This is an **academic project** for a Mobile Programming course at Universidad Nacional de San Agustín
- **Security**: Never commit API keys, `google-services.json`, or sensitive credentials
- **Language**: Code comments and variable names are primarily in Spanish; documentation is in Spanish
- **Firebase Rules**: Security rules exist in `firestore.rules` - review before modifying data access patterns
- The app targets the Peruvian market - consider locale/timezone in date formatting
- Material Design 3 is used throughout - maintain consistency with existing design patterns
