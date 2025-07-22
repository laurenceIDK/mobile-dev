package com.campfire.data.model

/**
 * User data model representing a user in the Campfire app
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profileUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: com.google.firebase.Timestamp? = null
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", null, false, null)
    
    /**
     * Converts User to Map for Firebase storage
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "profileUrl" to profileUrl,
            "isOnline" to isOnline,
            "lastSeen" to lastSeen
        )
    }
    
    companion object {
        /**
         * Creates User from Firebase Map
         */
        fun fromMap(map: Map<String, Any?>): User {
            return User(
                uid = map["uid"] as? String ?: "",
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                profileUrl = map["profileUrl"] as? String,
                isOnline = map["isOnline"] as? Boolean ?: false,
                lastSeen = map["lastSeen"] as? com.google.firebase.Timestamp
            )
        }
    }
}
