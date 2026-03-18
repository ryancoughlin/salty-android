package com.example.saltyoffshore.ui.screen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.auth.AuthError
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.ValidationHelper
import kotlinx.coroutines.launch

// Salty brand colors
private val SaltyBase = Color(0xFF0A0A0F)
private val SaltyAccent = Color(0xFF00D4AA)
private val SaltyBorder = Color(0xFF1A1A24)
private val SaltyTextPrimary = Color(0xFFFFFFFF)
private val SaltyTextSecondary = Color(0xFF8A8A9A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val isFormValid = firstName.isNotEmpty() &&
            lastName.isNotEmpty() &&
            email.isNotEmpty() &&
            ValidationHelper.isValidEmail(email) &&
            password.isNotEmpty() &&
            password == confirmPassword &&
            ValidationHelper.isValidPassword(password)

    val showFirstNameError = hasAttemptedSubmit && firstName.isEmpty()
    val showLastNameError = hasAttemptedSubmit && lastName.isEmpty()
    val showEmailError = hasAttemptedSubmit &&
            (!ValidationHelper.isValidEmail(email) || email.isEmpty())
    val showPasswordLengthError = hasAttemptedSubmit &&
            !ValidationHelper.isValidPassword(password)
    val showPasswordMatchError = hasAttemptedSubmit &&
            password.isNotEmpty() &&
            confirmPassword.isNotEmpty() &&
            password != confirmPassword

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SaltyTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SaltyBase
                )
            )
        },
        containerColor = SaltyBase
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            SignUpHeader()

            Spacer(modifier = Modifier.height(32.dp))

            // Form fields
            SignUpForm(
                firstName = firstName,
                onFirstNameChange = { firstName = it },
                lastName = lastName,
                onLastNameChange = { lastName = it },
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it },
                showFirstNameError = showFirstNameError,
                showLastNameError = showLastNameError,
                showEmailError = showEmailError,
                showPasswordLengthError = showPasswordLengthError,
                showPasswordMatchError = showPasswordMatchError,
                isLoading = AuthManager.isLoading,
                onCreateAccount = {
                    hasAttemptedSubmit = true
                    if (isFormValid) {
                        scope.launch {
                            try {
                                AuthManager.signUp(
                                    email = email,
                                    password = password,
                                    firstName = firstName,
                                    lastName = lastName
                                )
                                onSignUpSuccess()
                            } catch (e: AuthError) {
                                errorMessage = e.message
                                password = ""
                                confirmPassword = ""
                                hasAttemptedSubmit = false
                            }
                        }
                    }
                }
            )

            // Error message
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = Color.Red,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SignUpHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Logo placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SaltyAccent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                color = SaltyBase,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Create your account",
            color = SaltyTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SignUpForm(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    showFirstNameError: Boolean,
    showLastNameError: Boolean,
    showEmailError: Boolean,
    showPasswordLengthError: Boolean,
    showPasswordMatchError: Boolean,
    isLoading: Boolean,
    onCreateAccount: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // First name
        FormField(
            value = firstName,
            onValueChange = onFirstNameChange,
            placeholder = "First Name",
            isError = showFirstNameError,
            errorMessage = if (showFirstNameError) "First name is required" else null,
            capitalization = KeyboardCapitalization.Words
        )

        // Last name
        FormField(
            value = lastName,
            onValueChange = onLastNameChange,
            placeholder = "Last Name",
            isError = showLastNameError,
            errorMessage = if (showLastNameError) "Last name is required" else null,
            capitalization = KeyboardCapitalization.Words
        )

        // Email
        FormField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "Email",
            keyboardType = KeyboardType.Email,
            isError = showEmailError,
            errorMessage = if (showEmailError) {
                if (email.isEmpty()) "Email is required" else "Please enter a valid email address"
            } else null
        )

        // Password
        FormField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Password",
            isPassword = true,
            isError = showPasswordLengthError,
            errorMessage = if (showPasswordLengthError) "Password must be at least 6 characters" else null,
            helperText = if (!showPasswordLengthError && password.isNotEmpty()) "Minimum 6 characters" else null
        )

        // Confirm password
        FormField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "Confirm Password",
            isPassword = true,
            isError = showPasswordMatchError,
            errorMessage = if (showPasswordMatchError) "Passwords do not match" else null,
            imeAction = ImeAction.Done
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Create account button
        Button(
            onClick = onCreateAccount,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SaltyAccent,
                contentColor = SaltyBase,
                disabledContainerColor = SaltyAccent.copy(alpha = 0.5f),
                disabledContentColor = SaltyBase.copy(alpha = 0.5f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = SaltyBase,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Create Account",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    helperText: String? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, color = SaltyTextSecondary)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
                capitalization = capitalization
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SaltyTextPrimary,
                unfocusedTextColor = SaltyTextPrimary,
                focusedBorderColor = if (isError) Color.Red else SaltyAccent,
                unfocusedBorderColor = if (isError) Color.Red else SaltyBorder,
                cursorColor = SaltyAccent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else if (helperText != null) {
            Text(
                text = helperText,
                color = SaltyTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
