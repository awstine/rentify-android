package com.example.myapplication.di

import com.example.myapplication.BuildConfig.SUPABASE_KEY
import com.example.myapplication.BuildConfig.SUPABASE_URL
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.data.repository.MachineRepository
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.PropertyRepository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository {
        return AuthRepository()
    }

    @Provides
    @Singleton
    fun providePropertyRepository(): PropertyRepository {
        return PropertyRepository()
    }

    @Provides
    @Singleton
    fun provideBookingRepository(): BookingRepository {
        return BookingRepository()
    }

    @Provides
    @Singleton
    fun provideMachineRepository(): MachineRepository {
        return MachineRepository()
    }

    @Provides
    @Singleton
    fun providePaymentRepository(): PaymentRepository {
        return PaymentRepository()
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth)
            install(Functions)
            //install(Storage)
        }
    }
}
