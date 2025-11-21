package com.grupo2.ashley.chat.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.grupo2.ashley.R

@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var showPlayButton by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    // Solo configurar MediaController si NO hay onClick (modo mini-player)
                    if (onClick == null) {
                        val mediaController = MediaController(ctx)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                    }

                    // Configurar el video
                    setVideoURI(Uri.parse(videoUrl))

                    // Listener para cuando el video está listo
                    setOnPreparedListener { mp ->
                        isLoading = false
                        showPlayButton = true
                        mp.isLooping = false
                    }

                    // Listener para cuando comienza a reproducirse
                    // Solo si no hay onClick callback
                    if (onClick == null) {
                        setOnClickListener {
                            if (isPlaying) {
                                pause()
                                showPlayButton = true
                            } else {
                                start()
                                showPlayButton = false
                            }
                        }
                    }

                    // Listener para errores
                    setOnErrorListener { _, what, extra ->
                        isLoading = false
                        showPlayButton = true
                        android.util.Log.e("VideoPlayer", "Error playing video: what=$what, extra=$extra")
                        true
                    }
                }
            },
            update = { videoView ->
                // Actualizar el video si la URL cambia
                if (videoView.isPlaying.not()) {
                    videoView.setVideoURI(Uri.parse(videoUrl))
                }
            }
        )

        // Indicador de carga
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Botón de play superpuesto
        if (showPlayButton && !isLoading) {
            IconButton(
                onClick = { showPlayButton = false },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.reproducir_video),
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
