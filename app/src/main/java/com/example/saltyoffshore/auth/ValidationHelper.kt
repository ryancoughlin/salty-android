package com.example.saltyoffshore.auth

/**
 * Validation helpers for auth forms.
 * Matching iOS ValidationHelper enum.
 */
object ValidationHelper {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX.matches(email)
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}
