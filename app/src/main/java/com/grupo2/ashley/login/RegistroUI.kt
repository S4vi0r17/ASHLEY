package com.grupo2.ashley.login

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
import androidx.navigation.compose.rememberNavController
import com.grupo2.ashley.R
import com.grupo2.ashley.ui.theme.ASHLEYTheme

class RegistroUI : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASHLEYTheme {
                val navController = rememberNavController()
                val viewModel: RegistroViewModel = viewModel()
                Registro(
                    viewModel,
                    navController
                )
            }
        }
    }
}

@Composable
fun Registro(
    viewModel: RegistroViewModel,
    navController: NavController
){
    val email = viewModel.email.collectAsState().value
    val password = viewModel.password.collectAsState().value
    val rPassword = viewModel.rPassword.collectAsState().value
    val visibility1 = viewModel.visibility1.collectAsState().value
    val visibility2 = viewModel.visibility2.collectAsState().value
    val errorMessage = viewModel.errorMessage.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val registroExitoso = viewModel.registroExitoso.collectAsState().value
    val context = LocalContext.current

    // Navegar a login cuando el registro es exitoso
    LaunchedEffect(registroExitoso) {
        if (registroExitoso) {
            Toast.makeText(context, "¡Registro completado exitosamente!", Toast.LENGTH_LONG).show()
            navController.navigate("login") {
                popUpTo("registro") { inclusive = true }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            "Crear Cuenta",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Regístrate en ASHLEY",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mostrar mensaje de error si existe
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                viewModel.onEmailChange(it)
            },
            label = {
                Text("Correo electrónico")
            },
            enabled = !isLoading,
            singleLine = true,
            modifier = Modifier.width(320.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            visualTransformation = if (visibility1) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (visibility1) Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(
                    onClick = {
                        viewModel.toggleVisibility1()
                    }
                ) {
                    Icon(
                        imageVector = image,
                        contentDescription = if (visibility1) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            onValueChange = {
                viewModel.onPasswordChange(it)
            },
            label = {
                Text("Contraseña")
            },
            enabled = !isLoading,
            singleLine = true,
            modifier = Modifier.width(320.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = rPassword,
            visualTransformation = if (visibility2) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (visibility2) Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(
                    onClick = {
                        viewModel.toggleVisibility2()
                    }
                ) {
                    Icon(
                        imageVector = image,
                        contentDescription = if (visibility2) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            onValueChange = {
                viewModel.onRPasswordChange(it)
            },
            label = {
                Text("Confirmar contraseña")
            },
            enabled = !isLoading,
            singleLine = true,
            modifier = Modifier.width(320.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.registrarUsuario {
                    // La navegación se maneja en el LaunchedEffect
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .width(320.dp)
                .height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    "Registrarse",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Opción para iniciar sesión
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "¿Ya tienes cuenta?",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Iniciar Sesión",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    navController.navigate("main") {
                        popUpTo("registro") { inclusive = true }
                    }
                }
            )
        }
    }
}