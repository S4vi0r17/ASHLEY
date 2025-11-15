package com.grupo2.ashley.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.grupo2.ashley.home.HomeViewModel
import com.grupo2.ashley.map.UbicacionViewModel
import com.grupo2.ashley.navigation.Routes
import com.grupo2.ashley.product.ProductViewModel
import com.grupo2.ashley.product.models.ProductCategory
import com.grupo2.ashley.product.models.ProductCondition
import com.grupo2.ashley.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenderScreen(
    navController: NavHostController,
    viewModel: UbicacionViewModel,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    productViewModel: ProductViewModel = viewModel(),
    homeViewModel: HomeViewModel
) {
    val title by productViewModel.title.collectAsState()
    val brand by productViewModel.brand.collectAsState()
    val category by productViewModel.category.collectAsState()
    val condition by productViewModel.condition.collectAsState()
    val price by productViewModel.price.collectAsState()
    val description by productViewModel.description.collectAsState()
    val selectedImages by productViewModel.selectedImages.collectAsState()
    val deliveryLocationName by productViewModel.deliveryLocationName.collectAsState()
    val deliveryAddress by productViewModel.deliveryAddress.collectAsState()
    val useDefaultLocation by productViewModel.useDefaultLocation.collectAsState()
    val uploadState by productViewModel.uploadState.collectAsState()

    // Sincronizar ubicación del mapa
    val ubicacionMapa by viewModel.ubicacionSeleccionada.collectAsState()
    val direccionMapa by viewModel.direccionSeleccionada.collectAsState()
    val nombreUbicacionMapa by viewModel.nombreUbicacion.collectAsState()

    LaunchedEffect(ubicacionMapa, direccionMapa, nombreUbicacionMapa) {
        if (!useDefaultLocation && direccionMapa != "Sin dirección seleccionada") {
            productViewModel.updateDeliveryLocation(
                latitude = ubicacionMapa.latitude,
                longitude = ubicacionMapa.longitude,
                address = direccionMapa,
                locationName = nombreUbicacionMapa.ifEmpty { "Ubicación personalizada" }
            )
        }
    }

    var expandedCategory by remember { mutableStateOf(false) }
    var expandedCondition by remember { mutableStateOf(false) }

    // Launcher para seleccionar múltiples imágenes
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            productViewModel.setSelectedImages(uris)
        }
    }

    // Mostrar diálogo de éxito
    LaunchedEffect(uploadState.success) {
        if (uploadState.success) {
            // Aquí podrías mostrar un snackbar o navegar a otra pantalla
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.publicar_producto),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

        Spacer(modifier = Modifier.height(24.dp))

        // Sección de imágenes
        Text(
            text = stringResource(R.string.fotos_producto),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.limite_fotos),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón para agregar imágenes
            if (selectedImages.size < 5) {
                item {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                multiplePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.agregar_imagen),
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Imágenes seleccionadas
            items(selectedImages) { uri ->
                Box(
                    modifier = Modifier.size(120.dp)
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = stringResource(R.string.imagen_producto),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { productViewModel.removeImage(uri) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.eliminar_imagen),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Título del producto
        OutlinedTextField(
            value = title,
            onValueChange = { productViewModel.updateTitle(it) },
            label = { Text(stringResource(R.string.titulo_producto)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Marca
        OutlinedTextField(
            value = brand,
            onValueChange = { productViewModel.updateBrand(it) },
            label = { Text(stringResource(R.string.marca)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Categoría
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = ProductCategory.categories
                        .find { it.id == category }
                        ?.let { stringResource(it.labelResId) }
                        ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.categoria)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    ProductCategory.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(stringResource(cat.labelResId)) },
                            onClick = {
                                productViewModel.updateCategory(cat.id)
                                expandedCategory = false
                            }
                        )
                    }
                }
            }


        Spacer(modifier = Modifier.height(16.dp))

        // Condición
            ExposedDropdownMenuBox(
                expanded = expandedCondition,
                onExpandedChange = { expandedCondition = !expandedCondition }
            ) {
                OutlinedTextField(
                    value = stringResource(condition.labelResId),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.condicion)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCondition)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedCondition,
                    onDismissRequest = { expandedCondition = false }
                ) {
                    ProductCondition.entries.forEach { cond ->
                        DropdownMenuItem(
                            text = { Text(stringResource(cond.labelResId)) },
                            onClick = {
                                productViewModel.updateCondition(cond)
                                expandedCondition = false
                            }
                        )
                    }
                }
            }


        Spacer(modifier = Modifier.height(16.dp))

        // Precio
        OutlinedTextField(
            value = price,
            onValueChange = { 
                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                    productViewModel.updatePrice(it)
                }
            },
            label = { Text(stringResource(R.string.precio)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            prefix = { Text("S/. ") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Descripción
        OutlinedTextField(
            value = description,
            onValueChange = { productViewModel.updateDescription(it) },
            label = { Text(stringResource(R.string.descripcion)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Ubicación de entrega
        Text(
            text = stringResource(R.string.lugar_entrega),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = useDefaultLocation,
                onCheckedChange = { productViewModel.toggleUseDefaultLocation() }
            )
            Text(stringResource(R.string.usar_direccion_perfil))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mostrar ubicación si está configurada (predeterminada o personalizada)
        if (deliveryAddress.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (useDefaultLocation) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.ubicacion),
                        tint = if (useDefaultLocation) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // Mostrar el nombre de la ubicación
                        Text(
                            text = if (deliveryLocationName.isNotEmpty()) {
                                deliveryLocationName
                            } else if (useDefaultLocation) {
                                stringResource(R.string.direccion_perfil)
                            } else {
                                stringResource(R.string.ubicacion_personalizada)
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (useDefaultLocation) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Mostrar la dirección completa
                        Text(
                            text = deliveryAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (useDefaultLocation) 
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                            else 
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            // Mensaje cuando no hay ubicación seleccionada
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.sin_ubicacion),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.seleccionar_direccion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { navController.navigate(Routes.SELECCIONAR_UBICACION) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (useDefaultLocation) stringResource(R.string.cambiar_direccion) else stringResource(R.string.seleccionar_direccion))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Mostrar error si existe
        if (uploadState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = uploadState.error ?: "",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Botón publicar
        Button(
            onClick = { productViewModel.publishProduct() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !uploadState.isLoading
        ) {
            if (uploadState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uploadState.isUploadingImages) stringResource(R.string.subiendo_imagen) else stringResource(R.string.publicando))
            } else {
                Text(stringResource(R.string.publicar_producto), style = MaterialTheme.typography.titleMedium)
            }
        }

        // Espaciado final para que el último elemento no quede tapado por la barra de navegación
        Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
    }


    // Diálogo de éxito
    if (uploadState.success) {
        AlertDialog(
            onDismissRequest = { 
                productViewModel.resetUploadState()
                homeViewModel.refreshProducts() // Refrescar productos en home
            },
            title = { Text(stringResource(R.string.producto_publicado)) },
            text = { Text(stringResource(R.string.exito_publicar)) },
            confirmButton = {
                TextButton(onClick = {
                    productViewModel.resetUploadState()
                    homeViewModel.refreshProducts() // Refrescar productos en home
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }) {
                    Text(stringResource(R.string.aceptar))
                }
            }
        )
    }
    }
}
