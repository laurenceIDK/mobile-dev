package com.blinkchat.data.repository

import com.blinkchat.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user profile operations
 */
interface UserRepository {
    
    /**
     * Creates or updates user profile
     */
    suspend fun saveUser(user: User): Result<Unit>
    
    /**
     * Gets user by ID
     */
    suspend fun getUser(userId: String): Result<User?>
    
    /**
     * Updates user profile
     */
    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit>
    
    /**
     * Uploads user profile picture
     */
    suspend fun uploadProfilePicture(userId: String, imageUri: String): Result<String>
    
    /**
     * Updates user online status
     */
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit>
    
    /**
     * Searches users by name or email
     */
    suspend fun searchUsers(query: String): Result<List<User>>
    
    /**
     * Observes user profile changes
     */
    fun observeUser(userId: String): Flow<User?>
    
    /**
     * Gets multiple users by their IDs
     */
    suspend fun getUsers(userIds: List<String>): Result<List<User>>
    
    /**
     * Deletes user account and all associated data
     */
    suspend fun deleteUser(userId: String): Result<Unit>
}
