package com.grupo2.ashley.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.grupo2.ashley.MainActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.grupo2.ashley.LanguagePreferences
import com.grupo2.ashley.R
import com.grupo2.ashley.ui.components.GradientButton
import com.grupo2.ashley.ui.theme.ASHLEYTheme
import com.grupo2.ashley.ui.theme.AnimationConstants
import com.grupo2.ashley.ui.theme.AppGradients
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import com.grupo2.ashley.profile.ProfileSetupScreen
import com.grupo2.ashley.profile.ProfileViewModel
import com.grupo2.ashley.map.UbicacionViewModel

@AndroidEntryPoint
class Login : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASHLEYTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "main") {

                    composable("main") {
                        val viewModel: LoginViewModel = hiltViewModel()
                        LoginOpt(viewModel, navController)
                    }

                    composable("login") {
                        // Iniciar MainActivity y finalizar LoginActivity
                        val context = LocalContext.current
                        LaunchedEffect(Unit) {
                            val intent = Intent(context, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        }
                    }

                    composable("registro") {
                        val viewModel: RegistroViewModel = viewModel()
                        Registro(viewModel, navController)
                    }

                    composable("recover") {
                        RecuperarContra()
                    }

                    composable("profileSetup") {
                        val context = LocalContext.current
                        val profileViewModel: ProfileViewModel = viewModel()
                        val ubicacionViewModel: UbicacionViewModel = viewModel()

                        ProfileSetupScreen(
                            viewModel = profileViewModel,
                            ubicacionViewModel = ubicacionViewModel,
                            onProfileComplete = {
                                // Después de completar el perfil, ir a MainActivity
                                val intent = Intent(context, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                                (context as? Activity)?.finish()
                            },
                            onSelectLocation = {
                                // Para seleccionar ubicación, puedes navegar a un MapScreen aquí
                                // o manejarlo dentro de ProfileSetupScreen
                            }
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
) {
    val errorMessage = viewModel.errorMessage.collectAsState().value
    var isVisible by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // 🔥 NUEVO: Recompose key (soluciona que no cambie Email/Contraseña al instante)
    var languageKey by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { isVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(AnimationConstants.SLOW_DURATION))
        ) {
            key(languageKey) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                Text(
                    stringResource(R.string.bienvenido_a),
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    stringResource(R.string.titulo),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

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
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (errorMessage != null) Spacer(modifier = Modifier.height(8.dp))

                GoogleOption(viewModel, navController)
                EmailOption(viewModel, navController)
                RegistroTexto(navController)
                RecoverTexto(navController)

                // Texto para abrir popup
                LanguageTexto { showLanguageDialog = true }
                }
            }
        }


        if (showLanguageDialog) {
            LanguageDialog(
                onDismiss = { showLanguageDialog = false },
                onLanguageChanged = {
                    languageKey++
                }
            )
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
                viewModel.setGoogleError("Error: No se pudo obtener el token de Google")
            }
        } catch (ex: ApiException) {
            viewModel.setGoogleError("Error Google: ${ex.statusCode}")
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
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                stringResource(R.string.continuar_con_google),
                fontSize = 16.sp,
                color = Color.White
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

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        HorizontalDivider(
            modifier = Modifier.width(282.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = { Text(stringResource(R.string.contrasena)) },
            singleLine = true,
            visualTransformation =
                if (visibilidad) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { viewModel.toggleVisibility() }) {
                    Icon(
                        imageVector = if (visibilidad) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                    stringResource(R.string.ingresar),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun RegistroTexto(navController: NavController) {
    Row {
        Text(
            stringResource(R.string.no_tiene_cuenta),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            stringResource(R.string.registrese),
            fontSize = 12.sp,
            modifier = Modifier.clickable { navController.navigate("registro") },
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun RecoverTexto(navController: NavController) {
    Text(
        stringResource(R.string.olvide_mi_contrasena),
        fontSize = 12.sp,
        modifier = Modifier.clickable { navController.navigate("recover") },
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}


@Composable
fun LanguageTexto(onClick: () -> Unit) {
    Text(
        stringResource(R.string.idioma),
        fontSize = 12.sp,
        modifier = Modifier.clickable { onClick() },
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}


@Composable
fun LanguageDialog(
    onDismiss: () -> Unit,
    onLanguageChanged: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.idioma)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Text(
                    stringResource(R.string.espanol),
                    modifier = Modifier.clickable {
                        CoroutineScope(Dispatchers.IO).launch {
                            LanguagePreferences.saveLanguage(context, "es")
                        }
                        context.updateLocale("es")
                        onLanguageChanged()
                        onDismiss()
                    }

                )

                Text(
                    stringResource(R.string.ingles),
                    modifier = Modifier.clickable {
                        CoroutineScope(Dispatchers.IO).launch {
                            LanguagePreferences.saveLanguage(context, "en")
                        }
                        context.updateLocale("en")
                        onLanguageChanged()
                        onDismiss()
                    }

                )

                Text(
                    stringResource(R.string.portugues),
                    modifier = Modifier.clickable {
                        CoroutineScope(Dispatchers.IO).launch {
                            LanguagePreferences.saveLanguage(context, "pt")
                        }
                        context.updateLocale("pt")
                        onLanguageChanged()
                        onDismiss()
                    }

                )
            }
        },
        confirmButton = {},
        dismissButton = {
            Text(
                stringResource(R.string.volver),
                modifier = Modifier.clickable { onDismiss() }
            )
        }
    )
}


fun Context.updateLocale(language: String) {
    val locale = Locale(language)
    Locale.setDefault(locale)

    val config = Configuration(resources.configuration)
    config.setLocale(locale)

    @Suppress("DEPRECATION")
    resources.updateConfiguration(config, resources.displayMetrics)
}
