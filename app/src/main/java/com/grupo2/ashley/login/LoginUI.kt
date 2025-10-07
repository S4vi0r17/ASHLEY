package com.grupo2.ashley.login

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.grupo2.ashley.AshleyApp
import com.grupo2.ashley.R
import com.grupo2.ashley.ui.theme.ASHLEYTheme

class Login : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASHLEYTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "main"){
                    composable("main"){
                        val viewModel : LoginViewModel = viewModel()

                        LoginOpt(
                            viewModel,
                            navController
                        )
                    }
                    composable("login"){
                        AshleyApp()
                    }
                    composable("registro"){
                        val viewModel : RegistroViewModel = viewModel()
                        Registro(
                            viewModel,
                            navController
                        )
                    }
                    composable("recover"){
                        RecuperarContra()
                    }
                }
            }
        }
    }
}

@Composable
fun LoginOpt(
    viewModel: LoginViewModel,
    navController: NavController
){
    val errorMessage = viewModel.errorMessage.collectAsState().value

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ){
        Text(
            "Bienvenido a",
            fontSize = 24.sp
        )
        Spacer(
            modifier = Modifier.height(8.dp)
        )
        Text(
            "ASHLEY",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // Mostrar mensaje de error global
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        GoogleOption(
            viewModel,
            navController
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        EmailOption(
            viewModel,
            navController
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        RegistroTexto(
            navController
        )
        Spacer(
            modifier = Modifier.height(4.dp)
        )
        RecoverTexto(
            navController
        )
    }
}

@Composable
fun GoogleOption(
    viewModel: LoginViewModel,
    navController: NavController
){
    val googleLogo = R.drawable.googlelogowhite
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try{
            val account = task.getResult(ApiException::class.java)
            if (account.idToken != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                viewModel.authGmailSignIn(credential) {
                    navController.navigate("login")
                }
            } else {
                Log.e("GAUTH", "Error: idToken es null")
                viewModel.setGoogleError("Error: No se pudo obtener el token de Google")
            }
        } catch (ex: ApiException){
            Log.e("GAUTH", "Error en autenticación con Google (ApiException): ${ex.statusCode} - ${ex.message}", ex)
            viewModel.setGoogleError("Error al iniciar sesión con Google. Código: ${ex.statusCode}")
        } catch (ex: Exception){
            Log.e("GAUTH", "Error en autenticación con Google: ${ex.message}", ex)
            viewModel.setGoogleError("Error inesperado: ${ex.message ?: "Intenta nuevamente"}")
        }
    }
    val token = "440995167304-8hc733q2dgvfbf6k9pmf04mlmil1qsfb.apps.googleusercontent.com"
    val context = LocalContext.current
    val isLoading = viewModel.isLoading.collectAsState().value

    Button(
        onClick = {
            val opciones = GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            ).requestIdToken(token)
                .requestEmail()
                .build()
            val googleSignInFinal = GoogleSignIn.getClient(context,opciones)
            launcher.launch(googleSignInFinal.signInIntent)
        },
        enabled = !isLoading,
        modifier = Modifier.size(height = 52.dp, width = 286.dp),
        content = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Image(
                    painter = painterResource(googleLogo),
                    contentDescription = null
                )
                Spacer(
                    modifier = Modifier.width(16.dp)
                )
                Text(
                    "Ingresar con Gmail",
                    fontSize = 16.sp
                )
            }
        }
    )
}

@Composable
fun EmailOption(
    viewModel: LoginViewModel,
    navController: NavController
){
    val email = viewModel.email.collectAsState().value
    val password = viewModel.password.collectAsState().value
    val visibilidad = viewModel.visibility.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.width(282.dp)
        )
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = {
                viewModel.onEmailChange(it)
            },
            label = {
                Text(
                    "Email"
                )
            }
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = password,
            visualTransformation = if (visibilidad) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (visibilidad) Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(
                    onClick = {
                        viewModel.toggleVisibility()
                    }
                ) {
                    Icon(
                        imageVector = image,
                        contentDescription = null
                    )
                }
            },
            onValueChange = {
                viewModel.onPasswordChange(it)
            },
            label = {
                Text(
                    "Contraseña"
                )
            }
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedButton(
            onClick = {
                viewModel.authLoginEmail{
                    navController.navigate("login")
                }
            },
            enabled = !isLoading,
            modifier = Modifier.size(height = 52.dp, width = 286.dp),
            content = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Ingresar con Email",
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
}

@Composable
fun RegistroTexto(
    navController : NavController
){
    Row{
        Text(
            "¿No tiene cuenta?",
            fontSize = 12.sp
        )
        Spacer(
            modifier = Modifier.width(8.dp)
        )
        Text(
            "Regístrese",
            fontSize = 12.sp,
            modifier = Modifier.clickable{
                navController.navigate("registro")
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RecoverTexto(
    navController: NavController
){
    Text(
        "Olvidé mi contraseña",
        fontSize = 12.sp,
        modifier = Modifier.clickable{
            navController.navigate("recover")
        },
        fontWeight = FontWeight.Bold
    )
}