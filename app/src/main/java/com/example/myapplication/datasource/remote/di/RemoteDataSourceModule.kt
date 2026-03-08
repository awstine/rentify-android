package com.example.myapplication.datasource.remote.di

import com.example.myapplication.datasource.remote.AuthRemoteDataSource
import com.example.myapplication.datasource.remote.AuthRemoteDataSourceImpl
import com.example.myapplication.datasource.remote.user.UserRemoteDataSource
import com.example.myapplication.datasource.remote.user.UserRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        authRemoteDataSourceImpl: AuthRemoteDataSourceImpl
    ): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(
        userRemoteDataSourceImpl: UserRemoteDataSourceImpl
    ): UserRemoteDataSource
}