package com.grupo2.ashley.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.grupo2.ashley.chat.data.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnreadMessagesViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        startObservingUnreadMessages()
    }

    private fun startObservingUnreadMessages() {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                // Actualizar el contador cada cierto tiempo
                kotlinx.coroutines.delay(1000) // Esperar 1 segundo inicial
                while (true) {
                    try {
                        val count = chatRepository.getTotalUnreadCount(currentUserId)
                        _unreadCount.value = count
                    } catch (e: Exception) {
                        // Ignorar errores silenciosamente
                    }
                    kotlinx.coroutines.delay(5000) // Actualizar cada 5 segundos
                }
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                try {
                    val count = chatRepository.getTotalUnreadCount(currentUserId)
                    _unreadCount.value = count
                } catch (e: Exception) {
                    // Ignorar errores silenciosamente
                }
            }
        }
    }
}
