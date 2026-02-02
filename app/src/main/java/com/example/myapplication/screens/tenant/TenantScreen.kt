package com.example.myapplication.screens.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.models.MachineSession
import com.example.myapplication.data.models.WashingMachine
import com.example.myapplication.ui.theme.LightGreen
import com.example.myapplication.ui.theme.MediumAquamarine
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TenantHomeScreen(
    viewModel: TenantHomeViewModel = hiltViewModel(),
    onNavigateToPayment: (String, Double, String) -> Unit = { _, _, _ -> },
    onNavigateToPaymentHistory: () -> Unit = {}
) {
    val state = viewModel.uiState

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { 
            HeaderSection(
                userName = state.displayName,
                roomNumber = state.roomNumber
            ) 
        },
        bottomBar = { /* Add BottomNav here */ }
    ) { padding ->
        if (state.isLoading) {
             Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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

                // 2. Rent Status Card
                item {
                    val activeBooking = state.activeBooking
                    if (activeBooking != null) {
                        RentStatusCard(
                            amountDue = activeBooking.monthly_rent,
                            dueDate = activeBooking.end_date, // Using end_date as proxy for due date or you can calculate next due date
                            isPaid = activeBooking.payment_status == "paid",
                            onPayClick = {
                                onNavigateToPayment(activeBooking.id, activeBooking.monthly_rent, state.roomNumber)
                            }
                        )
                    } else {
                         // No active booking card
                         Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.padding(24.dp)) {
                                Text("No active booking found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // 3. Washing Machine Tracker
                if (state.machines.isNotEmpty()) {
                    item {
                        Text(
                            text = "Washing Machines",
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
                } else if (state.activeBooking != null) {
                     item {
                        Text(
                            text = "Washing mashing coming soon.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Light,
                            fontSize = 15.sp
                        )
                    }
                }

                // 4. Quick Actions
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
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickActionItem(
                            icon = Icons.Outlined.History, 
                            label = "Pay History",
                            onClick = onNavigateToPaymentHistory
                        )
                        QuickActionItem(Icons.Outlined.ReportProblem, "Report Issue")
                        QuickActionItem(Icons.Outlined.Forum, "Contact Us")
                        QuickActionItem(Icons.Outlined.Campaign, "Notices")
                    }
                }
                
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun HeaderSection(userName: String, roomNumber: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Hello,",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$userName",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

//            Text(
//                text = roomNumber,
//                fontSize = 14.sp,
//                color = Color.Gray
//            )

            // Notification Icon with Badge
            Box (
            ){
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
}

@Composable
fun RentStatusCard(
    amountDue: Double,
    dueDate: String,
    isPaid: Boolean,
    onPayClick: () -> Unit
) {
    val cardBrush = if (isPaid) {
        Brush.verticalGradient(
            colors = listOf(LightGreen, MediumAquamarine)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary) 
        )
    }
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .background(cardBrush)
                .padding(24.dp)
        ) {
            // Faint background icon
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.1f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(80.dp)
                    .offset(x = 10.dp, y = 10.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "Monthly Rent",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor.copy(alpha = 0.9f)
                    )

                    if (isPaid) {
                        Surface(
                            color = contentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PAID",
                                color = contentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                         Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "DUE",
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isPaid) "All clear!" else "KES ${amountDue.toInt()}",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )

                Text(
                    text = if (isPaid) "Thank you!" else "Due by $dueDate",
                    fontSize = 14.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )

                if (!isPaid) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = contentColor,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
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
    val statusText = if (isOccupied) {
        if (isMine) "In Use (You)" else "Occupied"
    } else "Available"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalLaundryService,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Machine ${machine.machine_number}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                if (isMine && userSession?.end_time != null) {
                     Text(text = "Ends at: ${userSession.end_time}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            Button(
                onClick = { /* Start Session */ },
                enabled = !isOccupied,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(if (isOccupied) "In Use" else "Use", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector, 
    label: String,
    onClick: () -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}
