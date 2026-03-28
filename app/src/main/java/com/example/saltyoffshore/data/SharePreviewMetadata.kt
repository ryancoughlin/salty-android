package com.example.saltyoffshore.data

import android.graphics.Bitmap
import java.util.Date

/**
 * Lightweight metadata for the share preview card.
 * Port of iOS SharePreviewMetadata.swift.
 */
data class SharePreviewMetadata(
    val mapSnapshot: Bitmap? = null,
    val regionName: String = "",
    val datasetName: String = "",
    val timestamp: Date = Date(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
