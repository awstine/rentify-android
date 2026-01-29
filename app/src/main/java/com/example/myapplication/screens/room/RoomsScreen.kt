package com.example.myapplication.screens.room

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.models.Property
import com.example.myapplication.data.models.Room
import com.example.myapplication.screens.tenant.GreenSuccess
import com.example.myapplication.ui.theme.NavyPrimary
import java.util.UUID


@Composable
fun RoomListScreen(
    viewModel: RoomsViewModel = hiltViewModel(),
    onNavigateToPayment: (String, Double, String) -> Unit = { _, _, _ -> }
) {
    val state = viewModel.uiState
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var roomToEdit by remember { mutableStateOf<Room?>(null) }
    var roomToDelete by remember { mutableStateOf<Room?>(null) }
    var roomToBook by remember { mutableStateOf<Room?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadRooms()
    }

    LaunchedEffect(state.isBookingSuccess) {
        if (state.isBookingSuccess && state.bookingId != null && state.bookingRoom != null) {
            onNavigateToPayment(state.bookingId, state.bookingRoom.monthly_rent, state.bookingRoom.room_number)
            viewModel.resetBookingState()
        }
    }

    Scaffold(
        floatingActionButton = {
            // Show FAB for both Landlord AND Admin
            if (state.isLandlord || state.isAdmin) {
                FloatingActionButton(
                    onClick = { showAddRoomDialog = true },
                    containerColor = NavyPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Room")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Property Status", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyPrimary)
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${state.error}", color = Color.Red, textAlign = TextAlign.Center)
                }
            } else if (state.rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.isLandlord || state.isAdmin) "You haven't added any rooms yet.\nTap + to add one." else "No rooms found.\n\nEnsure you are logged in and there are available rooms in the database.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.rooms) { roomWithTenant ->
                        RoomCard(
                            room = roomWithTenant.room,
                            tenantName = roomWithTenant.tenantName,
                            startDate = roomWithTenant.startDate,
                            endDate = roomWithTenant.endDate,
                            isAdmin = state.isAdmin,
                            isLandlord = state.isLandlord,
                            isTenant = state.isTenant,
                            onEdit = { roomToEdit = it },
                            onDelete = { roomToDelete = it },
                            onBook = { roomToBook = it }
                        )
                    }
                }
            }
        }
    }

    if (showAddRoomDialog) {
        AddRoomDialog(
            properties = state.properties,
            onDismiss = { showAddRoomDialog = false },
            onAddRoom = { room ->
                viewModel.createRoom(room)
                showAddRoomDialog = false
            },
            onCreateProperty = { name, address, room ->
                viewModel.createPropertyAndRoom(name, address, room)
                showAddRoomDialog = false
            }
        )
    }

    if (roomToEdit != null) {
        EditRoomDialog(
            room = roomToEdit!!,
            onDismiss = { roomToEdit = null },
            onUpdateRoom = { updatedRoom ->
                viewModel.updateRoom(updatedRoom)
                roomToEdit = null
            }
        )
    }

    if (roomToDelete != null) {
        AlertDialog(
            onDismissRequest = { roomToDelete = null },
            title = { Text("Delete Room") },
            text = { Text("Are you sure you want to delete Room ${roomToDelete!!.room_number}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRoom(roomToDelete!!.id)
                    roomToDelete = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { roomToDelete = null }) { Text("Cancel") }
            }
        )
    }
    
    if (roomToBook != null) {
        AlertDialog(
            onDismissRequest = { roomToBook = null },
            title = { Text("Book Room") },
            text = { 
                if (state.hasActiveBooking) {
                    Text("You already have an active or pending booking. You cannot book another room.")
                } else {
                    Text("Do you want to book Room ${roomToBook!!.room_number} for KES ${roomToBook!!.monthly_rent.toInt()}?") 
                }
            },
            confirmButton = {
                if (!state.hasActiveBooking) {
                    Button(
                        onClick = {
                            viewModel.bookRoom(roomToBook!!)
                            roomToBook = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) { Text("Confirm Booking") }
                } else {
                    Button(
                        onClick = {
                            roomToBook = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) { Text("OK") }
                }
            },
            dismissButton = {
                if (!state.hasActiveBooking) {
                    TextButton(onClick = { roomToBook = null }) { Text("Cancel") }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoomDialog(
    properties: List<Property>,
    onDismiss: () -> Unit,
    onAddRoom: (Room) -> Unit,
    onCreateProperty: (String, String, Room) -> Unit
) {
    var roomNumber by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var isAvailable by remember { mutableStateOf(true) }
    var selectedProperty by remember { mutableStateOf<Property?>(properties.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    // New state for creating a property
    var isCreatingNewProperty by remember { mutableStateOf(properties.isEmpty()) }
    var newPropertyName by remember { mutableStateOf("") }
    var newPropertyAddress by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (isCreatingNewProperty) "Create Property & Room" else "Add New Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                
                if (isCreatingNewProperty) {
                    Text("First, create a property to assign this room to:", fontSize = 14.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = newPropertyName,
                        onValueChange = { newPropertyName = it },
                        label = { Text("Property Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPropertyAddress,
                        onValueChange = { newPropertyAddress = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (properties.isNotEmpty()) {
                        TextButton(onClick = { isCreatingNewProperty = false }) {
                            Text("Or select existing property")
                        }
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = selectedProperty?.name ?: "Select Property",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            properties.forEach { property ->
                                DropdownMenuItem(
                                    text = { Text(text = property.name) },
                                    onClick = {
                                        selectedProperty = property
                                        expanded = false
                                    }
                                )
                            }
                            // Option to create new property even if list is not empty
                            DropdownMenuItem(
                                text = { Text(text = "+ Create New Property", fontWeight = FontWeight.Bold, color = NavyPrimary) },
                                onClick = {
                                    isCreatingNewProperty = true
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Room Details", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text("Floor (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rent,
                    onValueChange = { rent = it },
                    label = { Text("Monthly Rent") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                        colors = CheckboxDefaults.colors(checkedColor = NavyPrimary)
                    )
                    Text("Available immediately")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newRoom = Room(
                        id = UUID.randomUUID().toString(),
                        property_id = selectedProperty?.id ?: "", // Will be ignored if creating new property
                        room_number = roomNumber,
                        floor = floor.toIntOrNull(),
                        monthly_rent = rent.toDoubleOrNull() ?: 0.0,
                        is_available = isAvailable
                    )

                    if (isCreatingNewProperty) {
                         if (newPropertyName.isNotBlank() && newPropertyAddress.isNotBlank() && roomNumber.isNotBlank() && rent.isNotBlank()) {
                            onCreateProperty(newPropertyName, newPropertyAddress, newRoom)
                         }
                    } else {
                        val propertyId = selectedProperty?.id
                        if (propertyId != null && roomNumber.isNotBlank() && rent.isNotBlank()) {
                            onAddRoom(newRoom.copy(property_id = propertyId))
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                enabled = (isCreatingNewProperty && newPropertyName.isNotBlank() && newPropertyAddress.isNotBlank() && roomNumber.isNotBlank() && rent.isNotBlank()) ||
                          (!isCreatingNewProperty && selectedProperty != null && roomNumber.isNotBlank() && rent.isNotBlank())
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun EditRoomDialog(
    room: Room,
    onDismiss: () -> Unit,
    onUpdateRoom: (Room) -> Unit
) {
    var roomNumber by remember { mutableStateOf(room.room_number) }
    var floor by remember { mutableStateOf(room.floor?.toString() ?: "") }
    var rent by remember { mutableStateOf(room.monthly_rent.toString()) }
    var isAvailable by remember { mutableStateOf(room.is_available) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text("Floor (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rent,
                    onValueChange = { rent = it },
                    label = { Text("Monthly Rent") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                        colors = CheckboxDefaults.colors(checkedColor = NavyPrimary)
                    )
                    Text("Available")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedRoom = room.copy(
                        room_number = roomNumber,
                        floor = floor.toIntOrNull(),
                        monthly_rent = rent.toDoubleOrNull() ?: 0.0,
                        is_available = isAvailable
                    )
                    onUpdateRoom(updatedRoom)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                enabled = roomNumber.isNotBlank() && rent.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun RoomCard(
    room: Room,
    tenantName: String? = null,
    startDate: String? = null,
    endDate: String? = null,
    isAdmin: Boolean = false,
    isLandlord: Boolean = false,
    isTenant: Boolean = false,
    onEdit: (Room) -> Unit = {},
    onDelete: (Room) -> Unit = {},
    onBook: (Room) -> Unit = {}
) {
    val isOccupied = !room.is_available

    // Logic: Occupied rooms are softer/greyer, Vacant rooms pop with color
    val statusColor = if (isOccupied) Color.Gray else GreenSuccess
    val contentAlpha = if (isOccupied) 0.6f else 1f

    // Card Elevation: Higher for vacant (active), lower for occupied
    val elevation = if (isOccupied) 2.dp else 8.dp

    // Background: White usually looks cleanest, relying on the tag for status color
    val containerColor = Color.White

    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(16.dp), // Softer, modern corners
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp) // External spacing
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // Internal spacing
        ) {
            // --- TOP ROW: Room Number & Status Badge ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Room Icon
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        tint = if (isOccupied) Color.Gray else NavyPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Room ${room.room_number}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOccupied) Color.Gray else NavyPrimary
                        )
                        // Floor / Type
                        val type = if (room.floor != null) "${room.floor} Floor" else "Standard"
                        Text(
                            text = type,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Status Pill
                StatusBadge(isOccupied = isOccupied, color = statusColor)
            }

            // --- TENANT INFO (Only for Admin/Landlord and if Occupied) ---
            if (isOccupied && (isAdmin || isLandlord) && tenantName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Tenant",
                            tint = NavyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tenant: $tenantName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = NavyPrimary
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 1.dp,
                color = Color.LightGray.copy(alpha = 0.3f)
            )

            // --- BOTTOM ROW: Price & Actions ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price Section
                Column {
                    Text(
                        text = "Monthly Rent",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "KES ${room.monthly_rent.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOccupied) Color.Gray else NavyPrimary
                    )
                }

                // Action Buttons (Only for Admin/Landlord)
                if (isAdmin || isLandlord) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Using FilledTonalIconButtons for a modern look
                        FilledTonalIconButton(
                            onClick = { onEdit(room) },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = NavyPrimary.copy(alpha = 0.1f),
                                contentColor = NavyPrimary
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalIconButton(
                            onClick = { onDelete(room) },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Red.copy(alpha = 0.1f),
                                contentColor = Color.Red
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        }
                    }
                } else if (isTenant && !isOccupied) {
                    // Action Buttons for Tenants
                    FilledTonalButton(
                        onClick = { onBook(room) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NavyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Book Now")
                    }
                }
            }
        }
    }
}

// Helper Composable for the Status Pill
@Composable
fun StatusBadge(isOccupied: Boolean, color: Color) {
    val text = if (isOccupied) "Occupied" else "Vacant"
    val backgroundColor = color.copy(alpha = 0.15f)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(50), // Pill shape
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
