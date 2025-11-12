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

    private var currentConversationId: String? = null
    private var currentUserId: String? = null
    private var otherUserId: String? = null
    private var currentOffset = 0
    private val pageSize = 50
    private var typingJob: kotlinx.coroutines.Job? = null

    fun startListening(conversationId: String) {
        currentConversationId = conversationId
        currentOffset = 0

        // Set this conversation as active to prevent notifications
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
    }

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

    fun markMessagesAsRead(currentUserId: String) {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(conversationId, currentUserId)
        }
    }

    fun stopListening() {
        // Clear active conversation when leaving the chat
        (chatRepository as? com.grupo2.ashley.chat.data.ChatRepositoryImpl)?.setActiveConversation(null)
    }

    override fun onCleared() {
        super.onCleared()
        // Clear active conversation when leaving the chat
        (chatRepository as? com.grupo2.ashley.chat.data.ChatRepositoryImpl)?.setActiveConversation(null)
    }

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

    fun sendMessage(
        senderId: String?,
        text: String = "",
        imageBytes: ByteArray? = null,
        videoBytes: ByteArray? = null
    ) {
        val conversationId = currentConversationId ?: return
        if (senderId == null) return
        if (text.isBlank() && imageBytes == null && videoBytes == null) return

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

            result.onFailure { e ->
                _error.value = "Error al enviar mensaje"
                Log.e("ChatVM", "Error sending message", e)
            }
        }
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.retryFailedMessage(messageId)
        }
    }

    fun deleteMessage(messageId: String) {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId, conversationId)
        }
    }

    fun updateTypingStatus(userId: String, isTyping: Boolean) {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            chatRepository.updateTypingStatus(conversationId, userId, isTyping)
        }
    }

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

    private fun observeOtherUserTyping(conversationId: String, otherUserId: String) {
        viewModelScope.launch {
            chatRepository.observeTypingStatus(conversationId, otherUserId)
                .collectLatest { isTyping ->
                    _isOtherUserTyping.value = isTyping
                }
        }
    }

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

    fun clearError() {
        _error.value = null
    }
}