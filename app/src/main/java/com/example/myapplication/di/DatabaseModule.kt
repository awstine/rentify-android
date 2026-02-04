package com.example.myapplication.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.BookingDao
import com.example.myapplication.data.local.PaymentTransactionDao
import com.example.myapplication.data.local.PropertyDao
import com.example.myapplication.data.local.RewardDao
import com.example.myapplication.data.local.RoomDao
import com.example.myapplication.data.local.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app-database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    fun provideRewardDao(appDatabase: AppDatabase): RewardDao {
        return appDatabase.rewardDao()
    }

    @Provides
    fun provideRoomDao(appDatabase: AppDatabase): RoomDao {
        return appDatabase.roomDao()
    }

    @Provides
    fun providePropertyDao(appDatabase: AppDatabase): PropertyDao {
        return appDatabase.propertyDao()
    }

    @Provides
    fun provideBookingDao(appDatabase: AppDatabase): BookingDao {
        return appDatabase.bookingDao()
    }

    @Provides
    fun providePaymentTransactionDao(appDatabase: AppDatabase): PaymentTransactionDao {
        return appDatabase.paymentTransactionDao()
    }
}
