package com.example.saltyoffshore.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.R
import com.example.saltyoffshore.auth.AuthError
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.ValidationHelper
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * LoginScreen matching iOS LoginView.swift.
 *
 * Three-section layout:
 * - HeaderSection: SaltyMark logo (72x72) + heading
 * - AuthSection: "Continue with email" collapsed -> expand animation -> email/password form
 * - FooterSection: "Don't have an account? Sign Up"
 *
 * Uses MaterialTheme.colorScheme for proper light/dark mode support.
 */
@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToResetPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEmailForm by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isAuthLoading by AuthManager.isLoading.collectAsState()

    val isFormValid = email.isNotEmpty() &&
            ValidationHelper.isValidEmail(email) &&
            password.isNotEmpty()

    val showEmailError = hasAttemptedSubmit &&
            (!ValidationHelper.isValidEmail(email) || email.isEmpty())

    val showPasswordError = hasAttemptedSubmit && password.isEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SubtleGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            HeaderSection()

            Spacer(modifier = Modifier.height(80.dp))

            AuthSection(
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                showEmailError = showEmailError,
                showPasswordError = showPasswordError,
                showEmailForm = showEmailForm,
                isLoading = isAuthLoading,
                isFormValid = isFormValid,
                onContinueWithEmail = { showEmailForm = true },
                onForgotPassword = onNavigateToResetPassword,
                onSignIn = {
                    hasAttemptedSubmit = true
                    if (isFormValid) {
                        scope.launch {
                            try {
                                AuthManager.signIn(email, password)
                            } catch (e: AuthError) {
                                errorMessage = e.message
                                hasAttemptedSubmit = false
                            }
                        }
                    }
                }
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(Spacing.medium))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = SaltyType.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            FooterSection(onSignUp = onNavigateToSignUp)

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SubtleGridBackground() {
    val dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dotSpacing = 24.dp.toPx()
        val dotRadius = 1.dp.toPx()

        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(x, y)
                )
                y += dotSpacing
            }
            x += dotSpacing
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real SaltyMark logo (72x72, matching iOS)
        Image(
            painter = painterResource(id = R.drawable.salty_mark),
            contentDescription = "Salty Offshore",
            modifier = Modifier.size(72.dp)
        )

        Text(
            text = "Built for pros.\nDesigned for everyone.",
            style = SaltyType.headingLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthSection(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showEmailError: Boolean,
    showPasswordError: Boolean,
    showEmailForm: Boolean,
    isLoading: Boolean,
    isFormValid: Boolean,
    onContinueWithEmail: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DividerWithText(text = "or")

        AnimatedVisibility(
            visible = showEmailForm,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                // Email field
                Column {
                    SaltyTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        placeholder = "Email",
                        keyboardType = KeyboardType.Email,
                        isError = showEmailError
                    )
                    if (showEmailError) {
                        Text(
                            text = if (email.isEmpty()) "Email is required" else "Please enter a valid email",
                            color = MaterialTheme.colorScheme.error,
                            style = SaltyType.caption,
                            modifier = Modifier.padding(top = Spacing.small)
                        )
                    }
                }

                // Password field
                Column {
                    SaltyTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        placeholder = "Password",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        isError = showPasswordError,
                        imeAction = ImeAction.Done
                    )
                    if (showPasswordError) {
                        Text(
                            text = "Password is required",
                            color = MaterialTheme.colorScheme.error,
                            style = SaltyType.caption,
                            modifier = Modifier.padding(top = Spacing.small)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.small),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = SaltyType.caption,
                            modifier = Modifier.clickable { onForgotPassword() }
                        )
                    }
                }

                SaltyPrimaryButton(
                    text = "Sign In",
                    isLoading = isLoading,
                    isEnabled = !isLoading && isFormValid,
                    onClick = onSignIn
                )
            }
        }

        if (!showEmailForm) {
            SaltySecondaryButton(
                text = "Continue with email",
                icon = Icons.Default.Email,
                onClick = onContinueWithEmail
            )
        }
    }
}

@Composable
private fun DividerWithText(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SaltyType.caption
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SaltyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        shape = MaterialTheme.shapes.large
    )
}

/** Primary button — accent background, button text color */
@Composable
private fun SaltyPrimaryButton(
    text: String,
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = SaltyType.body,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Secondary button — sunken background, primary text, optional leading icon */
@Composable
private fun SaltySecondaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            text = text,
            style = SaltyType.body,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FooterSection(onSignUp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.extraLarge)
            .clickable { onSignUp() },
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Don't have an account? ",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SaltyType.bodySmall
        )
        Text(
            text = "Sign Up",
            color = MaterialTheme.colorScheme.onSurface,
            style = SaltyType.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
