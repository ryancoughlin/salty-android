package com.example.saltyoffshore.viewmodel

import androidx.compose.runtime.Stable
import com.example.saltyoffshore.data.Announcement
import com.example.saltyoffshore.data.AnnouncementDisplayState
import com.example.saltyoffshore.data.AppStatus

/**
 * App-level state only — domain state lives in individual stores.
 */
@Stable
data class AppState(
    val appStatus: AppStatus = AppStatus.Idle,
    val announcementDisplayState: AnnouncementDisplayState = AnnouncementDisplayState.Hidden,
    val showAnnouncementSheet: Boolean = false,
) {
    val announcement: Announcement?
        get() = (announcementDisplayState as? AnnouncementDisplayState.Visible)?.announcement

    val isAnnouncementVisible: Boolean
        get() = announcementDisplayState is AnnouncementDisplayState.Visible
}
