package com.grupo2.ashley.login

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.grupo2.ashley.AshleyApp
import com.grupo2.ashley.R
import com.grupo2.ashley.ui.components.GradientButton
import com.grupo2.ashley.ui.theme.ASHLEYTheme
import com.grupo2.ashley.ui.theme.AnimationConstants
import com.grupo2.ashley.ui.theme.AppGradients

class Login : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASHLEYTheme {
                val navController = rememberNavController()
                // ViewModel compartido para el perfil
                val profileViewModel: com.grupo2.ashley.profile.ProfileViewModel = viewModel()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        val viewModel: LoginViewModel = viewModel()

                        LoginOpt(
                            viewModel, navController
                        )
                    }
                    composable("login") {
                        AshleyApp()
                    }
                    composable("profileSetup") {
                        com.grupo2.ashley.profile.ProfileSetupScreen(
                            viewModel = profileViewModel,
                            onProfileComplete = {
                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true }
                                }
                            },
                            onSelectLocation = {
                                navController.navigate("selectProfileLocation")
                            }
                        )
                    }
                    composable("selectProfileLocation") {
                        com.grupo2.ashley.profile.ProfileLocationPickerScreen(
                            viewModel = profileViewModel,
                            onLocationSelected = {
                                navController.popBackStack()
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("registro") {
                        val viewModel: RegistroViewModel = viewModel()
                        Registro(
                            viewModel, navController
                        )
                    }
                    composable("recover") {
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
) {
    val errorMessage = viewModel.errorMessage.collectAsState().value
    var isVisible by remember { mutableStateOf(false) }

    // Animación de fade-in inicial
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ){
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(AnimationConstants.SLOW_DURATION))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Bienvenido a",
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    "ASHLEY",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Mostrar mensaje de error global
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
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                GoogleOption(
                    viewModel, navController
                )
                EmailOption(
                    viewModel, navController
                )
                RegistroTexto(
                    navController
                )
                RecoverTexto(
                    navController
                )
            }
        }
    }
}

@Composable
fun GoogleOption(
    viewModel: LoginViewModel, navController: NavController
) {
    val googleLogo = R.drawable.googlelogowhite
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account.idToken != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                viewModel.authGmailSignIn(
                    credential,
                    home = { navController.navigate("login") },
                    profileSetup = { navController.navigate("profileSetup") }
                )
            } else {
                Log.e("GAUTH", "Error: idToken es null")
                viewModel.setGoogleError("Error: No se pudo obtener el token de Google")
            }
        } catch (ex: ApiException) {
            Log.e(
                "GAUTH",
                "Error en autenticación con Google (ApiException): ${ex.statusCode} - ${ex.message}",
                ex
            )
            viewModel.setGoogleError("Error al iniciar sesión con Google. Código: ${ex.statusCode}")
        } catch (ex: Exception) {
            Log.e("GAUTH", "Error en autenticación con Google: ${ex.message}", ex)
            viewModel.setGoogleError("Error inesperado: ${ex.message ?: "Intenta nuevamente"}")
        }
    }
    val token = "440995167304-8hc733q2dgvfbf6k9pmf04mlmil1qsfb.apps.googleusercontent.com"
    val context = LocalContext.current
    val isLoading = viewModel.isLoading.collectAsState().value

    GradientButton(
        onClick = {
            val opciones = GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            ).requestIdToken(token).requestEmail().build()
            val googleSignInFinal = GoogleSignIn.getClient(context, opciones)
            launcher.launch(googleSignInFinal.signInIntent)
        },
        enabled = !isLoading,
        modifier = Modifier
            .width(286.dp)
            .height(52.dp),
        gradient = AppGradients.PrimaryGradient
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White
            )
        } else {
            Image(
                painter = painterResource(googleLogo),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(
                modifier = Modifier.width(16.dp)
            )
            Text(
                "Continuar con Google",
                fontSize = 16.sp,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun EmailOption(
    viewModel: LoginViewModel, navController: NavController
) {
    val email = viewModel.email.collectAsState().value
    val password = viewModel.password.collectAsState().value
    val visibilidad = viewModel.visibility.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                modifier = Modifier.width(282.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(
                modifier = Modifier.height(10.dp)
            )
            OutlinedTextField(
                value = email, onValueChange = {
                viewModel.onEmailChange(it)
            }, label = {
                Text(
                    "Email",
                    style = MaterialTheme.typography.bodyMedium
                )
            }, colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            ), singleLine = true
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
                        }) {
                        Icon(
                            imageVector = image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                onValueChange = {
                    viewModel.onPasswordChange(it)
                },
                label = {
                    Text(
                        "Contraseña", style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            GradientButton(
                onClick = {
                    viewModel.authLoginEmail(
                        home = { navController.navigate("login") },
                        profileSetup = { navController.navigate("profileSetup") }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .width(286.dp)
                    .height(52.dp),
                gradient = AppGradients.SecondaryGradient
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        "Ingresar",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun RegistroTexto(
    navController: NavController
) {
    Row {
        Text(
            "¿No tiene cuenta?",
            fontSize = 12.sp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(
            modifier = Modifier.width(8.dp)
        )
        Text(
            "Regístrese",
            fontSize = 12.sp,
            modifier = Modifier.clickable {
                navController.navigate("registro")
            },
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun RecoverTexto(
    navController: NavController
) {
    Text(
        "Olvidé mi contraseña",
        fontSize = 12.sp,
        modifier = Modifier.clickable {
            navController.navigate("recover")
        },
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
    )
}