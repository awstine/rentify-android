package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myapplication.data.models.Booking
import com.example.myapplication.data.models.PaymentTransaction
import com.example.myapplication.data.models.Property
import com.example.myapplication.data.models.Reward
import com.example.myapplication.data.models.Room
import com.example.myapplication.data.models.RoomItem
import com.example.myapplication.data.models.User

@Database(entities = [User::class, Reward::class, RoomItem::class, Property::class, Room::class, Booking::class, PaymentTransaction::class], version = 1)
@TypeConverters(ImageVectorConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun rewardDao(): RewardDao
    abstract fun roomDao(): RoomDao
    abstract fun propertyDao(): PropertyDao
    abstract fun bookingDao(): BookingDao
    abstract fun paymentTransactionDao(): PaymentTransactionDao
}
