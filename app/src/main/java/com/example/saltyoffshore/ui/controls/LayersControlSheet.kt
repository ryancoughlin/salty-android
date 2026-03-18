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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.GlobalLayerType
import com.example.saltyoffshore.data.LayerState
import com.example.saltyoffshore.data.LoranRegionConfig
import com.example.saltyoffshore.data.OverlayCategory
import com.example.saltyoffshore.data.Tournament
import com.example.saltyoffshore.ui.controls.layercontrols.DatasetLayerControls
import com.example.saltyoffshore.ui.controls.layercontrols.OverlayLayerControls

/**
 * Layer controls bottom sheet with Data and Overlays tabs.
 * Matches iOS LayerControls sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersControlSheet(
    // Dataset layer props
    dataset: Dataset,
    snapshot: DatasetRenderingSnapshot,
    onVisualToggle: () -> Unit,
    onVisualOpacity: (Double) -> Unit,
    onContoursToggle: () -> Unit,
    onContoursOpacity: (Double) -> Unit,
    onArrowsToggle: () -> Unit,
    onArrowsOpacity: (Double) -> Unit,
    onBreaksToggle: () -> Unit,
    onBreaksOpacity: (Double) -> Unit,
    onNumbersToggle: () -> Unit,
    onNumbersOpacity: (Double) -> Unit,
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
        containerColor = Color(0xFFF5F5F5),
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
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tab row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color.Black
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
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp, top = 16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // Data tab - existing dataset layer controls
                        DatasetLayerControls(
                            dataset = dataset,
                            snapshot = snapshot,
                            onVisualToggle = onVisualToggle,
                            onVisualOpacity = onVisualOpacity,
                            onContoursToggle = onContoursToggle,
                            onContoursOpacity = onContoursOpacity,
                            onArrowsToggle = onArrowsToggle,
                            onArrowsOpacity = onArrowsOpacity,
                            onBreaksToggle = onBreaksToggle,
                            onBreaksOpacity = onBreaksOpacity,
                            onNumbersToggle = onNumbersToggle,
                            onNumbersOpacity = onNumbersOpacity
                        )
                    }
                    1 -> {
                        // Overlays tab - global layer controls
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

                Spacer(modifier = Modifier.height(16.dp))
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
            .padding(vertical = 12.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth(0.1f)
                .background(
                    Color.Gray.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}
