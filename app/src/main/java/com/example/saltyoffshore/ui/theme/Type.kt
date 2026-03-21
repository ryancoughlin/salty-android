package com.example.saltyoffshore.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.R

val SplineSans = FontFamily(
    Font(R.font.spline_sans, FontWeight.Normal),
    Font(R.font.spline_sans, FontWeight.Medium),
    Font(R.font.spline_sans, FontWeight.SemiBold)
)

val SplineSansMono = FontFamily(
    Font(R.font.spline_sans_mono, FontWeight.Normal)
)

// ── Salty Typography Tokens ──────────────────────────────────────────────────
// Mirrors iOS Theme.swift: headingLarge → captionSmall + mono
//
// | Token        | Weight   | Size | Use                              |
// |-------------|----------|------|----------------------------------|
// | headingLarge | SemiBold | 28sp | Screen titles, sheet titles       |
// | heading      | Medium   | 20sp | Section headings, card titles     |
// | body         | Medium   | 16sp | Primary body text                 |
// | bodySmall    | Medium   | 14sp | Buttons, control labels           |
// | caption      | Medium   | 12sp | Supporting text, metadata         |
// | captionSmall | Normal   | 10sp | Tertiary info, badges, timestamps |
// | mono(size)   | Normal   | Any  | Numbers ONLY (not labels)         |

object SaltyType {
    val headingLarge = TextStyle(
        fontFamily = SplineSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    )

    val heading = TextStyle(
        fontFamily = SplineSans,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    )

    val body = TextStyle(
        fontFamily = SplineSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    )

    val bodySmall = TextStyle(
        fontFamily = SplineSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )

    val caption = TextStyle(
        fontFamily = SplineSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )

    val captionSmall = TextStyle(
        fontFamily = SplineSans,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )

    /** Numbers ONLY — fixed-width to prevent layout reflow when values update */
    fun mono(size: Int = 14) = TextStyle(
        fontFamily = SplineSansMono,
        fontWeight = FontWeight.Normal,
        fontSize = size.sp
    )
}

// Material 3 typography mapped to Salty tokens
val Typography = Typography(
    headlineLarge = SaltyType.headingLarge,
    headlineMedium = SaltyType.heading,
    bodyLarge = SaltyType.body,
    bodyMedium = SaltyType.bodySmall,
    bodySmall = SaltyType.caption,
    labelSmall = SaltyType.captionSmall,
    titleLarge = SaltyType.headingLarge,
    titleMedium = SaltyType.heading,
    labelMedium = SaltyType.bodySmall,
    labelLarge = SaltyType.body
)
