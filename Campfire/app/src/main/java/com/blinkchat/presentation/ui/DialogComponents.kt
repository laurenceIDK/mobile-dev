package com.blinkchat.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.blinkchat.data.model.ExpiryContract

/**
 * Dialog components for BlinkChat
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, ExpiryContract) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedContractType by remember { mutableStateOf(0) }
    var customDuration by remember { mutableStateOf("24") }
    var customMessageLimit by remember { mutableStateOf("100") }
    var customInactivityHours by remember { mutableStateOf("2") }
    
    val contractTypes = listOf(
        "Timed (24 hours)" to ExpiryContract.Presets.TWENTY_FOUR_HOURS,
        "Timed (6 hours)" to ExpiryContract.Presets.SIX_HOURS,
        "Timed (1 hour)" to ExpiryContract.Presets.ONE_HOUR,
        "Message Limit (100)" to ExpiryContract.Presets.HUNDRED_MESSAGES,
        "Message Limit (50)" to ExpiryContract.Presets.FIFTY_MESSAGES,
        "Inactivity (2 hours)" to ExpiryContract.Presets.TWO_HOUR_INACTIVITY,
        "Custom Timed" to null,
        "Custom Message Limit" to null,
        "Custom Inactivity" to null
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        )
                    )
                }
                
                item {
                    Text(
                        text = "Expiry Contract",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(contractTypes.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedContractType == index,
                                onClick = { selectedContractType = index }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedContractType == index,
                            onClick = { selectedContractType = index }
                        )
                        Text(
                            text = contractTypes[index].first,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                // Custom input fields
                when (selectedContractType) {
                    6 -> { // Custom Timed
                        item {
                            OutlinedTextField(
                                value = customDuration,
                                onValueChange = { customDuration = it },
                                label = { Text("Duration (hours)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    7 -> { // Custom Message Limit
                        item {
                            OutlinedTextField(
                                value = customMessageLimit,
                                onValueChange = { customMessageLimit = it },
                                label = { Text("Message Limit") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    8 -> { // Custom Inactivity
                        item {
                            OutlinedTextField(
                                value = customInactivityHours,
                                onValueChange = { customInactivityHours = it },
                                label = { Text("Inactivity Timeout (hours)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val expiryContract = when (selectedContractType) {
                        6 -> ExpiryContract.Timed(customDuration.toLongOrNull()?.let { it * 60 * 60 * 1000 } ?: 24 * 60 * 60 * 1000L)
                        7 -> ExpiryContract.MessageLimit(customMessageLimit.toIntOrNull() ?: 100)
                        8 -> ExpiryContract.Inactivity(customInactivityHours.toLongOrNull()?.let { it * 60 * 60 * 1000 } ?: 2 * 60 * 60 * 1000L)
                        else -> contractTypes[selectedContractType].second ?: ExpiryContract.Presets.TWENTY_FOUR_HOURS
                    }
                    
                    if (name.isNotBlank()) {
                        onCreate(name, description, expiryContract)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var joinCode by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Group") },
        text = {
            Column {
                Text(
                    text = "Enter the 6-character join code to join a group.",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { 
                        if (it.length <= 6) {
                            joinCode = it.uppercase()
                        }
                    },
                    label = { Text("Join Code") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ABC123") }
                )
                Text(
                    text = "Join codes are case-insensitive and contain only letters and numbers.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (joinCode.length == 6) {
                        onJoin(joinCode)
                    }
                },
                enabled = joinCode.length == 6
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoDialog(
    group: com.blinkchat.data.model.Group,
    currentUserId: String,
    onDismiss: () -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onRegenerateCode: () -> Unit
) {
    val isAdmin = group.isUserAdmin(currentUserId)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(group.name) },
        text = {
            LazyColumn {
                item {
                    if (group.description.isNotBlank()) {
                        Text(
                            text = group.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
                
                item {
                    InfoRow("Members", "${group.members.size}/${group.maxMembers}")
                }
                
                item {
                    InfoRow("Join Code", group.joinCode)
                }
                
                item {
                    InfoRow("Created", com.blinkchat.utils.FirebaseUtils.formatTimestamp(group.createdAt))
                }
                
                item {
                    InfoRow("Last Active", com.blinkchat.utils.FirebaseUtils.formatTimestamp(group.lastActive))
                }
                
                item {
                    val expiryInfo = when (val contract = group.expiryContract) {
                        is ExpiryContract.Timed -> "Expires ${com.blinkchat.utils.FirebaseUtils.getRelativeTimeString(System.currentTimeMillis() + group.getRemainingTimeMillis())}"
                        is ExpiryContract.MessageLimit -> "${contract.maxMessages - group.messageCount} messages remaining"
                        is ExpiryContract.Inactivity -> "Expires after inactivity"
                        is ExpiryContract.PollBased -> "Poll-based expiry"
                    }
                    InfoRow("Expiry", expiryInfo)
                }
                
                if (isAdmin) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Admin Actions",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    item {
                        OutlinedButton(
                            onClick = onRegenerateCode,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Regenerate Join Code")
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onDeleteGroup,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete Group")
                        }
                    }
                } else {
                    item {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onLeaveGroup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Leave Group")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f, false)
        )
    }
}
