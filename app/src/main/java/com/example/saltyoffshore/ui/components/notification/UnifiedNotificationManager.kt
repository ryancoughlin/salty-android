package com.example.saltyoffshore.ui.components.notification

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.saltyoffshore.data.AppNotification
import com.example.saltyoffshore.data.LoadOperation

/**
 * Manages app-level notifications shown in the top-center capsule area.
 * Matches iOS UnifiedNotificationManager.
 *
 * Spinner shows while any LoadOperation is active. Errors persist until cleared.
 * Lives as a property on AppViewModel — NOT a standalone ViewModel.
 */
class UnifiedNotificationManager {
    private val activeOps = mutableSetOf<LoadOperation>()
    private var currentError: String? = null

    var notifications by mutableStateOf<List<AppNotification>>(emptyList())
        private set

    val isLoading: Boolean get() = activeOps.isNotEmpty()

    fun startLoading(op: LoadOperation) {
        activeOps.add(op)
        rebuild()
    }

    fun finishLoading(op: LoadOperation) {
        activeOps.remove(op)
        // Clear error on successful completion when no more ops running
        if (activeOps.isEmpty()) {
            currentError = null
        }
        rebuild()
    }

    fun updateError(message: String?) {
        currentError = message
        rebuild()
    }

    private fun rebuild() {
        val result = mutableListOf<AppNotification>()
        if (activeOps.isNotEmpty()) {
            result.add(AppNotification.Loading())
        }
        currentError?.let {
            result.add(AppNotification.Error(it))
        }
        notifications = result
    }
}
