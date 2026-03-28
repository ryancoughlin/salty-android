package com.example.saltyoffshore.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.ValidationHelper
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Reset password screen matching iOS ResetPasswordView.swift.
 *
 * States: email entry → loading → success (email sent) / error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isLoading by AuthManager.isLoading.collectAsState()
    val scope = rememberCoroutineScope()

    val isEmailValid = ValidationHelper.isValidEmail(email)
    val showEmailError = hasAttemptedSubmit && !isEmailValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password", style = SaltyType.heading) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (showSuccessMessage) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Spacing.extraLarge),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(Spacing.large))
                    Text("Reset Email Sent", style = SaltyType.heading, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Text(
                        text = "Check your email for instructions to reset your password.",
                        style = SaltyType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = Spacing.extraLarge),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Back to Sign In", style = SaltyType.bodySmall)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Spacing.extraLarge),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.padding(top = Spacing.large)) {
                    Text(
                        text = "Enter your email address and we'll send you instructions to reset your password.",
                        style = SaltyType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.extraLarge))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email") },
                        isError = showEmailError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    if (showEmailError) {
                        Text(
                            text = if (email.isEmpty()) "Email is required" else "Please enter a valid email address",
                            color = MaterialTheme.colorScheme.error,
                            style = SaltyType.caption,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = SaltyType.caption)
                    }
                }

                Column(
                    modifier = Modifier.padding(bottom = Spacing.extraLarge),
                    verticalArrangement = Arrangement.spacedBy(Spacing.large)
                ) {
                    Button(
                        onClick = {
                            hasAttemptedSubmit = true
                            if (!isEmailValid) return@Button
                            scope.launch {
                                try {
                                    AuthManager.resetPassword(email)
                                    showSuccessMessage = true
                                    hasAttemptedSubmit = false
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Failed to send reset email"
                                    hasAttemptedSubmit = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Send Reset Email", style = SaltyType.bodySmall)
                        }
                    }
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to Sign In", color = MaterialTheme.colorScheme.onSurfaceVariant, style = SaltyType.bodySmall)
                    }
                }
            }
        }
    }
}
