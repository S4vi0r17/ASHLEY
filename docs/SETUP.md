# 🚀 Guía de Configuración de ASHLEY

Esta guía te ayudará a configurar el proyecto ASHLEY en tu máquina local para desarrollo y testing.

## 📋 Prerequisitos

Antes de empezar, asegúrate de tener instalado:

### Software Requerido

- **Android Studio** Hedgehog (2023.1.1) o superior
  - [Descargar Android Studio](https://developer.android.com/studio)

- **JDK (Java Development Kit)** 8 o superior
  - Verificar instalación: `java -version`
  - [Descargar JDK](https://www.oracle.com/java/technologies/downloads/)

- **Git**
  - Verificar instalación: `git --version`
  - [Descargar Git](https://git-scm.com/downloads)

### SDKs de Android

- **Android SDK** con API 26 (Android 8.0) o superior
- **Build Tools** versión 34.0.0 o superior
- **Android Emulator** (opcional, para testing)

### Cuentas Necesarias

- **Cuenta de Google** (para Firebase y Google Maps)
- **Cuenta de GitHub** (para clonar el repositorio)

---

## 📦 Instalación

### 1. Clonar el Repositorio

```bash
# Clonar el repositorio
git clone https://github.com/S4vi0r17/ASHLEY.git

# Navegar al directorio del proyecto
cd ASHLEY
```

### 2. Abrir en Android Studio

1. Abre Android Studio
2. Selecciona **File > Open**
3. Navega a la carpeta `ASHLEY` que clonaste
4. Click en **OK**
5. Espera a que Android Studio indexe el proyecto

### 3. Sincronizar Gradle

Android Studio debería sincronizar automáticamente. Si no:

1. Click en **File > Sync Project with Gradle Files**
2. Espera a que descargue todas las dependencias

---

## 🔥 Configuración de Firebase

### Paso 1: Crear Proyecto de Firebase

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Click en **Agregar proyecto**
3. Ingresa el nombre: `ASHLEY` (o el que prefieras)
4. Sigue los pasos del asistente
5. Click en **Crear proyecto**

### Paso 2: Agregar App Android

1. En la consola de Firebase, click en el ícono de Android
2. Ingresa el **package name**: `com.grupo2.ashley`
3. Ingresa un nickname (opcional): `ASHLEY Android`
4. Ingresa el **SHA-1** de tu keystore de debug:

```bash
# En Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

# En macOS/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

5. Copia el SHA-1 que aparece
6. Click en **Registrar app**

### Paso 3: Descargar google-services.json

1. Descarga el archivo `google-services.json`
2. Colócalo en la carpeta: `ASHLEY/app/`
3. **IMPORTANTE**: No subas este archivo a GitHub (ya está en .gitignore)

### Paso 4: Habilitar Servicios de Firebase

#### A. Firebase Authentication

1. En Firebase Console, ve a **Authentication**
2. Click en **Get Started**
3. Habilita **Email/Password**:
   - Activa "Email/Password"
   - Click en **Save**
4. Habilita **Google Sign-In**:
   - Click en **Google**
   - Activa el toggle
   - Selecciona tu email como **Project support email**
   - Click en **Save**

#### B. Cloud Firestore

1. En Firebase Console, ve a **Firestore Database**
2. Click en **Create database**
3. Selecciona **Start in test mode** (para desarrollo)
4. Selecciona la ubicación: `us-central1` (o la más cercana)
5. Click en **Enable**

**Configurar Reglas de Seguridad**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Products collection
    match /products/{productId} {
      allow read: if true; // Anyone can read products
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null &&
        request.auth.uid == resource.data.userId;
    }

    // Conversations collection
    match /conversations/{conversationId} {
      allow read, write: if request.auth != null &&
        request.auth.uid in resource.data.participants;
    }
  }
}
```

#### C. Firebase Storage

1. En Firebase Console, ve a **Storage**
2. Click en **Get Started**
3. Acepta las reglas de seguridad predeterminadas
4. Selecciona la ubicación: `us-central1`
5. Click en **Done**

**Configurar Reglas de Seguridad**:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Product images
    match /product_images/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId
        && request.resource.size < 5 * 1024 * 1024 // 5MB max
        && request.resource.contentType.matches('image/.*');
    }

    // Profile images
    match /profile_images/{userId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId
        && request.resource.size < 5 * 1024 * 1024 // 5MB max
        && request.resource.contentType.matches('image/.*');
    }
  }
}
```

#### D. Firebase Realtime Database

1. En Firebase Console, ve a **Realtime Database**
2. Click en **Create Database**
3. Selecciona la ubicación: `United States (us-central1)`
4. Selecciona **Start in test mode**
5. Click en **Enable**

**Configurar Reglas de Seguridad**:

```json
{
  "rules": {
    "conversations": {
      "$conversationId": {
        ".read": "auth != null",
        ".write": "auth != null",
        "messages": {
          "$messageId": {
            ".write": "auth != null",
            ".validate": "newData.hasChildren(['senderId', 'text', 'timestamp'])"
          }
        }
      }
    }
  }
}
```

---

## 🗺️ Configuración de Google Maps

### Paso 1: Obtener API Key

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuevo proyecto o selecciona el de Firebase
3. Ve a **APIs & Services > Credentials**
4. Click en **+ CREATE CREDENTIALS > API key**
5. Copia la API key generada

### Paso 2: Habilitar APIs

En Google Cloud Console, habilita las siguientes APIs:

1. **Maps SDK for Android**
   - Ve a **APIs & Services > Library**
   - Busca "Maps SDK for Android"
   - Click en **ENABLE**

2. **Places API**
   - Busca "Places API"
   - Click en **ENABLE**

3. **Geocoding API** (opcional)
   - Busca "Geocoding API"
   - Click en **ENABLE**

### Paso 3: Restringir la API Key (Recomendado)

1. En **Credentials**, click en tu API key
2. En **Application restrictions**:
   - Selecciona **Android apps**
   - Click en **+ ADD AN ITEM**
   - Ingresa:
     - **Package name**: `com.grupo2.ashley`
     - **SHA-1**: (el mismo que usaste para Firebase)
3. En **API restrictions**:
   - Selecciona **Restrict key**
   - Marca solo las APIs que habilitaste
4. Click en **SAVE**

### Paso 4: Configurar en el Proyecto

Crea un archivo `local.properties` en la raíz del proyecto (si no existe):

```properties
# local.properties

