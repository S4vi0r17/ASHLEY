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
     * Starts listening to conversations in real-time with participant info
     * @param userId The user ID to filter conversations
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
                }
                .collectLatest { conversationList ->
                    // Load participant info for each conversation
                    val enrichedConversations = conversationList.map { conversation ->
                        val participantIds = conversation.participants.filter { it != userId }
                        if (participantIds.isNotEmpty()) {
                            val userProfiles = userRepository.getUserProfiles(participantIds)
                            // Convert UserProfile to ParticipantInfo
                            val participantsInfo = userProfiles.mapValues { (_, profile) ->
                                com.grupo2.ashley.chat.models.ParticipantInfo(
                                    name = "${profile.firstName} ${profile.lastName}".trim(),
                                    photoUrl = profile.profileImageUrl.takeIf { it.isNotEmpty() }
                                )
                            }
                            conversation.copy(participantsInfo = participantsInfo)
                        } else {
                            conversation
                        }
                    }
                    _conversations.value = enrichedConversations
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