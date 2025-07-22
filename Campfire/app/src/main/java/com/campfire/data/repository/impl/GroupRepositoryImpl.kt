package com.campfire.data.repository.impl

import com.campfire.data.model.Group
import com.campfire.data.model.ExpiryContract
import com.campfire.data.repository.GroupRepository
import com.campfire.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of GroupRepository
 */
@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : GroupRepository {
    
    private val groupsCollection = firestore.collection(FirebaseUtils.GROUPS_COLLECTION)
    
    override suspend fun createGroup(
        name: String,
        description: String,
        createdBy: String,
        expiryContract: ExpiryContract,
        maxMembers: Int
    ): Result<Group> {
        return try {
            val groupId = FirebaseUtils.generateGroupId()
            val joinCode = FirebaseUtils.generateJoinCode()
            
            val group = Group(
                groupId = groupId,
                name = name,
                description = description,
                createdBy = createdBy,
                members = listOf(createdBy),
                adminIds = listOf(createdBy),
                expiryContract = expiryContract,
                createdAt = Timestamp.now(),
                lastActive = Timestamp.now(),
                joinCode = joinCode,
                maxMembers = maxMembers
            )
            
            groupsCollection.document(groupId).set(group.toMap()).await()
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getGroup(groupId: String): Result<Group?> {
        return try {
            val document = groupsCollection.document(groupId).get().await()
            if (document.exists()) {
                val groupData = document.data ?: return Result.success(null)
                val group = Group.fromMap(groupData)
                Result.success(group)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUserGroups(userId: String): Result<List<Group>> {
        return try {
            val querySnapshot = groupsCollection
                .whereArrayContains("members", userId)
                .whereEqualTo("isActive", true)
                .orderBy("lastActive", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val groups = querySnapshot.documents.mapNotNull { document ->
                document.data?.let { Group.fromMap(it) }
            }
            
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun joinGroupByCode(userId: String, joinCode: String): Result<Group> {
        return try {
            val querySnapshot = groupsCollection
                .whereEqualTo("joinCode", joinCode)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Invalid join code"))
            }
            
            val groupDoc = querySnapshot.documents.first()
            val group = Group.fromMap(groupDoc.data!!)
            
            // Check if user is already a member
            if (group.members.contains(userId)) {
                return Result.success(group)
            }
            
            // Check if group is full
            if (group.members.size >= group.maxMembers) {
                return Result.failure(Exception("Group is full"))
            }
            
            // Add user to group
            val updatedMembers = group.members + userId
            groupsCollection.document(group.groupId)
                .update("members", updatedMembers)
                .await()
            
            val updatedGroup = group.copy(members = updatedMembers)
            Result.success(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun addMemberToGroup(groupId: String, userId: String, addedBy: String): Result<Unit> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(groupResult.exceptionOrNull() ?: Exception("Group not found"))
            }
            
            val group = groupResult.getOrNull() ?: return Result.failure(Exception("Group not found"))
            
            // Check if user adding has admin privileges
            if (!group.isUserAdmin(addedBy)) {
                return Result.failure(Exception("Only admins can add members"))
            }
            
            // Check if user is already a member
            if (group.members.contains(userId)) {
                return Result.success(Unit)
            }
            
            // Check if group is full
            if (group.members.size >= group.maxMembers) {
                return Result.failure(Exception("Group is full"))
            }
            
            val updatedMembers = group.members + userId
            groupsCollection.document(groupId)
                .update("members", updatedMembers)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeMemberFromGroup(groupId: String, userId: String, removedBy: String): Result<Unit> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(groupResult.exceptionOrNull() ?: Exception("Group not found"))
            }
            
            val group = groupResult.getOrNull() ?: return Result.failure(Exception("Group not found"))
            
            // Check permissions: user can remove themselves, or admin can remove others
            if (userId != removedBy && !group.isUserAdmin(removedBy)) {
                return Result.failure(Exception("Only admins can remove other members"))
            }
            
            // Cannot remove the group creator
            if (userId == group.createdBy && userId != removedBy) {
                return Result.failure(Exception("Cannot remove group creator"))
            }
            
            val updatedMembers = group.members.filter { it != userId }
            val updatedAdmins = group.adminIds.filter { it != userId }
            
            val updates = mapOf(
                "members" to updatedMembers,
                "adminIds" to updatedAdmins
            )
            
            groupsCollection.document(groupId).update(updates).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateGroup(groupId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            groupsCollection.document(groupId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            // Mark group as inactive instead of deleting immediately
            // This allows for cleanup of messages and other data
            groupsCollection.document(groupId)
                .update("isActive", false)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val listener = groupsCollection.document(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            
            if (snapshot?.exists() == true) {
                snapshot.data?.let { data ->
                    try {
                        val group = Group.fromMap(data)
                        trySend(group)
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
    
    override fun observeUserGroups(userId: String): Flow<List<Group>> = callbackFlow {
        val listener = groupsCollection
            .whereArrayContains("members", userId)
            .whereEqualTo("isActive", true)
            .orderBy("lastActive", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val groups = snapshot?.documents?.mapNotNull { document ->
                    document.data?.let { Group.fromMap(it) }
                } ?: emptyList()
                
                trySend(groups)
            }
        
        awaitClose {
            listener.remove()
        }
    }
    
    override suspend fun updateGroupActivity(groupId: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId)
                .update("lastActive", Timestamp.now())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun incrementMessageCount(groupId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val groupRef = groupsCollection.document(groupId)
                val snapshot = transaction.get(groupRef)
                val currentCount = snapshot.getLong("messageCount") ?: 0
                transaction.update(groupRef, "messageCount", currentCount + 1)
                transaction.update(groupRef, "lastActive", Timestamp.now())
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun checkGroupExpiry(groupId: String): Result<Boolean> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(groupResult.exceptionOrNull() ?: Exception("Group not found"))
            }
            
            val group = groupResult.getOrNull() ?: return Result.failure(Exception("Group not found"))
            Result.success(group.hasExpired())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getExpiredGroups(): Result<List<Group>> {
        return try {
            val querySnapshot = groupsCollection
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val expiredGroups = querySnapshot.documents.mapNotNull { document ->
                document.data?.let { Group.fromMap(it) }
            }.filter { it.hasExpired() }
            
            Result.success(expiredGroups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun makeUserAdmin(groupId: String, userId: String, promotedBy: String): Result<Unit> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(groupResult.exceptionOrNull() ?: Exception("Group not found"))
            }
            
            val group = groupResult.getOrNull() ?: return Result.failure(Exception("Group not found"))
            
            // Check if promoter is admin
            if (!group.isUserAdmin(promotedBy)) {
                return Result.failure(Exception("Only admins can promote members"))
            }
            
            // Check if user is member
            if (!group.members.contains(userId)) {
                return Result.failure(Exception("User is not a member of this group"))
            }
            
            // Check if user is already admin
            if (group.adminIds.contains(userId)) {
                return Result.success(Unit)
            }
            
            val updatedAdmins = group.adminIds + userId
            groupsCollection.document(groupId)
                .update("adminIds", updatedAdmins)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeAdminPrivileges(groupId: String, userId: String, removedBy: String): Result<Unit> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(groupResult.exceptionOrNull() ?: Exception("Group not found"))
            }
            
            val group = groupResult.getOrNull() ?: return Result.failure(Exception("Group not found"))
            
            // Cannot remove creator's admin privileges
            if (userId == group.createdBy) {
                return Result.failure(Exception("Cannot remove creator's admin privileges"))
            }
            
            // Check if remover is admin
            if (!group.isUserAdmin(removedBy)) {
                return Result.failure(Exception("Only admins can remove admin privileges"))
            }
            
            val updatedAdmins = group.adminIds.filter { it != userId }
            groupsCollection.document(groupId)
                .update("adminIds", updatedAdmins)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun regenerateJoinCode(groupId: String): Result<String> {
        return try {
            val newJoinCode = FirebaseUtils.generateJoinCode()
            groupsCollection.document(groupId)
                .update("joinCode", newJoinCode)
                .await()
            Result.success(newJoinCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
