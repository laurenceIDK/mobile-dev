package com.blinkchat.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.blinkchat.data.model.Message
import com.blinkchat.presentation.viewmodel.ChatViewModel
import com.blinkchat.presentation.viewmodel.LoginViewModel
import com.blinkchat.utils.ColorUtils
import com.blinkchat.utils.FirebaseUtils
import com.blinkchat.utils.UiState
import kotlinx.coroutines.delay

/**
 * Chat screen for group messaging
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val messages by chatViewModel.messages.collectAsState()
    val currentGroup by chatViewModel.currentGroup.collectAsState()
    val currentUser by loginViewModel.currentUser.collectAsState()
    val actionResult by chatViewModel.actionResult.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    var showGroupInfo by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // Initialize chat
    LaunchedEffect(groupId, currentUser) {
        currentUser?.let { user ->
            chatViewModel.initializeChat(groupId, user.uid)
        }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages) {
        if (messages is UiState.Success && messages.data.isNotEmpty()) {
            delay(100) // Small delay to ensure UI is updated
            listState.animateScrollToItem(messages.data.size - 1)
        }
    }
    
    // Handle action results
    LaunchedEffect(actionResult) {
        when (actionResult) {
            is UiState.Success -> {
                chatViewModel.clearActionResult()
            }
            else -> { /* Handle in UI */ }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = currentGroup?.name ?: "Loading...",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Expiry info
                    currentGroup?.let { group ->
                        val expiryInfo = chatViewModel.getGroupExpiryInfo()
                        if (expiryInfo != null) {
                            Text(
                                text = expiryInfo,
                                fontSize = 12.sp,
                                color = if (expiryInfo.contains("Expired")) {
                                    MaterialTheme.colorScheme.error
                                } else if (expiryInfo.contains("left") || expiryInfo.contains("minutes")) {
                                    Color(0xFFFF6F00)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showGroupInfo = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Group Info")
                }
            }
        )
        
        // Messages List
        Box(
            modifier = Modifier.weight(1f)
        ) {
            when (messages) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is UiState.Empty -> {
                    EmptyMessagesView()
                }
                
                is UiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages.data) { message ->
                            MessageItem(
                                message = message,
                                isOwnMessage = message.senderId == currentUser?.uid,
                                onMessageLongClick = { /* Handle long click */ }
                            )
                        }
                    }
                }
                
                is UiState.Error -> {
                    ErrorView(
                        message = messages.message,
                        onRetry = {
                            currentUser?.let { user ->
                                chatViewModel.initializeChat(groupId, user.uid)
                            }
                        }
                    )
                }
            }
        }
        
        // Message Input
        MessageInput(
            value = messageText,
            onValueChange = { messageText = it },
            onSend = {
                currentUser?.let { user ->
                    currentGroup?.let { group ->
                        if (!group.hasExpired() && group.isActive) {
                            chatViewModel.sendMessage(
                                groupId = groupId,
                                senderId = user.uid,
                                senderName = user.name,
                                content = messageText
                            )
                            messageText = ""
                        }
                    }
                }
            },
            enabled = currentGroup?.let { !it.hasExpired() && it.isActive } ?: false
        )
    }
    
    // Group Info Dialog
    if (showGroupInfo && currentGroup != null && currentUser != null) {
        GroupInfoDialog(
            group = currentGroup!!,
            currentUserId = currentUser!!.uid,
            onDismiss = { showGroupInfo = false },
            onLeaveGroup = {
                // Handle leave group
                showGroupInfo = false
                onNavigateBack()
            },
            onDeleteGroup = {
                // Handle delete group
                showGroupInfo = false
                onNavigateBack()
            },
            onRegenerateCode = {
                // Handle regenerate code
            }
        )
    }
}

@Composable
fun MessageItem(
    message: Message,
    isOwnMessage: Boolean,
    onMessageLongClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage && message.messageType != Message.MessageType.SYSTEM) {
            // Avatar for other users
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ColorUtils.getUserColor(message.senderId)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.senderName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.width(8.dp))
        }
        
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            when (message.messageType) {
                Message.MessageType.SYSTEM -> {
                    SystemMessageBubble(message = message)
                }
                else -> {
                    MessageBubble(
                        message = message,
                        isOwnMessage = isOwnMessage,
                        onLongClick = onMessageLongClick
                    )
                }
            }
        }
        
        if (isOwnMessage) {
            Spacer(Modifier.width(8.dp))
            // Avatar for own messages
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ColorUtils.getUserColor(message.senderId)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.senderName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onLongClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isOwnMessage) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isOwnMessage) 16.dp else 4.dp,
            bottomEnd = if (isOwnMessage) 4.dp else 16.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (!isOwnMessage) {
                Text(
                    text = message.senderName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            Text(
                text = message.content,
                color = if (isOwnMessage) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 14.sp
            )
            
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = FirebaseUtils.formatTimestamp(message.timestamp),
                    fontSize = 10.sp,
                    color = if (isOwnMessage) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
                
                if (message.isEdited) {
                    Text(
                        text = "edited",
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = if (isOwnMessage) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                }
                
                // Self-destruct indicator
                message.selfDestructDuration?.let { duration ->
                    if (message.isRead) {
                        val remainingTime = message.getRemainingDestructTime()
                        if (remainingTime > 0) {
                            Text(
                                text = "💣 ${FirebaseUtils.formatDuration(remainingTime)}",
                                fontSize = 10.sp,
                                color = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemMessageBubble(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = message.content,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { 
                    Text(
                        if (enabled) "Type a message..." else "Group has expired"
                    ) 
                },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                maxLines = 4
            )
            
            Spacer(Modifier.width(8.dp))
            
            FloatingActionButton(
                onClick = {
                    if (value.isNotBlank()) {
                        onSend()
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                enabled = enabled && value.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun EmptyMessagesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "💬",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "No messages yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Start the conversation by sending the first message!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