# Android SDK location
sdk.dir=C\:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk

# Google Maps API Key
MAPS_API_KEY=TU_API_KEY_AQUI
```

**IMPORTANTE**:
- Reemplaza `TU_API_KEY_AQUI` con tu API key real
- Este archivo NO se sube a GitHub (está en .gitignore)
- En Windows, usa `\\` para las rutas
- En Mac/Linux, usa `/`

---

## ⚙️ Configuración del Proyecto

### build.gradle.kts (Project)

Verifica que tengas las dependencias correctas:

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

### build.gradle.kts (App)

```kotlin
android {
    compileSdk = 35

    defaultConfig {
        applicationId = "com.grupo2.ashley"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Google Maps API Key
        manifestPlaceholders["MAPS_API_KEY"] =
            project.findProperty("MAPS_API_KEY") as String? ?: ""
    }
}
```

---

## 📱 Ejecutar la Aplicación

### Opción 1: Usar un Emulador

1. En Android Studio, ve a **Tools > Device Manager**
2. Click en **+ Create Device**
3. Selecciona un dispositivo (recomendado: Pixel 6)
4. Selecciona una imagen del sistema (API 26+)
5. Click en **Finish**
6. Click en **▶️ Run** en Android Studio
7. Selecciona tu emulador

### Opción 2: Usar un Dispositivo Físico

1. Habilita **Opciones de Desarrollador** en tu dispositivo:
   - Ve a **Ajustes > Acerca del teléfono**
   - Toca 7 veces en **Número de compilación**
2. Habilita **Depuración USB**:
   - Ve a **Ajustes > Sistema > Opciones de desarrollador**
   - Activa **Depuración USB**
3. Conecta tu dispositivo con un cable USB
4. Acepta la autorización en tu dispositivo
5. Click en **▶️ Run** en Android Studio
6. Selecciona tu dispositivo

### Verificar Instalación

Si todo está configurado correctamente:

1. La app se instalará y abrirá
2. Verás la pantalla de login
3. Podrás registrarte con email o Google
4. Podrás ver productos (si hay en la base de datos)

---

## 🔍 Solución de Problemas

### Error: "google-services.json not found"

**Solución**:
- Asegúrate de que `google-services.json` esté en `app/`
- Sincroniza Gradle: **File > Sync Project with Gradle Files**

### Error: "MAPS_API_KEY not found"

**Solución**:
- Verifica que `local.properties` existe
- Asegúrate de que la línea `MAPS_API_KEY=...` esté correcta
- Sincroniza Gradle

### Error: "SHA-1 mismatch"

**Solución**:
- Regenera el SHA-1 con el comando keytool
- Agrégalo en Firebase Console: **Project Settings > Your apps > SHA certificate fingerprints**

### Error: "FirebaseException: PERMISSION_DENIED"

**Solución**:
- Verifica las reglas de Firestore/Storage/Realtime DB
- Asegúrate de estar autenticado
- Revisa que el usuario tenga permisos

### Error: "GoogleSignIn failed"

**Solución**:
- Verifica que Google Sign-In esté habilitado en Firebase Auth
- Asegúrate de que el SHA-1 esté configurado
- Revisa que el support email esté configurado

### Error de Compilación de Gradle

**Solución**:
```bash
# Limpiar y reconstruir
./gradlew clean build

