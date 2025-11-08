package com.grupo2.ashley.chat.models

data class Message(
    var id: String = "",
    val senderId: String? = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null
)