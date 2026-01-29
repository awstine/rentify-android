package com.example.myapplication.screens.reward

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.RoomPreferences
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Reward
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RewardsUiState(
    val streak: Int = 0,
    val target: Int = 6,
    val isLoading: Boolean = false,
    val rewards: List<Reward> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    var uiState by mutableStateOf(RewardsUiState())
        private set

    init {
        loadRewards()
    }

    fun loadRewards() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            val user = authRepository.getCurrentUser()
            if (user != null) {
                val bookingsResult = bookingRepository.getBookingsForTenant(user.id)
                val bookings = bookingsResult.getOrNull() ?: emptyList()
                
                // Simple logic: Count paid bookings as streak
                // In a real app, you would check if they are consecutive and on time.
                val paidBookingsCount = bookings.count { it.payment_status == "paid" }
                
                val rewardsList = listOf(
                    Reward("5% Rent Discount", "Pay rent on time for 6 consecutive months", 6, Icons.Default.Star),
                    Reward("Referral", "Refer a friend who moves in", 1, Icons.Default.RoomPreferences),
                    Reward("Car Wash", "Report a maintenance issue", 1, Icons.Default.LocalCarWash),
                    Reward("Gift Card", "Renew your lease early", 3, Icons.Default.CardGiftcard)
                )
                
                uiState = uiState.copy(
                    isLoading = false, 
                    streak = paidBookingsCount,
                    rewards = rewardsList
                )
            } else {
                uiState = uiState.copy(isLoading = false, error = "User not found")
            }
        }
    }
}
