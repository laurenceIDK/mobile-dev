package com.blinkchat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blinkchat.data.model.Message
import com.blinkchat.data.model.Group
import com.blinkchat.domain.usecase.MessageUseCase
import com.blinkchat.domain.usecase.GroupUseCase
import com.blinkchat.domain.usecase.UserUseCase
import com.blinkchat.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for chat operations
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageUseCase: MessageUseCase,
    private val groupUseCase: GroupUseCase,
    private val userUseCase: UserUseCase
) : ViewModel() {
    
    private val _messages = MutableStateFlow<UiState<List<Message>>>(UiState.Loading)
    val messages: StateFlow<UiState<List<Message>>> = _messages.asStateFlow()
    
    private val _currentGroup = MutableStateFlow<Group?>(null)
    val currentGroup: StateFlow<Group?> = _currentGroup.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _actionResult = MutableStateFlow<UiState<String>>(UiState.Empty)
    val actionResult: StateFlow<UiState<String>> = _actionResult.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    private var currentGroupId: String? = null
    private var currentUserId: String? = null
    
    /**
     * Initializes chat for a specific group
     */
    fun initializeChat(groupId: String, userId: String) {
        currentGroupId = groupId
        currentUserId = userId
        
        loadGroup(groupId)
        observeMessages(groupId, userId)
        loadUnreadCount(groupId, userId)
    }
    
    /**
     * Sends a message
     */
    fun sendMessage(
        groupId: String,
        senderId: String,
        senderName: String,
        content: String,
        selfDestructDuration: Long? = null
    ) {
        viewModelScope.launch {
            try {
                val message = messageUseCase.createSelfDestructMessage(
                    groupId = groupId,
                    senderId = senderId,
                    senderName = senderName,
                    content = content,
                    selfDestructDuration = selfDestructDuration
                )
                
                val result = messageUseCase.sendMessage(message)
                
                if (result.isFailure) {
                    val error = result.exceptionOrNull()?.message ?: "Failed to send message"
                    _actionResult.value = UiState.Error(error)
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    /**
     * Marks message as read
     */
    fun markMessageAsRead(messageId: String, userId: String) {
        viewModelScope.launch {
            try {
                messageUseCase.markMessageAsRead(messageId, userId)
                // Refresh unread count
                currentGroupId?.let { groupId ->
                    loadUnreadCount(groupId, userId)
                }
            } catch (e: Exception) {
                // Handle error silently for read status
            }
        }
    }
    
    /**
     * Deletes a message
     */
    fun deleteMessage(messageId: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = messageUseCase.deleteMessage(messageId, userId)
                
                if (result.isSuccess) {
                    _actionResult.value = UiState.Success("Message deleted")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to delete message"
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
     * Edits a message
     */
    fun editMessage(messageId: String, newContent: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = messageUseCase.editMessage(messageId, newContent, userId)
                
                if (result.isSuccess) {
                    _actionResult.value = UiState.Success("Message edited")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to edit message"
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
     * Reports a message
     */
    fun reportMessage(messageId: String, userId: String, reason: String) {
        viewModelScope.launch {
            try {
                val result = messageUseCase.reportMessage(messageId, userId, reason)
                
                if (result.isSuccess) {
                    _actionResult.value = UiState.Success("Message reported")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to report message"
                    _actionResult.value = UiState.Error(error)
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    /**
     * Searches messages in the current group
     */
    fun searchMessages(query: String, onResult: (List<Message>) -> Unit) {
        val groupId = currentGroupId
        val userId = currentUserId
        
        if (groupId == null || userId == null) return
        
        viewModelScope.launch {
            try {
                val result = messageUseCase.searchMessages(groupId, query, userId)
                
                if (result.isSuccess) {
                    onResult(result.getOrNull() ?: emptyList())
                } else {
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }
    
    /**
     * Gets remaining time for group expiry
     */
    fun getGroupExpiryInfo(): String? {
        val group = _currentGroup.value ?: return null
        
        if (!group.isActive) return "Group expired"
        
        return when (val contract = group.expiryContract) {
            is com.blinkchat.data.model.ExpiryContract.Timed -> {
                val remainingMillis = group.getRemainingTimeMillis()
                if (remainingMillis <= 0) "Expired" else formatRemainingTime(remainingMillis)
            }
            is com.blinkchat.data.model.ExpiryContract.MessageLimit -> {
                val remaining = contract.maxMessages - group.messageCount
                if (remaining <= 0) "Expired" else "$remaining messages left"
            }
            is com.blinkchat.data.model.ExpiryContract.Inactivity -> {
                val remainingMillis = group.getRemainingTimeMillis()
                if (remainingMillis <= 0) "Expired" else "Expires after inactivity: ${formatRemainingTime(remainingMillis)}"
            }
            is com.blinkchat.data.model.ExpiryContract.PollBased -> "Poll-based expiry"
        }
    }
    
    /**
     * Checks if current user is admin of the group
     */
    fun isCurrentUserAdmin(): Boolean {
        val group = _currentGroup.value
        val userId = currentUserId
        return group != null && userId != null && group.isUserAdmin(userId)
    }
    
    /**
     * Clears action result
     */
    fun clearActionResult() {
        _actionResult.value = UiState.Empty
    }
    
    private fun loadGroup(groupId: String) {
        viewModelScope.launch {
            try {
                groupUseCase.observeGroup(groupId).collect { group ->
                    _currentGroup.value = group
                    
                    // Check if group has expired
                    if (group != null && group.hasExpired() && group.isActive) {
                        // Notify about expiry
                        messageUseCase.sendSystemMessage(
                            groupId,
                            "💥 This group has expired and will be deleted soon!"
                        )
                    }
                }
            } catch (e: Exception) {
                _actionResult.value = UiState.Error("Failed to load group information")
            }
        }
    }
    
    private fun observeMessages(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                messageUseCase.observeGroupMessages(groupId, userId).collect { messageList ->
                    _messages.value = if (messageList.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(messageList)
                    }
                    
                    // Auto-mark messages as read when they appear
                    messageList.forEach { message ->
                        if (message.senderId != userId && !message.isReadBy(userId)) {
                            markMessageAsRead(message.messageId, userId)
                        }
                    }
                }
            } catch (e: Exception) {
                _messages.value = UiState.Error(e.message ?: "Failed to load messages")
            }
        }
    }
    
    private fun loadUnreadCount(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = messageUseCase.getUnreadMessageCount(groupId, userId)
                if (result.isSuccess) {
                    _unreadCount.value = result.getOrNull() ?: 0
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    private fun formatRemainingTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}d ${hours % 24}h"
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}
