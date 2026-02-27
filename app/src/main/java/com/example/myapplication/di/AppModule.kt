package com.example.myapplication.di

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.local.RoomDao
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.PropertyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
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

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            //install(GoTrue)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}