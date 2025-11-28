package com.grupo2.ashley.chat.models

data class Conversation(
    val id: String = "",
    val participants: Map<String, Boolean> = emptyMap(),
    val lastMessage: LastMessage? = null,
    val participantsInfo: Map<String, ParticipantInfo> = emptyMap(),
    val productId: String? = null,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false
)

data class LastMessage(
    val text: String = "",
    val timestamp: Long = 0L,
    val senderId: String? = "",
    val unreadCount: Int = 0
)

data class ParticipantInfo(
    val name: String = "",
    val photoUrl: String? = null,
    val email: String = "",
    val phoneNumber: String = "",
    val senderId: String = ""
)

// Información del producto para mostrar en el chat
data class ProductInfo(
    val productId: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val condition: String = "",
    val sellerId: String = ""
)

// Data class para la UI que incluye información más completa del otro usuario y producto
data class ConversationWithUser(
    val conversationId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserImageUrl: String = "",
    val lastMessage: LastMessage? = null,
    val isOnline: Boolean = false,
    val unreadCount: Int = 0,
    val productInfo: ProductInfo? = null,
    val isBlocked: Boolean = false
)
