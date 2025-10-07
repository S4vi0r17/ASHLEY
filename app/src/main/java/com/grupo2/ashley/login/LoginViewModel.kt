package com.grupo2.ashley.login

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {

    private var auth = FirebaseAuth.getInstance()

    private val _email = MutableStateFlow("")
    val email : StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password : StateFlow<String> = _password

    private val _visibility = MutableStateFlow(false)
    val visibility : StateFlow<Boolean> = _visibility

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage : StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading : StateFlow<Boolean> = _isLoading

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun toggleVisibility(){
        _visibility.value = !_visibility.value
    }
    fun authLoginEmail(
        home:()-> Unit
    ){
        val emailLogin = _email.value
        val passwordLogin = _password.value

        if(
            emailLogin.isBlank() ||
            passwordLogin.isBlank()
        ) {
            Log.i("AUTH", "Blank")
            _errorMessage.value = "Por favor, completa todos los campos"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        auth.signInWithEmailAndPassword(emailLogin,passwordLogin)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if(task.isSuccessful){
                    Log.i("AUTH", "COMPLETO")
                    _errorMessage.value = null
                    home()
                } else {
                    val errorMsg = when {
                        task.exception?.message?.contains("no user record", ignoreCase = true) == true ->
                            "Este correo no está registrado"
                        task.exception?.message?.contains("password is invalid", ignoreCase = true) == true ->
                            "La contraseña es incorrecta"
                        task.exception?.message?.contains("badly formatted", ignoreCase = true) == true ->
                            "El formato del correo no es válido"
                        task.exception?.message?.contains("disabled", ignoreCase = true) == true ->
                            "Esta cuenta ha sido deshabilitada"
                        task.exception?.message?.contains("network", ignoreCase = true) == true ->
                            "Error de conexión. Verifica tu internet"
                        else -> "Credenciales inválidas. Verifica tu correo y contraseña"
                    }
                    Log.e("AUTH", "ERROR: ${task.exception?.message}")
                    _errorMessage.value = errorMsg
                }
            }
    }

    fun authGmailSignIn(
        credential: AuthCredential,
        home:()-> Unit
    ){
        _isLoading.value = true
        _errorMessage.value = null

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if(task.isSuccessful){
                    Log.i("AUTHGOOGLE", "COMPLETO")
                    _errorMessage.value = null
                    home()
                } else {
                    Log.e("AUTHGOOGLE", "ERROR: ${task.exception?.message}")
                    _errorMessage.value = "Error al iniciar sesión con Google: ${task.exception?.message}"
                }
            }
    }

    fun setGoogleError(message: String) {
        _errorMessage.value = message
    }
}