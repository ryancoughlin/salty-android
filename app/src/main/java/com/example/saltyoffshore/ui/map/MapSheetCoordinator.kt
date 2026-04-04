package com.example.saltyoffshore.ui.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Single source of truth for which sheet is visible over the map.
 * Matches iOS MapUICoordinator — one observed property, nothing else.
 *
 * Only MapScreen and the controls overlay read activeSheet.
 * The map composable (MapContent) never reads it — this is the isolation boundary.
 */
class MapSheetCoordinator {
    /** The currently visible sheet, or null if no sheet is shown. */
    var activeSheet: MapSheet? by mutableStateOf(null)
        private set

    fun openSheet(sheet: MapSheet) {
        activeSheet = sheet
    }

    fun dismissSheet() {
        activeSheet = null
    }
}
