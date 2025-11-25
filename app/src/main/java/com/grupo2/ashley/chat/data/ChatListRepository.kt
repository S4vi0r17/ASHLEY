package com.grupo2.ashley.chat.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.grupo2.ashley.chat.models.Conversation
import com.grupo2.ashley.chat.models.ConversationWithUser
import com.grupo2.ashley.profile.models.UserProfile
import kotlinx.coroutines.tasks.await

class ChatListRepository(
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getUserConversations(userId: String?, onResult: (List<Conversation>) -> Unit) {
        val conversationsRef = db.child("conversations")

        conversationsRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Conversation>()

                for (child in snapshot.children) {
                    val conv = child.getValue(Conversation::class.java)
                    if (conv != null && conv.participants.contains(userId)) {
                        list.add(conv.copy(id = child.key ?: ""))
                    }
                }

                val sortedList = list.sortedByDescending { it.lastMessage?.timestamp ?: 0L }
                onResult(sortedList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatListRepo", "Error obteniendo conversaciones: ${error.message}")
                onResult(emptyList())
            }
        })
    }

    fun addConversationsListener(
        userId: String?,
        onChange: (List<Conversation>) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Conversation>()

                for (child in snapshot.children) {
                    val conv = child.getValue(Conversation::class.java)
                    if (conv != null && userId != null && conv.participants.contains(userId)) {
                        list.add(conv.copy(id = child.key ?: ""))
                    }
                }

                val sortedList = list.sortedByDescending { it.lastMessage?.timestamp ?: 0L }
                onChange(sortedList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatListRepo", "Error en listener de conversaciones: ${error.message}")
                onChange(emptyList())
            }
        }

        db.child("conversations").addValueEventListener(listener)
        return listener
    }

    fun removeConversationsListener(listener: ValueEventListener) {
        db.child("conversations").removeEventListener(listener)
    }

    suspend fun getUserConversationsWithUserData(
        userId: String?
    ): List<ConversationWithUser> {
        if (userId == null) return emptyList()

        return try {
            val conversationsSnapshot = db.child("conversations")
                .get()
                .await()

            val conversationsList = mutableListOf<ConversationWithUser>()

            for (child in conversationsSnapshot.children) {
                val conv = child.getValue(Conversation::class.java)
                if (conv != null && conv.participants.contains(userId)) {
                    // Obtener el ID del otro usuario
                    val otherUserId = conv.participants.firstOrNull { it != userId } ?: continue

                    // Obtener datos del otro usuario desde Firestore
                    val userProfile = try {
                        firestore.collection("users")
                            .document(otherUserId)
                            .get()
                            .await()
                            .toObject(UserProfile::class.java)
                    } catch (e: Exception) {
                        Log.e("ChatListRepo", "Error obteniendo perfil de usuario: ${e.message}")
                        null
                    }

                    // Obtener datos del producto si existe productId
                    val productInfo = if (!conv.productId.isNullOrEmpty()) {
                        try {
                            val productDoc = firestore.collection("products")
                                .document(conv.productId)
                                .get()
                                .await()

                            if (productDoc.exists()) {
                                com.grupo2.ashley.chat.models.ProductInfo(
                                    productId = productDoc.id,
                                    title = productDoc.getString("title") ?: "",
                                    price = productDoc.getDouble("price") ?: 0.0,
                                    imageUrl = (productDoc.get("images") as? List<*>)?.firstOrNull() as? String ?: "",
                                    condition = productDoc.getString("condition") ?: "",
                                    sellerId = productDoc.getString("userId") ?: ""
                                )
                            } else null
                        } catch (e: Exception) {
                            Log.e("ChatListRepo", "Error obteniendo producto: ${e.message}")
                            null
                        }
                    } else null

                    val conversationWithUser = ConversationWithUser(
                        conversationId = child.key ?: "",
                        otherUserId = otherUserId,
                        otherUserName = if (userProfile != null) {
                            "${userProfile.firstName} ${userProfile.lastName}".trim()
                                .ifEmpty { "Usuario" }
                        } else {
                            "Usuario"
                        },
                        otherUserImageUrl = userProfile?.profileImageUrl ?: "",
                        lastMessage = conv.lastMessage,
                        isOnline = false, // Para implementar lógica de presencia después :v
                        unreadCount = 0,
                        productInfo = productInfo
                    )

                    conversationsList.add(conversationWithUser)
                }
            }

            // Ordenar por timestamp del último mensaje
            conversationsList.sortedByDescending { it.lastMessage?.timestamp ?: 0L }
        } catch (e: Exception) {
            Log.e("ChatListRepo", "Error obteniendo conversaciones: ${e.message}")
            emptyList()
        }
    }
}
