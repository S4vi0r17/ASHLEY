package com.grupo2.ashley.chat.models

data class Message(
    var id: String = "",
    val senderId: String? = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val readAt: Long? = null
)

enum class MessageStatus {
    SENT,       // Enviado (✓)
    DELIVERED,  // Entregado (✓✓)
    READ        // Leído (✓✓ azul)
}
