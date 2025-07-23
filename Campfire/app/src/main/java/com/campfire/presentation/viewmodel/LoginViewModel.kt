package com.campfire.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campfire.data.model.User
import com.campfire.domain.usecase.AuthUseCase
import com.campfire.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication operations
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authUseCase: AuthUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Empty)
    val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        observeAuthState()
        checkCurrentUser()
    }
    
    /**
     * Signs in user with email and password
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = UiState.Loading
            
            try {
                val result = authUseCase.signIn(email.trim(), password)
                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    _uiState.value = UiState.Success(user)
                    _currentUser.value = user
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Sign in failed"
                    _uiState.value = UiState.Error(error)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Signs up new user
     */
    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = UiState.Loading
            
            try {
                val result = authUseCase.signUp(email.trim(), password, name.trim())
                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    _uiState.value = UiState.Success(user)
                    _currentUser.value = user
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Sign up failed"
                    _uiState.value = UiState.Error(error)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Signs in user with Google
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = UiState.Loading
            
            try {
                val result = authUseCase.signInWithGoogle(idToken)
                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    _uiState.value = UiState.Success(user)
                    _currentUser.value = user
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Google sign in failed"
                    _uiState.value = UiState.Error(error)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Signs out current user
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                val result = authUseCase.signOut()
                if (result.isSuccess) {
                    _currentUser.value = null
                    _uiState.value = UiState.Empty
                }
            } catch (e: Exception) {
                // Handle sign out error if needed
            }
        }
    }
    
    /**
     * Sends password reset email
     */
    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = authUseCase.sendPasswordResetEmail(email.trim())
                if (result.isSuccess) {
                    onResult(true, null)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                    onResult(false, error)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    /**
     * Checks if user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return authUseCase.isUserAuthenticated()
    }
    
    /**
     * Clears the UI state
     */
    fun clearUiState() {
        _uiState.value = UiState.Empty
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authUseCase.observeAuthState().collect { user ->
                _currentUser.value = user
                if (user != null && _uiState.value !is UiState.Success) {
                    _uiState.value = UiState.Success(user)
                }
            }
        }
    }
    
    private fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                val user = authUseCase.getCurrentUser()
                _currentUser.value = user
                if (user != null) {
                    _uiState.value = UiState.Success(user)
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
}
