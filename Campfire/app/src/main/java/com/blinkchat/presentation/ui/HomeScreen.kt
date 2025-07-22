package com.blinkchat.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
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
import com.blinkchat.data.model.Group
import com.blinkchat.data.model.ExpiryContract
import com.blinkchat.presentation.viewmodel.GroupViewModel
import com.blinkchat.presentation.viewmodel.LoginViewModel
import com.blinkchat.utils.ColorUtils
import com.blinkchat.utils.FirebaseUtils
import com.blinkchat.utils.UiState

/**
 * Home screen showing list of user's groups
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    groupViewModel: GroupViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val userGroups by groupViewModel.userGroups.collectAsState()
    val currentUser by loginViewModel.currentUser.collectAsState()
    val actionResult by groupViewModel.actionResult.collectAsState()
    
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var showUserMenu by remember { mutableStateOf(false) }
    
    // Load user's groups
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            groupViewModel.loadUserGroups(user.uid)
        }
    }
    
    // Handle action results
    LaunchedEffect(actionResult) {
        when (actionResult) {
            is UiState.Success -> {
                groupViewModel.clearActionResult()
                // Close dialogs on success
                showCreateGroupDialog = false
                showJoinGroupDialog = false
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
                Text(
                    text = "💬 BlinkChat",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                // User profile button
                IconButton(onClick = { showUserMenu = true }) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                currentUser?.uid?.let { ColorUtils.getUserColor(it) }
                                    ?: MaterialTheme.colorScheme.primary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser?.name?.take(1)?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
        
        // Content
        when (userGroups) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is UiState.Empty -> {
                EmptyGroupsView(
                    onCreateGroup = { showCreateGroupDialog = true },
                    onJoinGroup = { showJoinGroupDialog = true }
                )
            }
            
            is UiState.Success -> {
                GroupList(
                    groups = userGroups.data,
                    onGroupClick = onNavigateToChat,
                    onCreateGroup = { showCreateGroupDialog = true },
                    onJoinGroup = { showJoinGroupDialog = true }
                )
            }
            
            is UiState.Error -> {
                ErrorView(
                    message = userGroups.message,
                    onRetry = {
                        currentUser?.let { user ->
                            groupViewModel.loadUserGroups(user.uid)
                        }
                    }
                )
            }
        }
    }
    
    // User Menu Dropdown
    if (showUserMenu) {
        UserMenuDropdown(
            userName = currentUser?.name ?: "",
            userEmail = currentUser?.email ?: "",
            onDismiss = { showUserMenu = false },
            onSignOut = {
                showUserMenu = false
                loginViewModel.signOut()
                onNavigateToLogin()
            }
        )
    }
    
    // Create Group Dialog
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, description, expiryContract ->
                currentUser?.let { user ->
                    groupViewModel.createGroup(
                        name = name,
                        description = description,
                        createdBy = user.uid,
                        expiryContract = expiryContract
                    )
                }
            }
        )
    }
    
    // Join Group Dialog
    if (showJoinGroupDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinGroupDialog = false },
            onJoin = { joinCode ->
                currentUser?.let { user ->
                    groupViewModel.joinGroup(user.uid, user.name, joinCode)
                }
            }
        )
    }
    
    // Show error snackbar
    when (actionResult) {
        is UiState.Error -> {
            LaunchedEffect(actionResult) {
                // Show snackbar for errors
                // You can implement SnackbarHost here
            }
        }
        else -> { /* No error */ }
    }
}

@Composable
fun EmptyGroupsView(
    onCreateGroup: () -> Unit,
    onJoinGroup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔍",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "No Groups Yet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Create a new group or join an existing one to start chatting",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onCreateGroup,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Group")
            }
            
            OutlinedButton(
                onClick = onJoinGroup,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Group, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Join Group")
            }
        }
    }
}

@Composable
fun GroupList(
    groups: List<Group>,
    onGroupClick: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onJoinGroup: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onCreateGroup,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create")
                }
                
                OutlinedButton(
                    onClick = onJoinGroup,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Group, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Join")
                }
            }
        }
        
        items(groups) { group ->
            GroupItem(
                group = group,
                onClick = { onGroupClick(group.groupId) }
            )
        }
    }
}

@Composable
fun GroupItem(
    group: Group,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (group.description.isNotBlank()) {
                        Text(
                            text = group.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // Group info
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${group.members.size} members",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        Text(
                            text = FirebaseUtils.formatTimestamp(group.lastActive),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Expiry indicator
                ExpiryIndicator(group = group)
            }
        }
    }
}

@Composable
fun ExpiryIndicator(group: Group) {
    val remainingTime = group.getRemainingTimeMillis()
    val isExpiringSoon = remainingTime < 24 * 60 * 60 * 1000L // Less than 24 hours
    
    if (group.hasExpired()) {
        Chip(
            onClick = { },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = "💥 Expired",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    } else if (isExpiringSoon) {
        Chip(
            onClick = { },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Color(0xFFFFE082)
            )
        ) {
            Text(
                text = "⚠️ ${FirebaseUtils.getRelativeTimeString(System.currentTimeMillis() + remainingTime)}",
                fontSize = 12.sp,
                color = Color(0xFF5D4037)
            )
        }
    }
}

@Composable
fun UserMenuDropdown(
    userName: String,
    userEmail: String,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
            onDismiss()
        }
    ) {
        // User info
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = userName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = userEmail,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Divider()
        
        DropdownMenuItem(
            text = { Text("Sign Out") },
            onClick = {
                expanded = false
                onSignOut()
            },
            leadingIcon = {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
            }
        )
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "😞",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Something went wrong",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