# En Windows
gradlew.bat clean build
```

### Problemas con el Emulador

**Solución**:
- Asegúrate de tener Google Play Services en el emulador
- Usa una imagen del sistema con Google APIs
- Reinicia el emulador

---

## 🧪 Testing

### Ejecutar la App en Debug

```bash
# Instalar en el dispositivo conectado
./gradlew installDebug

# Ejecutar tests (cuando existan)
./gradlew test
./gradlew connectedAndroidTest
```

### Verificar Firebase

1. **Authentication**:
   - Registra un usuario
   - Ve a Firebase Console > Authentication
   - Deberías ver el usuario registrado

2. **Firestore**:
   - Publica un producto
   - Ve a Firebase Console > Firestore
   - Deberías ver el documento en `products/`

3. **Storage**:
   - Sube una imagen de producto
   - Ve a Firebase Console > Storage
   - Deberías ver la imagen en `product_images/`

4. **Realtime Database**:
   - Envía un mensaje en el chat
   - Ve a Firebase Console > Realtime Database
   - Deberías ver el mensaje en `/conversations/.../messages/`

---

## 📦 Estructura de Archivos de Configuración

```
ASHLEY/
├── app/
│   ├── google-services.json        # Config de Firebase (NO subir a git)
│   ├── build.gradle.kts            # Config de build
│   └── src/
├── local.properties                # Config local (NO subir a git)
├── gradle.properties               # Propiedades de Gradle
├── build.gradle.kts                # Config del proyecto
└── settings.gradle.kts             # Settings de Gradle
```

### Archivos que NO se suben a GitHub

Estos archivos están en `.gitignore`:
- `local.properties`
- `app/google-services.json` (debería estar)
- `*.keystore`
- `*.jks`

---

## 🚀 Próximos Pasos

Una vez configurado el proyecto:

1. **Lee la documentación**:
   - [ARCHITECTURE.md](ARCHITECTURE.md) - Arquitectura del proyecto
   - [CONTRIBUTING.md](CONTRIBUTING.md) - Cómo contribuir

2. **Explora el código**:
   - Comienza por `MainActivity.kt`
   - Revisa `AppNavigation.kt` para entender la navegación
   - Explora los features: `home/`, `chat/`, `product/`

3. **Haz tu primer cambio**:
   - Crea una rama: `git checkout -b feature/mi-feature`
   - Haz cambios
   - Commit: `git commit -m "Add: mi feature"`
   - Push: `git push origin feature/mi-feature`

4. **Configura el ambiente de desarrollo**:
   - Instala plugins útiles de Android Studio
   - Configura el formateador de código
   - Habilita auto-import

---

## 📞 Ayuda

Si tienes problemas:

1. **Revisa la documentación**:
   - [Firebase Documentation](https://firebase.google.com/docs)
   - [Android Developers](https://developer.android.com/)
   - [Jetpack Compose](https://developer.android.com/jetpack/compose)

2. **Busca en Issues**:
   - [GitHub Issues](https://github.com/S4vi0r17/ASHLEY/issues)

3. **Contacta al equipo**:
   - Abre un nuevo issue describiendo tu problema
   - Incluye logs, screenshots si es posible

---

## 🎉 ¡Listo!

Si completaste todos los pasos, tu ambiente de desarrollo está configurado y listo para usar.

**Happy coding!** 🚀

---

**Última actualización**: Noviembre 2025
**Versión**: 1.0
