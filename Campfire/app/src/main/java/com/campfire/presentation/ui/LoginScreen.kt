package com.campfire.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.campfire.presentation.viewmodel.LoginViewModel
import com.campfire.utils.GoogleSignInHelper
import com.campfire.utils.UiState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import javax.inject.Inject
import androidx.compose.material.icons.filled.VisibilityOff
import com.campfire.presentation.viewmodel.LoginViewModel
import com.campfire.utils.UiState

/**
 * Login screen for Campfire authentication
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleSignInHelper = remember { GoogleSignInHelper() }
    
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    
    // Navigate to home if user is authenticated
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onNavigateToHome()
        }
    }
    
    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> {
                viewModel.clearUiState()
                onNavigateToHome()
            }
            else -> { /* Handle other states in UI */ }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App logo/title
        Text(
            text = "💬 Campfire",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Self-destructing group chats",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Login/Signup Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isSignUp) "Create Account" else "Welcome Back",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Name field (only for sign up)
                if (isSignUp) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        enabled = !isLoading
                    )
                }
                
                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    enabled = !isLoading
                )
                
                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    enabled = !isLoading
                )
                
                // Submit button
                Button(
                    onClick = {
                        if (isSignUp) {
                            viewModel.signUp(email, password, name)
                        } else {
                            viewModel.signIn(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && 
                            (if (isSignUp) name.isNotBlank() else true)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = if (isSignUp) "Create Account" else "Sign In",
                            fontSize = 16.sp
                        )
                    }
                }
                
                // Toggle between sign in and sign up
                TextButton(
                    onClick = { 
                        isSignUp = !isSignUp
                        viewModel.clearUiState()
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = if (isSignUp) {
                            "Already have an account? Sign In"
                        } else {
                            "Don't have an account? Sign Up"
                        }
                    )
                }
                
                // Divider for Google Sign-In
                if (!isSignUp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f))
                        Text(
                            text = "OR",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Divider(modifier = Modifier.weight(1f))
                    }
                    
                    // Google Sign-In Button
                    GoogleSignInButton(
                        onClick = { 
                            scope.launch {
                                googleSignInHelper.signIn(
                                    context = context,
                                    webClientId = com.campfire.utils.FirebaseConfig.WEB_CLIENT_ID,
                                    onSuccess = { idToken ->
                                        viewModel.signInWithGoogle(idToken)
                                    },
                                    onError = { error ->
                                        // Handle error - you can show a snackbar or error message
                                        // For now, just log it (you can improve this later)
                                    }
                                )
                            }
                        },
                        isEnabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Forgot password
                if (!isSignUp) {
                    TextButton(
                        onClick = { showForgotPassword = true }
                    ) {
                        Text("Forgot Password?")
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = null
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Google Icon (you can replace with actual Google icon)
            Text(
                text = "🔍",
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Continue with Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}        // Error display
        when (uiState) {
            is UiState.Error -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> { /* No error to display */ }
        }
    }
    
    // Forgot Password Dialog
    if (showForgotPassword) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPassword = false },
            onSendReset = { resetEmail ->
                viewModel.sendPasswordResetEmail(resetEmail) { success, error ->
                    showForgotPassword = false
                    // Handle result (show snackbar, etc.)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSendReset: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password") },
        text = {
            Column {
                Text(
                    text = "Enter your email address and we'll send you a password reset link.",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (email.isNotBlank()) {
                        onSendReset(email)
                    }
                }
            ) {
                Text("Send Reset Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
