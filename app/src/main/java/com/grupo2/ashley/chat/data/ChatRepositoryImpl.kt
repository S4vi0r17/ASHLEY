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

    // Trackea la conversación activa para que no suelte notificaciones
    private var activeConversationId: String? = null

    init {
        // Autosincronizar cuando el internet vuelve
        coroutineScope.launch {
            connectivityObserver.observe().collectLatest { status ->
                if (status == ConnectivityObserver.Status.Available) {
                    syncOfflineMessages()
                }
            }
        }
    }

    // MESSAGES
    // Observa los mensajes de una conversación en tiempo real desde la base de datos local
    override fun observeMessages(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesByConversation(conversationId)
            .map { entities ->
                entities.map { it.toMessage() }
            }
            .onStart {
                // Escuchar al firebase en segundo plano
                if (connectivityObserver.isConnected()) {
                    coroutineScope.launch {
                        startFirebaseSync(conversationId)
                    }
                }
            }
    }

    // Envía un mensaje con texto, imagen o video. Guarda localmente primero y luego sincroniza con Firebase
    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        imageBytes: ByteArray?,
        videoBytes: ByteArray?
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "sendMessage: conversationId=$conversationId, hasImage=${imageBytes != null}, hasVideo=${videoBytes != null}, imageSize=${imageBytes?.size}, videoSize=${videoBytes?.size}")

            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            // Creacion de entidad de mensaje
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

            // Guardar localmente primero
            val entity = MessageEntity.fromMessage(message, conversationId, isSynced = false)
                .copy(localOnly = true)
            messageDao.insertMessage(entity)
            Log.d(TAG, "💾 Message saved to local DB: $messageId")

            // Si está online, sincronizar
            if (connectivityObserver.isConnected()) {
                Log.d(TAG, "Device is online, uploading media...")
                try {
                    val finalImageUrl = if (imageBytes != null) {
                        Log.d(TAG, "Uploading image...")
                        val url = uploadImageCompressed(imageBytes)
                        Log.d(TAG, "Image uploaded: $url")
                        url
                    } else null

                    val finalVideoUrl = if (videoBytes != null) {
                        Log.d(TAG, "Uploading video...")
                        val url = uploadVideo(videoBytes)
                        Log.d(TAG, "Video uploaded: $url")
                        url
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
                        status = MessageStatus.SENT
                    )

                    Log.d(TAG, "Updating message: imageUrl=$finalImageUrl, videoUrl=$finalVideoUrl, mediaType=$mediaType")

                    // Actualiza la base de datos local para mostrarse en la UI
                    messageDao.insertMessage(
                        MessageEntity.fromMessage(updatedMessage, conversationId, isSynced = false)
                            .copy(localOnly = false)
                    )
                    Log.d(TAG, "Local DB updated with URLs - should appear in UI now")

                    // Forzar un pequeño retraso para asegurar que Room procese la actualización
                    delay(100)

                    //Luego, sincroniza con Firebase.
                    syncMessageToFirebase(conversationId, updatedMessage)

                    // Marcar como sincronizado
                    messageDao.markAsSynced(messageId)
                    Log.d(TAG, "Message synced to Firebase")

                    Result.success(updatedMessage)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send message online", e)
                    messageDao.updateMessageStatus(messageId, MessageStatus.FAILED)
                    Result.failure(e)
                }
            } else {
                Log.w(TAG, "Device offline, queuing for later sync")
                //En cola para sincronización posterior
                Result.success(message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            Result.failure(e)
        }
    }

    // Carga más mensajes antiguos con paginación
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

    // Actualiza el estado de un mensaje (PENDING, SENT, DELIVERED, READ, FAILED)
    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        withContext(Dispatchers.IO) {
            messageDao.updateMessageStatus(messageId, status)
        }
    }

    // Elimina un mensaje localmente y en Firebase (soft delete)
    override suspend fun deleteMessage(messageId: String, conversationId: String) {
        withContext(Dispatchers.IO) {
            // Marcar como eliminado primero en la base de datos local
            messageDao.markAsDeleted(messageId)

            // Actualizar el último mensaje de la conversación
            updateConversationLastMessage(conversationId)

            // Marca como eliminado en Firebase si está en línea y espera la confirmación.
            if (connectivityObserver.isConnected()) {
                try {
                    suspendCoroutine<Unit> { cont ->
                        firebaseDb.child("conversations")
                            .child(conversationId)
                            .child("messages")
                            .child(messageId)
                            .updateChildren(mapOf(
                                "isDeleted" to true,
                                "deleted" to true
                            ))
                            .addOnSuccessListener {
                                Log.d(TAG, "Message marked as deleted in Firebase: $messageId")

                                // Actualizar el último mensaje de la conversación en Firebase
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

    // Reintenta enviar un mensaje que falló
    override suspend fun retryFailedMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            // Update status to pending and trigger sync
            messageDao.updateMessageStatus(messageId, MessageStatus.PENDING)
            syncOfflineMessages()
        }
    }

    // Marca todos los mensajes de una conversación como leídos
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

                // Marcar mensajes como leídos en la base de datos local
                val readTimestamp = System.currentTimeMillis()
                messageDao.markConversationMessagesAsRead(
                    conversationId = conversationId,
                    currentUserId = currentUserId,
                    status = MessageStatus.READ,
                    readAt = readTimestamp
                )

                // Actualiza Firebase si está en línea.
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

    // Observa todas las conversaciones de un usuario en tiempo real
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

    // Crea o recupera una conversación entre dos usuarios
    override suspend fun createOrGetConversation(
        userId1: String,
        userId2: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val conversationId = if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"

            // Check if conversation already exists locally (including archived ones)
            val existingConv = conversationDao.getConversationById(conversationId)

            if (existingConv != null) {
                // If conversation was archived, unarchive it when user reopens it
                // Note: We DON'T unblock automatically - user must unblock manually
                if (existingConv.isArchived) {
                    conversationDao.unarchiveConversation(conversationId)
                    Log.d(TAG, "📬 Unarchived conversation $conversationId (user reopened chat)")
                }

                // Return existing conversation (now unarchived if it was)
                return@withContext Result.success(conversationId)
            }

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
                    // Guarda en base local (nueva conversación)
                    val entity = ConversationEntity(
                        id = conversationId,
                        participantsJson = "$userId1,$userId2",
                        lastMessageText = null,
                        lastMessageTimestamp = null,
                        lastMessageSenderId = null,
                        isSynced = true,
                        isMuted = false,
                        isArchived = false,
                        isBlocked = false
                    )
                    conversationDao.insertConversation(entity)
                }
            } else {
                // Crear solo localmente (nueva conversación)
                val entity = ConversationEntity(
                    id = conversationId,
                    participantsJson = "$userId1,$userId2",
                    lastMessageText = null,
                    lastMessageTimestamp = null,
                    lastMessageSenderId = null,
                    isSynced = false,
                    isMuted = false,
                    isArchived = false,
                    isBlocked = false
                )
                conversationDao.insertConversation(entity)
            }

            Result.success(conversationId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create conversation", e)
            Result.failure(e)
        }
    }

    // Marca una conversación completa como leída
    override suspend fun markConversationAsRead(conversationId: String) {
        withContext(Dispatchers.IO) {
            conversationDao.markAsRead(conversationId)
        }
    }

    // Actualiza el estado de escritura de un usuario en Firebase
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

    // Sincroniza mensajes pendientes cuando se recupera la conexión
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

    // Sincroniza todas las conversaciones desde Firebase
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
                                val conversationId = child.key ?: ""

                                // Preserve local-only fields (isMuted, isArchived, isBlocked)
                                val existingConv = conversationDao.getConversationById(conversationId)
                                val isMuted = existingConv?.isMuted ?: false
                                val isArchived = existingConv?.isArchived ?: false
                                val isBlocked = existingConv?.isBlocked ?: false

                                conversations.add(
                                    ConversationEntity.fromConversation(
                                        conv.copy(id = conversationId, isMuted = isMuted, isBlocked = isBlocked)
                                    ).copy(isArchived = isArchived)
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

    // Inicia la sincronización en tiempo real con Firebase para una conversación
    private suspend fun startFirebaseSync(conversationId: String) {
        // Verificar si la conversación está bloqueada
        val isBlocked = conversationDao.isConversationBlocked(conversationId) ?: false
        if (isBlocked) {
            Log.d(TAG, "Conversation $conversationId is BLOCKED, skipping Firebase sync")
            return
        }

        val messagesRef = firebaseDb.child("conversations").child(conversationId).child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch {
                    // Double-check if conversation is still not blocked before syncing
                    val isBlocked = conversationDao.isConversationBlocked(conversationId) ?: false
                    if (isBlocked) {
                        Log.d(TAG, "Conversation $conversationId is BLOCKED, skipping message sync")
                        return@launch
                    }

                    val firebaseMessages = mutableListOf<MessageEntity>()
                    val currentUserId = auth.currentUser?.uid
                    val newMessagesForNotification = mutableListOf<Pair<Message, String>>()

                    // Recupera mensajes existentes para comparar ids y poder sincronizar
                    val existingMessageIds = messageDao.getMessageIds(conversationId).toSet()

                    // Recolecta todos
                    for (child in snapshot.children) {
                        val message = child.getValue(Message::class.java)
                        if (message != null) {
                            // Settea messageId
                            if (message.id.isEmpty()) {
                                message.id = child.key ?: ""
                            }

                            // Chequea si ha sido eliminado
                            val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                            val deleted = child.child("deleted").getValue(Boolean::class.java) ?: false

                            if (isDeleted || deleted) {
                                // El mensaje fue eliminado: borrar localmente
                                messageDao.deleteMessage(message.id)
                            } else {
                                // Mensaje válido: sincronizar desde Firebase
                                // Siempre actualizar para mantener estados sincronizados (status, readAt, etc.)
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
                        Log.d(TAG, "Inserted/Updated ${firebaseMessages.size} messages from Firebase")
                    }

                    // Update conversation's last message to reflect any deletions
                    updateConversationLastMessage(conversationId)

                    // Automatically unarchive conversation if there are new messages
                    if (newMessagesForNotification.isNotEmpty()) {
                        val isArchived = conversationDao.isConversationArchived(conversationId) ?: false
                        if (isArchived) {
                            conversationDao.unarchiveConversation(conversationId)
                            Log.d(TAG, "Auto-unarchived conversation $conversationId due to new message")
                        }
                    }

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

    // Muestra notificaciones para mensajes nuevos recibidos
    private suspend fun showNotificationsForNewMessages(
        conversationId: String,
        messages: List<Pair<Message, String>>
    ) {
        try {
            // Verificar si la conversación está silenciada
            val isMuted = conversationDao.isConversationMuted(conversationId) ?: false
            Log.d(TAG, "Checking mute state for conversation $conversationId: isMuted=$isMuted")
            if (isMuted) {
                Log.d(TAG, "Conversation $conversationId is MUTED, skipping notifications")
                return
            }
            Log.d(TAG, "Conversation $conversationId is NOT muted, showing notification")

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

    // Establece la conversación actualmente abierta
    fun setActiveConversation(conversationId: String?) {
        activeConversationId = conversationId
        if (conversationId != null) {
            chatNotificationManager.cancelNotification(conversationId)
        }
    }

    // Sincroniza un mensaje individual a Firebase
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

    // Comprime y sube una imagen a Firebase Storage
    private suspend fun uploadImageCompressed(imageBytes: ByteArray): String? = suspendCoroutine { cont ->
        try {
            // Compress image first
            val compressedBytes = imageCompressor.compress(imageBytes)
            Log.d(TAG, "Compressed image: ${imageBytes.size} -> ${compressedBytes.size} bytes")

            val fileName = "chat_images/${UUID.randomUUID()}.jpg"
            val imageRef = storage.child(fileName)
            Log.d(TAG, "Uploading to: $fileName")

            imageRef.putBytes(compressedBytes)
                .addOnSuccessListener {
                    Log.d(TAG, "Upload successful, getting download URL...")
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        Log.d(TAG, "Image URL obtained: $uri")
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

    // Sube un video a Firebase Storage
    private suspend fun uploadVideo(videoBytes: ByteArray): String? = suspendCoroutine { cont ->
        try {
            Log.d(TAG, "Uploading video: ${videoBytes.size} bytes")

            val fileName = "chat_videos/${UUID.randomUUID()}.mp4"
            val videoRef = storage.child(fileName)
            Log.d(TAG, "Uploading to: $fileName")

            videoRef.putBytes(videoBytes)
                .addOnSuccessListener {
                    Log.d(TAG, "Upload successful, getting download URL...")
                    videoRef.downloadUrl.addOnSuccessListener { uri ->
                        Log.d(TAG, "Video URL obtained: $uri")
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

    // Actualiza el último mensaje de una conversación en la base de datos local
    private suspend fun updateConversationLastMessage(conversationId: String) {
        try {
            // Reciba el último mensaje no eliminado
            val latestMessage = messageDao.getLatestMessages(conversationId, 1).firstOrNull()

            if (latestMessage != null) {
                // Actualiza la conversación con el último mensaje.
                conversationDao.updateLastMessage(
                    conversationId = conversationId,
                    text = latestMessage.text,
                    timestamp = latestMessage.timestamp,
                    senderId = latestMessage.senderId
                )
            } else {
                // No quedan mensajes, borrando el último mensaje
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

    // Actualiza el último mensaje de una conversación en Firebase
    private suspend fun updateFirebaseConversationLastMessage(conversationId: String) {
        try {
            // Reciba el último mensaje no eliminado
            val latestMessage = messageDao.getLatestMessages(conversationId, 1).firstOrNull()

            val conversationRef = firebaseDb.child("conversations").child(conversationId)

            if (latestMessage != null) {
                // Actualiza la conversación de Firebase con el mensaje más reciente.
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
                // No quedan mensajes, elimina el último mensaje de la conversación.
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

    // Obtiene información del producto asociado a una conversación
    override suspend fun getProductInfoForConversation(conversationId: String): ProductInfo? {
        return try {
            // Por ahora retorna null - puede implementarse si se necesita
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading product info", e)
            null
        }
    }

    // Observa el estado de escritura de otro usuario en tiempo real
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

    // Obtiene el conteo total de mensajes sin leer para un usuario
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

    // Silencia las notificaciones de una conversación
    override suspend fun muteConversation(conversationId: String) {
        withContext(Dispatchers.IO) {
            try {
                conversationDao.muteConversation(conversationId)
                Log.d(TAG, "Conversation $conversationId muted")
            } catch (e: Exception) {
                Log.e(TAG, "Error muting conversation", e)
            }
        }
    }

    // Activa las notificaciones de una conversación
    override suspend fun unmuteConversation(conversationId: String) {
        withContext(Dispatchers.IO) {
            try {
                conversationDao.unmuteConversation(conversationId)
                Log.d(TAG, "Conversation $conversationId unmuted")
            } catch (e: Exception) {
                Log.e(TAG, "Error unmuting conversation", e)
            }
        }
    }

    // Verifica si una conversación está silenciada
    override suspend fun isConversationMuted(conversationId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                conversationDao.isConversationMuted(conversationId) ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if conversation is muted", e)
                false
            }
        }
    }

    // Archiva una conversación (oculta sin eliminar datos)
    override suspend fun archiveConversation(conversationId: String, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Archivar localmente (solo ocultar de la lista)
                conversationDao.archiveConversation(conversationId)
                Log.d(TAG, "Archived conversation $conversationId locally")

                // NO eliminamos de Firebase - solo ocultamos localmente
                // Si el otro usuario envía un mensaje, la conversación se desarchivará automáticamente

                Log.d(TAG, "Conversation $conversationId archived successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error archiving conversation", e)
                throw e
            }
        }
    }

    // Desarchiva una conversación (la hace visible de nuevo)
    override suspend fun unarchiveConversation(conversationId: String) {
        withContext(Dispatchers.IO) {
            try {
                conversationDao.unarchiveConversation(conversationId)
                Log.d(TAG, "Unarchived conversation $conversationId")
            } catch (e: Exception) {
                Log.e(TAG, "Error unarchiving conversation", e)
                throw e
            }
        }
    }

    // Bloquea una conversación (impide sincronización de mensajes)
    override suspend fun blockConversation(conversationId: String) {
        withContext(Dispatchers.IO) {
            try {
                conversationDao.blockConversation(conversationId)
                Log.d(TAG, "Blocked conversation $conversationId")
            } catch (e: Exception) {
                Log.e(TAG, "Error blocking conversation", e)
                throw e
            }
        }
    }

    // Desbloquea una conversación (permite sincronización de mensajes)
    override suspend fun unblockConversation(conversationId: String) {
        withContext(Dispatchers.IO) {
            try {
                conversationDao.unblockConversation(conversationId)
                Log.d(TAG, "Unblocked conversation $conversationId")
            } catch (e: Exception) {
                Log.e(TAG, "Error unblocking conversation", e)
                throw e
            }
        }
    }

    // Verifica si una conversación está bloqueada
    override suspend fun isConversationBlocked(conversationId: String): Boolean {
        return withContext(Dispatchers.IO) {
            conversationDao.isConversationBlocked(conversationId) ?: false
        }
    }

    // Elimina una conversación completa (mensajes y conversación)
    // DEPRECATED: Ahora usa archiveConversation para evitar perder datos
    @Deprecated("Use archiveConversation instead to avoid data loss")
    override suspend fun deleteConversation(conversationId: String, userId: String) {
        // Redirigir a archiveConversation en lugar de eliminar
        archiveConversation(conversationId, userId)
    }
}
