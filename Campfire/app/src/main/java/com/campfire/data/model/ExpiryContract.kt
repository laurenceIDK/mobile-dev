package com.campfire.data.model

import com.google.firebase.Timestamp

/**
 * Sealed class representing different types of group expiry contracts
 * Each contract defines when a group should automatically be deleted
 */
sealed class ExpiryContract {
    
    /**
     * Group expires after a fixed duration from creation
     * @param durationMillis Duration in milliseconds
     */
    data class Timed(val durationMillis: Long) : ExpiryContract() {
        fun getExpiryTime(createdAt: Timestamp): Timestamp {
            return Timestamp(createdAt.seconds + (durationMillis / 1000), createdAt.nanoseconds)
        }
    }
    
    /**
     * Group expires after reaching maximum number of messages
     * @param maxMessages Maximum number of messages allowed
     */
    data class MessageLimit(val maxMessages: Int) : ExpiryContract()
    
    /**
     * Group expires after a period of inactivity
     * @param timeoutMillis Inactivity timeout in milliseconds
     */
    data class Inactivity(val timeoutMillis: Long) : ExpiryContract() {
        fun getExpiryTime(lastActivity: Timestamp): Timestamp {
            return Timestamp(lastActivity.seconds + (timeoutMillis / 1000), lastActivity.nanoseconds)
        }
    }
    
    /**
     * Group deleted by member vote (optional feature)
     */
    object PollBased : ExpiryContract()
    
    /**
     * Converts ExpiryContract to Map for Firebase storage
     */
    fun toMap(): Map<String, Any> {
        return when (this) {
            is Timed -> mapOf(
                "type" to "timed",
                "durationMillis" to durationMillis
            )
            is MessageLimit -> mapOf(
                "type" to "messageLimit",
                "maxMessages" to maxMessages
            )
            is Inactivity -> mapOf(
                "type" to "inactivity",
                "timeoutMillis" to timeoutMillis
            )
            is PollBased -> mapOf(
                "type" to "pollBased"
            )
        }
    }
    
    companion object {
        /**
         * Creates ExpiryContract from Firebase Map
         */
        fun fromMap(map: Map<String, Any>): ExpiryContract {
            return when (val type = map["type"] as? String) {
                "timed" -> Timed(map["durationMillis"] as Long)
                "messageLimit" -> MessageLimit((map["maxMessages"] as Long).toInt())
                "inactivity" -> Inactivity(map["timeoutMillis"] as Long)
                "pollBased" -> PollBased
                else -> throw IllegalArgumentException("Unknown expiry contract type: $type")
            }
        }
        
        /**
         * Predefined common expiry contracts
         */
        object Presets {
            val ONE_HOUR = Timed(60 * 60 * 1000L)
            val SIX_HOURS = Timed(6 * 60 * 60 * 1000L)
            val TWENTY_FOUR_HOURS = Timed(24 * 60 * 60 * 1000L)
            val ONE_WEEK = Timed(7 * 24 * 60 * 60 * 1000L)
            val FIFTY_MESSAGES = MessageLimit(50)
            val HUNDRED_MESSAGES = MessageLimit(100)
            val TWO_HOUR_INACTIVITY = Inactivity(2 * 60 * 60 * 1000L)
        }
    }
}
