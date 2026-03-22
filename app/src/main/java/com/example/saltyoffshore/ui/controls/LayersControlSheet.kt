package com.example.saltyoffshore.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderConfig
import com.example.saltyoffshore.data.GlobalLayerType
import com.example.saltyoffshore.data.LayerState
import com.example.saltyoffshore.data.LoranRegionConfig
import com.example.saltyoffshore.data.OverlayCategory
import com.example.saltyoffshore.data.Tournament
import com.example.saltyoffshore.ui.controls.layercontrols.DatasetLayerControls
import com.example.saltyoffshore.ui.controls.layercontrols.OverlayLayerControls
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Layer controls bottom sheet with Data and Overlays tabs.
 * Matches iOS LayerControls sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersControlSheet(
    // Dataset layer props
    dataset: Dataset,
    config: DatasetRenderConfig,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    isPrimary: Boolean = true,
    // Overlay layer props
    layersByCategory: List<Pair<OverlayCategory, List<LayerState>>>,
    onOverlayToggle: (GlobalLayerType) -> Unit,
    onOverlayOpacity: (GlobalLayerType, Double) -> Unit,
    selectedLoranConfig: LoranRegionConfig?,
    onLoranConfigChange: (LoranRegionConfig) -> Unit,
    tournaments: List<Tournament>,
    selectedTournament: Tournament?,
    onTournamentSelect: (Tournament) -> Unit,
    onTournamentDeselect: () -> Unit,
    // Sheet props
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Data", "Overlays")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header
            Text(
                text = "Layers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.small)
            )

            // Tab row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.large)
                    .padding(bottom = Spacing.extraLarge, top = Spacing.large)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        DatasetLayerControls(
                            dataset = dataset,
                            config = config,
                            onConfigChanged = onConfigChanged,
                            isPrimary = isPrimary
                        )
                    }
                    1 -> {
                        OverlayLayerControls(
                            layersByCategory = layersByCategory,
                            onToggle = onOverlayToggle,
                            onOpacityChange = onOverlayOpacity,
                            selectedLoranConfig = selectedLoranConfig,
                            onLoranConfigChange = onLoranConfigChange,
                            tournaments = tournaments,
                            selectedTournament = selectedTournament,
                            onTournamentSelect = onTournamentSelect,
                            onTournamentDeselect = onTournamentDeselect
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.large))
            }
        }
    }
}

/**
 * Drag handle for bottom sheet.
 */
@Composable
private fun SheetDragHandle() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.medium),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth(0.1f)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.extraSmall
                )
        )
    }
}
