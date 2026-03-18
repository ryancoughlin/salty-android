package com.example.saltyoffshore.auth

/**
 * Authentication errors matching iOS AuthError enum.
 * Sealed class for exhaustive when matching.
 */
sealed class AuthError(override val message: String) : Exception(message) {
    data object SignUpFailed : AuthError("Failed to create account")
    data object SignInFailed : AuthError("Failed to sign in")
    data object SessionMissing : AuthError("No active session found")
    data object NoUserID : AuthError("User ID not found")
    data object InvalidCredentials : AuthError("Invalid email or password")
    data object EmailAlreadyInUse : AuthError("Email is already in use")
    data object InvalidEmail : AuthError("Invalid email format")
    data object WeakPassword : AuthError("Password is too weak")
    data object AccountDeletionFailed : AuthError("Failed to delete account")
    data object PasswordResetFailed : AuthError("Failed to send password reset email")
    data object PasswordUpdateFailed : AuthError("Failed to update password")
    data class NetworkError(val underlying: Throwable) : AuthError("Network error: ${underlying.localizedMessage}")
    data class PreferencesError(val underlying: Throwable) : AuthError("Preferences error: ${underlying.localizedMessage}")
    data class Unknown(val underlying: Throwable) : AuthError("Unknown error: ${underlying.localizedMessage}")
}
