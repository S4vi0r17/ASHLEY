package com.grupo2.ashley.chat

import androidx.lifecycle.ViewModel
import com.grupo2.ashley.chat.data.ChatListRepository
import com.grupo2.ashley.chat.models.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatListViewModel(
    private val repo: ChatListRepository = ChatListRepository()
): ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    fun loadConversations(userId: String?) {
        repo.getUserConversations(userId) { list ->
            _conversations.value = list
        }
    }
}