package com.grupo2.ashley.login

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                            viewModel
                        )
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
        GoogleOption()
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        EmailOption(
            viewModel
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
        RecoverTexto()
    }
}

@Composable
fun GoogleOption(){
    val googleLogo = R.drawable.googlelogowhite

    Button(
        onClick = {
            // Debug
            Log.i("GBoton", "GBoton Funciona")
        },
        modifier = Modifier.size(height = 52.dp, width = 286.dp),
        content = {
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
    )
}

@Composable
fun EmailOption(
    viewModel: LoginViewModel
){
    var email = viewModel.email.collectAsState().value
    var password = viewModel.password.collectAsState().value
    var visibilidad = viewModel.visibility.collectAsState().value

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
            modifier = Modifier.height(24.dp)
        )

        OutlinedButton(
            onClick = {
                viewModel.authLoginEmail()
            },
            modifier = Modifier.size(height = 52.dp, width = 286.dp),
            content = {
                Text(
                    "Ingresar con Email",
                    fontSize = 16.sp
                )
            }
        )
    }
}

@Composable
fun RegistroTexto(
    navController : NavController
){
    Row(){
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
            modifier = Modifier.clickable() {
                navController.navigate("registro")
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RecoverTexto(){
    Text(
        "Olvidé mi contraseña",
        fontSize = 12.sp,
        modifier = Modifier.clickable() {
            Log.i("RVBoton","BotonFunciona")
        },
        fontWeight = FontWeight.Bold
    )
}