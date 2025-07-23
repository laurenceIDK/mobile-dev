package com.campfire.domain.usecase

import com.campfire.data.model.User
import com.campfire.data.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user authentication operations
 */
class AuthUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    /**
     * Signs in user with email and password
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        return if (email.isBlank() || password.isBlank()) {
            Result.failure(Exception("Email and password cannot be empty"))
        } else {
            authRepository.signIn(email, password)
        }
    }
    
    /**
     * Signs in user with Google
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return if (idToken.isBlank()) {
            Result.failure(Exception("Google ID token is required"))
        } else {
            authRepository.signInWithGoogle(idToken)
        }
    }
    
    /**
     * Signs up new user with validation
     */
    suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return when {
            name.isBlank() -> Result.failure(Exception("Name cannot be empty"))
            email.isBlank() -> Result.failure(Exception("Email cannot be empty"))
            password.isBlank() -> Result.failure(Exception("Password cannot be empty"))
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Result.failure(Exception("Please enter a valid email address"))
            }
            password.length < 6 -> {
                Result.failure(Exception("Password must be at least 6 characters long"))
            }
            else -> authRepository.signUp(email, password, name)
        }
    }
    
    /**
     * Signs out current user
     */
    suspend fun signOut(): Result<Unit> {
        return authRepository.signOut()
    }
    
    /**
     * Gets current authenticated user
     */
    suspend fun getCurrentUser(): User? {
        return authRepository.getCurrentUser()
    }
    
    /**
     * Observes authentication state changes
     */
    fun observeAuthState() = authRepository.observeAuthState()
    
    /**
     * Sends password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return if (email.isBlank()) {
            Result.failure(Exception("Email cannot be empty"))
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Result.failure(Exception("Please enter a valid email address"))
        } else {
            authRepository.sendPasswordResetEmail(email)
        }
    }
    
    /**
     * Checks if user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return authRepository.isUserAuthenticated()
    }
}
