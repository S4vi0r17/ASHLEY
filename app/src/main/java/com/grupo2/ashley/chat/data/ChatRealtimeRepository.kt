package com.grupo2.ashley.chat.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.grupo2.ashley.chat.models.Conversation
import com.grupo2.ashley.chat.models.LastMessage
import com.grupo2.ashley.chat.models.Message
import com.grupo2.ashley.chat.models.MessageStatus
import com.grupo2.ashley.chat.models.ProductInfo
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRealtimeRepository(
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val storage: StorageReference = FirebaseStorage.getInstance().reference

    // Envía un mensaje a Firebase. Si incluye imagen, la sube primero
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


    // Agrega un listener en tiempo real para los mensajes de una conversación
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

    // Remueve un listener de mensajes
    fun removeMessagesListener(conversationId: String, listener: ValueEventListener) {
        db.child("conversations").child(conversationId).child("messages")
            .removeEventListener(listener)
    }

    // Guarda un mensaje en Firebase y actualiza metadatos de la conversación
    private fun saveMessage(conversationId: String, message: Message, onComplete: (Boolean) -> Unit) {
        val ref = db.child("conversations").child(conversationId).child("messages").push()
        val messageId = ref.key ?: UUID.randomUUID().toString()
        val messageWithId = message.copy(id = messageId)

        ref.setValue(messageWithId)
            .addOnSuccessListener {
                // Create a LastMessage object from the sent message
                val lastMessage = LastMessage(
                    text = messageWithId.text,
                    timestamp = messageWithId.timestamp,
                    senderId = messageWithId.senderId,
                    unreadCount = 1 // New message starts with unread count of 1
                )

                val conversationUpdate = mapOf(
                    "lastMessage" to mapOf(
                        "text" to lastMessage.text,
                        "timestamp" to lastMessage.timestamp,
                        "senderId" to lastMessage.senderId,
                        "unreadCount" to lastMessage.unreadCount
                    )
                )

                db.child("conversations").child(conversationId)
                    .updateChildren(conversationUpdate)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    // Sube una imagen a Firebase Storage y retorna su URL
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

    // Crea una nueva conversación o recupera una existente entre dos usuarios
    fun createOrGetConversation(
        userId1: String,
        userId2: String,
        onResult: (String) -> Unit
    ) {
        val conversationId = if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
        val conversationRef = db.child("conversations").child(conversationId)

        conversationRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Create an empty LastMessage object for new conversations
                val emptyLastMessage = LastMessage(
                    text = "",
                    timestamp = System.currentTimeMillis(),
                    senderId = "",
                    unreadCount = 0
                )

                val newConversation = mapOf(
                    "id" to conversationId,
                    "participants" to listOf(userId1, userId2),
                    "lastMessage" to mapOf(
                        "text" to emptyLastMessage.text,
                        "timestamp" to emptyLastMessage.timestamp,
                        "senderId" to emptyLastMessage.senderId,
                        "unreadCount" to emptyLastMessage.unreadCount
                    )
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

    // Obtiene información del producto asociado a una conversación desde Firebase y Firestore
    suspend fun getProductInfoForConversation(conversationId: String): ProductInfo? {
        return try {
            val conversationSnapshot = db.child("conversations")
                .child(conversationId)
                .get()
                .await()

            val conversation = conversationSnapshot.getValue(Conversation::class.java)
            val productId = conversation?.productId

            if (!productId.isNullOrEmpty()) {
                val productDoc = firestore.collection("products")
                    .document(productId)
                    .get()
                    .await()

                if (productDoc.exists()) {
                    ProductInfo(
                        productId = productDoc.id,
                        title = productDoc.getString("title") ?: "",
                        price = productDoc.getDouble("price") ?: 0.0,
                        imageUrl = (productDoc.get("images") as? List<*>)?.firstOrNull() as? String ?: "",
                        condition = productDoc.getString("condition") ?: "",
                        sellerId = productDoc.getString("userId") ?: ""
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("ChatRepo", "Error obteniendo producto: ${e.message}")
            null
        }
    }

    // Marca todos los mensajes de una conversación como leídos en Firebase
    suspend fun markMessagesAsRead(conversationId: String, currentUserId: String) {
        try {
            val messagesRef = db.child("conversations")
                .child(conversationId)
                .child("messages")

            val snapshot = messagesRef.get().await()

            for (child in snapshot.children) {
                val message = child.getValue(Message::class.java)
                if (message != null && message.senderId != currentUserId) {
                    // Solo marcar como leído si no lo está ya
                    if (message.status != MessageStatus.READ) {
                        child.ref.child("status").setValue(MessageStatus.READ.name)
                        child.ref.child("readAt").setValue(System.currentTimeMillis())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepo", "Error marcando mensajes como leídos: ${e.message}")
        }
    }
}