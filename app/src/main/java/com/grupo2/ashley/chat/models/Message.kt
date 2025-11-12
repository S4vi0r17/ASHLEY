package com.grupo2.ashley.chat.models

data class Message(
    var id: String = "",
    val senderId: String? = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val mediaType: String? = null, // "image" o "video"
    val status: MessageStatus = MessageStatus.SENT,
    val readAt: Long? = null,
    val isDeleted: Boolean = false
)
