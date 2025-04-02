package com.example.caliindar

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.tasks.await
import com.google.android.gms.tasks.Task

class GoogleAuthHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    // Получите WEB_CLIENT_ID из Google Cloud Console
    private val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId("835523232919-o0ilepmg8ev25bu3ve78kdg0smuqp9i8.apps.googleusercontent.com")
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(true)
        .build()

    suspend fun signIn(): Result<GoogleIdTokenCredential> {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(
                context = context,
                request = request
            )
            Result.success(parseGoogleIdToken(response))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLastSignedInAccount(): GoogleIdTokenCredential? {
        return try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = context,
                request = request
            )
            parseGoogleIdToken(response)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseGoogleIdToken(response: GetCredentialResponse): GoogleIdTokenCredential {
        val credential = response.credential as? CustomCredential
            ?: throw IllegalArgumentException("Invalid credential type")

        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalArgumentException("Unexpected credential type")
        }

        return GoogleIdTokenCredential.createFrom(credential.data)
    }
}