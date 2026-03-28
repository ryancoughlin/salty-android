package com.example.saltyoffshore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Announcement from Supabase - singleton row in `announcement` table.
 * Matches iOS Announcement.swift.
 *
 * iOS ref: Models/Announcement.swift
 */
@Serializable
data class Announcement(
    val id: String,
    @SerialName("is_active") val isActive: Boolean,
    val title: String,
    val message: String,
    @SerialName("updated_at") val updatedAt: String,
    val version: Int
) {
    /** Normalize escaped newlines from database */
    val formattedMessage: String
        get() = message.replace("\\n", "\n")
}
