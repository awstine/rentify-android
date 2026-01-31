package com.example.myapplication.screens.room

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.models.Property
import com.example.myapplication.data.models.Room
import com.example.myapplication.ui.theme.DarkSlateBlue
import com.example.myapplication.ui.theme.LightGreen
import com.example.myapplication.ui.theme.MediumAquamarine
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
            Text("Property Status", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${state.error}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            } else if (state.rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.isLandlord || state.isAdmin) "You haven't added any rooms yet.\nTap + to add one." else "No rooms found.\n\nEnsure you are logged in and there are available rooms in the database.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Confirm Booking") }
                } else {
                    Button(
                        onClick = {
                            roomToBook = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
    // --- State Variables ---
    var roomNumber by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var isAvailable by remember { mutableStateOf(true) }
    var selectedProperty by remember { mutableStateOf<Property?>(properties.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    // State for creating a property
    var isCreatingNewProperty by remember { mutableStateOf(properties.isEmpty()) }
    var newPropertyName by remember { mutableStateOf("") }
    var newPropertyAddress by remember { mutableStateOf("") }

    // --- The Custom Dialog UI ---
    Dialog(onDismissRequest = onDismiss) {
        // 1. The Card Container (White with rounded corners)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp) // Outer margin
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()) // Make it scrollable for smaller screens
            ) {
                // 2. The Header Icon (Circular Badge like the image)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isCreatingNewProperty) Icons.Default.Apartment else Icons.Default.AddHome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Title Text
                Text(
                    text = if (isCreatingNewProperty) "New Property & Room" else "Add New Room",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Fill in the details below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Form Content
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Logic: Create Property vs Select Property
                    if (isCreatingNewProperty) {
                        OutlinedTextField(
                            value = newPropertyName,
                            onValueChange = { newPropertyName = it },
                            label = { Text("Property Name") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPropertyAddress,
                            onValueChange = { newPropertyAddress = it },
                            label = { Text("Address") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (properties.isNotEmpty()) {
                            TextButton(
                                onClick = { isCreatingNewProperty = false },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Select existing property")
                            }
                        }
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedProperty?.name ?: "Select Property",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Assign to Property") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
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
                                DropdownMenuItem(
                                    text = { Text(text = "+ Create New Property", color = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        isCreatingNewProperty = true
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    // Room Details Fields
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = roomNumber,
                            onValueChange = { roomNumber = it },
                            label = { Text("Room No.") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = floor,
                            onValueChange = { floor = it },
                            label = { Text("Floor") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = rent,
                        onValueChange = { rent = it },
                        label = { Text("Monthly Rent (KES)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isAvailable,
                            onCheckedChange = { isAvailable = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Available immediately",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Action Buttons (Stacked or Row, styled cleanly)
                Button(
                    onClick = {
                        val newRoom = Room(
                            id = UUID.randomUUID().toString(),
                            property_id = selectedProperty?.id ?: "",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = (isCreatingNewProperty && newPropertyName.isNotBlank() && newPropertyAddress.isNotBlank() && roomNumber.isNotBlank() && rent.isNotBlank()) ||
                            (!isCreatingNewProperty && selectedProperty != null && roomNumber.isNotBlank() && rent.isNotBlank())
                ) {
                    Text("Add Room", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
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
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = roomNumber.isNotBlank() && rent.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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

    val statusColor = if (isOccupied) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else LightGreen

    val elevation = if (isOccupied) 2.dp else 8.dp

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Room ${room.room_number}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOccupied) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    val type = if (room.floor != null) "Floor ${room.floor}" else "Standard"
                    Text(
                        text = type,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                StatusBadge(isOccupied = isOccupied, color = statusColor)
            }

            if (isOccupied && (isAdmin || isLandlord) && tenantName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Tenant",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tenantName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KES ${room.monthly_rent.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOccupied) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MediumAquamarine
                )

                if (isAdmin || isLandlord) {
                    Row {
                        IconButton(onClick = { onEdit(room) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDelete(room) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else if (isTenant && !isOccupied) {
                    Button(
                        onClick = { onBook(room) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSlateBlue)
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
    val backgroundColor = color.copy(alpha = 0.1f)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
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
