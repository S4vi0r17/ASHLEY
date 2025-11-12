package com.grupo2.ashley.chat

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.grupo2.ashley.chat.components.ChatInputBar
import com.grupo2.ashley.chat.components.MessageBubble
import androidx.navigation.NavController
import com.grupo2.ashley.chat.ai.GeminiAIService
import com.grupo2.ashley.chat.components.ChatInputBar
import com.grupo2.ashley.chat.components.MessageBubble
import com.grupo2.ashley.chat.components.ProductChatHeader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRealtimeScreen(
    conversationId: String,
    currentUserId: String?,
    onNavigateBack: () -> Unit = {},
    onNavigateToParticipantInfo: () -> Unit = {},
    viewModel: ChatRealtimeViewModel = hiltViewModel()
    navController: NavController? = null
) {
    val messages by viewModel.messages.collectAsState()
    val participantInfo by viewModel.participantInfo.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()
    val productInfo by viewModel.productInfo.collectAsState()
    var text by remember { mutableStateOf("") }
    var isImprovingText by remember { mutableStateOf(false) }

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
            // Abrimos el stream de bytes de la imagen seleccionada
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            // Enviar la imagen al ViewModel
            if (bytes != null) {
                viewModel.sendMessage(
                    senderId = currentUserId,
                    text = "",
                    imageBytes = bytes
                )
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
                                contentDescription = "Profile picture",
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
                                text = participantInfo?.name ?: "Loading...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Optional: Add "Online" or "Last seen" status here
                            Text(
                                text = "Tap here for contact info",
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
                            contentDescription = "Back"
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
                        } else null
                    )
                }
            }

            ChatInputBar(
                text = text,
                onTextChange = { text = it },
                onSend = {
                    if (text.isNotBlank()) {
                        viewModel.sendMessage(
                            senderId = currentUserId,
                            text = text,
                            imageBytes = null // luego agregaremos envío de imágenes
                        )
                        text = ""
                    }
                },
                onPickImage = { imagePickerLauncher.launch("image/*") }, // 🖼️ Aquí se abre la galería
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
                                    "¡Texto mejorado con IA! ✨",
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
}