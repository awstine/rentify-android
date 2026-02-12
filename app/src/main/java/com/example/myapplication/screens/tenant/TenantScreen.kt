package com.example.myapplication.screens.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.models.MachineSession
import com.example.myapplication.data.models.WashingMachine
import com.example.myapplication.screens.home.QuickActionItem
import com.example.myapplication.ui.theme.LightGreen
import com.example.myapplication.ui.theme.MediumAquamarine
import com.example.myapplication.ui.theme.NavyPrimary

@Composable
fun TenantHomeScreen(
    viewModel: TenantHomeViewModel = hiltViewModel(),
    onNavigateToPayment: (String, Double, String) -> Unit = { _, _, _ -> },
    onNavigateToPaymentHistory: () -> Unit = {},
    onNavigateToBrowseRooms: () -> Unit = {} // Added callback for browsing
) {
    val state = viewModel.uiState

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HeaderSection(
                userName = state.displayName,
                roomNumber = if (state.activeBooking != null) state.roomNumber else "Guest"
            )
        },
        bottomBar = { /* Add BottomNav here */ }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Error message
                if (state.error != null) {
                    item {
                        Text(text = state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                    }
                }

                // 1. MAIN STATUS CARD
                item {
                    val activeBooking = state.activeBooking
                    if (activeBooking != null) {
                        // User HAS a room -> Show Rent Status
                        RentStatusCard(
                            amountDue = activeBooking.monthly_rent,
                            dueDate = activeBooking.end_date,
                            isPaid = activeBooking.payment_status == "paid",
                            onPayClick = {
                                onNavigateToPayment(activeBooking.id, activeBooking.monthly_rent, state.roomNumber)
                            }
                        )
                    } else {
                        // User has NO room -> Show "Find a Room" Call to Action
                        NoActiveBookingCard(onBrowseClick = onNavigateToBrowseRooms)
                    }
                }

                // 2. Washing Machine Tracker (Only show if they have a room, or show generic info)
                if (state.machines.isNotEmpty()) {
                    item {
                        Text(
                            text = "Laundry Status",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    items(state.machines) { machine ->
                        val session = if (state.activeSession?.machine_id == machine.id) state.activeSession else null
                        WashingMachineCard(
                            machine = machine,
                            userSession = session
                        )
                    }
                }

                // 3. Quick Actions
                item {
                    Text(
                        text = "Quick Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement
                            .spacedBy(12.dp) // Space between items
                    ) {
                        // We use weights so they share the width equally
                        QuickActionItem(
                            icon = Icons.Outlined.History,
                            label = "History",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToPaymentHistory
                        )
                        QuickActionItem(
                            icon = Icons.Outlined.ReportProblem,
                            label = "Support",
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                        QuickActionItem(
                            icon = Icons.Outlined.Forum,
                            label = "Contact",
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                        QuickActionItem(
                            icon = Icons.Outlined.Campaign,
                            label = "Notices",
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

// --- NEW CUSTOM CARD: "No Room" State ---
@Composable
fun NoActiveBookingCard(onBrowseClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Gradient Background: Navy to a lighter Teal
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            NavyPrimary,
                            Color(0xFF26A69A) // Teal-ish color
                        )
                    )
                )
        ) {
            // Background Decoration (Faint big icon)
            Icon(
                imageVector = Icons.Rounded.Apartment,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(160.dp)
                    .offset(x = 40.dp, y = 20.dp)
            )

            // Content
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Welcome Home!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You don't have an active booking yet.\nFind your perfect space today.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

            }
        }
    }
}

// --- EXISTING COMPONENTS (Unchanged) ---

@Composable
fun HeaderSection(
    userName: String,
    roomNumber: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stacked Hello / Name
        Column {
            Text(
                text = "Hello,",
                fontSize = 23.sp,
                fontWeight = FontWeight.Thin,
                color = NavyPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = userName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

            Box {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Alerts",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .align(Alignment.TopEnd)
                )
        }
    }
}

@Composable
fun RentStatusCard(
    amountDue: Double,
    dueDate: String,
    isPaid: Boolean,
    onPayClick: () -> Unit
) {
    val cardBrush = if (isPaid) {
        Brush.verticalGradient(colors = listOf(LightGreen, MediumAquamarine))
    } else {
        Brush.verticalGradient(colors = listOf(NavyPrimary, Color(0xFF5C5CFF))) // Using Navy
    }
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.background(cardBrush).padding(24.dp)) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.1f),
                modifier = Modifier.align(Alignment.BottomEnd).size(80.dp).offset(x = 10.dp, y = 10.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Monthly Rent", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = contentColor.copy(alpha = 0.9f))
                    Surface(
                        color = if(isPaid) contentColor.copy(alpha=0.2f) else MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isPaid) "PAID" else "DUE",
                            color = if(isPaid) contentColor else Color.White,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isPaid) "All clear!" else "KES ${amountDue.toInt()}", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = contentColor)
                Text(if (isPaid) "Thank you!" else "Due by $dueDate", fontSize = 14.sp, color = contentColor.copy(alpha = 0.8f))

                if (!isPaid) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NavyPrimary)
                    ) {
                        Text("Pay Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WashingMachineCard(machine: WashingMachine, userSession: MachineSession?) {
    val isOccupied = machine.status == "in_use"
    val isMine = userSession != null
    val statusColor = if (isOccupied) MaterialTheme.colorScheme.error else LightGreen

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalLaundryService, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Machine ${machine.machine_number}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isOccupied) (if (isMine) "In Use (You)" else "Occupied") else "Available", color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Making these look like small elevated cards (Appealing style)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .height(90.dp) // Fixed height for uniformity
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with a light colored circle background
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = NavyPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray,
                maxLines = 1
            )
        }
    }
}