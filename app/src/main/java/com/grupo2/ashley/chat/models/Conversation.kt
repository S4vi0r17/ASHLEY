package com.grupo2.ashley.chat.models

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: LastMessage? = null,
    // Runtime-only field for participant information (not stored in Firebase)
    val participantsInfo: Map<String, ParticipantInfo> = emptyMap(),
    val productId: String? = null
)

data class LastMessage(
    val text: String = "",
    val timestamp: Long = 0L,
    val senderId: String = "", // Track who sent the last message
    val unreadCount: Int = 0 // Count of unread messages for current user
)

/**
 * Information about a conversation participant
 * Loaded from Firestore users collection at runtime
 */
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

// Data class para la UI que incluye información del otro usuario y producto
data class ConversationWithUser(
    val conversationId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserImageUrl: String = "",
    val lastMessage: LastMessage? = null,
    val isOnline: Boolean = false,
    val unreadCount: Int = 0,
    val productInfo: ProductInfo? = null
)
