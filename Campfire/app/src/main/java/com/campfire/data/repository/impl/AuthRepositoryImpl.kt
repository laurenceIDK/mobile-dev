package com.campfire.data.repository.impl

import com.campfire.data.model.User
import com.campfire.data.repository.AuthRepository
import com.campfire.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of AuthRepository
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) : AuthRepository {
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Get user profile from Firestore or create if doesn't exist
                val userResult = userRepository.getUser(firebaseUser.uid)
                if (userResult.isSuccess && userResult.getOrNull() != null) {
                    // Update online status
                    userRepository.updateOnlineStatus(firebaseUser.uid, true)
                    Result.success(userResult.getOrNull()!!)
                } else {
                    // Create user profile if doesn't exist
                    val user = User(
                        uid = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "",
                        email = firebaseUser.email ?: "",
                        profileUrl = firebaseUser.photoUrl?.toString(),
                        isOnline = true,
                        lastSeen = Timestamp.now()
                    )
                    userRepository.saveUser(user)
                    Result.success(user)
                }
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Update Firebase user profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                
                // Create user profile in Firestore
                val user = User(
                    uid = firebaseUser.uid,
                    name = name,
                    email = email,
                    isOnline = true,
                    lastSeen = Timestamp.now()
                )
                
                val saveResult = userRepository.saveUser(user)
                if (saveResult.isSuccess) {
                    Result.success(user)
                } else {
                    Result.failure(saveResult.exceptionOrNull() ?: Exception("Failed to save user profile"))
                }
            } else {
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                // Update online status before signing out
                userRepository.updateOnlineStatus(currentUser.uid, false)
            }
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        
        return try {
            val userResult = userRepository.getUser(firebaseUser.uid)
            userResult.getOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                // Get user from Firestore
                kotlinx.coroutines.launch {
                    try {
                        val userResult = userRepository.getUser(firebaseUser.uid)
                        trySend(userResult.getOrNull())
                    } catch (e: Exception) {
                        trySend(null)
                    }
                }
            } else {
                trySend(null)
            }
        }
        
        firebaseAuth.addAuthStateListener(listener)
        
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
