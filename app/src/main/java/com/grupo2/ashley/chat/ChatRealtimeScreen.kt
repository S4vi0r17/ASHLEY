package com.grupo2.ashley.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    viewModel: ChatRealtimeViewModel,
    conversationId: String,
    currentUserId: String?,
    navController: NavController? = null
) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val productInfo by viewModel.productInfo.collectAsState()
    var text by remember { mutableStateOf("") }
    var isImprovingText by remember { mutableStateOf(false) }

    val context = LocalContext.current
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
                title = { Text("Chat", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
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
                        isOwnMessage = msg.senderId == currentUserId
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