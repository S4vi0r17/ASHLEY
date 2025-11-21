package com.grupo2.ashley.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo2.ashley.chat.data.ChatRepository
import com.grupo2.ashley.chat.data.ChatUserRepository
import com.grupo2.ashley.chat.models.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.grupo2.ashley.chat.data.ChatListRepository
import com.grupo2.ashley.chat.models.ConversationWithUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatListUiState(
    val conversations: List<ConversationWithUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: ChatUserRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentUserId: String? = null

    /**
     * Se utiliza para escuchar las conversaciones en tiempo real
     * Se usa userId para filtrar desde el primer hijo en participantes :)
     */
    fun startListening(userId: String?) {
        if (userId == null) return
        currentUserId = userId
        _isLoading.value = true

        viewModelScope.launch {
            chatRepository.observeConversations(userId)
                .catch { e ->
                    Log.e("ChatListVM", "Error observing conversations", e)
                    _error.value = "Error al cargar conversaciones"
                    _isLoading.value = false
                    _uiState.value = ChatListUiState(error = "Error al cargar conversaciones")
                }
                .collectLatest { conversationList ->
                    val enrichedConversations = conversationList.map { conversation ->
                        val participantIds = conversation.participants.filter { it != userId }
                        val otherUserId = participantIds.firstOrNull() ?: ""
                        val userProfile = if (otherUserId.isNotEmpty()) {
                            try {
                                userRepository.getUserProfiles(listOf(otherUserId))[otherUserId]
                            } catch (e: Exception) {
                                Log.e("ChatListVM", "Error loading user profile", e)
                                null
                            }
                        } else null

                        // Convertir a ConversationWithUser
                        ConversationWithUser(
                            conversationId = conversation.id,
                            otherUserId = otherUserId,
                            otherUserName = if (userProfile != null) {
                                "${userProfile.firstName} ${userProfile.lastName}".trim().ifEmpty { "Usuario" }
                            } else "Usuario",
                            otherUserImageUrl = userProfile?.profileImageUrl ?: "",
                            lastMessage = conversation.lastMessage,
                            isOnline = false,
                            unreadCount = 0,
                            productInfo = null,
                            isBlocked = conversation.isBlocked
                        )
                    }
                    _uiState.value = ChatListUiState(conversations = enrichedConversations)
                    _isLoading.value = false
                }
        }
    }

    fun createConversation(userId1: String, userId2: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.createOrGetConversation(userId1, userId2)
            result.onSuccess { conversationId ->
                onResult(conversationId)
            }.onFailure { e ->
                Log.e("ChatListVM", "Error creating conversation", e)
                _error.value = "Error al crear conversación"
                onResult(null)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}