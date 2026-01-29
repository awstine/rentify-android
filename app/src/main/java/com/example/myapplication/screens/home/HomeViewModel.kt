package com.example.myapplication.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Property
import com.example.myapplication.data.models.Room
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.PropertyRepository
import com.example.myapplication.navigation.Property as PropertyUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val properties: List<PropertyUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val username: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        refreshData()
    }

    fun refreshData() {
        loadProperties()
        loadUserProfile()
    }

    private fun loadUserProfile() {
         viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                val emailName = user?.email?.substringBefore("@")
                
                val formattedName = if (!emailName.isNullOrBlank()) {
                    emailName.replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() 
                    }
                } else {
                    "Tenant"
                }
                
                uiState = uiState.copy(username = formattedName)
            } catch (e: Exception) {
                // Ignore error, just keep default or current
            }
         }
    }

    fun loadProperties() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            
            // 1. Get all available rooms to know which properties to show and their prices
            val roomsResult = propertyRepository.getAllAvailableRooms()
            if (roomsResult.isFailure) {
                uiState = uiState.copy(isLoading = false, error = roomsResult.exceptionOrNull()?.message)
                return@launch
            }
            val rooms = roomsResult.getOrDefault(emptyList())
            
            if (rooms.isEmpty()) {
                uiState = uiState.copy(isLoading = false, properties = emptyList())
                return@launch
            }

            // 2. Extract unique property IDs
            val propertyIds = rooms.mapNotNull { it.property_id }.distinct()
            
            // 3. Fetch properties details
            val propertiesResult = propertyRepository.getPropertiesByIds(propertyIds)
            if (propertiesResult.isFailure) {
                uiState = uiState.copy(isLoading = false, error = propertiesResult.exceptionOrNull()?.message)
                return@launch
            }
            val properties = propertiesResult.getOrDefault(emptyList())

            // 4. Map to UI Model
            val uiProperties = properties.map { property ->
                val propertyRooms = rooms.filter { it.property_id == property.id }
                val minPrice = propertyRooms.minOfOrNull { it.monthly_rent } ?: 0.0
                
                PropertyUiModel(
                    id = property.id.hashCode(), // Int ID expected by UI model, though ideally should be changed to String or match DB
                    imageUrl = "", // Placeholder
                    dateStatus = "Available",
                    price = "KES ${minPrice.toInt()}",
                    address = property.address,
                    baths = 1, // Default as not in DB
                    beds = property.total_rooms, // Using total_rooms as proxy or default
                    sqft = 0 // Default
                )
            }

            uiState = uiState.copy(isLoading = false, properties = uiProperties)
        }
    }
}
