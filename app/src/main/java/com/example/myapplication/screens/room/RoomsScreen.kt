package com.example.myapplication.screens.room

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = {
                    Text("Property Status", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkSlateBlue)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (state.isLandlord || state.isAdmin) {
                FloatingActionButton(
                    onClick = { showAddRoomDialog = true },
                    containerColor = DarkSlateBlue,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Room")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DarkSlateBlue)
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${state.error}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            } else if (state.rooms.isEmpty()) {
                EmptyRoomState(isManager = state.isLandlord || state.isAdmin)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Added animation indexing
                    itemsIndexed(state.rooms) { index, roomWithTenant ->
                        RoomCard(
                            room = roomWithTenant.room,
                            index = index,
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

    // Dialogs remain unchanged functionally, but will inherit app theme
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
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSlateBlue)
                    ) { Text("Confirm Booking") }
                } else {
                    Button(
                        onClick = { roomToBook = null },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSlateBlue)
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

@Composable
fun EmptyRoomState(isManager: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3E5E8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MeetingRoom,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No rooms available",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF1E232C)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isManager) "You haven't added any rooms yet.\nTap the + button to add one."
            else "Check back later for available spaces.",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RoomCard(
    room: Room,
    index: Int,
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

    // Animation Logic
    val alphaAnim = remember { Animatable(0f) }
    val translationYAnim = remember { Animatable(100f) }

    LaunchedEffect(Unit) {
        delay(index * 75L)
        launch { alphaAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { translationYAnim.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
    }

    val statusColor = if (isOccupied) Color.Gray else LightGreen
    val cardOpacity = if (isOccupied) 0.8f else 1f
    val formattedRent = NumberFormat.getCurrencyInstance(Locale("en", "KE")).format(room.monthly_rent)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = cardOpacity)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOccupied) 0.5.dp else 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = alphaAnim.value
                translationY = translationYAnim.value
            }
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
                        color = Color(0xFF1E232C)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (room.floor != null) "Floor ${room.floor}" else "Standard Room",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                StatusBadge(isOccupied = isOccupied, color = statusColor)
            }

            if (isOccupied && (isAdmin || isLandlord) && tenantName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFF5F6F8),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Tenant",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tenantName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E232C)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = Color.LightGray.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Rent",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = formattedRent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOccupied) Color.DarkGray else MediumAquamarine
                    )
                }

                if (isAdmin || isLandlord) {
                    Row {
                        IconButton(onClick = { onEdit(room) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                        }
                        IconButton(onClick = { onDelete(room) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                } else if (isTenant && !isOccupied) {
                    Button(
                        onClick = { onBook(room) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSlateBlue),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text("Book Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(isOccupied: Boolean, color: Color) {
    val text = if (isOccupied) "Occupied" else "Vacant"
    val backgroundColor = color.copy(alpha = 0.15f)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Text(
            text = text.uppercase(),
            color = if (isOccupied) Color.DarkGray else color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// NOTE: AddRoomDialog and EditRoomDialog logic remain the same,
// just omitted here to keep the response focused on the main layout changes.
// You can keep your existing implementations for those!

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = DarkSlateBlue.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isCreatingNewProperty) Icons.Default.Apartment else Icons.Default.AddHome,
                        contentDescription = null,
                        tint = DarkSlateBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isCreatingNewProperty) "New Property & Room" else "Add New Room",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E232C),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Fill in the details below",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Form Content
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isCreatingNewProperty) {
                        CustomOutlinedTextField(
                            value = newPropertyName,
                            onValueChange = { newPropertyName = it },
                            label = "Property Name"
                        )
                        CustomOutlinedTextField(
                            value = newPropertyAddress,
                            onValueChange = { newPropertyAddress = it },
                            label = "Address"
                        )
                        if (properties.isNotEmpty()) {
                            TextButton(
                                onClick = { isCreatingNewProperty = false },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Select existing property", color = DarkSlateBlue)
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DarkSlateBlue,
                                    focusedLabelColor = DarkSlateBlue,
                                    unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }                            ) {
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
                                    text = { Text(text = "+ Create New Property", color = DarkSlateBlue, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        isCreatingNewProperty = true
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CustomOutlinedTextField(
                            value = roomNumber,
                            onValueChange = { roomNumber = it },
                            label = "Room No.",
                            modifier = Modifier.weight(1f)
                        )
                        CustomOutlinedTextField(
                            value = floor,
                            onValueChange = { floor = it },
                            label = "Floor",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    CustomOutlinedTextField(
                        value = rent,
                        onValueChange = { rent = it },
                        label = "Monthly Rent (KES)",
                        keyboardType = KeyboardType.Number
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isAvailable,
                            onCheckedChange = { isAvailable = it },
                            colors = CheckboxDefaults.colors(checkedColor = DarkSlateBlue)
                        )
                        Text(
                            text = "Available immediately",
                            fontSize = 14.sp,
                            color = Color(0xFF1E232C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSlateBlue),
                    enabled = (isCreatingNewProperty && newPropertyName.isNotBlank() && newPropertyAddress.isNotBlank() && roomNumber.isNotBlank() && rent.isNotBlank()) ||
                            (!isCreatingNewProperty && selectedProperty != null && roomNumber.isNotBlank() && rent.isNotBlank())
                ) {
                    Text("Add Room", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color.Gray)
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                // Header Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = DarkSlateBlue.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = DarkSlateBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Edit Room",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E232C)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomOutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = "Room Number"
                    )
                    CustomOutlinedTextField(
                        value = floor,
                        onValueChange = { floor = it },
                        label = "Floor (Optional)",
                        keyboardType = KeyboardType.Number
                    )
                    CustomOutlinedTextField(
                        value = rent,
                        onValueChange = { rent = it },
                        label = "Monthly Rent",
                        keyboardType = KeyboardType.Number
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isAvailable,
                            onCheckedChange = { isAvailable = it },
                            colors = CheckboxDefaults.colors(checkedColor = DarkSlateBlue)
                        )
                        Text("Available immediately", fontSize = 14.sp, color = Color(0xFF1E232C))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }

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
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSlateBlue),
                        enabled = roomNumber.isNotBlank() && rent.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// Helper Composable to keep your code DRY and your styling consistent across dialogs
@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DarkSlateBlue,
            focusedLabelColor = DarkSlateBlue,
            unfocusedBorderColor = Color.LightGray,
            cursorColor = DarkSlateBlue
        ),
        modifier = modifier.fillMaxWidth()
    )
}