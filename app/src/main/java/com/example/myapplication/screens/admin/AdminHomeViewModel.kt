package com.example.myapplication.screens.admin

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AdminDashboardEntity
import com.example.myapplication.data.models.RecentPayment
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.PropertyRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.datasource.local.AdminDashboardDao
import com.example.myapplication.datasource.preferences.RentifyPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardState(
    val isLoading: Boolean = false,
    val totalRooms: Int = 0,
    val availableRooms: Int = 0,
    val activeTenants: Int = 0,
    val pendingRequests: Int = 0,
    val estimatedRevenue: Double = 0.0,
    val recentPayments: List<RecentPayment> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class AdminHomeViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val propertyRepository: PropertyRepository,
        private val bookingRepository: BookingRepository,
        private val paymentRepository: PaymentRepository,
        private val adminDashboardDao: AdminDashboardDao,
        private val userRepository: UserRepository,
        private val preferences: RentifyPreferences,
    ) : ViewModel() {
        var uiState by mutableStateOf(AdminDashboardState())
            private set

        init {
            loadDashboardData()
        }

        fun loadDashboardData() {
            viewModelScope.launch {
                uiState = uiState.copy(isLoading = true, error = null)

                // 1. Get User ID - try network/metadata first, then cached
                val profile =
                    userRepository.getUserProfile().getOrNull()
                        ?: userRepository.getUserFromMetadata()
                        ?: preferences.getUserId().let { id ->
                            // Create a shell user if we only have the ID
                            var userId = ""
                            id.map {
                                userId = it
                            }
                            com.example.myapplication.data.models.User(
                                id = userId,
                                email = "",
                                phone_number = null,
                                id_number = null,
                                role = "admin",
                                full_name = null,
                                created_at = null,
                            )
                        }

                val userId = profile.id
                Log.d("AdminHomeViewModel", "Loading data for user: $userId")

                // 2. Try to load cached data immediately for better offline UX
                val cached = adminDashboardDao.getDashboard(userId)
                if (cached != null) {
                    uiState =
                        uiState.copy(
                            totalRooms = cached.totalRooms,
                            availableRooms = cached.availableRooms,
                            activeTenants = cached.activeTenants,
                            pendingRequests = cached.pendingRequests,
                            estimatedRevenue = cached.estimatedRevenue,
                        )
                    // If we have cached data, we can stop showing the spinner if we're offline
                }

                try {
                    // 3. Fetch Fresh Data from Network
                    // Fetch Rooms
                    val roomsResult = propertyRepository.getRoomsForLandlord(userId)
                    val rooms = roomsResult.getOrThrow()
                    val totalRooms = rooms.size
                    val availableRooms = rooms.count { it.is_available }

                    // Fetch Bookings
                    val bookingsResult = bookingRepository.getBookingsForLandlord(userId)
                    val bookings = bookingsResult.getOrThrow()

                    val activeTenants = bookings.count { it.status == "active" }
                    val pendingRequests = bookings.count { it.status == "pending" }

                    val estimatedRevenue =
                        bookings
                            .filter { it.status == "active" && it.payment_status == "paid" }
                            .sumOf { it.monthly_rent }

                    // Fetch Recent Payments
                    val recentPaymentsResult = paymentRepository.getRecentPayments(userId)
                    val recentPayments = recentPaymentsResult.getOrThrow()

                    // 4. Update UI and Cache
                    uiState =
                        uiState.copy(
                            isLoading = false,
                            totalRooms = totalRooms,
                            availableRooms = availableRooms,
                            activeTenants = activeTenants,
                            pendingRequests = pendingRequests,
                            estimatedRevenue = estimatedRevenue,
                            recentPayments = recentPayments,
                            error = null,
                        )

                    adminDashboardDao.saveDashboard(
                        AdminDashboardEntity(
                            userId = userId,
                            totalRooms = totalRooms,
                            availableRooms = availableRooms,
                            activeTenants = activeTenants,
                            pendingRequests = pendingRequests,
                            estimatedRevenue = estimatedRevenue,
                        ),
                    )
                } catch (e: Exception) {
                    Log.e("AdminHomeViewModel", "Network fetch failed", e)
                    // If network fails, we keep the cached data already in state
                    if (cached != null) {
                        uiState = uiState.copy(isLoading = false, error = null)
                    } else {
                        uiState = uiState.copy(isLoading = false, error = "Offline: No cached data available")
                    }
                }
            }
        }
    }
