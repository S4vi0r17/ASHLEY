package com.grupo2.ashley.login

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.grupo2.ashley.ui.components.GradientButton
import com.grupo2.ashley.ui.theme.ASHLEYTheme
import com.grupo2.ashley.ui.theme.AnimationConstants
import com.grupo2.ashley.ui.theme.AppGradients

class RegistroUI : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASHLEYTheme {
                val navController = rememberNavController()
                val viewModel: RegistroViewModel = viewModel()
                Registro(
                    viewModel, navController
                )
            }
        }
    }
}

@Composable
fun Registro(
    viewModel: RegistroViewModel, navController: NavController
) {
    val email = viewModel.email.collectAsState().value
    val password = viewModel.password.collectAsState().value
    val rPassword = viewModel.rPassword.collectAsState().value
    val visibility1 = viewModel.visibility1.collectAsState().value
    val visibility2 = viewModel.visibility2.collectAsState().value
    val errorMessage = viewModel.errorMessage.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val registroExitoso = viewModel.registroExitoso.collectAsState().value
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    // Animación de fade-in inicial
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Navegar a login cuando el registro es exitoso
    LaunchedEffect(registroExitoso) {
        if (registroExitoso) {
            Toast.makeText(context, "¡Registro completado exitosamente!", Toast.LENGTH_LONG).show()
            navController.navigate("login") {
                popUpTo("registro") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ){
        AnimatedVisibility(
            visible = isVisible, enter = fadeIn(animationSpec = tween(AnimationConstants.SLOW_DURATION))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Text(
                    "Crear Cuenta",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Regístrate en ASHLEY",
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Mostrar mensaje de error si existe
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(animationSpec = tween(AnimationConstants.FLUID_DURATION)),
                    exit = fadeOut(animationSpec = tween(AnimationConstants.FLUID_DURATION))
                ) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        viewModel.onEmailChange(it)
                    },
                    label = {
                        Text("Correo electrónico", style = MaterialTheme.typography.bodyMedium)
                    },
                    enabled = !isLoading,
                    singleLine = true,
                    modifier = Modifier.width(320.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
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
                            }) {
                            Icon(
                                imageVector = image,
                                contentDescription = if (visibility1) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onValueChange = {
                        viewModel.onPasswordChange(it)
                    },
                    label = {
                        Text("Contraseña", style = MaterialTheme.typography.bodyMedium)
                    },
                    enabled = !isLoading,
                    singleLine = true,
                    modifier = Modifier.width(320.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
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
                            }) {
                            Icon(
                                imageVector = image,
                                contentDescription = if (visibility2) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onValueChange = {
                        viewModel.onRPasswordChange(it)
                    },
                    label = {
                        Text("Confirmar contraseña", style = MaterialTheme.typography.bodyMedium)
                    },
                    enabled = !isLoading,
                    singleLine = true,
                    modifier = Modifier.width(320.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                GradientButton(
                    onClick = {
                        viewModel.registrarUsuario {
                            // La navegación se maneja en el LaunchedEffect
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .width(320.dp)
                        .height(52.dp),
                    gradient = AppGradients.PrimaryGradient
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "Registrarme",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Iniciar Sesión",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            navController.navigate("main") {
                                popUpTo("registro") { inclusive = true }
                            }
                        })
                }
            }
        }
    }
}