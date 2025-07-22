package com.blinkchat.data.repository

import com.blinkchat.data.model.Group
import com.blinkchat.data.model.ExpiryContract
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for group operations
 */
interface GroupRepository {
    
    /**
     * Creates a new group
     */
    suspend fun createGroup(
        name: String,
        description: String,
        createdBy: String,
        expiryContract: ExpiryContract,
        maxMembers: Int = 50
    ): Result<Group>
    
    /**
     * Gets group by ID
     */
    suspend fun getGroup(groupId: String): Result<Group?>
    
    /**
     * Gets groups where user is a member
     */
    suspend fun getUserGroups(userId: String): Result<List<Group>>
    
    /**
     * Joins group using join code
     */
    suspend fun joinGroupByCode(userId: String, joinCode: String): Result<Group>
    
    /**
     * Adds member to group
     */
    suspend fun addMemberToGroup(groupId: String, userId: String, addedBy: String): Result<Unit>
    
    /**
     * Removes member from group
     */
    suspend fun removeMemberFromGroup(groupId: String, userId: String, removedBy: String): Result<Unit>
    
    /**
     * Updates group information
     */
    suspend fun updateGroup(groupId: String, updates: Map<String, Any>): Result<Unit>
    
    /**
     * Deletes group and all associated data
     */
    suspend fun deleteGroup(groupId: String): Result<Unit>
    
    /**
     * Observes group changes
     */
    fun observeGroup(groupId: String): Flow<Group?>
    
    /**
     * Observes user's groups
     */
    fun observeUserGroups(userId: String): Flow<List<Group>>
    
    /**
     * Updates group's last activity timestamp
     */
    suspend fun updateGroupActivity(groupId: String): Result<Unit>
    
    /**
     * Increments message count for the group
     */
    suspend fun incrementMessageCount(groupId: String): Result<Unit>
    
    /**
     * Checks if group has expired based on its contract
     */
    suspend fun checkGroupExpiry(groupId: String): Result<Boolean>
    
    /**
     * Gets groups that have expired and need cleanup
     */
    suspend fun getExpiredGroups(): Result<List<Group>>
    
    /**
     * Makes user admin of the group
     */
    suspend fun makeUserAdmin(groupId: String, userId: String, promotedBy: String): Result<Unit>
    
    /**
     * Removes admin privileges from user
     */
    suspend fun removeAdminPrivileges(groupId: String, userId: String, removedBy: String): Result<Unit>
    
    /**
     * Generates new join code for group
     */
    suspend fun regenerateJoinCode(groupId: String): Result<String>
}
