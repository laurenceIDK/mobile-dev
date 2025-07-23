package com.campfire.utils

/**
 * Configuration constants for Firebase and Google Sign-In
 * 
 * IMPORTANT: Replace YOUR_WEB_CLIENT_ID_HERE with your actual Web Client ID from Firebase Console
 * 
 * To get your Web Client ID:
 * 1. Go to Firebase Console > Project Settings
 * 2. Scroll down to "Your apps" section
 * 3. Click on your Android app
 * 4. Look for "Web Client ID" in the OAuth 2.0 client IDs section
 * OR
 * 1. Go to Google Cloud Console > APIs & Services > Credentials
 * 2. Find the "Web client" entry (not Android client)
 * 3. Copy the Client ID
 */
object FirebaseConfig {
    // TODO: Replace this with your actual Web Client ID from Firebase Console
    const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE"
    
    // Example format (don't use this):
    // const val WEB_CLIENT_ID = "123456789-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com"
}
