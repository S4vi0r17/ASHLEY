package com.grupo2.ashley.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.grupo2.ashley.profile.ProfileViewModel
import com.grupo2.ashley.ui.components.GradientButton
import com.grupo2.ashley.ui.theme.AnimationConstants
import com.grupo2.ashley.ui.theme.AppGradients
import com.grupo2.ashley.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentaScreen(
    innerPadding: PaddingValues,
    viewModel: ProfileViewModel = viewModel(),
    ubicacionViewModel: com.grupo2.ashley.map.UbicacionViewModel? = null,
    onNavigateToMap: (() -> Unit)? = null,
    onNavigateToDashboard: (() -> Unit)? = null
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    val userProfile by viewModel.userProfile.collectAsState()
    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val fullAddress by viewModel.fullAddress.collectAsState()
    val defaultPickupLocationName by viewModel.defaultPickupLocationName.collectAsState()
    val profileImageUrl by viewModel.profileImageUrl.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    // Estado para mostrar indicador de ubicación actualizada
    var locationJustUpdated by remember { mutableStateOf(false) }

    // Flag para evitar actualizaciones circulares durante la carga inicial
    var isInitialLoad by remember { mutableStateOf(true) }

    // Sincronizar ubicación del mapa si está disponible
    ubicacionViewModel?.let { ubicacionVM ->
        val ubicacionMapa by ubicacionVM.ubicacionSeleccionada.collectAsState()
        val direccionMapa by ubicacionVM.direccionSeleccionada.collectAsState()
        val nombreUbicacion by ubicacionVM.nombreUbicacion.collectAsState()

        // Cargar ubicación del perfil al UbicacionViewModel cuando se entra a la pantalla
        // Observar userProfile para esperar a que se cargue desde Firestore
        LaunchedEffect(userProfile) {
            userProfile?.let {
                val savedLat = it.defaultPickupLatitude
                val savedLng = it.defaultPickupLongitude
                val savedAddress = it.fullAddress
                val savedName = it.defaultPickupLocationName

                // Solo cargar si hay una ubicación válida guardada
                if (savedAddress.isNotEmpty() && savedLat != 0.0 && savedLng != 0.0) {
                    isInitialLoad = true // Marcar como carga inicial
                    ubicacionVM.actualizarUbicacion(
                        lat = savedLat,
                        lng = savedLng,
                        direccion = savedAddress,
                        nombre = savedName
                    )
                    // Esperar un frame para que el LaunchedEffect de direccionMapa vea el flag
                    kotlinx.coroutines.delay(100)
                    isInitialLoad = false
                }
            }
        }

        // Actualizar cuando la dirección cambie (desde el mapa)
        LaunchedEffect(direccionMapa) {
            // NO actualizar si es la carga inicial (para evitar loop)
            if (isInitialLoad) return@LaunchedEffect

            // Actualizar SOLO si viene del mapa (dirección válida y diferente a la inicial)
            if (direccionMapa.isNotBlank() &&
                direccionMapa != "Sin dirección seleccionada" &&
                direccionMapa != context.getString(R.string.sin_ubicacion)) {
                viewModel.updateLocation(
                    address = direccionMapa,
                    latitude = ubicacionMapa.latitude,
                    longitude = ubicacionMapa.longitude,
                    locationName = nombreUbicacion
                )
                locationJustUpdated = true

                // Resetear el indicador después de 2 segundos
                kotlinx.coroutines.delay(2000)
                locationJustUpdated = false
            }
        }
    }

    var isEditing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher para seleccionar imagen
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            viewModel.uploadProfileImage(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // Mostrar mensaje cuando se actualiza exitosamente
    LaunchedEffect(updateState.success) {
        if (updateState.success) {
            isEditing = false
            viewModel.resetSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = innerPadding.calculateTopPadding())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con información del usuario
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable(enabled = isEditing && !isUploadingImage) {
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (profileImageUrl.isNotEmpty() || selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri ?: profileImageUrl,
                            contentDescription = stringResource(R.string.foto_perfil),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Badge para editar foto
                    if (isEditing) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.cambiar_foto),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (userProfile != null) {
                        "${userProfile?.firstName} ${userProfile?.lastName}"
                    } else {
                        stringResource(R.string.cargando)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = currentUser?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Start
                )
            }
        }

        // Botón para ir al Dashboard
        if (onNavigateToDashboard != null && !isEditing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToDashboard() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.mi_dashboard),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.ver_mis_estadisticas),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Mensaje de error
        AnimatedVisibility(
            visible = updateState.error != null,
            enter = fadeIn(animationSpec = tween(AnimationConstants.FLUID_DURATION)),
            exit = fadeOut(animationSpec = tween(AnimationConstants.FLUID_DURATION))
        ) {
            updateState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Mensaje de éxito
        AnimatedVisibility(
            visible = updateState.success,
            enter = fadeIn(animationSpec = tween(AnimationConstants.FLUID_DURATION)),
            exit = fadeOut(animationSpec = tween(AnimationConstants.FLUID_DURATION))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.perfil_actualizado),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Text(
            stringResource(R.string.informacion_personal),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Campos del perfil
        OutlinedTextField(
            value = firstName,
            onValueChange = viewModel::onFirstNameChange,
            label = { Text(stringResource(R.string.nombre)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing && !updateState.isLoading,
            singleLine = true
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = viewModel::onLastNameChange,
            label = { Text(stringResource(R.string.apellido)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing && !updateState.isLoading,
            singleLine = true
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = viewModel::onPhoneNumberChange,
            label = { Text(stringResource(R.string.telefono)) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = isEditing && !updateState.isLoading,
            singleLine = true
        )

        // Sección de Dirección
        Text(
            text = stringResource(R.string.direccion),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (fullAddress.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (locationJustUpdated) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.ubicacion),
                        tint = if (locationJustUpdated) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (defaultPickupLocationName.isNotEmpty()) {
                                    defaultPickupLocationName
                                } else {
                                    stringResource(R.string.tu_ubicacion)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (locationJustUpdated) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                            if (locationJustUpdated) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.perfil_actualizado),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fullAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (locationJustUpdated) {
                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            }
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.sin_ubicacion),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.no_direccion_configurada),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                // Cargar la ubicación actual del perfil en el UbicacionViewModel antes de navegar
                ubicacionViewModel?.let { vm ->
                    val currentLat = viewModel.defaultPickupLatitude.value
                    val currentLng = viewModel.defaultPickupLongitude.value
                    val currentAddress = viewModel.fullAddress.value
                    val currentName = viewModel.defaultPickupLocationName.value

                    if (currentAddress.isNotEmpty() && currentLat != 0.0 && currentLng != 0.0) {
                        // Si ya tiene una dirección guardada, cargarla en el mapa
                        vm.actualizarUbicacion(
                            lat = currentLat,
                            lng = currentLng,
                            direccion = currentAddress,
                            nombre = currentName
                        )
                    }
                }
                onNavigateToMap?.invoke()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing && !updateState.isLoading
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (fullAddress.isEmpty()) stringResource(R.string.seleccionar_direccion) else stringResource(R.string.cambiar_direccion))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botones de acción
        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        isEditing = false
                        viewModel.loadUserProfile()
                        viewModel.clearError()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = !updateState.isLoading
                ) {
                    Text(stringResource(R.string.cancelar))
                }

                GradientButton(
                    onClick = {
                        viewModel.updateProfile(context) {
                            // Perfil actualizado
                        }
                    },
                    enabled = !updateState.isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    gradient = AppGradients.SecondaryGradient,
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ){
                        if (updateState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.guardar), color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            GradientButton(
                onClick = { isEditing = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                gradient = AppGradients.SecondaryGradient,
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.editar_perfil),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón de cerrar sesión
        OutlinedButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.cerrar_sesion), fontSize = 16.sp)
        }

        // Espaciado final para que el último elemento no quede tapado por la barra de navegación
        Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
    }
    }

    // Diálogo de confirmación de cierre de sesión
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.cerrar_sesion)) },
            text = { Text(stringResource(R.string.confirmar_cerrar_sesion)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        auth.signOut()
                        showLogoutDialog = false
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text(stringResource(R.string.si_cerrar_sesion), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}
