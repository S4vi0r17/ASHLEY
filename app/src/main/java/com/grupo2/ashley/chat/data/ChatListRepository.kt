package com.grupo2.ashley.chat.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.grupo2.ashley.chat.models.Conversation
import com.google.firebase.database.ValueEventListener
class ChatListRepository(
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
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
}
