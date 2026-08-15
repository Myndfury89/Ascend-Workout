package com.ascend.core.data.community

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Outcome of asking Credential Manager for a Google ID token. */
sealed interface GoogleIdTokenResult {
    data class Success(val idToken: String) : GoogleIdTokenResult

    /** The user dismissed the Google chooser — treat silently, not as an error. */
    data object Cancelled : GoogleIdTokenResult

    /** No web client id configured — the Google path is simply unavailable. */
    data object Unavailable : GoogleIdTokenResult

    data class Failure(val message: String) : GoogleIdTokenResult
}

/**
 * Acquires a Google ID token via Credential Manager, to be exchanged for a Supabase session. Uses the
 * Google Web (server) client id only — no client secret is ever present in the app. This is device
 * runtime (Play Services); it is exercised on-device, not in unit tests.
 */
class GoogleIdTokenRequester {
    suspend fun request(
        activityContext: Context,
        webClientId: String,
    ): GoogleIdTokenResult {
        if (webClientId.isBlank()) return GoogleIdTokenResult.Unavailable
        return try {
            val option =
                GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val response = CredentialManager.create(activityContext).getCredential(activityContext, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleIdTokenResult.Failure("Unexpected credential type from Google.")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleIdTokenResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleIdTokenResult.Failure("No Google account is available on this device.")
        } catch (e: GetCredentialException) {
            GoogleIdTokenResult.Failure(e.message ?: "Google sign-in failed.")
        }
    }
}
