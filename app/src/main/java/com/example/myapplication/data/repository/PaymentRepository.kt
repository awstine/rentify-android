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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class PaymentRepository @Inject constructor(
    private val bookingDao: BookingDao,
    private val paymentTransactionDao: PaymentTransactionDao
) {

    private fun <T> networkBoundResource(
        query: () -> Flow<T>,
        fetch: suspend () -> T,
        saveFetchResult: suspend (T) -> Unit
    ): Flow<Result<T>> = flow {
        val data = query().first()
        if (data != null) {
            emit(Result.success(data))
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
