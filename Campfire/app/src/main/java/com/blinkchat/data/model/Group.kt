package com.blinkchat.data.model

import com.google.firebase.Timestamp

/**
 * Group data model representing a chat group in BlinkChat
 */
data class Group(
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val members: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val expiryContract: ExpiryContract = ExpiryContract.Timed(24 * 60 * 60 * 1000L),
    val createdAt: Timestamp = Timestamp.now(),
    val lastActive: Timestamp = Timestamp.now(),
    val messageCount: Int = 0,
    val joinCode: String = "",
    val isActive: Boolean = true,
    val maxMembers: Int = 50
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", emptyList(), emptyList(), ExpiryContract.Timed(24 * 60 * 60 * 1000L), Timestamp.now(), Timestamp.now(), 0, "", true, 50)
    
    /**
     * Checks if the group has expired based on its expiry contract
     */
    fun hasExpired(): Boolean {
        val now = Timestamp.now()
        return when (val contract = expiryContract) {
            is ExpiryContract.Timed -> {
                val expiryTime = contract.getExpiryTime(createdAt)
                now.seconds >= expiryTime.seconds
            }
            is ExpiryContract.MessageLimit -> {
                messageCount >= contract.maxMessages
            }
            is ExpiryContract.Inactivity -> {
                val expiryTime = contract.getExpiryTime(lastActive)
                now.seconds >= expiryTime.seconds
            }
            is ExpiryContract.PollBased -> false // Handled separately
        }
    }
    
    /**
     * Gets remaining time until expiry in milliseconds
     */
    fun getRemainingTimeMillis(): Long {
        val now = Timestamp.now()
        return when (val contract = expiryContract) {
            is ExpiryContract.Timed -> {
                val expiryTime = contract.getExpiryTime(createdAt)
                maxOf(0L, (expiryTime.seconds - now.seconds) * 1000)
            }
            is ExpiryContract.Inactivity -> {
                val expiryTime = contract.getExpiryTime(lastActive)
                maxOf(0L, (expiryTime.seconds - now.seconds) * 1000)
            }
            else -> Long.MAX_VALUE
        }
    }
    
    /**
     * Checks if user is admin of the group
     */
    fun isUserAdmin(userId: String): Boolean {
        return adminIds.contains(userId) || createdBy == userId
    }
    
    /**
     * Checks if user is member of the group
     */
    fun isUserMember(userId: String): Boolean {
        return members.contains(userId)
    }
    
    /**
     * Converts Group to Map for Firebase storage
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "groupId" to groupId,
            "name" to name,
            "description" to description,
            "createdBy" to createdBy,
            "members" to members,
            "adminIds" to adminIds,
            "expiryContract" to expiryContract.toMap(),
            "createdAt" to createdAt,
            "lastActive" to lastActive,
            "messageCount" to messageCount,
            "joinCode" to joinCode,
            "isActive" to isActive,
            "maxMembers" to maxMembers
        )
    }
    
    companion object {
        /**
         * Creates Group from Firebase Map
         */
        fun fromMap(map: Map<String, Any>): Group {
            return Group(
                groupId = map["groupId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                createdBy = map["createdBy"] as? String ?: "",
                members = (map["members"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                adminIds = (map["adminIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                expiryContract = (map["expiryContract"] as? Map<String, Any>)?.let { 
                    ExpiryContract.fromMap(it) 
                } ?: ExpiryContract.Timed(24 * 60 * 60 * 1000L),
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                lastActive = map["lastActive"] as? Timestamp ?: Timestamp.now(),
                messageCount = (map["messageCount"] as? Long)?.toInt() ?: 0,
                joinCode = map["joinCode"] as? String ?: "",
                isActive = map["isActive"] as? Boolean ?: true,
                maxMembers = (map["maxMembers"] as? Long)?.toInt() ?: 50
            )
        }
    }
}
