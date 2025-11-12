package com.grupo2.ashley.chat.models

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: LastMessage? = null,
    // Runtime-only field for participant information (not stored in Firebase)
    val participantsInfo: Map<String, ParticipantInfo> = emptyMap()
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
    val phoneNumber: String = ""
)
