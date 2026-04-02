package com.example.saltyoffshore.ui.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.data.waypoint.CrewService
import com.example.saltyoffshore.ui.crew.components.CodeValidationState
import com.example.saltyoffshore.ui.crew.components.CrewCodeEntry
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinCrewSheet(
    onDismiss: () -> Unit,
    onCrewJoined: (Crew) -> Unit,
    onSaveName: suspend (firstName: String, lastName: String) -> Unit,
    hasDisplayName: Boolean,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val scope = rememberCoroutineScope()
    var validationState by remember { mutableStateOf<CodeValidationState>(CodeValidationState.Idle) }
    var showDisplayNameSheet by remember { mutableStateOf(false) }
    var pendingCode by remember { mutableStateOf<String?>(null) }

    fun doJoin(code: String) {
        scope.launch {
            validationState = CodeValidationState.Validating
            try {
                val crew = CrewService.joinCrewByCode(code)
                validationState = CodeValidationState.Success(crew.name)
                delay(800)
                onCrewJoined(crew)
                onDismiss()
            } catch (e: Exception) {
                validationState = CodeValidationState.Error(e.message ?: "Failed to join crew")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyColors.overlay,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            )
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Spacing.large),
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SaltyColors.sunken),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = null,
                    tint = SaltyColors.accent,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(Modifier.height(Spacing.large))

            // Title
            Text(
                text = "Enter Invite Code",
                style = SaltyType.heading,
                color = SaltyColors.textPrimary,
            )

            Spacer(Modifier.height(Spacing.small))

            // Subtitle
            Text(
                text = "Ask your crew captain for the 6-character code",
                style = SaltyType.bodySmall,
                color = SaltyColors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // Code entry
            CrewCodeEntry(
                validationState = validationState,
                onComplete = { code ->
                    if (!hasDisplayName) {
                        pendingCode = code
                        showDisplayNameSheet = true
                    } else {
                        doJoin(code)
                    }
                },
                onClear = { validationState = CodeValidationState.Idle },
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    // Display name pre-gate
    if (showDisplayNameSheet) {
        SetDisplayNameSheet(
            onComplete = {
                showDisplayNameSheet = false
                pendingCode?.let { doJoin(it) }
            },
            onDismiss = { showDisplayNameSheet = false },
            onSaveName = onSaveName,
        )
    }
}
