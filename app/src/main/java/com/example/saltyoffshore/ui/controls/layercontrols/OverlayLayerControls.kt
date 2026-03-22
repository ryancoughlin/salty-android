package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.GlobalLayerType
import com.example.saltyoffshore.data.LayerState
import com.example.saltyoffshore.data.LoranRegionConfig
import com.example.saltyoffshore.data.OverlayCategory
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.saltyoffshore.data.Tournament

/**
 * Overlay layer controls grouped by category.
 * Matches iOS OverlayLayerControls.
 */
@Composable
fun OverlayLayerControls(
    layersByCategory: List<Pair<OverlayCategory, List<LayerState>>>,
    onToggle: (GlobalLayerType) -> Unit,
    onOpacityChange: (GlobalLayerType, Double) -> Unit,
    // LORAN
    selectedLoranConfig: LoranRegionConfig?,
    onLoranConfigChange: (LoranRegionConfig) -> Unit,
    // Tournaments
    tournaments: List<Tournament>,
    selectedTournament: Tournament?,
    onTournamentSelect: (Tournament) -> Unit,
    onTournamentDeselect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        layersByCategory.forEach { (category, layers) ->
            LayerCategorySection(
                category = category,
                layers = layers,
                onToggle = onToggle,
                onOpacityChange = onOpacityChange,
                selectedLoranConfig = selectedLoranConfig,
                onLoranConfigChange = onLoranConfigChange,
                tournaments = tournaments,
                selectedTournament = selectedTournament,
                onTournamentSelect = onTournamentSelect,
                onTournamentDeselect = onTournamentDeselect
            )
        }
    }
}

@Composable
private fun LayerCategorySection(
    category: OverlayCategory,
    layers: List<LayerState>,
    onToggle: (GlobalLayerType) -> Unit,
    onOpacityChange: (GlobalLayerType, Double) -> Unit,
    selectedLoranConfig: LoranRegionConfig?,
    onLoranConfigChange: (LoranRegionConfig) -> Unit,
    tournaments: List<Tournament>,
    selectedTournament: Tournament?,
    onTournamentSelect: (Tournament) -> Unit,
    onTournamentDeselect: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Category header
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        layers.forEach { layerState ->
            LayerToggleRow(
                layerState = layerState,
                onToggle = { onToggle(layerState.type) },
                onOpacityChange = { onOpacityChange(layerState.type, it) }
            )

            // Special handling for LORAN
            if (layerState.type == GlobalLayerType.LORAN_GRID_LINES && layerState.isEnabled) {
                LoranRegionPicker(
                    selectedConfig = selectedLoranConfig,
                    onConfigChange = onLoranConfigChange,
                    modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 8.dp)
                )
            }

            // Special handling for Tournaments
            if (layerState.type == GlobalLayerType.TOURNAMENTS && layerState.isEnabled) {
                TournamentPicker(
                    tournaments = tournaments,
                    selectedTournament = selectedTournament,
                    onSelect = onTournamentSelect,
                    modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LayerToggleRow(
    layerState: LayerState,
    onToggle: () -> Unit,
    onOpacityChange: (Double) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = layerState.type.iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (layerState.isEnabled) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = layerState.type.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (layerState.isEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = layerState.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }

        // Opacity slider (if enabled and supports opacity)
        if (layerState.isEnabled && layerState.type.supportsOpacity) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Opacity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = layerState.opacity.toFloat(),
                    onValueChange = { onOpacityChange(it.toDouble()) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )

                Text(
                    text = "${(layerState.opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoranRegionPicker(
    selectedConfig: LoranRegionConfig?,
    onConfigChange: (LoranRegionConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val regions = LoranRegionConfig.availableRegions

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedConfig?.name ?: "Select Region",
            onValueChange = {},
            readOnly = true,
            label = { Text("LORAN Region") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            regions.forEach { config ->
                DropdownMenuItem(
                    text = { Text(config.name) },
                    onClick = {
                        onConfigChange(config)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TournamentPicker(
    tournaments: List<Tournament>,
    selectedTournament: Tournament?,
    onSelect: (Tournament) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    if (tournaments.isEmpty()) {
        Text(
            text = "No tournaments available",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedTournament?.displayName ?: "Select Tournament",
            onValueChange = {},
            readOnly = true,
            label = { Text("Tournament") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            tournaments.forEach { tournament ->
                DropdownMenuItem(
                    text = { Text(tournament.displayName) },
                    onClick = {
                        onSelect(tournament)
                        expanded = false
                    }
                )
            }
        }
    }
}
