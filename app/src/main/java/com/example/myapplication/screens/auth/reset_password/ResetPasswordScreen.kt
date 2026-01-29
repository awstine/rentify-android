package com.example.myapplication.screens.auth.reset_password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.screens.auth.register.NavyBlue
import com.example.myapplication.screens.auth.register.TextGray

// Define a light grey for the input fields to contrast with the white background
val InputLightGrey = Color(0xFFF3F4F6)

@Composable
fun ResetPasswordScreen(
    onPasswordReset: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState

    // Navigate away on successful password reset
    if (uiState.resetSuccess) {
        LaunchedEffect(Unit) {
            onPasswordReset()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(80.dp))

        // Title
        Text(
            text = "Reset Password",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your email to reset your password",
            color = TextGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Email Input Field
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 8.dp,
            color = InputLightGrey
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                StyledResetPasswordTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEvent(ResetPasswordUiEvent.EmailChanged(it)) },
                    label = "Email",
                    trailingIconVector = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    error = uiState.error
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Reset Button
        ElevatedButton(
            onClick = { viewModel.onEvent(ResetPasswordUiEvent.Submit) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = NavyBlue,
                contentColor = Color.White
            ),
            enabled = !uiState.isLoading,
            elevation = ButtonDefaults.elevatedButtonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 4.dp
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    "Reset Password",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StyledResetPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    trailingIconVector: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null
) {
    Column {
        Text(
            text = label,
            color = if (error != null) Color.Red else TextGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            singleLine = true,
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = {
                Icon(trailingIconVector, contentDescription = null, tint = TextGray)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.Black,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                errorContainerColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                errorTrailingIconColor = TextGray,
                errorCursorColor = Color.Red
            )
        )
        if (error != null) {
            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
