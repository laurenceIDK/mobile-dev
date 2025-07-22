package com.campfire.data.model

import com.google.firebase.Timestamp

/**
 * Message data model representing a chat message in Campfire
 */
data class Message(
    val messageId: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val readBy: List<String> = emptyList(),
    val isRead: Boolean = false,
    val selfDestructDuration: Long? = null, // milliseconds after read
    val messageType: MessageType = MessageType.TEXT,
    val replyToMessageId: String? = null,
    val isEdited: Boolean = false,
    val editedAt: Timestamp? = null
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", "", Timestamp.now(), emptyList(), false, null, MessageType.TEXT, null, false, null)
    
    /**
     * Types of messages supported by Campfire
     */
    enum class MessageType {
        TEXT,
        IMAGE,
        SYSTEM // For system messages like "User joined", "Group expires in..."
    }
    
    /**
     * Checks if message should be deleted based on self-destruct timer
     */
    fun shouldSelfDestruct(): Boolean {
        if (selfDestructDuration == null || !isRead) return false
        
        val destructTime = Timestamp(
            timestamp.seconds + (selfDestructDuration / 1000),
            timestamp.nanoseconds
        )
        return Timestamp.now().seconds >= destructTime.seconds
    }
    
    /**
     * Gets remaining time until self-destruction in milliseconds
     */
    fun getRemainingDestructTime(): Long {
        if (selfDestructDuration == null || !isRead) return Long.MAX_VALUE
        
        val destructTime = Timestamp(
            timestamp.seconds + (selfDestructDuration / 1000),
            timestamp.nanoseconds
        )
        val now = Timestamp.now()
        return maxOf(0L, (destructTime.seconds - now.seconds) * 1000)
    }
    
    /**
     * Marks message as read by a user
     */
    fun markAsReadBy(userId: String): Message {
        return if (!readBy.contains(userId)) {
            copy(
                readBy = readBy + userId,
                isRead = true
            )
        } else {
            this
        }
    }
    
    /**
     * Checks if message is read by specific user
     */
    fun isReadBy(userId: String): Boolean {
        return readBy.contains(userId)
    }
    
    /**
     * Converts Message to Map for Firebase storage
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "messageId" to messageId,
            "groupId" to groupId,
            "senderId" to senderId,
            "senderName" to senderName,
            "content" to content,
            "timestamp" to timestamp,
            "readBy" to readBy,
            "isRead" to isRead,
            "selfDestructDuration" to selfDestructDuration,
            "messageType" to messageType.name,
            "replyToMessageId" to replyToMessageId,
            "isEdited" to isEdited,
            "editedAt" to editedAt
        )
    }
    
    companion object {
        /**
         * Creates Message from Firebase Map
         */
        fun fromMap(map: Map<String, Any?>): Message {
            return Message(
                messageId = map["messageId"] as? String ?: "",
                groupId = map["groupId"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                senderName = map["senderName"] as? String ?: "",
                content = map["content"] as? String ?: "",
                timestamp = map["timestamp"] as? Timestamp ?: Timestamp.now(),
                readBy = (map["readBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                isRead = map["isRead"] as? Boolean ?: false,
                selfDestructDuration = map["selfDestructDuration"] as? Long,
                messageType = try {
                    MessageType.valueOf(map["messageType"] as? String ?: "TEXT")
                } catch (e: IllegalArgumentException) {
                    MessageType.TEXT
                },
                replyToMessageId = map["replyToMessageId"] as? String,
                isEdited = map["isEdited"] as? Boolean ?: false,
                editedAt = map["editedAt"] as? Timestamp
            )
        }
        
        /**
         * Creates a system message
         */
        fun createSystemMessage(
            groupId: String,
            content: String,
            messageId: String = java.util.UUID.randomUUID().toString()
        ): Message {
            return Message(
                messageId = messageId,
                groupId = groupId,
                senderId = "system",
                senderName = "System",
                content = content,
                messageType = MessageType.SYSTEM,
                isRead = true
            )
        }
    }
}
