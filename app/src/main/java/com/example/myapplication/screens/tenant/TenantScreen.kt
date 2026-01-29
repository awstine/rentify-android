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
import java.text.SimpleDateFormat
import java.util.Locale

// --- Colors & Styles ---
// Removed previous color palette to use default Material Theme colors or transparent/white as requested,
// except for the GreenSuccess which is needed for the paid card.
val GreenSuccess = Color(0xFF4CAF50)

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
                            text = "No washing machines available.",
                            color = Color.Gray,
                            fontSize = 14.sp
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
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraLight,
                    //color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$userName",
                    fontSize = 42.sp,
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
    // Keep green if paid, otherwise use default theme colors
    val cardBackground = if (isPaid) {
        Brush.horizontalGradient(listOf(GreenSuccess, Color(0xFF81C784)))
    } else {
         Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
    }
    
    val contentColor = Color.White // Keeping white text on colored cards for readability

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.background(cardBackground).padding(24.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Rent Status", color = contentColor.copy(alpha = 0.8f))
                    if (!isPaid) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "DUE SOON",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isPaid) "Paid" else "KES ${amountDue.toInt()}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                
                // Format date string nicely if possible, assuming YYYY-MM-DD from DB
                val formattedDate = try {
                     // Simple parsing, better to use proper DateTime formatter
                     dueDate
                } catch (e: Exception) { dueDate }
                
                Text(
                    text = if (isPaid) "Paid for this month" else "Due by $formattedDate",
                    color = contentColor.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!isPaid) {
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pay via M-Pesa", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
    
    val statusColor = if (isOccupied) MaterialTheme.colorScheme.error else GreenSuccess
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
                     Text(text = "Ends at: ${userSession.end_time}", fontSize = 10.sp, color = Color.Gray)
                }
            }

            Button(
                onClick = { /* Start Session */ },
                enabled = !isOccupied,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, disabledContainerColor = Color.LightGray),
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
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}
