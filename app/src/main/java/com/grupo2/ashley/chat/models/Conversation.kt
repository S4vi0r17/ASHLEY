package com.grupo2.ashley.chat.models

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: LastMessage? = null
)

data class LastMessage(
    val text: String = "",
    val timestamp: Long = 0L
)
