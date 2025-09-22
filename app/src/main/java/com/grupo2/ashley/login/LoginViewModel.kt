package com.grupo2.ashley.login

import android.util.Log
import androidx.lifecycle.ViewModel
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

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun toggleVisibility(){
        _visibility.value = !_visibility.value
    }
    fun authLoginEmail(){
        val emailLogin = _email.value
        val passwordLogin = _password.value

        if(
            emailLogin.isBlank() ||
            passwordLogin.isBlank()
        ) {
            Log.i("AUTH", "Blank")
        }

        auth.signInWithEmailAndPassword(emailLogin,passwordLogin)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    Log.i("AUTH", "COMPLETO")
                } else {
                    Log.e("AUTH", "ERROR")
                }
            }
    }
}