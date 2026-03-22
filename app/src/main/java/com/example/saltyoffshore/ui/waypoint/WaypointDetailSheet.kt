package com.example.saltyoffshore.ui.waypoint

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.coordinate.CoordinateFormatter
import com.example.saltyoffshore.data.coordinate.GPSFormat
import com.example.saltyoffshore.data.waypoint.Waypoint
import com.example.saltyoffshore.data.waypoint.WaypointConditionsService
import com.example.saltyoffshore.data.waypoint.WaypointSource
import com.example.saltyoffshore.data.waypoint.WaypointWeatherService
import com.example.saltyoffshore.ui.waypoint.components.ConditionsUiState
import com.example.saltyoffshore.ui.waypoint.components.WaypointConditionsContent
import com.example.saltyoffshore.ui.waypoint.components.WaypointWeatherContent
import com.example.saltyoffshore.ui.waypoint.components.WeatherUiState
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

/**
 * Waypoint detail bottom sheet matching iOS WaypointDetailsView.
 *
 * Sections:
 * 1. Header (symbol icon + name + metadata)
 * 2. Coordinates (copyable)
 * 3. Action buttons (Share to Crew / Edit)
 * 4. Inline notes (auto-save on focus lost)
 * 5. Tab selector (Conditions / Weather Forecast)
 * 6. Tab content (stubs for Phase 9)
 * 7. Bottom actions (Share as GPX / Remove Waypoint)
 */
