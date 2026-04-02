package com.example.saltyoffshore.ui.crew.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import kotlinx.coroutines.delay

// -- Validation State --

sealed class CodeValidationState {
    data object Idle : CodeValidationState()
    data object Validating : CodeValidationState()
    data class Success(val crewName: String) : CodeValidationState()
    data class Error(val message: String) : CodeValidationState()
}

// -- Constants --

private const val CODE_LENGTH = 6
private const val ALLOWED_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

private fun filterCode(input: String): String =
    input.uppercase().filter { it in ALLOWED_CHARS }.take(CODE_LENGTH)

// -- Component --

@Composable
fun CrewCodeEntry(
    validationState: CodeValidationState,
    onComplete: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val clipboardManager = LocalClipboardManager.current
    val isError = validationState is CodeValidationState.Error

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
    ) {
        // Hidden input field
        BasicTextField(
            value = code,
            onValueChange = { newValue ->
                val filtered = filterCode(newValue)
                code = filtered
                if (filtered.length == CODE_LENGTH) onComplete(filtered)
            },
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                autoCorrectEnabled = false,
            ),
        )

        // Display boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focusRequester.requestFocus() },
        ) {
            for (i in 0 until CODE_LENGTH) {
                if (i == 3) {
                    Text(
                        text = "\u2013",
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = SaltyColors.textSecondary,
                    )
                }
                CodeBox(
                    character = code.getOrNull(i)?.toString().orEmpty(),
                    isActive = code.length == i,
                    isError = isError,
                )
            }
        }

        // Buttons (only in idle state)
        if (validationState is CodeValidationState.Idle) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                OutlinedButton(onClick = {
                    val text = clipboardManager.getText()?.text
                        ?.let(::filterCode)
                    if (text != null) {
                        code = text
                        if (text.length == CODE_LENGTH) onComplete(text)
                    }
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste Code", style = SaltyType.bodySmall)
                }

                if (code.isNotEmpty()) {
                    TextButton(onClick = {
                        code = ""
                        onClear()
                    }) {
                        Text("Clear", style = SaltyType.bodySmall)
                    }
                }
            }
        }

        // Status display
        when (validationState) {
            is CodeValidationState.Validating -> {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            is CodeValidationState.Success -> {
                Text(
                    text = "\u2713 ${validationState.crewName}",
                    style = SaltyType.bodySmall,
                    color = Color(0xFF4CAF50),
                )
            }
            is CodeValidationState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = validationState.message,
                        style = SaltyType.caption,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = {
                        code = ""
                        onClear()
                    }) {
                        Text("Try Again", style = SaltyType.bodySmall)
                    }
                }
            }
            is CodeValidationState.Idle -> {}
        }
    }
}

// -- Individual Code Box --

@Composable
private fun CodeBox(
    character: String,
    isActive: Boolean,
    isError: Boolean,
) {
    val borderWidth = if (isActive) 1.5.dp else 0.5.dp
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isActive -> SaltyColors.accent
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 60.dp)
            .clip(MaterialTheme.shapes.large)
            .background(SaltyColors.sunken)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = MaterialTheme.shapes.large,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (character.isNotEmpty()) {
            Text(
                text = character,
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                ),
                color = SaltyColors.textPrimary,
            )
        }
    }
}
