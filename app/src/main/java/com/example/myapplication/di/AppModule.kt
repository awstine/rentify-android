package com.example.myapplication.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.myapplication.datasource.local.RoomDao
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.AuthRepositoryImpl
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.PropertyRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.repository.UserRepositoryImpl
import com.example.myapplication.datasource.remote.AuthRemoteDataSource
import com.example.myapplication.datasource.remote.user.UserRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePaymentRepository(): PaymentRepository {
        return PaymentRepository()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        remoteDataSource: AuthRemoteDataSource
    ): AuthRepository {
        return AuthRepositoryImpl(remoteDataSource)
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

    @Provides
    @Singleton
    fun provideUserRepository(
        userRemoteDataSource: UserRemoteDataSource
    ): UserRepository {
        return UserRepositoryImpl(userRemoteDataSource)
    }
}
