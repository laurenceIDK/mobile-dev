package com.blinkchat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blinkchat.data.model.Group
import com.blinkchat.data.model.ExpiryContract
import com.blinkchat.domain.usecase.GroupUseCase
import com.blinkchat.domain.usecase.MessageUseCase
import com.blinkchat.domain.usecase.UserUseCase
import com.blinkchat.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for group management operations
 */
@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupUseCase: GroupUseCase,
    private val messageUseCase: MessageUseCase,
    private val userUseCase: UserUseCase
) : ViewModel() {
    
    private val _userGroups = MutableStateFlow<UiState<List<Group>>>(UiState.Loading)
    val userGroups: StateFlow<UiState<List<Group>>> = _userGroups.asStateFlow()
    
    private val _currentGroup = MutableStateFlow<Group?>(null)
    val currentGroup: StateFlow<Group?> = _currentGroup.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _actionResult = MutableStateFlow<UiState<String>>(UiState.Empty)
    val actionResult: StateFlow<UiState<String>> = _actionResult.asStateFlow()
    
    /**
     * Loads user's groups
     */
    fun loadUserGroups(userId: String) {
        viewModelScope.launch {
            _userGroups.value = UiState.Loading
            
            try {
                // Observe user groups in real-time
                groupUseCase.observeUserGroups(userId).collect { groups ->
                    _userGroups.value = if (groups.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(groups)
                    }
                }
            } catch (e: Exception) {
                _userGroups.value = UiState.Error(e.message ?: "Failed to load groups")
            }
        }
    }
    
    /**
     * Creates a new group
     */
    fun createGroup(
        name: String,
        description: String,
        createdBy: String,
        expiryContract: ExpiryContract,
        maxMembers: Int = 50
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _actionResult.value = UiState.Loading
            
            try {
                val result = groupUseCase.createGroup(
                    name.trim(),
                    description.trim(),
                    createdBy,
                    expiryContract,
                    maxMembers
                )
                
                if (result.isSuccess) {
                    val group = result.getOrNull()!!
                    _currentGroup.value = group
                    _actionResult.value = UiState.Success("Group created successfully")
                    
                    // Send welcome system message
                    messageUseCase.sendSystemMessage(
                        group.groupId,
                        "Welcome to ${group.name}! This group will expire ${getExpiryDescription(expiryContract)}."
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to create group"
                    _actionResult.value = UiState.Error(error)
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Joins a group using join code
     */
    fun joinGroup(userId: String, userName: String, joinCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _actionResult.value = UiState.Loading
            
            try {
                val result = groupUseCase.joinGroupByCode(userId, joinCode)
                
                if (result.isSuccess) {
                    val group = result.getOrNull()!!
                    _currentGroup.value = group
                    _actionResult.value = UiState.Success("Joined group successfully")
                    
                    // Notify group about new member
                    messageUseCase.notifyMemberJoined(group.groupId, userName)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to join group"
                    _actionResult.value = UiState.Error(error)
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Leaves a group
     */
    fun leaveGroup(groupId: String, userId: String, userName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _actionResult.value = UiState.Loading
            
            try {
                val result = groupUseCase.removeMemberFromGroup(groupId, userId, userId)
                
                if (result.isSuccess) {
                    _actionResult.value = UiState.Success("Left group successfully")
                    
                    // Notify group about member leaving
                    messageUseCase.notifyMemberLeft(groupId, userName)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to leave group"
                    _actionResult.value = UiState.Error(error)
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Deletes a group
     */
    fun deleteGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _actionResult.value = UiState.Loading
            
            try {
                // Check if user is admin
                val groupResult = groupUseCase.getGroup(groupId)
                if (groupResult.isSuccess) {
                    val group = groupResult.getOrNull()
                    if (group != null && group.isUserAdmin(userId)) {
                        val result = groupUseCase.deleteGroup(groupId)
                        
                        if (result.isSuccess) {
                            _actionResult.value = UiState.Success("Group deleted successfully")
                        } else {
                            val error = result.exceptionOrNull()?.message ?: "Failed to delete group"
                            _actionResult.value = UiState.Error(error)
                        }
                    } else {
                        _actionResult.value = UiState.Error("Only admins can delete groups")
                    }
                } else {
                    _actionResult.value = UiState.Error("Group not found")
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Observes a specific group
     */
    fun observeGroup(groupId: String) {
        viewModelScope.launch {
            try {
                groupUseCase.observeGroup(groupId).collect { group ->
                    _currentGroup.value = group
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
    
    /**
     * Regenerates join code for group
     */
    fun regenerateJoinCode(groupId: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Check if user is admin
                val groupResult = groupUseCase.getGroup(groupId)
                if (groupResult.isSuccess) {
                    val group = groupResult.getOrNull()
                    if (group != null && group.isUserAdmin(userId)) {
                        val result = groupUseCase.regenerateJoinCode(groupId)
                        
                        if (result.isSuccess) {
                            _actionResult.value = UiState.Success("Join code regenerated")
                        } else {
                            val error = result.exceptionOrNull()?.message ?: "Failed to regenerate join code"
                            _actionResult.value = UiState.Error(error)
                        }
                    } else {
                        _actionResult.value = UiState.Error("Only admins can regenerate join codes")
                    }
                } else {
                    _actionResult.value = UiState.Error("Group not found")
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clears action result
     */
    fun clearActionResult() {
        _actionResult.value = UiState.Empty
    }
    
    /**
     * Gets expiry description for display
     */
    private fun getExpiryDescription(contract: ExpiryContract): String {
        return when (contract) {
            is ExpiryContract.Timed -> {
                val hours = contract.durationMillis / (1000 * 60 * 60)
                when {
                    hours < 24 -> "in ${hours}h"
                    hours == 24L -> "in 1 day"
                    else -> "in ${hours / 24} days"
                }
            }
            is ExpiryContract.MessageLimit -> "after ${contract.maxMessages} messages"
            is ExpiryContract.Inactivity -> {
                val hours = contract.timeoutMillis / (1000 * 60 * 60)
                "after ${hours}h of inactivity"
            }
            is ExpiryContract.PollBased -> "when members vote to end"
        }
    }
}
