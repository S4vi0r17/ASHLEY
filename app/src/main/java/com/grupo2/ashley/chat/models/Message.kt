package com.grupo2.ashley.chat.models

data class Message(
    var id: String = "",
    var senderId: String? = "",
    var text: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var imageUrl: String? = null,
    var videoUrl: String? = null,
    var mediaType: String? = null, // "image" o "video"
    var status: MessageStatus = MessageStatus.SENT,
    var readAt: Long? = null,
    var isDeleted: Boolean = false
)
