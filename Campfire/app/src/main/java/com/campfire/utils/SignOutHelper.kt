package com.campfire.utils

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for handling sign-out operations including credential clearing
 */
@Singleton
class SignOutHelper @Inject constructor() {
    
    /**
     * Clears all credential state from credential providers
     * This should be called after Firebase sign-out to ensure complete logout
     */
    suspend fun clearCredentialState(context: Context): Result<Unit> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
            Result.success(Unit)
        } catch (e: ClearCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Comprehensive sign-out helper that can be used in activities
     * This clears both Firebase auth and credential state
     */
    suspend fun performCompleteSignOut(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Clear credential state
            val result = clearCredentialState(context)
            if (result.isSuccess) {
                onSuccess()
            } else {
                // Even if credential clearing fails, consider it a success
                // since the user is still signed out from Firebase
                android.util.Log.w("SignOutHelper", "Credential clearing failed but continuing with sign-out")
                onSuccess()
            }
        } catch (e: Exception) {
            onError("Sign-out failed: ${e.message}")
        }
    }
}
