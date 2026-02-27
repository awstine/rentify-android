package com.example.myapplication.screens.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AdminDashboardDao
import com.example.myapplication.data.local.AdminDashboardEntity
import com.example.myapplication.data.models.RecentPayment
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val error: String? = null
)

@HiltViewModel
class AdminHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val propertyRepository: PropertyRepository,
    private val bookingRepository: BookingRepository,
    private val paymentRepository: PaymentRepository,
    private val adminDashboardDao: AdminDashboardDao
) : ViewModel() {

    var uiState by mutableStateOf(AdminDashboardState())
        private set

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            val profileResult = authRepository.getUserProfile()
            val profile = profileResult.getOrNull()

            if (profile == null) {
                uiState = uiState.copy(isLoading = false, error = "User not found")
                return@launch
            }

            try {
                // 1. Fetch Rooms
                val roomsResult = propertyRepository.getRoomsForLandlord(profile.id)
                val rooms = roomsResult.getOrThrow()
                val totalRooms = rooms.size
                val availableRooms = rooms.count { it.is_available }

                // 2. Fetch Bookings
                val bookingsResult = bookingRepository.getBookingsForLandlord(profile.id)
                val bookings = bookingsResult.getOrThrow()

                val activeTenants = bookings.count { it.status == "active" }
                val pendingRequests = bookings.count { it.status == "pending" }

                val estimatedRevenue = bookings
                    .filter { it.status == "active" && it.payment_status == "paid" }
                    .sumOf { it.monthly_rent }

                // 3. Fetch Recent Payments
                val recentPaymentsResult = paymentRepository.getRecentPayments(profile.id)
                val recentPayments = recentPaymentsResult.getOrThrow()

                uiState = uiState.copy(
                    isLoading = false,
                    totalRooms = totalRooms,
                    availableRooms = availableRooms,
                    activeTenants = activeTenants,
                    pendingRequests = pendingRequests,
                    estimatedRevenue = estimatedRevenue,
                    recentPayments = recentPayments
                )

                adminDashboardDao.saveDashboard(
                    AdminDashboardEntity(
                        userId = profile.id,
                        totalRooms = totalRooms,
                        availableRooms = availableRooms,
                        activeTenants = activeTenants,
                        pendingRequests = pendingRequests,
                        estimatedRevenue = estimatedRevenue
                    )
                )

            } catch (e: Exception) {
                val cached = adminDashboardDao.getDashboard(profile.id)

                if (cached != null) {
                    uiState = uiState.copy(
                        isLoading = false,
                        totalRooms = cached.totalRooms,
                        availableRooms = cached.availableRooms,
                        activeTenants = cached.activeTenants,
                        pendingRequests = cached.pendingRequests,
                        estimatedRevenue = cached.estimatedRevenue
                    )
                } else {
                    uiState = uiState.copy(isLoading = false, error = e.message)
                }
            }
        }
    }
}
