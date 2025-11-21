package com.grupo2.ashley.chat.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.grupo2.ashley.R

@Composable
fun FullScreenMediaViewer(
    imageUrl: String? = null,
    videoUrl: String? = null,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Mostrar imagen
            if (imageUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = stringResource(R.string.imagen_pantalla_completa),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onDismiss() }
                )
            }

            // Mostrar video
            if (videoUrl != null) {
                val context = LocalContext.current
                var isLoading by remember { mutableStateOf(true) }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                // Configurar MediaController para controles de reproducción
                                val mediaController = MediaController(ctx)
                                mediaController.setAnchorView(this)
                                setMediaController(mediaController)

                                // Configurar el video
                                setVideoURI(Uri.parse(videoUrl))

                                // IMPORTANTE: Habilitar audio
                                setOnPreparedListener { mp ->
                                    isLoading = false
                                    mp.isLooping = false
                                    // Asegurar que el volumen esté al máximo
                                    mp.setVolume(1.0f, 1.0f)
                                    android.util.Log.d("FullScreenVideo", "Video preparado con audio habilitado")
                                    // Iniciar reproducción automática
                                    start()
                                }

                                // Listener para errores
                                setOnErrorListener { _, what, extra ->
                                    isLoading = false
                                    android.util.Log.e("FullScreenVideo", "Error playing video: what=$what, extra=$extra")
                                    true
                                }
                            }
                        }
                    )

                    // Indicador de carga
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = Color.White
                        )
                    }
                }
            }

            // Botón de cerrar
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.volver),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
