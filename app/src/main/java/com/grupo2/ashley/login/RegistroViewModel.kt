package com.grupo2.ashley.login

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RegistroViewModel : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _rPassword = MutableStateFlow("")
    val rPassword: StateFlow<String> = _rPassword

    private val _visibility1 = MutableStateFlow(false)
    val visibility1: StateFlow<Boolean> = _visibility1

    private val _visibility2 = MutableStateFlow(false)
    val visibility2: StateFlow<Boolean> = _visibility2

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _registroExitoso = MutableStateFlow(false)
    val registroExitoso: StateFlow<Boolean> = _registroExitoso

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun onRPasswordChange(newRPassword: String) {
        _rPassword.value = newRPassword
    }

    fun toggleVisibility1() {
        _visibility1.value = !_visibility1.value
    }

    fun toggleVisibility2() {
        _visibility2.value = !_visibility2.value
    }

    fun validateEmail(): Pair<Boolean, String> {
        val emailRegister = _email.value
        return when {
            emailRegister.isEmpty() -> Pair(false, "Email Vacio")
            !Patterns.EMAIL_ADDRESS.matcher(emailRegister).matches() -> Pair(false, "No es email")
            else -> Pair(true, "Completado")
        }
    }

    fun validatePassword(): Pair<Boolean, String> {
        val passwordRegister = _password.value
        val rPasswordRegister = _rPassword.value
        return when {
            passwordRegister.isEmpty() -> Pair(false, "Password Vacio")
            rPasswordRegister.isEmpty() -> Pair(false, "Repetir Password Vacio")
            passwordRegister == rPasswordRegister -> Pair(true, "Completado")
            else -> Pair(false, "No son iguales")
        }
    }

    fun registrarUsuario(onSuccess: () -> Unit) {
        val emailRegister = _email.value.trim()
        val passwordRegister = _password.value
        val auth = Firebase.auth

        // Validar email
        val emailValidation = validateEmail()
        if (!emailValidation.first) {
            _errorMessage.value = emailValidation.second
            return
        }

        // Validar contraseña
        val passwordValidation = validatePassword()
        if (!passwordValidation.first) {
            _errorMessage.value = passwordValidation.second
            return
        }

        // Validar longitud de contraseña (Firebase requiere mínimo 6 caracteres)
        if (passwordRegister.length < 6) {
            _errorMessage.value = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        auth.createUserWithEmailAndPassword(emailRegister, passwordRegister)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    Log.i("REGISTRO", "Registro Completado")
                    _errorMessage.value = null
                    _registroExitoso.value = true
                    onSuccess()
                } else {
                    val errorMsg = when {
                        task.exception?.message?.contains("badly formatted") == true -> "El correo electrónico está mal formateado"

                        task.exception?.message?.contains("already in use") == true -> "Este correo ya está registrado"

                        task.exception?.message?.contains("weak password") == true -> "La contraseña es muy débil"

                        else -> "Error: ${task.exception?.message ?: "Intenta nuevamente"}"
                    }
                    Log.e("REGISTRO", "ERROR: ${task.exception?.message}")
                    _errorMessage.value = errorMsg
                }
            }
    }
}