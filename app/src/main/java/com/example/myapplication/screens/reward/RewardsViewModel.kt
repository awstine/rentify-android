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
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.datasource.preferences.RentifyPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
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
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val preferences: RentifyPreferences
) : ViewModel() {

    var uiState by mutableStateOf(RewardsUiState())
        private set

    init {
        loadRewards()
    }

    fun loadRewards() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            // Try fresh profile, fallback to metadata or cached ID
            var profile = userRepository.getUserProfile().getOrNull()
            if (profile == null) {
                profile = userRepository.getUserFromMetadata()
            }

            val userId = profile?.id ?: preferences.getUserId().firstOrNull()

            if (userId == null) {
                uiState = uiState.copy(isLoading = false, error = "User not found")
                return@launch
            }

            val rewardsList = listOf(
                Reward("5% Rent Discount", "Pay rent on time for 6 consecutive months", 6, Icons.Default.Star),
                Reward("Referral", "Refer a friend who moves in", 1, Icons.Default.RoomPreferences),
                Reward("Car Wash", "Report a maintenance issue", 1, Icons.Default.LocalCarWash),
                Reward("Gift Card", "Renew your lease early", 3, Icons.Default.CardGiftcard)
            )

            try {
                val bookingsResult = bookingRepository.getBookingsForTenant(userId)
                val bookings = bookingsResult.getOrNull() ?: emptyList()

                // Simple logic: Count paid bookings as streak
                val paidBookingsCount = bookings.count { it.payment_status == "paid" }

                uiState = uiState.copy(
                    isLoading = false,
                    streak = paidBookingsCount,
                    rewards = rewardsList
                )
            } catch (e: Exception) {
                // If offline, still show the rewards list with 0 streak or show as "Offline"
                uiState = uiState.copy(
                    isLoading = false,
                    streak = 0,
                    rewards = rewardsList,
                    error = "Offline: Showing default rewards"
                )
            }
        }
    }
}
