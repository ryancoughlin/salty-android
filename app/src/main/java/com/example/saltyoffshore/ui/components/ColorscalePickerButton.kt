package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyLayout
import com.example.saltyoffshore.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorscalePickerButton(
    selection: Colorscale?,
    defaultColorscale: Colorscale,
    onChanged: (Colorscale?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayColorscale = selection ?: defaultColorscale
    val gradientColors = displayColorscale.hexColors.map { hex ->
        Color(android.graphics.Color.parseColor(hex))
    }

    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(SaltyLayout.controlCornerRadius))
            .background(Color.Gray.copy(alpha = 0.15f))
            .clickable { showPicker = true }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(gradientColors))
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SaltyColors.textSecondary,
            modifier = Modifier.size(14.dp)
        )
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SaltyColors.base
        ) {
            ColorscalePicker(
                currentSelection = selection,
                defaultColorscale = defaultColorscale,
                onSelected = { newValue ->
                    onChanged(newValue)
                    showPicker = false
                }
            )
        }
    }
}
