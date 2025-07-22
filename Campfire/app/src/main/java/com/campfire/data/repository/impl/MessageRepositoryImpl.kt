package com.campfire.data.repository.impl

import com.campfire.data.model.Message
import com.campfire.data.repository.MessageRepository
import com.campfire.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of MessageRepository
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MessageRepository {
    
    private val messagesCollection = firestore.collection(FirebaseUtils.MESSAGES_COLLECTION)
    
    override suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            val messageWithId = if (message.messageId.isEmpty()) {
                message.copy(messageId = FirebaseUtils.generateMessageId())
            } else {
                message
            }
            
            messagesCollection.document(messageWithId.messageId)
                .set(messageWithId.toMap())
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getGroupMessages(
        groupId: String,
        limit: Int,
        lastMessageId: String?
    ): Result<List<Message>> {
        return try {
            var query = messagesCollection
                .whereEqualTo("groupId", groupId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            // Add pagination if lastMessageId is provided
            if (lastMessageId != null) {
                val lastMessageDoc = messagesCollection.document(lastMessageId).get().await()
                if (lastMessageDoc.exists()) {
                    query = query.startAfter(lastMessageDoc)
                }
            }
            
            val querySnapshot = query.get().await()
            
            val messages = querySnapshot.documents.mapNotNull { document ->
                document.data?.let { Message.fromMap(it) }
            }.reversed() // Reverse to get chronological order
            
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeGroupMessages(groupId: String): Flow<List<Message>> = callbackFlow {
        val listener = messagesCollection
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { document ->
                    document.data?.let { Message.fromMap(it) }
                } ?: emptyList()
                
                trySend(messages)
            }
        
        awaitClose {
            listener.remove()
        }
    }
    
    override suspend fun markMessageAsRead(messageId: String, userId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val messageRef = messagesCollection.document(messageId)
                val snapshot = transaction.get(messageRef)
                
                if (snapshot.exists()) {
                    val message = Message.fromMap(snapshot.data!!)
                    if (!message.readBy.contains(userId)) {
                        val updatedReadBy = message.readBy + userId
                        transaction.update(messageRef, "readBy", updatedReadBy)
                        transaction.update(messageRef, "isRead", true)
                    }
                }
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteMessage(messageId: String, deletedBy: String): Result<Unit> {
        return try {
            // Get message to check permissions
            val messageDoc = messagesCollection.document(messageId).get().await()
            if (!messageDoc.exists()) {
                return Result.failure(Exception("Message not found"))
            }
            
            val message = Message.fromMap(messageDoc.data!!)
            
            // Only sender can delete their own messages (or system messages can be deleted by admins)
            if (message.senderId != deletedBy && message.messageType != Message.MessageType.SYSTEM) {
                return Result.failure(Exception("You can only delete your own messages"))
            }
            
            messagesCollection.document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun editMessage(messageId: String, newContent: String, editedBy: String): Result<Unit> {
        return try {
            // Get message to check permissions
            val messageDoc = messagesCollection.document(messageId).get().await()
            if (!messageDoc.exists()) {
                return Result.failure(Exception("Message not found"))
            }
            
            val message = Message.fromMap(messageDoc.data!!)
            
            // Only sender can edit their own messages
            if (message.senderId != editedBy) {
                return Result.failure(Exception("You can only edit your own messages"))
            }
            
            // Cannot edit system messages
            if (message.messageType == Message.MessageType.SYSTEM) {
                return Result.failure(Exception("Cannot edit system messages"))
            }
            
            val updates = mapOf(
                "content" to newContent,
                "isEdited" to true,
                "editedAt" to Timestamp.now()
            )
            
            messagesCollection.document(messageId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getMessagesToDestruct(): Result<List<Message>> {
        return try {
            val now = Timestamp.now()
            
            // Get messages that are read and have self-destruct timer
            val querySnapshot = messagesCollection
                .whereEqualTo("isRead", true)
                .whereNotEqualTo("selfDestructDuration", null)
                .get()
                .await()
            
            val messagesToDestruct = querySnapshot.documents.mapNotNull { document ->
                document.data?.let { Message.fromMap(it) }
            }.filter { message ->
                message.selfDestructDuration != null && message.shouldSelfDestruct()
            }
            
            Result.success(messagesToDestruct)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteAllGroupMessages(groupId: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            val querySnapshot = messagesCollection
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            
            querySnapshot.documents.forEach { document ->
                batch.delete(document.reference)
            }
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUnreadMessageCount(groupId: String, userId: String): Result<Int> {
        return try {
            val querySnapshot = messagesCollection
                .whereEqualTo("groupId", groupId)
                .whereArrayContains("readBy", userId)
                .get()
                .await()
            
            val readMessageIds = querySnapshot.documents.map { it.id }.toSet()
            
            val allMessagesSnapshot = messagesCollection
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            
            val unreadCount = allMessagesSnapshot.documents.count { document ->
                val message = Message.fromMap(document.data!!)
                message.senderId != userId && !readMessageIds.contains(document.id)
            }
            
            Result.success(unreadCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun searchMessages(groupId: String, query: String): Result<List<Message>> {
        return try {
            // Note: Firestore doesn't support full-text search natively
            // For production, consider using Algolia or Elasticsearch
            val querySnapshot = messagesCollection
                .whereEqualTo("groupId", groupId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100) // Limit for performance
                .get()
                .await()
            
            val messages = querySnapshot.documents.mapNotNull { document ->
                document.data?.let { Message.fromMap(it) }
            }.filter { message ->
                message.content.contains(query, ignoreCase = true) ||
                message.senderName.contains(query, ignoreCase = true)
            }
            
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getMessage(messageId: String): Result<Message?> {
        return try {
            val document = messagesCollection.document(messageId).get().await()
            if (document.exists()) {
                val messageData = document.data ?: return Result.success(null)
                val message = Message.fromMap(messageData)
                Result.success(message)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun reportMessage(messageId: String, reportedBy: String, reason: String): Result<Unit> {
        return try {
            val reportData = mapOf(
                "messageId" to messageId,
                "reportedBy" to reportedBy,
                "reason" to reason,
                "timestamp" to Timestamp.now()
            )
            
            firestore.collection("message_reports")
                .add(reportData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getLatestGroupMessages(groupIds: List<String>): Result<Map<String, Message>> {
        return try {
            if (groupIds.isEmpty()) {
                return Result.success(emptyMap())
            }
            
            val latestMessages = mutableMapOf<String, Message>()
            
            // Process in batches due to Firestore 'in' query limitation
            groupIds.chunked(10).forEach { batch ->
                val querySnapshot = messagesCollection
                    .whereIn("groupId", batch)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val messagesMap = mutableMapOf<String, Message>()
                
                querySnapshot.documents.forEach { document ->
                    document.data?.let { data ->
                        val message = Message.fromMap(data)
                        val groupId = message.groupId
                        
                        // Keep only the latest message for each group
                        if (!messagesMap.containsKey(groupId) || 
                            messagesMap[groupId]!!.timestamp.seconds < message.timestamp.seconds) {
                            messagesMap[groupId] = message
                        }
                    }
                }
                
                latestMessages.putAll(messagesMap)
            }
            
            Result.success(latestMessages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
