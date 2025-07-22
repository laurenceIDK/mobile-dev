package com.blinkchat.data.repository

import com.blinkchat.data.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for message operations
 */
interface MessageRepository {
    
    /**
     * Sends a message to a group
     */
    suspend fun sendMessage(message: Message): Result<Unit>
    
    /**
     * Gets messages for a group with pagination
     */
    suspend fun getGroupMessages(
        groupId: String,
        limit: Int = 50,
        lastMessageId: String? = null
    ): Result<List<Message>>
    
    /**
     * Observes real-time messages for a group
     */
    fun observeGroupMessages(groupId: String): Flow<List<Message>>
    
    /**
     * Marks message as read by user
     */
    suspend fun markMessageAsRead(messageId: String, userId: String): Result<Unit>
    
    /**
     * Deletes a message
     */
    suspend fun deleteMessage(messageId: String, deletedBy: String): Result<Unit>
    
    /**
     * Edits a message
     */
    suspend fun editMessage(messageId: String, newContent: String, editedBy: String): Result<Unit>
    
    /**
     * Gets messages that should self-destruct
     */
    suspend fun getMessagesToDestruct(): Result<List<Message>>
    
    /**
     * Deletes all messages in a group
     */
    suspend fun deleteAllGroupMessages(groupId: String): Result<Unit>
    
    /**
     * Gets unread message count for user in a group
     */
    suspend fun getUnreadMessageCount(groupId: String, userId: String): Result<Int>
    
    /**
     * Searches messages in a group
     */
    suspend fun searchMessages(groupId: String, query: String): Result<List<Message>>
    
    /**
     * Gets message by ID
     */
    suspend fun getMessage(messageId: String): Result<Message?>
    
    /**
     * Reports a message
     */
    suspend fun reportMessage(messageId: String, reportedBy: String, reason: String): Result<Unit>
    
    /**
     * Gets latest message for each group (for group list preview)
     */
    suspend fun getLatestGroupMessages(groupIds: List<String>): Result<Map<String, Message>>
}
