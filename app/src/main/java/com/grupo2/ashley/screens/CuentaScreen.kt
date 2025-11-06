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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.grupo2.ashley.profile.ProfileViewModel
import com.grupo2.ashley.ui.components.GradientButton
import com.grupo2.ashley.ui.theme.AnimationConstants
import com.grupo2.ashley.ui.theme.AppGradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentaScreen(
    innerPadding: PaddingValues,
    viewModel: ProfileViewModel = viewModel(),
    ubicacionViewModel: com.grupo2.ashley.map.UbicacionViewModel? = null,
    onNavigateToMap: (() -> Unit)? = null
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
    
    // Sincronizar ubicación del mapa si está disponible
    ubicacionViewModel?.let { ubicacionVM ->
        val ubicacionMapa by ubicacionVM.ubicacionSeleccionada.collectAsState()
        val direccionMapa by ubicacionVM.direccionSeleccionada.collectAsState()
        val nombreUbicacion by ubicacionVM.nombreUbicacion.collectAsState()
        
        // Actualizar cuando la dirección cambie
        LaunchedEffect(direccionMapa) {
            // Actualizar SIEMPRE que haya una dirección válida
            if (direccionMapa.isNotBlank() && direccionMapa != "Sin dirección seleccionada") {
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
            .padding(innerPadding)
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
                horizontalAlignment = Alignment.CenterHorizontally
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
                            contentDescription = "Foto de perfil",
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
                                contentDescription = "Cambiar foto",
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
                        "Cargando..."
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = currentUser?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                        text = "Perfil actualizado exitosamente",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Text(
            "Información Personal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Campos del perfil
        OutlinedTextField(
            value = firstName,
            onValueChange = viewModel::onFirstNameChange,
            label = { Text("Nombre") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing && !updateState.isLoading,
            singleLine = true
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = viewModel::onLastNameChange,
            label = { Text("Apellido") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing && !updateState.isLoading,
            singleLine = true
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = viewModel::onPhoneNumberChange,
            label = { Text("Teléfono") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = isEditing && !updateState.isLoading,
            singleLine = true
        )

        // Sección de Dirección
        Text(
            text = "Dirección",
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
                        contentDescription = "Ubicación",
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
                                    "Tu ubicación"
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
                                    text = "✓ Actualizado",
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
                        contentDescription = "Sin ubicación",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "No has configurado una dirección",
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
            Text(if (fullAddress.isEmpty()) "Seleccionar dirección" else "Cambiar dirección")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botones de acción
        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isEditing = false
                        viewModel.loadUserProfile()
                        viewModel.clearError()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !updateState.isLoading
                ) {
                    Text("Cancelar")
                }

                GradientButton(
                    onClick = {
                        viewModel.updateProfile {
                            // Perfil actualizado
                        }
                    },
                    enabled = !updateState.isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    gradient = AppGradients.SecondaryGradient
                ) {
                    if (updateState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        } else {
            GradientButton(
                onClick = { isEditing = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                gradient = AppGradients.SecondaryGradient
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Editar Perfil",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
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
            Text("Cerrar Sesión", fontSize = 16.sp)
        }
    }
    }

    // Diálogo de confirmación de cierre de sesión
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        auth.signOut()
                        showLogoutDialog = false
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text("Sí, cerrar sesión", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
