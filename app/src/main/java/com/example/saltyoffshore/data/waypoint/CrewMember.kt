package com.example.saltyoffshore.data.waypoint

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a member of a crew with their user preferences data.
 * Port of iOS CrewTypes.swift -> CrewMember struct.
 *
 * userName is derived from a JOIN with user_preferences at decode time.
 */
@Serializable
data class CrewMember(
    val id: String,
    @SerialName("crew_id") val crewId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("joined_at") val joinedAt: String,
    val userName: String? = null
) {
    /** Initials for avatar display (e.g., "JD" for "John Doe") */
    val initials: String
        get() {
            val name = userName ?: return "?"
            val words = name.split(" ")
            return if (words.size >= 2) {
                "${words[0].first()}${words[1].first()}".uppercase()
            } else {
                name.take(2).uppercase()
            }
        }
}
