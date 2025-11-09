package com.grupo2.ashley.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ValueEventListener
import com.grupo2.ashley.chat.data.ChatRealtimeRepository
import com.grupo2.ashley.chat.models.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatRealtimeViewModel(
    private val repo: ChatRealtimeRepository = ChatRealtimeRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var listener: ValueEventListener? = null
    private var currentConversationId: String? = null

    fun startListening(conversationId: String) {
        stopListening()
        currentConversationId = conversationId
        listener = repo.addMessagesListener(conversationId) { list ->
            _messages.value = list
        }
    }

    fun stopListening() {
        currentConversationId?.let { id ->
            listener?.let { repo.removeMessagesListener(id, it) }
        }
        listener = null
    }

    fun sendMessage(
        senderId: String?,
        text: String = "",
        imageBytes: ByteArray? = null
    ) {
        val conversationId = currentConversationId ?: return
        if (text.isBlank() && imageBytes == null) return // Evita mensajes vacíos

        val msg = Message(
            senderId = senderId,
            text = text.trim()
        )

        viewModelScope.launch {
            _isSending.value = true
            repo.sendMessage(conversationId, msg, imageBytes) { success ->
                _isSending.value = false
                if (!success) {
                    _error.value = "Error al enviar mensaje"
                    Log.e("ChatVM", "Error enviando mensaje")
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}