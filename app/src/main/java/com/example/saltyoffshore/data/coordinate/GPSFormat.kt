package com.example.saltyoffshore.data.coordinate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/// GPS coordinate display format preference
///
/// Defines how coordinates are displayed to the user:
/// - DMM: Degrees & Decimal Minutes (42 20.740' N) - Default, common for marine navigation
/// - DMS: Degrees, Minutes, Seconds (42 20' 44" N) - Traditional format
/// - DD: Decimal Degrees (42.3456 N) - Compact scientific format
@Serializable
enum class GPSFormat(val displayName: String, val example: String) {
    @SerialName("dmm")
    DMM("Degrees & Minutes", "42\u00B0 20.740\u2032 N"),

    @SerialName("dms")
    DMS("Degrees, Minutes & Seconds", "42\u00B0 20\u2032 44\u2033 N"),

    @SerialName("dd")
    DD("Decimal Degrees", "42.3456\u00B0 N")
}
