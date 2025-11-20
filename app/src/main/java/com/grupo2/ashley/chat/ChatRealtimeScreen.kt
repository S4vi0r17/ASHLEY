package com.grupo2.ashley.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.grupo2.ashley.chat.ai.GeminiAIService
import com.grupo2.ashley.chat.components.ChatInputBar
import com.grupo2.ashley.chat.components.FullScreenMediaViewer
import com.grupo2.ashley.chat.components.MessageBubble
import com.grupo2.ashley.chat.components.ProductChatHeader
import com.grupo2.ashley.R
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.min

// Comprime y redimensiona una imagen manteniendo la proporción
private fun compressImage(bytes: ByteArray, maxWidth: Int = 1024, maxHeight: Int = 1024, quality: Int = 80): ByteArray {
    val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // Calcular nuevo tamaño manteniendo la proporción
    val width = originalBitmap.width
    val height = originalBitmap.height
    val ratio = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)

    val newWidth = (width * ratio).toInt()
    val newHeight = (height * ratio).toInt()

    // Redimensionar
    val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

    // Comprimir
    val outputStream = ByteArrayOutputStream()
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    val compressedBytes = outputStream.toByteArray()

    // Liberar recursos
    originalBitmap.recycle()
    resizedBitmap.recycle()
    outputStream.close()

    android.util.Log.d("ChatScreen", "Image compressed: ${bytes.size} -> ${compressedBytes.size} bytes")
    return compressedBytes
}

