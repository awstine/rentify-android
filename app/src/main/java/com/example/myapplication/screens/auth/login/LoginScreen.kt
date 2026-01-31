package com.example.myapplication.screens.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.R
import com.example.myapplication.screens.auth.register.NavyBlue
import com.example.myapplication.screens.auth.register.TextGray

// Define a light grey for the input fields to contrast with the white background
val InputLightGrey = Color(0xFFF3F4F6)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // KEEP WHITE BACKGROUND
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        // LOGO
        Image(
            painter = painterResource(R.drawable.img),
            contentDescription = "Logo",
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // TITLE
        Text(
            text = "Welcome!",
            color = TextGray,
            fontSize = 18.sp
        )

        Text(
            text = "Sign in",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "Please fill your informations",
            color = TextGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // INPUT CARD CONTAINER (LIKE SCREENSHOT)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 8.dp,
            color = InputLightGrey
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {

                // EMAIL FIELD
                StyledLoginTextField(
                    value = uiState.username,
                    onValueChange = { viewModel.onEvent(LoginUiEvent.UsernameChanged(it)) },
                    label = "Email",
                    trailingIconVector = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                // PASSWORD FIELD
                StyledLoginTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onEvent(LoginUiEvent.PasswordChanged(it)) },
                    label = "Password",
                    trailingIconVector = Icons.Default.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // FORGOT PASSWORD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Forgot Password?",
                color = NavyBlue,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onForgotPassword() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // SIGN IN BUTTON (PILL)
        ElevatedButton(
            onClick = { viewModel.onEvent(LoginUiEvent.Submit) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(15.dp),
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
                    "Sign in now",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SIGN UP TEXT
        Text(
            text = "Sign up",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NavyBlue,
            modifier = Modifier.clickable { onNavigateToRegister() }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}


@Composable
fun StyledLoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    trailingIconVector: ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(
            text = label,
            color = TextGray,
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
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
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
                unfocusedTextColor = Color.Black
            )
        )
    }
}
