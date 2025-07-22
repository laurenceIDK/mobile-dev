package com.campfire.domain.usecase

import com.campfire.data.model.Message
import com.campfire.data.repository.MessageRepository
import com.campfire.data.repository.GroupRepository
import com.campfire.utils.ValidationUtils
import com.campfire.utils.FirebaseUtils
import javax.inject.Inject

/**
 * Use case for message operations
 */
class MessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val groupRepository: GroupRepository
) {
    
    /**
     * Sends a message to a group with validation
     */
    suspend fun sendMessage(message: Message): Result<Unit> {
        val contentError = ValidationUtils.validateMessage(message.content)
        if (contentError != null) {
            return Result.failure(Exception(contentError))
        }
        
        // Verify user is member of the group
        val groupResult = groupRepository.getGroup(message.groupId)
        if (groupResult.isFailure) {
            return Result.failure(Exception("Group not found"))
        }
        
        val group = groupResult.getOrNull()
        if (group == null || !group.isUserMember(message.senderId)) {
            return Result.failure(Exception("You are not a member of this group"))
        }
        
        // Check if group is still active
        if (!group.isActive) {
            return Result.failure(Exception("This group is no longer active"))
        }
        
        // Check if group has expired
        if (group.hasExpired()) {
            return Result.failure(Exception("This group has expired"))
        }
        
        // Send the message
        val sendResult = messageRepository.sendMessage(message)
        if (sendResult.isFailure) {
            return sendResult
        }
        
        // Update group activity and increment message count
        groupRepository.updateGroupActivity(message.groupId)
        groupRepository.incrementMessageCount(message.groupId)
        
        return Result.success(Unit)
    }
    
    /**
     * Gets messages for a group with pagination
     */
    suspend fun getGroupMessages(
        groupId: String,
        userId: String,
        limit: Int = 50,
        lastMessageId: String? = null
    ): Result<List<Message>> {
        // Verify user is member of the group
        val groupResult = groupRepository.getGroup(groupId)
        if (groupResult.isFailure) {
            return Result.failure(Exception("Group not found"))
        }
        
        val group = groupResult.getOrNull()
        if (group == null || !group.isUserMember(userId)) {
            return Result.failure(Exception("You are not a member of this group"))
        }
        
        return messageRepository.getGroupMessages(groupId, limit, lastMessageId)
    }
    
    /**
     * Observes real-time messages for a group
     */
    suspend fun observeGroupMessages(groupId: String, userId: String) = 
        messageRepository.observeGroupMessages(groupId)
    
    /**
     * Marks message as read by user
     */
    suspend fun markMessageAsRead(messageId: String, userId: String): Result<Unit> {
        return messageRepository.markMessageAsRead(messageId, userId)
    }
    
    /**
     * Deletes a message
     */
    suspend fun deleteMessage(messageId: String, deletedBy: String): Result<Unit> {
        return messageRepository.deleteMessage(messageId, deletedBy)
    }
    
    /**
     * Edits a message
     */
    suspend fun editMessage(messageId: String, newContent: String, editedBy: String): Result<Unit> {
        val contentError = ValidationUtils.validateMessage(newContent)
        if (contentError != null) {
            return Result.failure(Exception(contentError))
        }
        
        return messageRepository.editMessage(messageId, newContent, editedBy)
    }
    
    /**
     * Processes self-destructing messages
     */
    suspend fun processMessageDestruction(): Result<Int> {
        return try {
            val messagesToDestructResult = messageRepository.getMessagesToDestruct()
            if (messagesToDestructResult.isFailure) {
                return Result.failure(
                    messagesToDestructResult.exceptionOrNull() ?: Exception("Failed to get messages to destruct")
                )
            }
            
            val messagesToDestruct = messagesToDestructResult.getOrNull() ?: emptyList()
            var destructedCount = 0
            
            messagesToDestruct.forEach { message ->
                val deleteResult = messageRepository.deleteMessage(message.messageId, "system")
                if (deleteResult.isSuccess) {
                    destructedCount++
                }
            }
            
            Result.success(destructedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets unread message count for user in a group
     */
    suspend fun getUnreadMessageCount(groupId: String, userId: String): Result<Int> {
        return messageRepository.getUnreadMessageCount(groupId, userId)
    }
    
    /**
     * Searches messages in a group
     */
    suspend fun searchMessages(groupId: String, query: String, userId: String): Result<List<Message>> {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }
        
        // Verify user is member of the group
        val groupResult = groupRepository.getGroup(groupId)
        if (groupResult.isFailure) {
            return Result.failure(Exception("Group not found"))
        }
        
        val group = groupResult.getOrNull()
        if (group == null || !group.isUserMember(userId)) {
            return Result.failure(Exception("You are not a member of this group"))
        }
        
        return messageRepository.searchMessages(groupId, query)
    }
    
    /**
     * Gets message by ID
     */
    suspend fun getMessage(messageId: String): Result<Message?> {
        return messageRepository.getMessage(messageId)
    }
    
    /**
     * Reports a message
     */
    suspend fun reportMessage(messageId: String, reportedBy: String, reason: String): Result<Unit> {
        if (reason.isBlank()) {
            return Result.failure(Exception("Report reason cannot be empty"))
        }
        
        return messageRepository.reportMessage(messageId, reportedBy, reason)
    }
    
    /**
     * Gets latest message for each group (for group list preview)
     */
    suspend fun getLatestGroupMessages(groupIds: List<String>): Result<Map<String, Message>> {
        return messageRepository.getLatestGroupMessages(groupIds)
    }
    
    /**
     * Sends a system message to inform about group events
     */
    suspend fun sendSystemMessage(groupId: String, content: String): Result<Unit> {
        val systemMessage = Message.createSystemMessage(groupId, content)
        return messageRepository.sendMessage(systemMessage)
    }
    
    /**
     * Notifies group about member joining
     */
    suspend fun notifyMemberJoined(groupId: String, memberName: String): Result<Unit> {
        val content = "$memberName joined the group"
        return sendSystemMessage(groupId, content)
    }
    
    /**
     * Notifies group about member leaving
     */
    suspend fun notifyMemberLeft(groupId: String, memberName: String): Result<Unit> {
        val content = "$memberName left the group"
        return sendSystemMessage(groupId, content)
    }
    
    /**
     * Notifies group about expiry warning
     */
    suspend fun notifyGroupExpiryWarning(groupId: String, timeRemaining: String): Result<Unit> {
        val content = "⚠️ Group expires in $timeRemaining"
        return sendSystemMessage(groupId, content)
    }
    
    /**
     * Creates a message with self-destruct timer
     */
    fun createSelfDestructMessage(
        groupId: String,
        senderId: String,
        senderName: String,
        content: String,
        selfDestructDuration: Long? = null
    ): Message {
        return Message(
            messageId = FirebaseUtils.generateMessageId(),
            groupId = groupId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            timestamp = com.google.firebase.Timestamp.now(),
            selfDestructDuration = selfDestructDuration
        )
    }
}
