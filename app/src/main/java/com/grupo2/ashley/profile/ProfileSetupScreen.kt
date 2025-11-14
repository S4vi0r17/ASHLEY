package com.grupo2.ashley.profile

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grupo2.ashley.ui.components.GradientButton
import com.grupo2.ashley.ui.theme.AnimationConstants
import com.grupo2.ashley.ui.theme.AppGradients
import com.grupo2.ashley.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileViewModel = viewModel(),
    onProfileComplete: () -> Unit,
    onSelectLocation: () -> Unit = {},
    ubicacionViewModel: com.grupo2.ashley.map.UbicacionViewModel? = null
) {
    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val fullAddress by viewModel.fullAddress.collectAsState()
    val profileImageUrl by viewModel.profileImageUrl.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val defaultPickupLocationName by viewModel.defaultPickupLocationName.collectAsState()
    val context = LocalContext.current

    // Sincronizar ubicación del mapa si está disponible
    ubicacionViewModel?.let { ubicacionVM ->
        val ubicacionMapa by ubicacionVM.ubicacionSeleccionada.collectAsState()
        val direccionMapa by ubicacionVM.direccionSeleccionada.collectAsState()
        val nombreUbicacion by ubicacionVM.nombreUbicacion.collectAsState()

        // Actualizar cuando cualquier valor cambie
        LaunchedEffect(direccionMapa) {
            android.util.Log.d("ProfileSetupScreen", "=== LaunchedEffect ejecutado ===")
            android.util.Log.d("ProfileSetupScreen", "Dirección mapa: '$direccionMapa'")
            android.util.Log.d("ProfileSetupScreen", "Nombre ubicación: '$nombreUbicacion'")
            android.util.Log.d("ProfileSetupScreen", "Lat: ${ubicacionMapa.latitude}, Lng: ${ubicacionMapa.longitude}")

            // Actualizar SIEMPRE que haya una dirección válida, sin importar el texto
            if (direccionMapa.isNotBlank() && direccionMapa != "Sin dirección seleccionada") {
                android.util.Log.d("ProfileSetupScreen", "✓ Actualizando ubicación en ViewModel...")
                viewModel.updateLocation(
                    address = direccionMapa,
                    latitude = ubicacionMapa.latitude,
                    longitude = ubicacionMapa.longitude,
                    locationName = nombreUbicacion
                )
                android.util.Log.d("ProfileSetupScreen", "✓ Ubicación actualizada")
            } else {
                android.util.Log.d("ProfileSetupScreen", "✗ Dirección no válida: '$direccionMapa'")
            }
        }
    }

    var isVisible by remember { mutableStateOf(false) }
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
        isVisible = true
    }

    // Navegar cuando el perfil se guarda exitosamente
    LaunchedEffect(updateState.success) {
        if (updateState.success) {
            onProfileComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.completar_perfil),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(AnimationConstants.SLOW_DURATION))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mensaje informativo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            stringResource(R.string.mensaje_informativo),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selector de foto de perfil
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .clickable(enabled = !isUploadingImage && !updateState.isLoading) {
                                imagePickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp
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
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(R.string.agregar_foto_desc),
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isUploadingImage && !updateState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (profileImageUrl.isNotEmpty() || selectedImageUri != null)
                                stringResource(R.string.cambiar_foto)
                            else
                                stringResource(R.string.agregar_foto)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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

                // Nombre
                OutlinedTextField(
                    value = firstName,
                    onValueChange = viewModel::onFirstNameChange,
                    label = { Text(stringResource(R.string.nombre_label)) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !updateState.isLoading
                )

                // Apellido
                OutlinedTextField(
                    value = lastName,
                    onValueChange = viewModel::onLastNameChange,
                    label = { Text(stringResource(R.string.apellido_label)) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !updateState.isLoading
                )

                // Teléfono
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = viewModel::onPhoneNumberChange,
                    label = { Text(stringResource(R.string.telefono_label)) },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    enabled = !updateState.isLoading,
                    placeholder = { Text(stringResource(R.string.telefono_placeholder)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sección de Dirección
                Text(
                    text = stringResource(R.string.direccion_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (fullAddress.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Ubicación", // NOTA: No se proveyó string para "Ubicación"
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (defaultPickupLocationName.isNotEmpty()) {
                                        stringResource(R.string.sin_ubicacion) // ->
                                    } else {
                                        stringResource(R.string.ubicacion_seleccionada)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fullAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onSelectLocation,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !updateState.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fullAddress.isEmpty()) stringResource(R.string.seleccionar_direccion) else stringResource(R.string.cambiar_direccion))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ubicación de entrega predeterminada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (defaultPickupLocationName.isBlank())
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !updateState.isLoading) {
                                onSelectLocation()
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (defaultPickupLocationName.isBlank())
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.ubicacion_entrega_predeterminada),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (defaultPickupLocationName.isBlank())
                                        stringResource(R.string.toca_para_seleccionar)
                                    else
                                        stringResource(R.string.sin_ubicacion),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (defaultPickupLocationName.isBlank())
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.mensaje_entrega_predeterminada),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botón guardar
                GradientButton(
                    onClick = {
                        viewModel.saveProfile(context,onProfileComplete)
                    },
                    enabled = !updateState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    gradient = AppGradients.SecondaryGradient
                ) {
                    if (updateState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            stringResource(R.string.guardar_y_continuar),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Text(
                    stringResource(R.string.campos_obligatorios),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