@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun WaypointDetailSheet(
    waypoint: Waypoint,
    source: WaypointSource,
    gpsFormat: GPSFormat,
    onDismiss: () -> Unit,
    onEdit: (Waypoint) -> Unit,
    onDelete: (Waypoint) -> Unit,
    onShareToCrew: (Waypoint) -> Unit,
    onShareGPX: (Waypoint) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var notes by remember(waypoint.id) { mutableStateOf(waypoint.notes ?: "") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCopiedFeedback by remember { mutableStateOf(false) }

    // ── Conditions & Weather state ──────────────────────────────────────
    var conditionsState by remember { mutableStateOf(ConditionsUiState()) }
    var weatherState by remember { mutableStateOf(WeatherUiState()) }

    // Fetch conditions + weather when waypoint changes
    LaunchedEffect(waypoint.id) {
        conditionsState = ConditionsUiState(isLoading = true)
        weatherState = WeatherUiState(isLoading = true)

        val lat = waypoint.latitude
        val lon = waypoint.longitude

        // Kick off all requests in parallel on IO
        val conditionsDeferred = async(Dispatchers.IO) {
            runCatching { WaypointConditionsService.fetchConditions(lat, lon) }
        }
        val historyDeferred = async(Dispatchers.IO) {
            runCatching { WaypointConditionsService.fetchHistory(lat, lon) }
        }
        val weatherDeferred = async(Dispatchers.IO) {
            runCatching { WaypointWeatherService.fetchWeather(lat, lon) }
        }
        val wavesDeferred = async(Dispatchers.IO) {
            runCatching { WaypointWeatherService.fetchWaves(lat, lon) }
        }
        val summaryDeferred = async(Dispatchers.IO) {
            runCatching { WaypointWeatherService.fetchSummary(lat, lon) }
        }

        // Await conditions + history
        val condResult = conditionsDeferred.await()
        val historyResult = historyDeferred.await()
        val condResponse = condResult.getOrNull()
        conditionsState = ConditionsUiState(
            isLoading = false,
            response = condResponse,
            historyResponse = historyResult.getOrNull(),
            error = condResult.exceptionOrNull()?.message
        )

        // Extract sunrise/sunset hours from solar data for wind chart night zones
        val sunriseHour = condResponse?.solar?.sunrise?.let { parseHourFromISO(it) }
        val sunsetHour = condResponse?.solar?.sunset?.let { parseHourFromISO(it) }

        // Await weather/waves/summary
        val weatherResp = weatherDeferred.await().getOrNull()
        val wavesResp = wavesDeferred.await().getOrNull()
        val summaryResp = summaryDeferred.await().getOrNull()
        weatherState = WeatherUiState(
            isLoading = false,
            weatherResponse = weatherResp,
            waveResponse = wavesResp,
            summaryText = summaryResp?.summary,
            error = if (weatherResp == null) "Failed to load forecast" else null,
            sunriseHour = sunriseHour,
            sunsetHour = sunsetHour
        )
    }

    // Debounce notes auto-save
    LaunchedEffect(waypoint.id) {
        snapshotFlow { notes }
            .drop(1) // skip initial emission
            .debounce(800)
            .collect { value ->
                val trimmed = value.trim()
                if (trimmed != (waypoint.notes ?: "")) {
                    onNotesChanged(trimmed)
                }
            }
    }

    // Reset copied feedback after delay
    LaunchedEffect(showCopiedFeedback) {
        if (showCopiedFeedback) {
            kotlinx.coroutines.delay(1600)
            showCopiedFeedback = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ── 1. HEADER ───────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Symbol icon in circle
                val iconResId = context.resources.getIdentifier(
                    waypoint.symbol.imageName.lowercase().replace(" ", "_"),
                    "drawable",
                    context.packageName
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(72.dp)
                    ) {}
                    if (iconResId != 0) {
                        Image(
                            painter = painterResource(id = iconResId),
                            contentDescription = waypoint.symbol.rawValue,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name + metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = waypoint.name ?: "Unnamed Waypoint",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Crew name if shared
                        if (source.isShared) {
                            val sharedBy = source.sharedWaypointOrNull?.sharedByName
                            if (sharedBy != null) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = sharedBy,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Symbol name + date
                    Text(
                        text = "${waypoint.symbol.rawValue} \u2022 ${waypoint.createdAt.take(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 2. COORDINATES (copyable) ────────────────────────────────────
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showCopiedFeedback) "Copied" else CoordinateFormatter.formatCoordinate(
                            waypoint.latitude,
                            waypoint.longitude,
                            gpsFormat
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = if (showCopiedFeedback) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val clipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val coordString = CoordinateFormatter.formatCoordinate(
                            waypoint.latitude,
                            waypoint.longitude,
                            gpsFormat
                        )
                        clipboardManager.setPrimaryClip(
                            ClipData.newPlainText("Coordinates", coordString)
                        )
                        showCopiedFeedback = true
                    }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy coordinates",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 3. ACTION BUTTONS ────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onShareToCrew(waypoint) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share to Crew")
                }
                OutlinedButton(
                    onClick = { onEdit(waypoint) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 4. INLINE NOTES ──────────────────────────────────────────────
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                placeholder = { Text("Add a note...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 12
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 5. TAB SELECTOR ──────────────────────────────────────────────
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Conditions") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Weather Forecast") }
                )
            }

            // ── 6. TAB CONTENT ────────────────────────────────────────────────
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    0 -> WaypointConditionsContent(
                        conditionsState = conditionsState,
                        summaryText = weatherState.summaryText,
                        summaryLoading = weatherState.isLoading
                    )
                    1 -> WaypointWeatherContent(
                        weatherState = weatherState
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 7. BOTTOM ACTIONS ────────────────────────────────────────────
            TextButton(
                onClick = { onShareGPX(waypoint) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share as GPX")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove Waypoint")
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ── Delete confirmation dialog ───────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    if (source.isShared) "Delete shared waypoint?"
                    else "Remove this waypoint?"
                )
            },
            text = {
                Text(
                    if (source.isShared) "This will remove for everyone. Cannot be undone."
                    else "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(waypoint)
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/** Extract hour-of-day from an ISO-8601 timestamp for sunrise/sunset. */
private fun parseHourFromISO(iso: String): Int? {
    return try {
        val instant = java.time.Instant.parse(iso)
        instant.atZone(java.time.ZoneId.systemDefault()).hour
    } catch (_: Exception) {
        null
    }
}
