package com.example.saltyoffshore.data

/**
 * Errors that can occur during crew operations.
 * Port of iOS CrewTypes.swift -> CrewError enum.
 */
sealed class CrewError : Exception() {
    data object NotAuthenticated : CrewError() {
        override val message = "Please sign in to continue"
    }
    data object InvalidCodeFormat : CrewError() {
        override val message = "Invite code must be 6 characters"
    }
    data object CrewNotFound : CrewError() {
        override val message = "Crew not found. Check the code and try again."
    }
    data object AlreadyMember : CrewError() {
        override val message = "You're already in this crew"
    }
    data object EmptyName : CrewError() {
        override val message = "Crew name cannot be empty"
    }
    data object NameTooLong : CrewError() {
        override val message = "Crew name is too long (max 50 characters)"
    }
    data object NotCreator : CrewError() {
        override val message = "Only the crew creator can perform this action"
    }
    data object CannotRemoveSelf : CrewError() {
        override val message = "You cannot remove yourself. Use 'Leave Crew' instead."
    }
    data class DatabaseError(val detail: String) : CrewError() {
        override val message = "Database error: $detail"
    }
    data class NetworkError(val detail: String) : CrewError() {
        override val message = "Network error: $detail"
    }
    data object UnknownError : CrewError() {
        override val message = "Something went wrong. Please try again."
    }
}
