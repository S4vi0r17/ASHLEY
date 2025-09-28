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
    val email : StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password : StateFlow<String> = _password

    private val _rPassword = MutableStateFlow("")
    val rPassword : StateFlow<String> = _rPassword

    private val _visibility1 = MutableStateFlow(false)
    val visibility1 : StateFlow<Boolean> = _visibility1

    private val _visibility2 = MutableStateFlow(false)
    val visibility2 : StateFlow<Boolean> = _visibility2

    fun onEmailChange(newEmail: String){
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String){
        _password.value = newPassword
    }

    fun onRPasswordChange(newRPassword: String){
        _rPassword.value = newRPassword
    }

    fun toggleVisibility1(){
        _visibility1.value = !_visibility1.value
    }

    fun toggleVisibility2(){
        _visibility2.value = !_visibility2.value
    }

    fun validateEmail() : Pair<Boolean, String>{
        val emailRegister = _email.value
        return when{
            emailRegister.isEmpty() -> Pair(false,"Email Vacio")
            !Patterns.EMAIL_ADDRESS.matcher(emailRegister).matches() -> Pair(false,"No es email")
            else -> Pair(true,"Completado")
        }
    }

    fun validatePassword() : Pair<Boolean, String>{
        val passwordRegister = _password.value
        val rPasswordRegister = _rPassword.value
        return when{
            passwordRegister.isEmpty() -> Pair(false, "Password Vacio")
            rPasswordRegister.isEmpty() -> Pair(false,"Repetir Password Vacio")
            passwordRegister == rPasswordRegister -> Pair(true, "Completado")
            else -> Pair(false, "No son iguales")
        }
    }

    fun registrarUsuario() : String{
        val emailRegister = _email.value
        val passwordRegister = _password.value
        val auth = Firebase.auth
        var success = false

        if(validateEmail().first && validatePassword().first){
            auth.createUserWithEmailAndPassword(emailRegister,passwordRegister)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        Log.i("LOGIN", "Registro Completado")
                        success = true
                    }
                    else
                        Log.e("LOGIN","ERROR")
                }
        }

        return if(success){
            "Completado"
        } else {
            "Error al registrarse"
        }
    }
}