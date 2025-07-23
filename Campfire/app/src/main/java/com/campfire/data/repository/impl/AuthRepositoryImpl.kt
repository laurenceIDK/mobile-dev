package com.campfire.data.repository.impl

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import com.campfire.data.model.User
import com.campfire.data.repository.AuthRepository
import com.campfire.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Firebase implementation of AuthRepository
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
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
    
    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Check if user profile exists in Firestore
                val userResult = userRepository.getUser(firebaseUser.uid)
                if (userResult.isSuccess && userResult.getOrNull() != null) {
                    // Update online status for existing user
                    userRepository.updateOnlineStatus(firebaseUser.uid, true)
                    Result.success(userResult.getOrNull()!!)
                } else {
                    // Create user profile for new Google sign-in user
                    val user = User(
                        uid = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "",
                        email = firebaseUser.email ?: "",
                        profileUrl = firebaseUser.photoUrl?.toString(),
                        isOnline = true,
                        lastSeen = Timestamp.now()
                    )
                    
                    val saveResult = userRepository.saveUser(user)
                    if (saveResult.isSuccess) {
                        Result.success(user)
                    } else {
                        Result.failure(saveResult.exceptionOrNull() ?: Exception("Failed to save user profile"))
                    }
                }
            } else {
                Result.failure(Exception("Google authentication failed"))
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
            
            // Firebase sign out
            firebaseAuth.signOut()
            
            // Clear credential state from all credential providers
            try {
                val credentialManager = CredentialManager.create(context)
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
            } catch (e: ClearCredentialException) {
                // Log the error but don't fail the sign out process
                // The user is still signed out from Firebase
                android.util.Log.e("AuthRepository", "Couldn't clear user credentials: ${e.localizedMessage}")
            }
            
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
