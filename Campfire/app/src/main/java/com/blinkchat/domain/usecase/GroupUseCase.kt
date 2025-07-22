package com.blinkchat.domain.usecase

import com.blinkchat.data.model.Group
import com.blinkchat.data.model.ExpiryContract
import com.blinkchat.data.repository.GroupRepository
import com.blinkchat.data.repository.MessageRepository
import com.blinkchat.utils.ValidationUtils
import javax.inject.Inject

/**
 * Use case for group management operations
 */
class GroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository
) {
    
    /**
     * Creates a new group with validation
     */
    suspend fun createGroup(
        name: String,
        description: String,
        createdBy: String,
        expiryContract: ExpiryContract,
        maxMembers: Int = 50
    ): Result<Group> {
        val nameError = ValidationUtils.validateGroupName(name)
        if (nameError != null) {
            return Result.failure(Exception(nameError))
        }
        
        if (maxMembers < 2 || maxMembers > 100) {
            return Result.failure(Exception("Group size must be between 2 and 100 members"))
        }
        
        return groupRepository.createGroup(name, description, createdBy, expiryContract, maxMembers)
    }
    
    /**
     * Gets group by ID
     */
    suspend fun getGroup(groupId: String): Result<Group?> {
        return groupRepository.getGroup(groupId)
    }
    
    /**
     * Gets groups where user is a member
     */
    suspend fun getUserGroups(userId: String): Result<List<Group>> {
        return groupRepository.getUserGroups(userId)
    }
    
    /**
     * Joins group using join code with validation
     */
    suspend fun joinGroupByCode(userId: String, joinCode: String): Result<Group> {
        val codeError = ValidationUtils.validateJoinCode(joinCode)
        if (codeError != null) {
            return Result.failure(Exception(codeError))
        }
        
        return groupRepository.joinGroupByCode(userId, joinCode.uppercase())
    }
    
    /**
     * Adds member to group
     */
    suspend fun addMemberToGroup(groupId: String, userId: String, addedBy: String): Result<Unit> {
        return groupRepository.addMemberToGroup(groupId, userId, addedBy)
    }
    
    /**
     * Removes member from group
     */
    suspend fun removeMemberFromGroup(groupId: String, userId: String, removedBy: String): Result<Unit> {
        return groupRepository.removeMemberFromGroup(groupId, userId, removedBy)
    }
    
    /**
     * Updates group information
     */
    suspend fun updateGroup(groupId: String, updates: Map<String, Any>): Result<Unit> {
        // Validate updates if they contain name
        if (updates.containsKey("name")) {
            val name = updates["name"] as? String ?: ""
            val nameError = ValidationUtils.validateGroupName(name)
            if (nameError != null) {
                return Result.failure(Exception(nameError))
            }
        }
        
        return groupRepository.updateGroup(groupId, updates)
    }
    
    /**
     * Deletes group and all associated data
     */
    suspend fun deleteGroup(groupId: String): Result<Unit> {
        // First delete all messages in the group
        val deleteMessagesResult = messageRepository.deleteAllGroupMessages(groupId)
        if (deleteMessagesResult.isFailure) {
            return Result.failure(
                deleteMessagesResult.exceptionOrNull() ?: Exception("Failed to delete group messages")
            )
        }
        
        // Then delete the group
        return groupRepository.deleteGroup(groupId)
    }
    
    /**
     * Observes group changes
     */
    fun observeGroup(groupId: String) = groupRepository.observeGroup(groupId)
    
    /**
     * Observes user's groups
     */
    fun observeUserGroups(userId: String) = groupRepository.observeUserGroups(userId)
    
    /**
     * Updates group's last activity timestamp
     */
    suspend fun updateGroupActivity(groupId: String): Result<Unit> {
        return groupRepository.updateGroupActivity(groupId)
    }
    
    /**
     * Increments message count for the group and checks expiry
     */
    suspend fun incrementMessageCount(groupId: String): Result<Unit> {
        val incrementResult = groupRepository.incrementMessageCount(groupId)
        if (incrementResult.isFailure) {
            return incrementResult
        }
        
        // Check if group has expired after incrementing message count
        val expiryResult = groupRepository.checkGroupExpiry(groupId)
        if (expiryResult.isSuccess && expiryResult.getOrNull() == true) {
            // Group has expired, mark it as inactive
            groupRepository.deleteGroup(groupId)
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Checks if group has expired based on its contract
     */
    suspend fun checkGroupExpiry(groupId: String): Result<Boolean> {
        return groupRepository.checkGroupExpiry(groupId)
    }
    
    /**
     * Gets groups that have expired and need cleanup
     */
    suspend fun getExpiredGroups(): Result<List<Group>> {
        return groupRepository.getExpiredGroups()
    }
    
    /**
     * Makes user admin of the group
     */
    suspend fun makeUserAdmin(groupId: String, userId: String, promotedBy: String): Result<Unit> {
        return groupRepository.makeUserAdmin(groupId, userId, promotedBy)
    }
    
    /**
     * Removes admin privileges from user
     */
    suspend fun removeAdminPrivileges(groupId: String, userId: String, removedBy: String): Result<Unit> {
        return groupRepository.removeAdminPrivileges(groupId, userId, removedBy)
    }
    
    /**
     * Regenerates join code for group
     */
    suspend fun regenerateJoinCode(groupId: String): Result<String> {
        return groupRepository.regenerateJoinCode(groupId)
    }
    
    /**
     * Cleans up expired groups
     */
    suspend fun cleanupExpiredGroups(): Result<Int> {
        return try {
            val expiredGroupsResult = getExpiredGroups()
            if (expiredGroupsResult.isFailure) {
                return Result.failure(
                    expiredGroupsResult.exceptionOrNull() ?: Exception("Failed to get expired groups")
                )
            }
            
            val expiredGroups = expiredGroupsResult.getOrNull() ?: emptyList()
            var cleanedCount = 0
            
            expiredGroups.forEach { group ->
                val deleteResult = deleteGroup(group.groupId)
                if (deleteResult.isSuccess) {
                    cleanedCount++
                }
            }
            
            Result.success(cleanedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
