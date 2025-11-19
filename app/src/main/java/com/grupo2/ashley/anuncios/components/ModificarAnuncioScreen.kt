package com.grupo2.ashley.anuncios.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grupo2.ashley.product.models.Product
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.grupo2.ashley.R
import com.grupo2.ashley.anuncios.AnunciosViewModel
import com.grupo2.ashley.map.UbicacionViewModel
import com.grupo2.ashley.navigation.Routes
import com.grupo2.ashley.product.models.ProductCategory
import com.grupo2.ashley.product.models.ProductCondition
import kotlin.text.ifEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModificarAnuncioScreen(
    product: Product,
    onBackClick: () -> Unit,
    bottomPadding: Dp = 0.dp,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: ModificarAnuncioViewModel = viewModel(),
    ubicacionViewModel: UbicacionViewModel,
    anunciosViewModel: AnunciosViewModel,
    navController: NavHostController
) {
    val selectedImages by viewModel.selectedImages.collectAsState()
    val title by viewModel.title.collectAsState()
    val brand by viewModel.brand.collectAsState()
    val category by viewModel.category.collectAsState()
    val condition by viewModel.condition.collectAsState()
    val price by viewModel.price.collectAsState()
    val description by viewModel.description.collectAsState()
    val images = product.images
    val deliveryLocationName by viewModel.deliveryLocationName.collectAsState()
    val deliveryAddress by viewModel.deliveryAddress.collectAsState()
    val useDefaultLocation by viewModel.useDefaultLocation.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val context = LocalContext.current

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImage(uris)
        }
    }

    var expandedCategory by remember { mutableStateOf(false) }
    var expandedCondition by remember { mutableStateOf(false) }

    val ubicacionMapa by ubicacionViewModel.ubicacionSeleccionada.collectAsState()
    val direccionMapa by ubicacionViewModel.direccionSeleccionada.collectAsState()
    val nombreUbicacionMapa by ubicacionViewModel.nombreUbicacion.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setProduct(product)
        viewModel.loadSelectedImages(images)
        viewModel.loadProductValues(product)
        viewModel.loadProductLocation(product)
    }

    LaunchedEffect(ubicacionMapa, direccionMapa, nombreUbicacionMapa) {
        if (!useDefaultLocation && direccionMapa != "Sin dirección seleccionada") {
            viewModel.updateDeliveryLocation(
                latitude = ubicacionMapa.latitude,
                longitude = ubicacionMapa.longitude,
                address = direccionMapa,
                locationName = nombreUbicacionMapa.ifEmpty { "Ubicación personalizada" }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.volver)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
                Text(
                    text = stringResource(R.string.editar_producto),
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
                                onClick = { viewModel.removeImage(uri) },
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
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text(stringResource(R.string.titulo_producto)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Marca
                OutlinedTextField(
                    value = brand,
                    onValueChange = { viewModel.updateBrand(it) },
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
                                    viewModel.updateCategory(cat.id)
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
                                    viewModel.updateCondition(cond)
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
                            viewModel.updatePrice(it)
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
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text(stringResource(R.string.descripcion)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        onCheckedChange = { viewModel.toggleUseDefaultLocation() }
                    )
                    Text(stringResource(R.string.usar_direccion_perfil))
                }
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
                    onClick = { viewModel.modifyProduct(context) },
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
                        Text(if (uploadState.isUploadingImages) stringResource(R.string.subiendo_imagen) else stringResource(R.string.modificando))
                    } else {
                        Text(stringResource(R.string.editar_producto), style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Espaciado final para que el último elemento no quede tapado por la barra de navegación
                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }

        if (uploadState.success) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.loadSelectedImages(product.images)
                    viewModel.loadProductLocation(product)
                    viewModel.loadProductValues(product)
                    anunciosViewModel.refreshProducts()
                },
                title = { Text(stringResource(R.string.producto_editado)) },
                text = { Text(stringResource(R.string.editado_satisfactoriamente)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetUploadState()
                        anunciosViewModel.refreshProducts()
                        navController.navigate(Routes.ANUNCIOS) {
                            popUpTo(Routes.ANUNCIOS) { inclusive = true }
                        }
                    }) {
                        Text(stringResource(R.string.aceptar))
                    }
                }
            )
        }
    }
