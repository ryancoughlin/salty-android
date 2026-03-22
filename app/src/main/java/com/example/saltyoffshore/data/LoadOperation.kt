package com.example.saltyoffshore.data

/**
 * Tracks which async operations are currently loading.
 * Matches iOS LoadOperation enum in UnifiedNotificationManager.swift.
 */
enum class LoadOperation {
    Region,
    Dataset,
    Overlay
}
