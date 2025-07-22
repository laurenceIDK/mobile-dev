package com.campfire.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Extension functions for Campfire app
 */

/**
 * String extensions
 */
fun String.isValidEmail(): Boolean {
    return FirebaseUtils.isValidEmail(this)
}

fun String.isValidPassword(): Boolean {
    return FirebaseUtils.isValidPassword(this)
}

fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

/**
 * List extensions
 */
fun <T> List<T>.isNotEmpty(): Boolean = this.isNotEmpty()

fun <T> List<T>.containsAny(other: List<T>): Boolean {
    return this.any { other.contains(it) }
}

/**
 * Modifier extensions for common UI patterns
 */
@Composable
fun Modifier.defaultPadding(): Modifier = this.padding(16.dp)

@Composable
fun Modifier.fullWidthWithPadding(): Modifier = this.fillMaxWidth().padding(horizontal = 16.dp)

/**
 * Animation extensions
 */
object AnimationUtils {
    
    /**
     * Generates random confetti-like positions for boom animation
     */
    fun generateConfettiPositions(count: Int = 20): List<Pair<Float, Float>> {
        return (1..count).map {
            Pair(
                Random.nextFloat() * 2 - 1, // -1 to 1
                Random.nextFloat() * 2 - 1  // -1 to 1
            )
        }
    }
    
    /**
     * Creates a shake animation offset
     */
    fun getShakeOffset(animationValue: Float): Float {
        return kotlin.math.sin(animationValue * kotlin.math.PI * 8).toFloat() * 10
    }
}

/**
 * Color extensions
 */
object ColorUtils {
    
    /**
     * Generates a consistent color based on user ID for avatars
     */
    fun getUserColor(userId: String): androidx.compose.ui.graphics.Color {
        val colors = listOf(
            androidx.compose.ui.graphics.Color(0xFF9C27B0), // Purple
            androidx.compose.ui.graphics.Color(0xFF2196F3), // Blue
            androidx.compose.ui.graphics.Color(0xFF009688), // Teal
            androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green
            androidx.compose.ui.graphics.Color(0xFFFF9800), // Orange
            androidx.compose.ui.graphics.Color(0xFFE91E63), // Pink
            androidx.compose.ui.graphics.Color(0xFF607D8B), // Blue Grey
            androidx.compose.ui.graphics.Color(0xFF795548)  // Brown
        )
        
        val hash = userId.hashCode()
        val index = kotlin.math.abs(hash) % colors.size
        return colors[index]
    }
}

/**
 * Validation utilities
 */
object ValidationUtils {
    
    fun validateGroupName(name: String): String? {
        return when {
            name.isBlank() -> "Group name cannot be empty"
            name.length < 3 -> "Group name must be at least 3 characters"
            name.length > 50 -> "Group name cannot exceed 50 characters"
            else -> null
        }
    }
    
    fun validateMessage(content: String): String? {
        return when {
            content.isBlank() -> "Message cannot be empty"
            content.length > 1000 -> "Message cannot exceed 1000 characters"
            else -> null
        }
    }
    
    fun validateJoinCode(code: String): String? {
        return when {
            code.isBlank() -> "Join code cannot be empty"
            code.length != 6 -> "Join code must be 6 characters"
            !code.matches(Regex("[A-Z0-9]+")) -> "Invalid join code format"
            else -> null
        }
    }
}

/**
 * UI State extensions
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}

fun <T> UiState<T>.isLoading(): Boolean = this is UiState.Loading
fun <T> UiState<T>.isSuccess(): Boolean = this is UiState.Success
fun <T> UiState<T>.isError(): Boolean = this is UiState.Error
fun <T> UiState<T>.isEmpty(): Boolean = this is UiState.Empty

fun <T> UiState<T>.getDataOrNull(): T? {
    return if (this is UiState.Success) this.data else null
}

fun <T> UiState<T>.getErrorOrNull(): String? {
    return if (this is UiState.Error) this.message else null
}
