package com.example.myapplication.data.repository

import android.net.Uri
import android.util.Log
import com.example.myapplication.data.models.SignInRequest
import com.example.myapplication.data.models.SignUpRequest
import com.example.myapplication.data.models.User
import com.example.myapplication.data.models.toDto
import com.example.myapplication.data.models.toUser
import com.example.myapplication.di.SupabaseClient
import com.example.myapplication.datasource.remote.AuthRemoteDataSource
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AuthRepository{
    suspend fun signUp(request: SignUpRequest): Result<User>
    suspend fun signIn(request: SignInRequest): Result<Boolean>
    suspend fun signOut(): Result<Boolean>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun hasValidSession(): Boolean
}

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
) : AuthRepository {

    override suspend fun signUp(request: SignUpRequest): Result<User> {
        val response = remoteDataSource.signUp(request.toDto())
        return response.fold(
            onSuccess = { userResponse ->
                Result.success(userResponse.toUser())
            },

            onFailure = {
                Result.failure(it)
            }
        )

    }

    override suspend fun signIn(request: SignInRequest): Result<Boolean> {
        val response = remoteDataSource.signIn(request.toDto())
        return response.fold(
            onSuccess = {Result.success(true)},
            onFailure = {Result.success(false)}
        )
    }

    override suspend fun signOut(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.signOut()
                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting to reset password for email: $email")
                SupabaseClient.client.auth.resetPasswordForEmail(email)
                Log.d("AuthRepository", "Password reset email sent successfully.")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to send password reset email", e)
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    override suspend fun hasValidSession(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.loadFromStorage()
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                session != null
            } catch (e: Exception) {
                Log.e("AuthRepository", "Session check failed", e)
                false
            }
        }
    }
}