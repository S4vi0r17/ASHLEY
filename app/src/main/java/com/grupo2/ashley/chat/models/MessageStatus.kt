package com.grupo2.ashley.chat.models

enum class MessageStatus {
    PENDING,    // Message is being prepared/uploaded
    SENT,       // Message sent to Firebase
    DELIVERED,  // Message delivered to recipient's device
    READ,       // Message has been read by recipient
    FAILED      // Message failed to send
}
