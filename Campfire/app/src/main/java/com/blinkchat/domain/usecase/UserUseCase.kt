package com.blinkchat.domain.usecase

import com.blinkchat.data.model.User
import com.blinkchat.data.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for user profile operations
 */
class UserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    
    /**
     * Creates or updates user profile
     */
    suspend fun saveUser(user: User): Result<Unit> {
        return if (user.name.isBlank()) {
            Result.failure(Exception("Name cannot be empty"))
        } else if (user.email.isBlank()) {
            Result.failure(Exception("Email cannot be empty"))
        } else {
            userRepository.saveUser(user)
        }
    }
    
    /**
     * Gets user by ID
     */
    suspend fun getUser(userId: String): Result<User?> {
        return userRepository.getUser(userId)
    }
    
    /**
     * Updates user profile with validation
     */
    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> {
        // Validate name if being updated
        if (updates.containsKey("name")) {
            val name = updates["name"] as? String ?: ""
            if (name.isBlank()) {
                return Result.failure(Exception("Name cannot be empty"))
            }
        }
        
        // Validate email if being updated
        if (updates.containsKey("email")) {
            val email = updates["email"] as? String ?: ""
            if (email.isBlank()) {
                return Result.failure(Exception("Email cannot be empty"))
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                return Result.failure(Exception("Please enter a valid email address"))
            }
        }
        
        return userRepository.updateUser(userId, updates)
    }
    
    /**
     * Uploads user profile picture
     */
    suspend fun uploadProfilePicture(userId: String, imageUri: String): Result<String> {
        return userRepository.uploadProfilePicture(userId, imageUri)
    }
    
    /**
     * Updates user online status
     */
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return userRepository.updateOnlineStatus(userId, isOnline)
    }
    
    /**
     * Searches users by name or email
     */
    suspend fun searchUsers(query: String): Result<List<User>> {
        return if (query.isBlank()) {
            Result.success(emptyList())
        } else {
            userRepository.searchUsers(query.trim())
        }
    }
    
    /**
     * Observes user profile changes
     */
    fun observeUser(userId: String) = userRepository.observeUser(userId)
    
    /**
     * Gets multiple users by their IDs
     */
    suspend fun getUsers(userIds: List<String>): Result<List<User>> {
        return userRepository.getUsers(userIds)
    }
    
    /**
     * Deletes user account and all associated data
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return userRepository.deleteUser(userId)
    }
    
    /**
     * Updates user profile name
     */
    suspend fun updateUserName(userId: String, name: String): Result<Unit> {
        return if (name.isBlank()) {
            Result.failure(Exception("Name cannot be empty"))
        } else if (name.length > 50) {
            Result.failure(Exception("Name cannot exceed 50 characters"))
        } else {
            updateUser(userId, mapOf("name" to name.trim()))
        }
    }
    
    /**
     * Validates and updates user email
     */
    suspend fun updateUserEmail(userId: String, email: String): Result<Unit> {
        return if (email.isBlank()) {
            Result.failure(Exception("Email cannot be empty"))
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Result.failure(Exception("Please enter a valid email address"))
        } else {
            updateUser(userId, mapOf("email" to email.trim().lowercase()))
        }
    }
    
    /**
     * Gets user's display name (fallback to email if name is empty)
     */
    suspend fun getUserDisplayName(userId: String): String {
        val userResult = getUser(userId)
        val user = userResult.getOrNull()
        
        return when {
            user == null -> "Unknown User"
            user.name.isNotBlank() -> user.name
            user.email.isNotBlank() -> user.email.substringBefore("@")
            else -> "User"
        }
    }
    
    /**
     * Gets user initials for avatar display
     */
    suspend fun getUserInitials(userId: String): String {
        val userResult = getUser(userId)
        val user = userResult.getOrNull()
        
        return when {
            user == null -> "?"
            user.name.isNotBlank() -> {
                user.name.split(" ")
                    .take(2)
                    .map { it.firstOrNull()?.uppercaseChar() ?: "" }
                    .joinToString("")
            }
            user.email.isNotBlank() -> {
                user.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            }
            else -> "?"
        }
    }
}
