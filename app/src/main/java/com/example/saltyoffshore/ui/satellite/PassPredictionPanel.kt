package com.example.saltyoffshore.ui.satellite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.NotInterested
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.satellite.DataFreshness
import com.example.saltyoffshore.data.satellite.NextPass
import com.example.saltyoffshore.data.satellite.SatelliteCoverage
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

// MARK: - DataFreshness Colors

private val DataFreshness.color: Color
    get() = when (this) {
        DataFreshness.CURRENT -> Color.Green
        DataFreshness.RECENT -> Color.Yellow
        DataFreshness.STALE -> Color(0xFFFF9800)
        DataFreshness.UNKNOWN -> Color.White.copy(alpha = 0.3f)
    }

// MARK: - NextPassRow

@Composable
fun NextPassRow(coverage: SatelliteCoverage, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(onClick = onTap)
            .padding(horizontal = Spacing.medium)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Freshness dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(coverage.freshness.color)
        )

        val nextPass = coverage.nextUsablePass
        if (nextPass != null) {
            Text(
                text = nextPass.satellite,
                style = SaltyType.mono(12),
                color = Color.White,
                modifier = Modifier.width(64.dp)
            )
            Text(
                text = "~${nextPass.countdownDisplay}",
                style = SaltyType.mono(12),
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = nextPass.estimatedTimeLocal,
                style = SaltyType.mono(12),
                color = Color.White.copy(alpha = 0.5f)
            )
        } else {
            Text(
                text = "No upcoming passes",
                style = SaltyType.mono(12),
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(10.dp)
        )
    }
}

// MARK: - PassPredictionSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassPredictionSheet(coverage: SatelliteCoverage, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Black,
        dragHandle = null
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large)
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pass Predictions",
                    style = SaltyType.heading,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Next usable pass summary
            val nextPass = coverage.nextUsablePass
            if (nextPass != null) {
                PredictionPassRow(pass = nextPass, isNext = true)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = Spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(coverage.freshness.color)
                    )
                    Text(
                        text = "No usable passes predicted",
                        style = SaltyType.mono(12),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Divider()

            // Upcoming passes list
            if (coverage.upcomingPasses.isNotEmpty()) {
                LazyColumn(modifier = Modifier.height(264.dp)) {
                    items(coverage.upcomingPasses, key = { it.id }) { pass ->
                        PredictionPassRow(pass = pass, isNext = false)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Divider()
            Text(
                text = "Not all satellite passes have usable data.",
                style = SaltyType.caption,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .padding(horizontal = Spacing.large)
                    .padding(vertical = Spacing.medium)
            )
        }
    }
}

// MARK: - PredictionPassRow

@Composable
private fun PredictionPassRow(pass: NextPass, isNext: Boolean) {
    val textColor = if (isNext) Color.Black else Color.White
    val bgColor = if (isNext) Color.White else Color.White.copy(alpha = 0.03f)
    val strikethrough = if (!pass.isUsable) TextDecoration.LineThrough else TextDecoration.None

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(bgColor)
            .padding(horizontal = Spacing.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Usability dot
        if (pass.isUsable) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Green)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.NotInterested,
                contentDescription = null,
                tint = if (isNext) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(10.dp)
            )
        }

        // Satellite name
        Text(
            text = pass.satellite,
            style = SaltyType.mono(12),
            color = textColor,
            textDecoration = strikethrough,
            modifier = Modifier.width(64.dp)
        )

        // Countdown
        Text(
            text = "~${pass.countdownDisplay}",
            style = SaltyType.mono(12),
            color = textColor.copy(alpha = if (pass.isUsable) 0.7f else 0.4f),
            textDecoration = strikethrough
        )

        // Estimated time
        Text(
            text = pass.estimatedTimeLocal,
            style = SaltyType.mono(12),
            color = textColor.copy(alpha = if (pass.isUsable) 1.0f else 0.4f),
            textDecoration = strikethrough,
            modifier = Modifier.width(72.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Dataset type badge
        Text(
            text = pass.datasetType.uppercase(),
            style = SaltyType.mono(9).copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
            color = textColor.copy(alpha = 0.4f),
            fontSize = 9.sp
        )
    }
}

// MARK: - Divider

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}
