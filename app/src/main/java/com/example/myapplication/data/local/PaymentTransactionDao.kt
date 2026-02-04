package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.models.PaymentTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<PaymentTransaction>)

    @Query("SELECT * FROM payment_transactions WHERE bookingId IN (:bookingIds)")
    fun getTransactionsForBookings(bookingIds: List<String>): Flow<List<PaymentTransaction>>
}
