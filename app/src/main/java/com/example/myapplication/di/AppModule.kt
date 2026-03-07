package com.example.myapplication.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.myapplication.datasource.local.RoomDao
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.AuthRepositoryImpl
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.PropertyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("user_prefs") }
        )
    }

    @Provides
    @Singleton
    fun providePaymentRepository(): PaymentRepository {
        return PaymentRepository()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository {
        return AuthRepository()
    }

    @Provides
    @Singleton
    fun providePropertyRepository(roomDao: RoomDao): PropertyRepository {
        return PropertyRepository(roomDao)
    }

    @Provides
    @Singleton
    fun provideBookingRepository(): BookingRepository {
        return BookingRepository()
    }
}
