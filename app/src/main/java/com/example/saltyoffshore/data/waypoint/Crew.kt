package com.example.saltyoffshore.data.waypoint

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a fishing crew that users can join and share waypoints with.
 * Port of iOS CrewTypes.swift -> Crew struct.
 */
@Serializable
data class Crew(
    val id: String,
    val name: String,
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String
) {
    /** Initials for avatar display (e.g., "WW" for "Weekend Warriors") */
    val initials: String
        get() {
            val words = name.split(" ")
            return if (words.size >= 2) {
                "${words[0].first()}${words[1].first()}".uppercase()
            } else {
                name.take(2).uppercase()
            }
        }
}
