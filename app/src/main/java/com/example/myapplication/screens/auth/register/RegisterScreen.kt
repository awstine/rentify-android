package com.example.myapplication.screens.auth.register

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R

val NavyBlue = Color(0xFF1F2F43)
val InputLightGrey = Color(0xFFF3F4F6)
val TextGray = Color(0xFF6A6A6A)

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit, // This is also used to navigate to Login
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    LaunchedEffect(key1 = uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            onRegisterSuccess()
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

        Spacer(modifier = Modifier.height(60.dp))

        Image(
            painter = painterResource(R.drawable.img),
            contentDescription = "Logo",
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Get Started!",
            color = TextGray,
            fontSize = 18.sp
        )

        Text(
            text = "Create an Account",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "Please fill your informations",
            color = TextGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

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
                StyledRegisterTextField(
                    value = uiState.username,
                    onValueChange = { viewModel.onEvent(RegisterUiEvent.UsernameChanged(it)) },
                    label = "Email",
                    trailingIconVector = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                StyledRegisterTextField(
                    value = uiState.mobile,
                    onValueChange = { viewModel.onEvent(RegisterUiEvent.MobileChanged(it)) },
                    label = "Mobile Number",
                    trailingIconVector = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                StyledRegisterTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onEvent(RegisterUiEvent.PasswordChanged(it)) },
                    label = "Password",
                    trailingIconVector = Icons.Default.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        ElevatedButton(
            onClick = { viewModel.onEvent(RegisterUiEvent.Submit) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
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
                Text("Sign up now", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Already have an account? ", color = TextGray)
            Text(
                text = "Sign In",
                color = NavyBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.clickable { onRegisterSuccess() } // Navigate to Login
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun StyledRegisterTextField(
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
