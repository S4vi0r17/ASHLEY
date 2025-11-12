package com.grupo2.ashley.chat.data

import android.util.Log
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.grupo2.ashley.chat.database.dao.ConversationDao
import com.grupo2.ashley.chat.database.dao.MessageDao
import com.grupo2.ashley.chat.database.entities.ConversationEntity
import com.grupo2.ashley.chat.database.entities.MessageEntity
import com.grupo2.ashley.chat.models.*
import com.grupo2.ashley.chat.notifications.ChatNotificationManager
import com.grupo2.ashley.core.network.ConnectivityObserver
import com.grupo2.ashley.core.utils.ImageCompressor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val firebaseDb: DatabaseReference,
    private val connectivityObserver: ConnectivityObserver,
    private val imageCompressor: ImageCompressor,
    private val chatNotificationManager: ChatNotificationManager,
    private val userRepository: ChatUserRepository,
    private val auth: FirebaseAuth,
    @com.grupo2.ashley.di.ApplicationScope private val coroutineScope: CoroutineScope
) : ChatRepository {

    private val storage = FirebaseStorage.getInstance().reference
    private val TAG = "ChatRepositoryImpl"

    // Track which conversation is currently active to prevent notifications
    private var activeConversationId: String? = null

    init {
        // Auto-sync when connection is restored
        coroutineScope.launch {
            connectivityObserver.observe().collectLatest { status ->
                if (status == ConnectivityObserver.Status.Available) {
                    syncOfflineMessages()
                }
            }
        }
    }

    // MESSAGES

    override fun observeMessages(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesByConversation(conversationId)
            .map { entities ->
                entities.map { it.toMessage() }
            }
            .onStart {
                // Start listening to Firebase in background
                if (connectivityObserver.isConnected()) {
                    coroutineScope.launch {
                        startFirebaseSync(conversationId)
                    }
                }
            }
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        imageBytes: ByteArray?,
        videoBytes: ByteArray?
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            // Create message entity
            val message = Message(
                id = messageId,
                senderId = senderId,
                text = text,
                timestamp = timestamp,
                imageUrl = null,
                videoUrl = null,
                mediaType = null,
                status = MessageStatus.PENDING
            )

            // Save to local database first
            val entity = MessageEntity.fromMessage(message, conversationId, isSynced = false)
                .copy(localOnly = true)
            messageDao.insertMessage(entity)

            // If online, upload and sync
            if (connectivityObserver.isConnected()) {
                try {
                    val finalImageUrl = if (imageBytes != null) {
                        uploadImageCompressed(imageBytes)
                    } else null

                    val finalVideoUrl = if (videoBytes != null) {
                        uploadVideo(videoBytes)
                    } else null

                    val mediaType = when {
                        finalImageUrl != null -> "image"
                        finalVideoUrl != null -> "video"
                        else -> null
                    }

                    val updatedMessage = message.copy(
                        imageUrl = finalImageUrl,
                        videoUrl = finalVideoUrl,
                        mediaType = mediaType,
                        status = MessageStatus.SENT  // Cambiado a SENT, será DELIVERED cuando el receptor lo reciba
                    )

                    // Upload to Firebase
                    syncMessageToFirebase(conversationId, updatedMessage)

                    // Update local database
                    messageDao.insertMessage(
                        MessageEntity.fromMessage(updatedMessage, conversationId, isSynced = true)
                    )

                    Result.success(updatedMessage)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send message online", e)
                    messageDao.updateMessageStatus(messageId, MessageStatus.FAILED)
                    Result.failure(e)
                }
            } else {
                // Queue for later sync
                Result.success(message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            Result.failure(e)
        }
    }

    override suspend fun loadMoreMessages(
        conversationId: String,
        offset: Int,
        limit: Int
    ): List<Message> = withContext(Dispatchers.IO) {
        try {
            val entities = messageDao.getMessagesPaginated(conversationId, limit, offset)
            entities.map { it.toMessage() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load more messages", e)
            emptyList()
        }
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        withContext(Dispatchers.IO) {
            messageDao.updateMessageStatus(messageId, status)
        }
    }

    override suspend fun deleteMessage(messageId: String, conversationId: String) {
        withContext(Dispatchers.IO) {
            // Mark as deleted in local database first
            messageDao.markAsDeleted(messageId)

            // Update conversation's last message
            updateConversationLastMessage(conversationId)

            // Mark as deleted in Firebase if online and wait for confirmation
            if (connectivityObserver.isConnected()) {
                try {
                    suspendCoroutine<Unit> { cont ->
                        firebaseDb.child("conversations")
                            .child(conversationId)
                            .child("messages")
                            .child(messageId)
                            .updateChildren(mapOf(
                                "isDeleted" to true,
                                "deleted" to true  // For backwards compatibility
                            ))
                            .addOnSuccessListener {
                                Log.d(TAG, "Message marked as deleted in Firebase: $messageId")

                                // Update conversation's lastMessage in Firebase
                                coroutineScope.launch {
                                    updateFirebaseConversationLastMessage(conversationId)
                                }

                                cont.resume(Unit)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to mark as deleted in Firebase", e)
                                cont.resume(Unit)
                            }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mark as deleted in Firebase", e)
                }
            }
        }
    }

    override suspend fun retryFailedMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            // Update status to pending and trigger sync
            messageDao.updateMessageStatus(messageId, MessageStatus.PENDING)
            syncOfflineMessages()
        }
    }

    override suspend fun markMessagesAsRead(conversationId: String, currentUserId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Get all unread message IDs first
                val unreadMessageIds = messageDao.getUnreadMessageIds(
                    conversationId = conversationId,
                    currentUserId = currentUserId,
                    readStatus = MessageStatus.READ
                )

                if (unreadMessageIds.isEmpty()) {
                    Log.d(TAG, "No unread messages to mark as read")
                    return@withContext
                }

                // Mark messages as read in local database
                val readTimestamp = System.currentTimeMillis()
                messageDao.markConversationMessagesAsRead(
                    conversationId = conversationId,
                    currentUserId = currentUserId,
                    status = MessageStatus.READ,
                    readAt = readTimestamp
                )

                // Update Firebase if online
                if (connectivityObserver.isConnected()) {
                    unreadMessageIds.forEach { messageId ->
                        val updates = mapOf(
                            "status" to MessageStatus.READ.name,
                            "readAt" to readTimestamp
                        )
                        firebaseDb.child("conversations")
                            .child(conversationId)
                            .child("messages")
                            .child(messageId)
                            .updateChildren(updates)
                            .addOnSuccessListener {
                                Log.d(TAG, "Marked message $messageId as read in Firebase")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to mark message as read in Firebase", e)
                            }
                    }
                }

                Log.d(TAG, "Marked ${unreadMessageIds.size} messages as read")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark messages as read", e)
            }
        }
    }

    // CONVERSATIONS

    override fun observeConversations(userId: String): Flow<List<Conversation>> {
        return conversationDao.getUserConversations(userId)
            .map { entities ->
                entities.map { entity ->
                    val participants = entity.participantsJson.split(",")
                    entity.toConversation(participants)
                }
            }
            .onStart {
                // Start listening to Firebase in background
                if (connectivityObserver.isConnected()) {
                    coroutineScope.launch {
                        syncConversations(userId)
                    }
                }
            }
    }

    override suspend fun createOrGetConversation(
        userId1: String,
        userId2: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val conversationId = if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"

            if (connectivityObserver.isConnected()) {
                // Create in Firebase
                val created = suspendCoroutine { cont ->
                    val conversationRef = firebaseDb.child("conversations").child(conversationId)
                    conversationRef.get().addOnSuccessListener { snapshot ->
                        if (!snapshot.exists()) {
                            val newConversation = mapOf(
                                "id" to conversationId,
                                "participants" to mapOf(
                                    userId1 to true,
                                    userId2 to true
                                ),
                                "timestamp" to System.currentTimeMillis()
                            )
                            conversationRef.setValue(newConversation)
                                .addOnSuccessListener { cont.resume(true) }
                                .addOnFailureListener { cont.resume(false) }
                        } else {
                            cont.resume(true)
                        }
                    }.addOnFailureListener { cont.resume(false) }
                }

                if (created) {
                    // Save to local database
                    val entity = ConversationEntity(
                        id = conversationId,
                        participantsJson = "$userId1,$userId2",
                        lastMessageText = null,
                        lastMessageTimestamp = null,
                        lastMessageSenderId = null,
                        isSynced = true
                    )
                    conversationDao.insertConversation(entity)
                }
            } else {
                // Create locally only
                val entity = ConversationEntity(
                    id = conversationId,
                    participantsJson = "$userId1,$userId2",
                    lastMessageText = null,
                    lastMessageTimestamp = null,
                    lastMessageSenderId = null,
                    isSynced = false
                )
                conversationDao.insertConversation(entity)
            }

            Result.success(conversationId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create conversation", e)
            Result.failure(e)
        }
    }

    override suspend fun markConversationAsRead(conversationId: String) {
        withContext(Dispatchers.IO) {
            conversationDao.markAsRead(conversationId)
        }
    }

    override suspend fun updateTypingStatus(
        conversationId: String,
        userId: String,
        isTyping: Boolean
    ) {
        if (connectivityObserver.isConnected()) {
            firebaseDb.child("conversations").child(conversationId)
                .child("typing").child(userId)
                .setValue(if (isTyping) System.currentTimeMillis() else null)
        }
    }

    // SYNC

    override suspend fun syncOfflineMessages() = withContext(Dispatchers.IO) {
        if (!connectivityObserver.isConnected()) return@withContext

        try {
            val unsyncedMessages = messageDao.getUnsyncedMessages()
            Log.d(TAG, "Syncing ${unsyncedMessages.size} offline messages")

            unsyncedMessages.forEach { entity ->
                try {
                    val message = entity.toMessage()
                    syncMessageToFirebase(entity.conversationId, message)
                    messageDao.markAsSynced(entity.id)
                    messageDao.updateMessageStatus(entity.id, MessageStatus.DELIVERED)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync message ${entity.id}", e)
                    messageDao.updateMessageStatus(entity.id, MessageStatus.FAILED)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync offline messages", e)
        }
    }

    override suspend fun syncConversations(userId: String) = withContext(Dispatchers.IO) {
        if (!connectivityObserver.isConnected()) return@withContext

        try {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    coroutineScope.launch {
                        val conversations = mutableListOf<ConversationEntity>()
                        for (child in snapshot.children) {
                            val conv = child.getValue(Conversation::class.java)
                            if (conv != null && conv.participants.contains(userId)) {
                                conversations.add(
                                    ConversationEntity.fromConversation(
                                        conv.copy(id = child.key ?: "")
                                    )
                                )
                            }
                        }
                        conversationDao.insertConversations(conversations)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to sync conversations", error.toException())
                }
            }

            firebaseDb.child("conversations").addValueEventListener(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync conversations", e)
        }
    }

    // PRIVATE HELPERS

    private suspend fun startFirebaseSync(conversationId: String) {
        val messagesRef = firebaseDb.child("conversations").child(conversationId).child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch {
                    val firebaseMessages = mutableListOf<MessageEntity>()
                    val currentUserId = auth.currentUser?.uid
                    val newMessagesForNotification = mutableListOf<Pair<Message, String>>()

                    // Get existing message IDs to detect new messages
                    val existingMessageIds = messageDao.getMessageIds(conversationId).toSet()

                    // Collect all messages from Firebase
                    for (child in snapshot.children) {
                        val message = child.getValue(Message::class.java)
                        if (message != null) {
                            // Set message ID from Firebase key if not already set
                            if (message.id.isEmpty()) {
                                message.id = child.key ?: ""
                            }

                            // Check both 'deleted' and 'isDeleted' fields for backwards compatibility
                            val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                            val deleted = child.child("deleted").getValue(Boolean::class.java) ?: false

                            if (isDeleted || deleted) {
                                // 🚫 El mensaje fue eliminado: borrar localmente
                                messageDao.deleteMessage(message.id)
                            } else {
                                // ✅ Mensaje válido: sincronizar
                                firebaseMessages.add(
                                    MessageEntity.fromMessage(message, conversationId, isSynced = true)
                                )

                                // Check if this is a new message for notification
                                val isNewMessage = !existingMessageIds.contains(message.id)
                                val isFromOtherUser = currentUserId != null && message.senderId != currentUserId
                                val isConversationNotActive = activeConversationId != conversationId

                                if (isNewMessage && isFromOtherUser && isConversationNotActive) {
                                    newMessagesForNotification.add(Pair(message, message.senderId!!))
                                }

                                // 📬 Marcar como DELIVERED si somos el receptor y el mensaje no está leído
                                if (isFromOtherUser && message.status == MessageStatus.SENT) {
                                    // Actualizar a DELIVERED en Firebase
                                    messagesRef.child(message.id).child("status")
                                        .setValue(MessageStatus.DELIVERED.name)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Message ${message.id} marked as DELIVERED")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Failed to mark message as DELIVERED", e)
                                        }
                                }
                            }
                        }
                    }

                    // Insert/update messages from Firebase
                    // Room will replace existing messages with the updated data (including isDeleted flag)
                    if (firebaseMessages.isNotEmpty()) {
                        messageDao.insertMessages(firebaseMessages)
                    }

                    // Update conversation's last message to reflect any deletions
                    updateConversationLastMessage(conversationId)

                    // Show notifications for new messages
                    if (newMessagesForNotification.isNotEmpty()) {
                        showNotificationsForNewMessages(conversationId, newMessagesForNotification)
                    }

                    Log.d(TAG, "Synced ${firebaseMessages.size} messages from Firebase")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase sync cancelled", error.toException())
            }
        }

        messagesRef.addValueEventListener(listener)
    }

    private suspend fun showNotificationsForNewMessages(
        conversationId: String,
        messages: List<Pair<Message, String>>
    ) {
        try {
            // Get unique sender IDs
            val senderIds = messages.map { it.second }.distinct()

            // Get user profiles for all senders
            val userProfiles = userRepository.getUserProfiles(senderIds)

            // Get conversation details
            val conversation = conversationDao.getConversationById(conversationId)

            if (messages.size == 1) {
                // Single message notification
                val (message, senderId) = messages.first()
                val profile = userProfiles[senderId]
                val senderName = if (profile != null) {
                    "${profile.firstName} ${profile.lastName}".trim()
                } else {
                    "Unknown User"
                }

                chatNotificationManager.showMessageNotification(
                    conversationId = conversationId,
                    message = message,
                    senderName = senderName,
                    senderPhotoUrl = profile?.profileImageUrl
                )
            } else {
                // Multiple messages - show grouped notification
                val messagesWithNames = messages.map { (message, senderId) ->
                    val profile = userProfiles[senderId]
                    val senderName = if (profile != null) {
                        "${profile.firstName} ${profile.lastName}".trim()
                    } else {
                        "Unknown User"
                    }
                    Pair(message, senderName)
                }

                val conversationName = if (senderIds.size == 1) {
                    messagesWithNames.first().second
                } else {
                    "Group Chat"
                }

                chatNotificationManager.showGroupedMessageNotification(
                    conversationId = conversationId,
                    messages = messagesWithNames,
                    conversationName = conversationName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notifications", e)
        }
    }

    fun setActiveConversation(conversationId: String?) {
        activeConversationId = conversationId
        // Cancel notifications for this conversation when it becomes active
        if (conversationId != null) {
            chatNotificationManager.cancelNotification(conversationId)
        }
    }

    private suspend fun syncMessageToFirebase(
        conversationId: String,
        message: Message
    ): Unit = suspendCoroutine { cont ->
        val ref = firebaseDb.child("conversations").child(conversationId).child("messages").child(message.id)

        ref.setValue(message)
            .addOnSuccessListener {
                // Update conversation metadata
                val conversationUpdate = mapOf(
                    "lastMessage" to mapOf(
                        "text" to message.text,
                        "timestamp" to message.timestamp,
                        "senderId" to message.senderId
                    ),
                    "timestamp" to message.timestamp
                )

                firebaseDb.child("conversations").child(conversationId)
                    .updateChildren(conversationUpdate)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resume(Unit) }
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to sync message to Firebase", it)
                cont.resume(Unit)
            }
    }

    private suspend fun uploadImageCompressed(imageBytes: ByteArray): String? = suspendCoroutine { cont ->
        try {
            // Compress image first
            val compressedBytes = imageCompressor.compress(imageBytes)
            Log.d(TAG, "Compressed image: ${imageBytes.size} -> ${compressedBytes.size} bytes")

            val fileName = "chat_images/${UUID.randomUUID()}.jpg"
            val imageRef = storage.child(fileName)

            imageRef.putBytes(compressedBytes)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        cont.resume(uri.toString())
                    }.addOnFailureListener {
                        Log.e(TAG, "Failed to get download URL", it)
                        cont.resume(null)
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Failed to upload image", it)
                    cont.resume(null)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image", e)
            cont.resume(null)
        }
    }

    private suspend fun uploadVideo(videoBytes: ByteArray): String? = suspendCoroutine { cont ->
        try {
            Log.d(TAG, "Uploading video: ${videoBytes.size} bytes")

            val fileName = "chat_videos/${UUID.randomUUID()}.mp4"
            val videoRef = storage.child(fileName)

            videoRef.putBytes(videoBytes)
                .addOnSuccessListener {
                    videoRef.downloadUrl.addOnSuccessListener { uri ->
                        Log.d(TAG, "Video uploaded successfully")
                        cont.resume(uri.toString())
                    }.addOnFailureListener {
                        Log.e(TAG, "Failed to get video download URL", it)
                        cont.resume(null)
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Failed to upload video", it)
                    cont.resume(null)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process video", e)
            cont.resume(null)
        }
    }

    private suspend fun updateConversationLastMessage(conversationId: String) {
        try {
            // Get the latest non-deleted message
            val latestMessage = messageDao.getLatestMessages(conversationId, 1).firstOrNull()

            if (latestMessage != null) {
                // Update conversation with the latest message
                conversationDao.updateLastMessage(
                    conversationId = conversationId,
                    text = latestMessage.text,
                    timestamp = latestMessage.timestamp,
                    senderId = latestMessage.senderId
                )
            } else {
                // No messages left, clear last message
                conversationDao.updateLastMessage(
                    conversationId = conversationId,
                    text = null,
                    timestamp = null,
                    senderId = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update conversation last message", e)
        }
    }

    private suspend fun updateFirebaseConversationLastMessage(conversationId: String) {
        try {
            // Get the latest non-deleted message
            val latestMessage = messageDao.getLatestMessages(conversationId, 1).firstOrNull()

            val conversationRef = firebaseDb.child("conversations").child(conversationId)

            if (latestMessage != null) {
                // Update Firebase conversation with latest message
                val lastMessageUpdate = mapOf(
                    "lastMessage" to mapOf(
                        "text" to latestMessage.text,
                        "timestamp" to latestMessage.timestamp,
                        "senderId" to latestMessage.senderId
                    ),
                    "timestamp" to latestMessage.timestamp
                )
                conversationRef.updateChildren(lastMessageUpdate)
                    .addOnSuccessListener {
                        Log.d(TAG, "Updated Firebase conversation lastMessage")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update Firebase conversation lastMessage", e)
                    }
            } else {
                // No messages left, remove lastMessage from conversation
                conversationRef.child("lastMessage").removeValue()
                    .addOnSuccessListener {
                        Log.d(TAG, "Removed Firebase conversation lastMessage")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to remove Firebase conversation lastMessage", e)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Firebase conversation last message", e)
        }
    }

    override suspend fun getProductInfoForConversation(conversationId: String): ProductInfo? {
        return try {
            // Por ahora retorna null - puede implementarse si se necesita
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading product info", e)
            null
        }
    }

    override fun observeTypingStatus(conversationId: String, otherUserId: String): Flow<Boolean> {
        return callbackFlow {
            val typingRef = firebaseDb.child("conversations")
                .child(conversationId)
                .child("typing")
                .child(otherUserId)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val typingTimestamp = snapshot.getValue(Long::class.java)
                    // Considerar que está escribiendo si el timestamp es reciente (últimos 5 segundos)
                    val isTyping = if (typingTimestamp != null) {
                        val currentTime = System.currentTimeMillis()
                        (currentTime - typingTimestamp) < 5000
                    } else {
                        false
                    }
                    trySend(isTyping)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error observing typing status", error.toException())
                    trySend(false)
                }
            }

            typingRef.addValueEventListener(listener)

            awaitClose {
                typingRef.removeEventListener(listener)
            }
        }
    }

    override suspend fun getTotalUnreadCount(currentUserId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                // Obtener todas las conversaciones del usuario
                val conversations = conversationDao.getUserConversationsSync(currentUserId)

                // Contar mensajes sin leer en cada conversación
                var totalUnread = 0
                conversations.forEach { conversation ->
                    val unreadCount = messageDao.getUnreadMessageCount(
                        conversationId = conversation.id,
                        currentUserId = currentUserId
                    )
                    totalUnread += unreadCount
                }

                totalUnread
            } catch (e: Exception) {
                Log.e(TAG, "Error getting total unread count", e)
                0
            }
        }
    }
}
