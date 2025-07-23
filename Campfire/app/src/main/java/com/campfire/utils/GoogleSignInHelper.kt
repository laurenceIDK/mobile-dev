package com.campfire.utils

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Google Sign-In using Credential Manager API
 */
@Singleton
class GoogleSignInHelper @Inject constructor() {
    
    /**
     * Initiates Google Sign-In using Credential Manager
     */
    suspend fun signIn(
        context: Context,
        webClientId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val credentialManager = CredentialManager.create(context)
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )
            
            val credential = result.credential
            if (credential is GoogleIdTokenCredential) {
                val idToken = credential.idToken
                onSuccess(idToken)
            } else {
                onError("Invalid credential type received")
            }
            
        } catch (e: GetCredentialCancellationException) {
            // User cancelled the sign-in flow
            onError("Sign-in was cancelled")
        } catch (e: NoCredentialException) {
            // No credential available
            onError("No Google account available. Please add a Google account to your device.")
        } catch (e: GetCredentialException) {
            // Other credential exceptions
            onError("Google Sign-In failed: ${e.message}")
        } catch (e: Exception) {
            // Unexpected errors
            onError("Unexpected error during sign-in: ${e.message}")
        }
    }
}
