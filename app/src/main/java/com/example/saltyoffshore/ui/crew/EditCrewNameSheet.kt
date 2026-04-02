package com.example.saltyoffshore.ui.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCrewNameSheet(
    currentName: String,
    onSave: suspend (newName: String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(currentName) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val canSave = name.isNotBlank() && name.trim() != currentName && !isSaving

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
            modifier = Modifier.padding(Spacing.large),
        ) {
            Text(
                text = "Edit Crew Name",
                style = SaltyType.heading,
                color = SaltyColors.textPrimary,
            )

            Spacer(Modifier.height(Spacing.large))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = { Text("Crew Name") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SaltyColors.accent,
                    unfocusedBorderColor = SaltyColors.borderSubtle,
                    focusedLabelColor = SaltyColors.accent,
                    unfocusedLabelColor = SaltyColors.textSecondary,
                    cursorColor = SaltyColors.accent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(Spacing.small))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(Spacing.extraLarge))

            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.length > 50) {
                        errorMessage = "Crew name must be 50 characters or less"
                        return@Button
                    }
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            onSave(trimmed)
                            onDismiss()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to rename crew"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaltyColors.accent,
                    contentColor = SaltyColors.buttonText,
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SaltyColors.buttonText,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save", style = SaltyType.bodySmall)
                }
            }

            Spacer(Modifier.height(Spacing.large))
        }
    }
}