// Extrae el primer fotograma de un video como miniatura
private fun extractVideoFrame(context: android.content.Context, uri: android.net.Uri): ByteArray? {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        // Extraer frame en el segundo 0
        val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

        if (bitmap != null) {
            // Redimensionar para preview
            val maxWidth = 400
            val maxHeight = 400
            val width = bitmap.width
            val height = bitmap.height
            val ratio = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()

            bitmap.recycle()
            resizedBitmap.recycle()
            outputStream.close()

            android.util.Log.d("ChatScreen", "Video frame extracted: ${bytes.size} bytes")
            return bytes
        }
    } catch (e: Exception) {
        android.util.Log.e("ChatScreen", "Error extracting video frame", e)
    } finally {
        retriever.release()
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
// Pantalla principal de chat en tiempo real con mensajes, medios y estado de escritura
@Composable
fun ChatRealtimeScreen(
    conversationId: String,
    currentUserId: String?,
    onNavigateBack: () -> Unit = {},
    onNavigateToParticipantInfo: () -> Unit = {},
    viewModel: ChatRealtimeViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val messages by viewModel.messages.collectAsState()
    val participantInfo by viewModel.participantInfo.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()
    val productInfo by viewModel.productInfo.collectAsState()
    val isOtherUserTyping by viewModel.isOtherUserTyping.collectAsState()
    val pendingImageBytes by viewModel.pendingImageBytes.collectAsState()
    val pendingVideoBytes by viewModel.pendingVideoBytes.collectAsState()
    val pendingVideoThumbnail by viewModel.pendingVideoThumbnail.collectAsState()
    var text by remember { mutableStateOf("") }
    var isImprovingText by remember { mutableStateOf(false) }

    // Estados para pantalla completa de media
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUrl by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    val scope = rememberCoroutineScope()
    val geminiService = remember { GeminiAIService() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            android.util.Log.d("ChatScreen", "Image selected: $uri")
            // Abrimos el stream de bytes de la imagen seleccionada
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            // Comprimir y guardar la imagen como pendiente para preview
            if (bytes != null) {
                try {
                    val compressedBytes = compressImage(bytes)
                    android.util.Log.d("ChatScreen", "Image compressed and set as pending preview: ${compressedBytes.size} bytes")
                    viewModel.setPendingImage(compressedBytes)
                } catch (e: Exception) {
                    android.util.Log.e("ChatScreen", "Error compressing image", e)
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.error_imagen),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                android.util.Log.e("ChatScreen", "Failed to read image bytes")
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            android.util.Log.d("ChatScreen", "Video selected: $uri")
            // Abrimos el stream de bytes del video seleccionado
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            // Guardar el video como pendiente para preview
            if (bytes != null) {
                try {
                    // Extraer fotograma del video para el preview
                    val thumbnail = extractVideoFrame(context, uri)
                    android.util.Log.d("ChatScreen", "Video set as pending preview: ${bytes.size} bytes, thumbnail: ${thumbnail?.size} bytes")
                    viewModel.setPendingVideo(bytes, thumbnail)
                } catch (e: Exception) {
                    android.util.Log.e("ChatScreen", "Error processing video", e)
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.error_video),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                android.util.Log.e("ChatScreen", "Failed to read video bytes")
            }
        }
    }

    LaunchedEffect(conversationId) {
        viewModel.startListening(conversationId)
        viewModel.loadParticipantInfo(conversationId, currentUserId)
        viewModel.markAsRead(currentUserId)
    }

    // Marcar mensajes como leídos cuando se abre el chat
    LaunchedEffect(messages) {
        if (messages.isNotEmpty() && currentUserId != null) {
            viewModel.markMessagesAsRead(currentUserId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onNavigateToParticipantInfo)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Profile picture
                        if (!participantInfo?.photoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = participantInfo?.photoUrl,
                                contentDescription = stringResource(R.string.foto_perfil),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Participant name
                        Column {
                            Text(
                                text = participantInfo?.name ?: stringResource(R.string.cargando),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Optional: Add "Online" or "Last seen" status here
                            Text(
                                text = stringResource(R.string.tocar_contacto),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.volver)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Subtle divider under top bar
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            // Mostrar información del producto si existe
            productInfo?.let { product ->
                ProductChatHeader(
                    productInfo = product,
                    onProductClick = {
                        // Navegar a los detalles del producto
                        navController?.navigate("productDetail/${product.productId}")
                    }
                )
            }

            val displayedMessages = messages.reversed()

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true // último mensaje al fondo
            ) {
                items(displayedMessages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        isOwnMessage = msg.senderId == currentUserId,
                        onDelete = if (msg.senderId == currentUserId) {
                            { viewModel.deleteMessage(msg.id) }
                        } else null,
                        onRetry = if (msg.status == com.grupo2.ashley.chat.models.MessageStatus.FAILED) {
                            { viewModel.retryMessage(msg.id) }
                        } else null,
                        onImageClick = { imageUrl ->
                            fullScreenImageUrl = imageUrl
                        },
                        onVideoClick = { videoUrl ->
                            fullScreenVideoUrl = videoUrl
                        }
                    )
                }
            }

            // Indicador "escribiendo..."
            androidx.compose.animation.AnimatedVisibility(
                visible = isOtherUserTyping,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = stringResource(R.string.escribiendo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Puntos animados
                    repeat(3) { index ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay((index * 200).toLong())
                            while (true) {
                                visible = true
                                kotlinx.coroutines.delay(200)
                                visible = false
                                kotlinx.coroutines.delay(400)
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = visible,
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut()
                        ) {
                            Text(
                                text = ".",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            ChatInputBar(
                text = text,
                onTextChange = { newText ->
                    text = newText
                    viewModel.onTextChanged(newText)
                },
                onSend = {
                    if (text.isNotBlank() || pendingImageBytes != null || pendingVideoBytes != null) {
                        viewModel.sendMessage(
                            senderId = currentUserId,
                            text = text,
                            imageBytes = pendingImageBytes,
                            videoBytes = pendingVideoBytes
                        )
                        text = ""
                        viewModel.clearPendingMedia()
                    }
                },
                onPickImage = { imagePickerLauncher.launch("image/*") }, // 🖼️ Aquí se abre la galería
                onPickVideo = { videoPickerLauncher.launch("video/*") }, // 🎥 Aquí se abre la galería de videos
                pendingImageBytes = pendingImageBytes,
                pendingVideoBytes = pendingVideoBytes,
                pendingVideoThumbnail = pendingVideoThumbnail,
                onClearPendingMedia = { viewModel.clearPendingMedia() },
                onImproveWithAI = {
                    if (!geminiService.isConfigured()) {
                        // Mostrar mensaje de que necesita configurar la API key
                        android.widget.Toast.makeText(
                            context,
                            "Por favor configura la API key de Gemini AI en GeminiAIService.kt",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        return@ChatInputBar
                    }

                    isImprovingText = true
                    scope.launch {
                        try {
                            val result = geminiService.improveMessage(text)
                            result.onSuccess { improvedText ->
                                text = improvedText
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.ia_mejora),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { error ->
                                android.widget.Toast.makeText(
                                    context,
                                    "Error: ${error.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } finally {
                            isImprovingText = false
                        }
                    }
                },
                isSending = isSending,
                isImprovingText = isImprovingText
            )
        }
    }

    // Mostrar pantalla completa de imagen si está definida
    if (fullScreenImageUrl != null) {
        FullScreenMediaViewer(
            imageUrl = fullScreenImageUrl,
            onDismiss = { fullScreenImageUrl = null }
        )
    }

    // Mostrar pantalla completa de video si está definido
    if (fullScreenVideoUrl != null) {
        FullScreenMediaViewer(
            videoUrl = fullScreenVideoUrl,
            onDismiss = { fullScreenVideoUrl = null }
        )
    }
}