package com.campfire.data.repository

import com.campfire.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user authentication operations
 */
interface AuthRepository {
    
    /**
     * Signs in user with email and password
     */
    suspend fun signIn(email: String, password: String): Result<User>
    
    /**
     * Signs up new user with email, password and name
     */
    suspend fun signUp(email: String, password: String, name: String): Result<User>
    
    /**
     * Signs out current user
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Gets current authenticated user
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Observes authentication state changes
     */
    fun observeAuthState(): Flow<User?>
    
    /**
     * Sends password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    
    /**
     * Checks if user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean
}
