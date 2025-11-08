package com.grupo2.ashley.chat.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.grupo2.ashley.chat.models.Message
import java.util.UUID

class ChatRealtimeRepository(
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
) {

    private val storage: StorageReference = FirebaseStorage.getInstance().reference

    fun sendMessage(
        conversationId: String,
        message: Message,
        imageBytes: ByteArray? = null,
        onComplete: (Boolean) -> Unit
    ) {
        if (imageBytes != null) {
            uploadImage(imageBytes) { imageUrl ->
                if (imageUrl != null) {
                    val messageWithImage = message.copy(imageUrl = imageUrl)
                    saveMessage(conversationId, messageWithImage, onComplete)
                } else {
                    onComplete(false)
                }
            }
        } else {
            saveMessage(conversationId, message, onComplete)
        }
    }


    fun addMessagesListener(
        conversationId: String,
        onChange: (List<Message>) -> Unit
    ): ValueEventListener {
        val messagesRef = db.child("conversations").child(conversationId).child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                for (child in snapshot.children) {
                    child.getValue(Message::class.java)?.let { messages.add(it) }
                }
                onChange(messages.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepo", "Error escuchando mensajes: ${error.message}")
            }
        }

        messagesRef.addValueEventListener(listener)
        return listener
    }

    fun removeMessagesListener(conversationId: String, listener: ValueEventListener) {
        db.child("conversations").child(conversationId).child("messages")
            .removeEventListener(listener)
    }

    private fun saveMessage(conversationId: String, message: Message, onComplete: (Boolean) -> Unit) {
        val ref = db.child("conversations").child(conversationId).child("messages").push()
        val messageId = ref.key ?: UUID.randomUUID().toString()
        val messageWithId = message.copy(id = messageId)

        ref.setValue(messageWithId)
            .addOnSuccessListener {
                // Actualiza metadatos de conversación
                val conversationUpdate = mapOf(
                    "lastMessage" to messageWithId,
                    "timestamp" to messageWithId.timestamp
                )

                db.child("conversations").child(conversationId)
                    .updateChildren(conversationUpdate)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    private fun uploadImage(imageBytes: ByteArray, onResult: (String?) -> Unit) {
        val fileName = "chat_images/${UUID.randomUUID()}.jpg"
        val imageRef = storage.child(fileName)

        imageRef.putBytes(imageBytes)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    onResult(uri.toString())
                }.addOnFailureListener {
                    Log.e("ChatRepo", "Error obteniendo URL: ${it.message}")
                    onResult(null)
                }
            }
            .addOnFailureListener {
                Log.e("ChatRepo", "Error subiendo imagen: ${it.message}")
                onResult(null)
            }
    }

    fun createOrGetConversation(
        userId1: String,
        userId2: String,
        onResult: (String) -> Unit
    ) {
        val conversationId = if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
        val conversationRef = db.child("conversations").child(conversationId)

        conversationRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val newConversation = mapOf(
                    "id" to conversationId,
                    "participants" to listOf(userId1, userId2),
                    "lastMessage" to "",
                    "timestamp" to System.currentTimeMillis()
                )

                conversationRef.setValue(newConversation)
                    .addOnSuccessListener { onResult(conversationId) }
                    .addOnFailureListener {
                        Log.e("ChatRepo", "Error creando conversación: ${it.message}")
                    }
            } else {
                onResult(conversationId)
            }
        }.addOnFailureListener {
            Log.e("ChatRepo", "Error verificando conversación: ${it.message}")
        }
    }
}