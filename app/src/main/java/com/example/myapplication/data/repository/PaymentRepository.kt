package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.BookingDao
import com.example.myapplication.data.local.PaymentTransactionDao
import com.example.myapplication.data.models.Booking
import com.example.myapplication.data.models.PaymentTransaction
import com.example.myapplication.di.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
<<<<<<< HEAD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
=======
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
>>>>>>> 1a72838 (updated the payment screen)

class PaymentRepository @Inject constructor(
    private val bookingDao: BookingDao,
    private val paymentTransactionDao: PaymentTransactionDao
) {

<<<<<<< HEAD
    private fun <T> networkBoundResource(
        query: () -> Flow<T>,
        fetch: suspend () -> T,
        saveFetchResult: suspend (T) -> Unit
    ): Flow<Result<T>> = flow {
        val data = query().first()
        if (data != null) {
            emit(Result.success(data))
=======
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
    val created_at: String? = null // Necessary for sorting and display
    // val amount: Double? = null // Optional: You can add this column to your DB if you want to show amount here
)

// --- REPOSITORY CLASS ---

class PaymentRepository {

    private val json = Json { ignoreUnknownKeys = true } // For safe parsing
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

                val response: HttpResponse = SupabaseClient.client.functions.invoke(
                    function = "mpesa-stk-push",
                    body = request
                )

                // --- FIX: Read body only ONCE ---
                val responseString = response.body<String>()
                Log.d(TAG, "Raw Response: $responseString")

                // Now parse the stored string
                val edgeResponse = json.decodeFromString<EdgeFunctionResponse>(responseString)

                if (edgeResponse.success) {
                    Result.success("Request sent! Check your phone.")
                } else {
                    Result.failure(Exception(edgeResponse.error ?: "Unknown error from server${response.status}"))
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
>>>>>>> 1a72838 (updated the payment screen)
        }

        val fetchResult = try {
            Result.success(fetch())
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }

        if (fetchResult.isSuccess) {
            saveFetchResult(fetchResult.getOrThrow())
            emit(Result.success(query().first()))
        } else {
            if (data != null) {
                emit(Result.success(data))
            } else {
                emit(Result.failure(fetchResult.exceptionOrNull()!!))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getTenantPaymentHistory(tenantId: String): Flow<Result<List<PaymentTransaction>>> {
        return networkBoundResource(
            query = {
                val bookings = bookingDao.getBookingsForTenant(tenantId).first()
                val bookingIds = bookings.map { it.id }
                paymentTransactionDao.getTransactionsForBookings(bookingIds)
            },
            fetch = {
                val bookings = SupabaseClient.client.from("bookings").select { filter { eq("tenant_id", tenantId) } }.decodeList<Booking>()
                val bookingIds = bookings.map { it.id }
                SupabaseClient.client.from("payment_transactions").select { filter { isIn("booking_id", bookingIds) } }.decodeList()
            },
            saveFetchResult = { transactions ->
                paymentTransactionDao.insertTransactions(transactions)
            }
        )
    }
}
