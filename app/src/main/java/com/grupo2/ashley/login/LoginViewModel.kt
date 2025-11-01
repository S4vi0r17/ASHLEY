package com.grupo2.ashley.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.grupo2.ashley.profile.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private var auth = FirebaseAuth.getInstance()
    private val profileRepository = ProfileRepository()

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
                // Guardar foto de perfil de Google si existe
                val currentUser = auth.currentUser
                val photoUrl = currentUser?.photoUrl?.toString()
                if (!photoUrl.isNullOrBlank()) {
                    saveGoogleProfilePhoto(photoUrl)
                }
                
                val isComplete = profileRepository.isProfileComplete()
                _isLoading.value = false
                
                if (isComplete) {
                    Log.d("AUTH", "Perfil completo, navegando a Home")
                    _needsProfileSetup.value = false
                    home()
                } else {
                    Log.d("AUTH", "Perfil incompleto, navegando a ProfileSetup")
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
                
                // Verificar si ya existe una foto
                val docRef = firestore.collection("users").document(userId)
                val snapshot = docRef.get().await()
                
                val currentPhotoUrl = snapshot.getString("profileImageUrl") ?: ""
                
                // Solo actualizar si no tiene foto o si está vacía
                if (currentPhotoUrl.isBlank()) {
                    docRef.update("profileImageUrl", photoUrl).await()
                    Log.d("AUTH", "Foto de Google guardada: $photoUrl")
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