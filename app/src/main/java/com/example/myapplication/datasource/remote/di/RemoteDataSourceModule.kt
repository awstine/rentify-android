package com.example.myapplication.datasource.remote.di

import com.example.myapplication.datasource.remote.AuthRemoteDataSource
import com.example.myapplication.datasource.remote.AuthRepositoryImpl
import com.example.myapplication.di.SupabaseClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteDataSourceModule {

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(supabaseApi: SupabaseClient): AuthRemoteDataSource =
        AuthRepositoryImpl(apiClient = supabaseApi)
}