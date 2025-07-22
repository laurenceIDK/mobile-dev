package com.campfire.utils

import java.util.UUID
import kotlin.random.Random

/**
 * Firebase utility class containing collection names and helper functions
 */
object FirebaseUtils {
    
    // Collection names
    const val USERS_COLLECTION = "users"
    const val GROUPS_COLLECTION = "groups"
    const val MESSAGES_COLLECTION = "messages"
    const val GROUP_MEMBERS_COLLECTION = "group_members"
    
    // Storage paths
    const val PROFILE_PICTURES_PATH = "profile_pictures"
    const val MESSAGE_IMAGES_PATH = "message_images"
    
    /**
     * Generates a unique group join code
     */
    fun generateJoinCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * Generates a unique message ID
     */
    fun generateMessageId(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Generates a unique group ID
     */
    fun generateGroupId(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Validates email format
     */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Validates password strength
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    /**
     * Formats timestamp for display
     */
    fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
        val now = System.currentTimeMillis()
        val messageTime = timestamp.seconds * 1000
        val diff = now - messageTime
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> {
                val date = java.util.Date(messageTime)
                java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(date)
            }
        }
    }
    
    /**
     * Formats duration in milliseconds to human readable format
     */
    fun formatDuration(durationMillis: Long): String {
        val seconds = durationMillis / 1000
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
    
    /**
     * Gets relative time string (e.g., "2 hours left", "Expired")
     */
    fun getRelativeTimeString(targetTimeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = targetTimeMillis - now
        
        return when {
            diff <= 0 -> "Expired"
            diff < 60_000 -> "Less than 1 minute left"
            diff < 3600_000 -> "${diff / 60_000} minutes left"
            diff < 86400_000 -> "${diff / 3600_000} hours left"
            else -> "${diff / 86400_000} days left"
        }
    }
}
