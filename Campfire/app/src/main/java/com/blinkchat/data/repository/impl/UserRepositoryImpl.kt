package com.blinkchat.data.repository.impl

import com.blinkchat.data.model.User
import com.blinkchat.data.repository.UserRepository
import com.blinkchat.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import android.net.Uri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of UserRepository
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {
    
    private val usersCollection = firestore.collection(FirebaseUtils.USERS_COLLECTION)
    private val storageRef = storage.reference
    
    override suspend fun saveUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUser(userId: String): Result<User?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                val userData = document.data ?: return Result.success(null)
                val user = User.fromMap(userData)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            usersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun uploadProfilePicture(userId: String, imageUri: String): Result<String> {
        return try {
            val imageRef = storageRef.child("profile_pictures/$userId.jpg")
            val uploadTask = imageRef.putFile(Uri.parse(imageUri)).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            
            // Update user profile with new image URL
            updateUser(userId, mapOf("profileUrl" to downloadUrl))
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "isOnline" to isOnline,
                "lastSeen" to Timestamp.now()
            )
            
            usersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            // Search by name (case-insensitive)
            val nameQuery = usersCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .limit(20)
                .get()
                .await()
            
            // Search by email
            val emailQuery = usersCollection
                .whereGreaterThanOrEqualTo("email", query)
                .whereLessThanOrEqualTo("email", query + "\uf8ff")
                .limit(20)
                .get()
                .await()
            
            val users = mutableSetOf<User>()
            
            // Add results from name search
            nameQuery.documents.forEach { document ->
                document.data?.let { data ->
                    users.add(User.fromMap(data))
                }
            }
            
            // Add results from email search
            emailQuery.documents.forEach { document ->
                document.data?.let { data ->
                    users.add(User.fromMap(data))
                }
            }
            
            Result.success(users.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            
            if (snapshot?.exists() == true) {
                snapshot.data?.let { data ->
                    try {
                        val user = User.fromMap(data)
                        trySend(user)
                    } catch (e: Exception) {
                        trySend(null)
                    }
                }
            } else {
                trySend(null)
            }
        }
        
        awaitClose {
            listener.remove()
        }
    }
    
    override suspend fun getUsers(userIds: List<String>): Result<List<User>> {
        return try {
            if (userIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            val users = mutableListOf<User>()
            
            // Firestore 'in' queries are limited to 10 items, so we need to batch
            userIds.chunked(10).forEach { batch ->
                val querySnapshot = usersCollection
                    .whereIn("uid", batch)
                    .get()
                    .await()
                
                querySnapshot.documents.forEach { document ->
                    document.data?.let { data ->
                        users.add(User.fromMap(data))
                    }
                }
            }
            
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            // Delete profile picture from storage
            try {
                storageRef.child("profile_pictures/$userId.jpg").delete().await()
            } catch (e: Exception) {
                // Profile picture might not exist, continue with user deletion
            }
            
            // Delete user document
            usersCollection.document(userId).delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
