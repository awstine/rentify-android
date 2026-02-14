package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.models.Booking
import com.example.myapplication.di.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

// --- DATA MODELS ---

@Serializable
data class MpesaPaymentRequest(
    @SerialName("booking_id") val bookingId: String,
    @SerialName("amount") val amount: Double,
    @SerialName("phone_number") val phoneNumber: String
)

@Serializable
data class EdgeFunctionResponse(
    val success: Boolean = false,
    val data: MpesaData? = null,
    val error: String? = null
)

@Serializable
data class MpesaData(
    @SerialName("CheckoutRequestID") val checkoutRequestID: String? = null,
    @SerialName("ResponseDescription") val responseDescription: String? = null
)

@Serializable
data class PaymentTransaction(
    val id: String? = null,
    val status: String? = null,
    @SerialName("booking_id") val bookingId: String? = null,
    val mpesa_receipt_number: String? = null,
    val created_at: String? = null, // Necessary for sorting and display
    val amount: Double? = null // Optional: You can add this column to your DB if you want to show amount here
)

// --- REPOSITORY CLASS ---

class PaymentRepository {

    private val TAG = "PaymentRepo"

    // 1. Send STK Push
    suspend fun initiateMpesaPayment(
        bookingId: String,
        amount: Double,
        phoneNumber: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = MpesaPaymentRequest(
                    bookingId = bookingId,
                    amount = amount,
                    phoneNumber = phoneNumber
                )

                Log.d(TAG, "Sending STK Push to Edge Function...")

                // Make sure the function name matches your Supabase Dashboard EXACTLY
                val response: HttpResponse = SupabaseClient.client.functions.invoke(
                    function = "mpesa-stk-push",
                    body = request
                )

                val responseString = response.body<String>()
                Log.d(TAG, "Raw Response: $responseString")

                // We re-parse to object to be safe
                val edgeResponse = response.body<EdgeFunctionResponse>()

                if (edgeResponse.success) {
                    Result.success("Request sent! Check your phone.")
                } else {
                    Result.failure(Exception(edgeResponse.error ?: "Unknown error from server"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "STK Push Error", e)
                Result.failure(e)
            }
        }
    }

    // 2. Realtime Listener (Original String Version)
    suspend fun observePaymentStatus(bookingId: String): Flow<String?> {
        // Just delegates to the new one and maps to status string for backward compatibility
        return observePaymentTransaction(bookingId).map { it?.status }
    }

    // 2b. Realtime Listener (New Object Version)
    suspend fun observePaymentTransaction(bookingId: String): Flow<PaymentTransaction?> {
        val channelId = "payment-$bookingId"
        Log.d(TAG, "Subscribing to Realtime Channel (Obj): $channelId")

        val channel = SupabaseClient.client.channel(channelId)

        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "payment_transactions"
            filter = "booking_id=eq.$bookingId"
        }

        channel.subscribe()

        return flow
            .onCompletion {
                Log.d(TAG, "Unsubscribing from $channelId")
                channel.unsubscribe()
            }
            .map { action ->
                try {
                    // Log.d(TAG, "Realtime Event Received: $action")
                    val record = when (action) {
                        is PostgresAction.Update -> action.decodeRecord<PaymentTransaction>()
                        is PostgresAction.Insert -> action.decodeRecord<PaymentTransaction>()
                        else -> null
                    }
                    // Log.d(TAG, "New Status: ${record?.status}")
                    record
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding realtime record", e)
                    null
                }
            }
    }

    // 3. Manual Polling (Original String Version)
    suspend fun getPaymentStatus(bookingId: String): String? {
        val tx = getLatestPaymentTransaction(bookingId)
        return tx?.status
    }

    // 3b. Manual Polling (New Object Version)
    suspend fun getLatestPaymentTransaction(bookingId: String): PaymentTransaction? {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("payment_transactions")
                    .select {
                        filter {
                            eq("booking_id", bookingId)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(1)
                    }.decodeSingleOrNull<PaymentTransaction>()
            } catch (e: Exception) {
                Log.e(TAG, "Polling Error", e)
                null
            }
        }
    }

    // D. Fetch History for a Tenant
    suspend fun getTenantPaymentHistory(tenantId: String): Result<List<PaymentTransaction>> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. First, get all booking IDs that belong to this tenant
                val bookings = SupabaseClient.client.from("bookings")
                    .select {
                        filter {
                            eq("tenant_id", tenantId)
                        }
                    }.decodeList<Booking>()

                if (bookings.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val bookingIds = bookings.map { it.id }

                // 2. Fetch all transactions linked to those bookings
                // We order by 'created_at' descending (Newest first)
                val transactions = SupabaseClient.client.from("payment_transactions")
                    .select {
                        filter {
                            isIn("booking_id", bookingIds)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }.decodeList<PaymentTransaction>()

                Result.success(transactions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // E. Update Booking End Date based on months paid
    // In PaymentRepository.kt

    // In PaymentRepository.kt

    suspend fun updateBookingDuration(bookingId: String, months: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Fetch current booking
                val booking = SupabaseClient.client.from("bookings")
                    .select {
                        filter { eq("id", bookingId) }
                    }
                    .decodeSingleOrNull<Booking>()

                if (booking != null) {
                    val today = LocalDate.now()
                    val startDate = LocalDate.parse(booking.start_date)

                    // --- THE FIX ---

                    // We check if the booking is currently 'pending'.
                    // If it is 'pending', it means this is the VERY FIRST payment.
                    // Therefore, we MUST calculate the end date from the START DATE.
                    val isFirstPayment = booking.status?.equals("pending", ignoreCase = true) == true

                    val baseDate = if (isFirstPayment) {
                        // CASE 1: New Booking (First Payment)
                        // Ignore the placeholder end_date. Start counting from the Booking Start Date.
                        startDate
                    } else {
                        // CASE 2: Renewal (Active Tenant)
                        // The tenant is extending. Add time to their current expiry date.
                        val currentEndDate = LocalDate.parse(booking.end_date)

                        // If they are already expired, start fresh from Today.
                        if (currentEndDate.isBefore(today)) today else currentEndDate
                    }

                    // Calculate the new end date
                    val newEndDate = baseDate.plusMonths(months.toLong()).toString()

                    // Update the database
                    SupabaseClient.client.from("bookings").update(
                        {
                            set("end_date", newEndDate)
                            set("status", "active")         // Mark as Active
                            set("payment_status", "paid")   // Mark as Paid
                        }
                    ) {
                        filter {
                            eq("id", bookingId)
                        }
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Booking not found"))
                }
            } catch (e: Exception) {
                Log.e("PaymentRepo", "Failed to update booking dates", e)
                Result.failure(e)
            }
        }
    }
}