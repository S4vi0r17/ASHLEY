package com.grupo2.ashley.anuncios.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.grupo2.ashley.R
import com.grupo2.ashley.anuncios.AnunciosViewModel
import com.grupo2.ashley.navigation.Routes
import com.grupo2.ashley.product.models.Product
import com.grupo2.ashley.product.models.ProductDeletedState
import com.grupo2.ashley.ui.components.GradientButton
import com.grupo2.ashley.ui.components.GradientTextButton
import com.grupo2.ashley.ui.theme.AnimationConstants
import com.grupo2.ashley.ui.theme.AppGradients

@SuppressLint("DefaultLocale")
@Composable
fun AnuncioCard(
    product: Product,
    onClick: () -> Unit,
    navController: NavHostController
) {
    val viewModel: ModificarAnuncioViewModel = viewModel()
    val anunciosViewModel : AnunciosViewModel = viewModel()
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(AnimationConstants.FLUID_DURATION),
        label = "product_card_scale"
    )
    val context = LocalContext.current
    val deletedState by viewModel.deletedState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onClick() }
            .animateContentSize(animationSpec = tween(AnimationConstants.FLUID_DURATION)),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp, pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {

                // --- IMAGEN ---
                if (product.images.firstOrNull() != null) {
                    AsyncImage(
                        model = product.images.firstOrNull(),
                        contentDescription = product.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // --- BOTÓN ELIMINAR ---
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(
                            color = Color(0xFFD32F2F), // rojo fuerte
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.eliminar),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                GradientTextButton(
                    onClick = {
                        viewModel.updateStateProduct(product.productId, !product.isActive)
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(width = 108.dp, height = 36.dp),
                    gradient = if (product.isActive) AppGradients.SuccessGradient else AppGradients.ErrorGradient,
                ) {
                    Text(
                        text = if (product.isActive) stringResource(R.string.activo) else stringResource(R.string.inactivo),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = product.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = product.deliveryAddress,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "S/. ${String.format("%.2f", product.price)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (deletedState.success) {
            AlertDialog(
                onDismissRequest = {
                    anunciosViewModel.refreshProducts()
                },
                title = { Text(stringResource(R.string.producto_eliminado)) },
                text = { Text(stringResource(R.string.producto_eliminado_satisfactoriamente)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetDeletedState()
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

        if(showDeleteDialog){
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                },
                title = { Text(stringResource(R.string.desea_eliminar_este_producto)) },
                text = { Text(stringResource(R.string.una_vez_eliminado_no_se_puede_recuperar))  },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteProductbyID(product.productId, product.images, context)
                    }) {
                        Text(stringResource(R.string.aceptar))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                    }) {
                        Text(stringResource(R.string.cancelar))
                    }
                }
            )
        }
    }
}

