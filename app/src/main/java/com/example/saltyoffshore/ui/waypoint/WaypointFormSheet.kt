package com.example.saltyoffshore.ui.waypoint

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.coordinate.GPSFormat
import com.example.saltyoffshore.data.waypoint.Waypoint
import com.example.saltyoffshore.data.waypoint.WaypointFormState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.saltyoffshore.ui.waypoint.components.CoordinateInputView
import com.example.saltyoffshore.ui.waypoint.components.SymbolChipPicker

/**
 * Waypoint create/edit bottom sheet.
 * Ports iOS WaypointSheetFormView.
 *
 * Layout:
 * - Header: title + close button
 * - Scrollable form: name, symbol picker, notes, coordinates
 * - Sticky save button pinned at bottom
 * - Cancel confirmation dialog when form is dirty
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointFormSheet(
    waypoint: Waypoint,
    isNewWaypoint: Boolean,
    gpsFormat: GPSFormat,
    formState: WaypointFormState,
    onFormStateChange: (WaypointFormState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean = false
) {
    // Track the initial form state for dirty checking
    val initialFormState = remember(waypoint.id) { formState }
    val isDirty = formState != initialFormState

    var showCancelAlert by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = {
            if (isDirty) {
                showCancelAlert = true
            } else {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── HEADER ───────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = if (isNewWaypoint) "Add Waypoint" else "Edit Waypoint",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    showCancelAlert = true
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider()

            // ── SCROLLABLE FORM ──────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { onFormStateChange(formState.copy(name = it)) },
                    label = { Text("Name") },
                    placeholder = { Text("Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Symbol picker
                Text("Symbol", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                SymbolChipPicker(
                    selectedSymbol = formState.symbol,
                    onSymbolSelected = { onFormStateChange(formState.copy(symbol = it)) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Notes
                OutlinedTextField(
                    value = formState.notes,
                    onValueChange = { onFormStateChange(formState.copy(notes = it)) },
                    label = { Text("Notes") },
                    placeholder = { Text("Notes (optional)") },
                    minLines = 4,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Coordinates
                Text("Coordinates", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                CoordinateInputView(
                    formState = formState,
                    gpsFormat = gpsFormat,
                    onFormStateChange = onFormStateChange
                )

                // Bottom padding for sticky button clearance
                Spacer(modifier = Modifier.height(100.dp))
            }

            // ── STICKY SAVE BUTTON ───────────────────────────────
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.background
            ) {
                HorizontalDivider()
                Button(
                    onClick = onSave,
                    enabled = formState.isCoordinateValid(gpsFormat) && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        }
    }

    // ── CANCEL CONFIRMATION DIALOG ───────────────────────────────
    if (showCancelAlert) {
        AlertDialog(
            onDismissRequest = { showCancelAlert = false },
            title = { Text("Discard changes?") },
            text = { Text("Any changes you've made will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelAlert = false
                    onCancel()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelAlert = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
