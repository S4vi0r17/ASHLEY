package com.grupo2.ashley.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grupo2.ashley.chat.models.Message
import com.grupo2.ashley.chat.models.MessageStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Burbuja de mensaje que muestra texto, imagen, video, estado y opciones de menú
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    onDelete: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null,
    onVideoClick: ((String) -> Unit)? = null
) {
    // Debug log
    android.util.Log.d("MessageBubble", "Message ${message.id}: imageUrl=${message.imageUrl}, videoUrl=${message.videoUrl}, mediaType=${message.mediaType}")

    var showMenu by remember { mutableStateOf(false) }
    val backgroundColor = if (isOwnMessage)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isOwnMessage)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    val alignment = if (isOwnMessage) Alignment.End else Alignment.Start
    val shape = if (isOwnMessage) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(color = backgroundColor)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (!message.isDeleted && (onDelete != null || onRetry != null)) {
                                showMenu = true
                            }
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 280.dp)
            ) {
                Column {
                    // Show deleted placeholder
                    if (message.isDeleted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Deleted",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Este mensaje fue eliminado",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Si el mensaje tiene imagen
                        val imageUrl = message.imageUrl
                        if (!imageUrl.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick?.invoke(imageUrl) }
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Imagen del mensaje",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 260.dp)
                                )
                            }

                            if (message.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // Si el mensaje tiene video
                        val videoUrl = message.videoUrl
                        if (!videoUrl.isNullOrEmpty()) {
                            VideoPlayer(
                                videoUrl = videoUrl,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .fillMaxWidth()
                                    .height(200.dp),
                                onClick = { onVideoClick?.invoke(videoUrl) }
                            )

                            if (message.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // Texto del mensaje
                        if (message.text.isNotBlank()) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Dropdown menu for actions
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                onRetry?.let {
                    DropdownMenuItem(
                        text = { Text("Reintentar envío") },
                        onClick = {
                            it()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                        }
                    )
                }
                onDelete?.let {
                    DropdownMenuItem(
                        text = { Text("Eliminar mensaje") },
                        onClick = {
                            it()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimestamp(message.timestamp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Show status indicator for own messages (only if not deleted)
            if (isOwnMessage && !message.isDeleted) {
                when (message.status) {
                    MessageStatus.PENDING -> {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Sending",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MessageStatus.SENT -> {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Sent",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MessageStatus.DELIVERED -> {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MessageStatus.READ -> {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    MessageStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Failed",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}