package com.example.saltyoffshore.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Global authentication manager.
 * Singleton for app-wide auth state, matching iOS AuthManager.
 */
object AuthManager {

    private const val TAG = "AuthManager"

    private val supabase get() = SupabaseClientProvider.client

    /** Loading state for UI binding */
    var isLoading by mutableStateOf(false)
        private set

    /** Synchronous session check */
    val hasStoredSession: Boolean
        get() = supabase.auth.currentSessionOrNull() != null

    /** Current user ID if session exists */
    val currentUserId: String?
        get() = supabase.auth.currentSessionOrNull()?.user?.id

    // MARK: - Sign Up

    suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ) {
        if (!ValidationHelper.isValidEmail(email)) {
            throw AuthError.InvalidEmail
        }

        if (!ValidationHelper.isValidPassword(password)) {
            throw AuthError.WeakPassword
        }

        isLoading = true
        try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("first_name", firstName)
                    put("last_name", lastName)
                }
            }
            Log.d(TAG, "Sign up successful for $email")
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed: ${e.message}")
            when {
                e.message?.contains("already registered") == true -> throw AuthError.EmailAlreadyInUse
                else -> throw AuthError.Unknown(e)
            }
        } finally {
            isLoading = false
        }
    }

    // MARK: - Sign In

    suspend fun signIn(email: String, password: String) {
        isLoading = true
        try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Log.d(TAG, "Sign in successful for $email")
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}")
            when {
                e.message?.contains("Invalid login credentials") == true ||
                e.message?.contains("invalid_credentials") == true -> throw AuthError.InvalidCredentials
                else -> throw AuthError.SignInFailed
            }
        } finally {
            isLoading = false
        }
    }

    // MARK: - Password Reset

    suspend fun resetPassword(email: String) {
        if (!ValidationHelper.isValidEmail(email)) {
            throw AuthError.InvalidEmail
        }

        isLoading = true
        try {
            supabase.auth.resetPasswordForEmail(email)
            Log.d(TAG, "Password reset email sent to $email")
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed: ${e.message}")
            throw AuthError.PasswordResetFailed
        } finally {
            isLoading = false
        }
    }

    // MARK: - Sign Out

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
            Log.d(TAG, "Sign out successful")
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
            // Always clean up regardless of API result (matching iOS behavior)
        }
    }
}
