package com.grupo2.ashley.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.grupo2.ashley.auth.SessionManager
import com.grupo2.ashley.profile.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val sessionManager: SessionManager,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _visibility = MutableStateFlow(false)
    val visibility: StateFlow<Boolean> = _visibility

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _needsProfileSetup = MutableStateFlow(false)
    val needsProfileSetup: StateFlow<Boolean> = _needsProfileSetup

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun toggleVisibility() {
        _visibility.value = !_visibility.value
    }

    fun authLoginEmail(
        home: () -> Unit,
        profileSetup: () -> Unit
    ) {
        val emailLogin = _email.value
        val passwordLogin = _password.value

        if (emailLogin.isBlank() || passwordLogin.isBlank()) {
            Log.i("AUTH", "Blank")
            _errorMessage.value = "Por favor, completa todos los campos"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        auth.signInWithEmailAndPassword(emailLogin, passwordLogin).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i("AUTH", "COMPLETO")
                    _errorMessage.value = null
                    checkProfileAndNavigate(home, profileSetup)
                } else {
                    _isLoading.value = false
                    val errorMsg = when {
                        task.exception?.message?.contains(
                            "no user record", ignoreCase = true
                        ) == true -> "Este correo no está registrado"

                        task.exception?.message?.contains(
                            "password is invalid", ignoreCase = true
                        ) == true -> "La contraseña es incorrecta"

                        task.exception?.message?.contains(
                            "badly formatted", ignoreCase = true
                        ) == true -> "El formato del correo no es válido"

                        task.exception?.message?.contains(
                            "disabled",
                            ignoreCase = true
                        ) == true -> "Esta cuenta ha sido deshabilitada"

                        task.exception?.message?.contains(
                            "network",
                            ignoreCase = true
                        ) == true -> "Error de conexión. Verifica tu internet"

                        else -> "Credenciales inválidas. Verifica tu correo y contraseña"
                    }
                    Log.e("AUTH", "ERROR: ${task.exception?.message}")
                    _errorMessage.value = errorMsg
                }
            }
    }

    fun authGmailSignIn(
        credential: AuthCredential,
        home: () -> Unit,
        profileSetup: () -> Unit
    ) {
        _isLoading.value = true
        _errorMessage.value = null

        auth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i("AUTHGOOGLE", "COMPLETO")
                    _errorMessage.value = null
                    checkProfileAndNavigate(home, profileSetup)
                } else {
                    _isLoading.value = false
                    Log.e("AUTHGOOGLE", "ERROR: ${task.exception?.message}")
                    _errorMessage.value =
                        "Error al iniciar sesión con Google: ${task.exception?.message}"
                }
            }
    }

    /**
     * Verifica si el usuario tiene perfil completo y navega según corresponda
     */
    private fun checkProfileAndNavigate(
        home: () -> Unit,
        profileSetup: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                val userId = currentUser?.uid

                Log.d("AUTH", "================================================")
                Log.d("AUTH", "INICIO DE VERIFICACIÓN DE PERFIL")
                Log.d("AUTH", "Usuario autenticado: $userId")
                Log.d("AUTH", "Email: ${currentUser?.email}")

                // Crear token de sesión para el usuario autenticado
                if (userId != null) {
                    Log.d("AUTH", "Creando token de sesión para usuario: $userId")
                    sessionManager.createSession(userId)
                    Log.d("AUTH", "Token de sesión creado exitosamente")
                }

                // Pequeña pausa para asegurar que Firestore esté sincronizado
                Log.d("AUTH", "Esperando 500ms para sincronización...")
                kotlinx.coroutines.delay(500)

                // Verificar si el perfil está completo (ahora lee desde servidor)
                Log.d("AUTH", "Llamando a profileRepository.isProfileComplete()...")
                val isComplete = profileRepository.isProfileComplete()

                Log.d("AUTH", "================================================")
                Log.d("AUTH", "RESULTADO FINAL: isComplete = $isComplete")
                Log.d("AUTH", "================================================")

                // Guardar foto de perfil de Google si existe (después de verificar)
                val photoUrl = currentUser?.photoUrl?.toString()
                if (!photoUrl.isNullOrBlank() && !isComplete) {
                    Log.d("AUTH", "Guardando foto de Google para nuevo usuario")
                    saveGoogleProfilePhoto(photoUrl)
                }

                _isLoading.value = false

                if (isComplete) {
                    Log.d("AUTH", "✓ Perfil completo, navegando a Home")
                    _needsProfileSetup.value = false
                    home()
                } else {
                    Log.d("AUTH", "✗ Perfil incompleto, navegando a ProfileSetup")
                    _needsProfileSetup.value = true
                    profileSetup()
                }
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("AUTH", "Error al verificar perfil: ${e.message}", e)
                // En caso de error, ir a setup de perfil para estar seguros
                _needsProfileSetup.value = true
                profileSetup()
            }
        }
    }

    /**
     * Guarda la foto de perfil de Google en Firestore
     */
    private fun saveGoogleProfilePhoto(photoUrl: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val firestore = FirebaseFirestore.getInstance()
                
                // Verificar si ya existe el documento
                val docRef = firestore.collection("users").document(userId)
                val snapshot = docRef.get().await()
                
                if (snapshot.exists()) {
                    // El documento existe, solo actualizar la foto si está vacía
                    val currentPhotoUrl = snapshot.getString("profileImageUrl") ?: ""
                    if (currentPhotoUrl.isBlank()) {
                        docRef.update("profileImageUrl", photoUrl).await()
                        Log.d("AUTH", "Foto de Google guardada en perfil existente: $photoUrl")
                    } else {
                        Log.d("AUTH", "El usuario ya tiene foto, no se reemplaza")
                    }
                } else {
                    // El documento no existe, solo guardar la URL para usar después
                    // No crear el documento aquí para no interferir con isProfileComplete
                    Log.d("AUTH", "Documento no existe, la foto se guardará al completar perfil")
                }
            } catch (e: Exception) {
                // No fallar el login si no se puede guardar la foto
                Log.w("AUTH", "No se pudo guardar la foto de Google: ${e.message}")
            }
        }
    }

    fun setGoogleError(message: String) {
        _errorMessage.value = message
    }
}