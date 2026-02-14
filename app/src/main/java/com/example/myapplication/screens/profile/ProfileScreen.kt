package com.example.myapplication.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.R


@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onSignOut: () -> Unit = {}
) {
    val state = viewModel.uiState
    var expandedCard by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            viewModel.updateProfilePhoto(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    LaunchedEffect(viewModel.profilePhotoUpdateSuccess) {
        viewModel.profilePhotoUpdateSuccess.collect {
            selectedImageUri = null
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top =30.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${state.error}", color = Color.Red)
            }
        } else {
            val user = state.user

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = selectedImageUri ?: user?.profile_image_url
                                ?: R.drawable.ic_launcher_background
                            ),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Profile Photo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { imagePickerLauncher.launch("image/*") }
                                .padding(6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = user?.full_name ?: "Guest User",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = user?.email ?: "No email",
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        color = Color(0xFFE0F7FA),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = user?.role?.uppercase() ?: "TENANT",
                            color = Color(0xFF006064),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                    ) {
                        ProfileListRow(
                            icon = Icons.Default.Badge,
                            title = "ID Number",
                            subtitle = user?.id_number ?: "Not set",
                            isExpanded = expandedCard == "ID",
                            onClick = { expandedCard = if (expandedCard == "ID") null else "ID" }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileListRow(
                            icon = Icons.Default.Phone,
                            title = "Phone Number",
                            subtitle = user?.phone_number ?: "Not set",
                            isExpanded = expandedCard == "Phone",
                            onClick = { expandedCard = if (expandedCard == "Phone") null else "Phone" }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileListRow(
                            icon = Icons.Default.Email,
                            title = "Email Address",
                            subtitle = user?.email ?: "Not set",
                            isExpanded = expandedCard == "Email",
                            onClick = { expandedCard = if (expandedCard == "Email") null else "Email" }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileListRow(
                            icon = Icons.Default.ExitToApp,
                            title = "Sign Out",
                            subtitle = null,
                            isExpanded = false,
                            isDestructive = true,
                            onClick = { viewModel.signOut(onSignOut) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
fun ProfileListRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    isExpanded: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 90f else 0f, label = "arrow")
    val textColor = if (isDestructive) Color.Red else Color(0xFF1E232C)
    val iconBgColor = if (isDestructive) Color(0xFFFFEBEE) else Color(0xFFF5F6F8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            if (subtitle != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Expand",
                    tint = Color.Gray,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded && subtitle != null) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = subtitle ?: "",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }
        }
    }
}
