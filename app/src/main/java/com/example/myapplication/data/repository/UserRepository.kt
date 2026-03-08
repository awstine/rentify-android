package com.example.myapplication.data.repository

import android.net.Uri
import com.example.myapplication.data.models.User
import com.example.myapplication.data.models.toModel
import com.example.myapplication.datasource.remote.UserDto
import com.example.myapplication.datasource.remote.user.UserRemoteDataSource
import javax.inject.Inject

interface UserRepository {
    suspend fun uploadProfilePhoto(uri: Uri): Result<String>

    suspend fun getUserProfile(): Result<User>

    suspend fun getUserFromMetadata(): User?
}

class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource
): UserRepository {
    override suspend fun uploadProfilePhoto(uri: Uri): Result<String> =
        userRemoteDataSource.uploadProfilePhoto(uri = uri)

    override suspend fun getUserProfile(): Result<User> =
        userRemoteDataSource.getUserProfile().map { it.toModel() }

    override suspend fun getUserFromMetadata(): User? =
        userRemoteDataSource.getUserFromMetadata()?.toModel()
}