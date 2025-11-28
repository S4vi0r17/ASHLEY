package com.grupo2.ashley.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo2.ashley.chat.data.ChatRepository
import com.grupo2.ashley.chat.data.ChatUserRepository
import com.grupo2.ashley.chat.models.Message
import com.grupo2.ashley.chat.models.MessageStatus
import com.grupo2.ashley.chat.models.ParticipantInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import com.grupo2.ashley.chat.models.ProductInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatRealtimeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: ChatUserRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _participantInfo = MutableStateFlow<ParticipantInfo?>(null)
    val participantInfo: StateFlow<ParticipantInfo?> = _participantInfo.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _productInfo = MutableStateFlow<ProductInfo?>(null)
    val productInfo: StateFlow<ProductInfo?> = _productInfo.asStateFlow()

    private val _isOtherUserTyping = MutableStateFlow(false)
    val isOtherUserTyping: StateFlow<Boolean> = _isOtherUserTyping.asStateFlow()

    private val _pendingImageBytes = MutableStateFlow<ByteArray?>(null)
    val pendingImageBytes: StateFlow<ByteArray?> = _pendingImageBytes.asStateFlow()

    private val _pendingVideoBytes = MutableStateFlow<ByteArray?>(null)
    val pendingVideoBytes: StateFlow<ByteArray?> = _pendingVideoBytes.asStateFlow()

    private val _pendingVideoThumbnail = MutableStateFlow<ByteArray?>(null)
    val pendingVideoThumbnail: StateFlow<ByteArray?> = _pendingVideoThumbnail.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked.asStateFlow()

    private var currentConversationId: String? = null
    private var currentUserId: String? = null
    private var otherUserId: String? = null
    private var currentOffset = 0
    private val pageSize = 50
    private var typingJob: kotlinx.coroutines.Job? = null

    // Inicia la escucha de mensajes en tiempo real para una conversación
    fun startListening(conversationId: String) {
        // Si ya estamos escuchando esta conversación, solo actualizar el estado de mute y block
        if (currentConversationId == conversationId) {
            Log.d("ChatVM", "Already listening to $conversationId, refreshing mute and block state only")
            loadMuteState(conversationId)
            loadBlockState(conversationId)
            return
        }

        currentConversationId = conversationId
        currentOffset = 0

        // Establezca esta conversación como activa para evitar notificaciones
        (chatRepository as? com.grupo2.ashley.chat.data.ChatRepositoryImpl)?.setActiveConversation(conversationId)

        viewModelScope.launch {
            chatRepository.observeMessages(conversationId)
                .catch { e ->
                    Log.e("ChatVM", "Error observing messages", e)
                    _error.value = "Error al cargar mensajes"
                }
                .collectLatest { messageList ->
                    _messages.value = messageList
                }
        }

        // Cargar información del producto
        loadProductInfo(conversationId)

        // Cargar estado de silenciado
        loadMuteState(conversationId)

        // Cargar estado de bloqueo
        loadBlockState(conversationId)
    }

    // Carga información del producto asociado a la conversación
    private fun loadProductInfo(conversationId: String) {
        viewModelScope.launch {
            try {
                val product = chatRepository.getProductInfoForConversation(conversationId)
                _productInfo.value = product
            } catch (e: Exception) {
                Log.e("ChatVM", "Error loading product info", e)
            }
        }
    }

    // Marca los mensajes de la conversación actual como leídos
    fun markMessagesAsRead(currentUserId: String) {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(conversationId, currentUserId)
        }
    }

    // Detiene la escucha de mensajes y limpia el estado de conversación activa
    fun stopListening() {
        // Clear active conversation when leaving the chat
        (chatRepository as? com.grupo2.ashley.chat.data.ChatRepositoryImpl)?.setActiveConversation(null)
    }

    override fun onCleared() {
        super.onCleared()
        // Clear active conversation when leaving the chat
        (chatRepository as? com.grupo2.ashley.chat.data.ChatRepositoryImpl)?.setActiveConversation(null)
    }

    // Carga más mensajes antiguos con paginación
    fun loadMoreMessages() {
        val conversationId = currentConversationId ?: return
        if (_isLoadingMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                currentOffset += pageSize
                // Pagination is handled by Room Flow - just for future use
                _isLoadingMore.value = false
            } catch (e: Exception) {
                Log.e("ChatVM", "Error loading more messages", e)
                _isLoadingMore.value = false
            }
        }
    }

    // Envía un mensaje con texto, imagen o video
    fun sendMessage(
        senderId: String?,
        text: String = "",
        imageBytes: ByteArray? = null,
        videoBytes: ByteArray? = null
    ) {
        val conversationId = currentConversationId ?: return
        if (senderId == null) return
        if (text.isBlank() && imageBytes == null && videoBytes == null) return

        Log.d("ChatVM", "sendMessage called: conversationId=$conversationId, hasImage=${imageBytes != null}, hasVideo=${videoBytes != null}")

        viewModelScope.launch {
            _isSending.value = true
            _error.value = null

            val result = chatRepository.sendMessage(
                conversationId = conversationId,
                senderId = senderId,
                text = text.trim(),
                imageBytes = imageBytes,
                videoBytes = videoBytes
            )

            _isSending.value = false

            result.onSuccess { message ->
                Log.d("ChatVM", "Message sent successfully: imageUrl=${message.imageUrl}, videoUrl=${message.videoUrl}")
            }

            result.onFailure { e ->
                _error.value = "Error al enviar mensaje"
                Log.e("ChatVM", "Error sending message", e)
            }
        }
    }

    // Reintenta enviar un mensaje que falló
    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.retryFailedMessage(messageId)
        }
    }

    // Elimina un mensaje de la conversación
    fun deleteMessage(messageId: String) {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId, conversationId)
        }
    }

    // Actualiza el estado de escritura del usuario en Firebase
    fun updateTypingStatus(userId: String, isTyping: Boolean) {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            chatRepository.updateTypingStatus(conversationId, userId, isTyping)
        }
    }

    // Marca la conversación y todos sus mensajes como leídos
    fun markAsRead(currentUserId: String?) {
        val conversationId = currentConversationId ?: return
        if (currentUserId == null) return
        viewModelScope.launch {
            // Mark conversation as read (clears unread count)
            chatRepository.markConversationAsRead(conversationId)
            // Mark all messages from other users as READ
            chatRepository.markMessagesAsRead(conversationId, currentUserId)
        }
    }

    // Carga la información del otro participante de la conversación
    fun loadParticipantInfo(conversationId: String, currentUserId: String?) {
        if (currentUserId == null) return
        this.currentUserId = currentUserId

        viewModelScope.launch {
            try {
                // Get conversation to find the other participant
                val conversation = chatRepository.observeConversations(currentUserId)
                    .first()
                    .find { it.id == conversationId }

                if (conversation != null) {
                    val otherUserIdFound = conversation.participants.firstOrNull { it != currentUserId }
                    otherUserId = otherUserIdFound

                    if (otherUserIdFound != null) {
                        val userProfiles = userRepository.getUserProfiles(listOf(otherUserIdFound))
                        val profile = userProfiles[otherUserIdFound]
                        if (profile != null) {
                            _participantInfo.value = ParticipantInfo(
                                name = "${profile.firstName} ${profile.lastName}".trim(),
                                photoUrl = profile.profileImageUrl.takeIf { it.isNotEmpty() },
                                email = profile.email,
                                phoneNumber = profile.phoneNumber
                            )
                        }

                        // Observar estado de typing del otro usuario
                        observeOtherUserTyping(conversationId, otherUserIdFound)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Error loading participant info", e)
            }
        }
    }

    // Observa el estado de escritura del otro usuario en tiempo real
    private fun observeOtherUserTyping(conversationId: String, otherUserId: String) {
        viewModelScope.launch {
            chatRepository.observeTypingStatus(conversationId, otherUserId)
                .collectLatest { isTyping ->
                    _isOtherUserTyping.value = isTyping
                }
        }
    }

    // Maneja cambios en el texto del input y actualiza el indicador de escritura
    fun onTextChanged(text: String) {
        val conversationId = currentConversationId ?: return
        val userId = currentUserId ?: return

        // Cancelar el job anterior
        typingJob?.cancel()

        // Si hay texto, marcar como escribiendo
        if (text.isNotEmpty()) {
            viewModelScope.launch {
                chatRepository.updateTypingStatus(conversationId, userId, true)
            }

            // Después de 3 segundos de inactividad, quitar el estado de escribiendo
            typingJob = viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                chatRepository.updateTypingStatus(conversationId, userId, false)
            }
        } else {
            // Si el texto está vacío, quitar el estado inmediatamente
            viewModelScope.launch {
                chatRepository.updateTypingStatus(conversationId, userId, false)
            }
        }
    }

    // Limpia el mensaje de error actual
    fun clearError() {
        _error.value = null
    }

    // Establece una imagen pendiente para enviar
    fun setPendingImage(bytes: ByteArray) {
        _pendingImageBytes.value = bytes
        _pendingVideoBytes.value = null // Clear video if image is selected
        _pendingVideoThumbnail.value = null
    }

    // Establece un video pendiente para enviar con su miniatura
    fun setPendingVideo(bytes: ByteArray, thumbnail: ByteArray?) {
        _pendingVideoBytes.value = bytes
        _pendingVideoThumbnail.value = thumbnail
        _pendingImageBytes.value = null // Clear image if video is selected
    }

    // Limpia todos los medios pendientes (imagen/video)
    fun clearPendingMedia() {
        _pendingImageBytes.value = null
        _pendingVideoBytes.value = null
        _pendingVideoThumbnail.value = null
    }

    // Carga el estado de silenciado de la conversación
    private fun loadMuteState(conversationId: String) {
        viewModelScope.launch {
            try {
                val isMuted = chatRepository.isConversationMuted(conversationId)
                _isMuted.value = isMuted
                Log.d("ChatVM", "Loaded mute state for $conversationId: isMuted=$isMuted")
            } catch (e: Exception) {
                Log.e("ChatVM", "Error loading mute state", e)
            }
        }
    }

    // Carga el estado de bloqueo de la conversación
    private fun loadBlockState(conversationId: String) {
        viewModelScope.launch {
            try {
                val isBlocked = chatRepository.isConversationBlocked(conversationId)
                _isBlocked.value = isBlocked
                Log.d("ChatVM", "Loaded block state for $conversationId: isBlocked=$isBlocked")
            } catch (e: Exception) {
                Log.e("ChatVM", "Error loading block state", e)
            }
        }
    }

    // Silencia la conversación actual
    fun muteConversation() {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            try {
                chatRepository.muteConversation(conversationId)
                _isMuted.value = true
                Log.d("ChatVM", "Conversation $conversationId MUTED successfully")
            } catch (e: Exception) {
                Log.e("ChatVM", "Error muting conversation", e)
                _error.value = "Error al silenciar conversación"
            }
        }
    }

    // Activa las notificaciones de la conversación actual
    fun unmuteConversation() {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            try {
                chatRepository.unmuteConversation(conversationId)
                _isMuted.value = false
                Log.d("ChatVM", "Conversation $conversationId UNMUTED successfully")
            } catch (e: Exception) {
                Log.e("ChatVM", "Error unmuting conversation", e)
                _error.value = "Error al activar notificaciones"
            }
        }
    }

    // Alterna el estado de silenciado
    fun toggleMute() {
        if (_isMuted.value) {
            unmuteConversation()
        } else {
            muteConversation()
        }
    }

    // Bloquea la conversación actual
    fun blockConversation() {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            try {
                chatRepository.blockConversation(conversationId)
                _isBlocked.value = true
                Log.d("ChatVM", "Conversation $conversationId BLOCKED successfully")
            } catch (e: Exception) {
                Log.e("ChatVM", "Error blocking conversation", e)
                _error.value = "Error al bloquear conversación"
            }
        }
    }

    // Desbloquea la conversación actual
    fun unblockConversation() {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            try {
                chatRepository.unblockConversation(conversationId)
                _isBlocked.value = false
                Log.d("ChatVM", "Conversation $conversationId UNBLOCKED successfully")
            } catch (e: Exception) {
                Log.e("ChatVM", "Error unblocking conversation", e)
                _error.value = "Error al desbloquear conversación"
            }
        }
    }

    // Alterna el estado de bloqueo
    fun toggleBlock() {
        if (_isBlocked.value) {
            unblockConversation()
        } else {
            blockConversation()
        }
    }

    // Archiva la conversación actual (oculta sin eliminar)
    fun deleteCurrentConversation(onSuccess: () -> Unit) {
        val conversationId = currentConversationId ?: return
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                // Usar archiveConversation en lugar de deleteConversation
                chatRepository.archiveConversation(conversationId, userId)
                Log.d("ChatVM", "Conversation archived successfully")
                // Detener la escucha
                stopListening()
                // Notificar éxito
                onSuccess()
            } catch (e: Exception) {
                Log.e("ChatVM", "Error archiving conversation", e)
                _error.value = "Error al archivar la conversación"
            }
        }
    }
}