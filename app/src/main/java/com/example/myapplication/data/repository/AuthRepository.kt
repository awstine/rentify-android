package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.UserDao
import com.example.myapplication.data.models.User
import com.example.myapplication.di.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val userDao: UserDao
) {

    private fun <T> networkBoundResource(
        query: () -> Flow<T>,
        fetch: suspend () -> T,
        saveFetchResult: suspend (T) -> Unit
    ): Flow<Result<T>> = flow {
        val data = query().first()
        if (data != null) {
            emit(Result.success(data))
        }

        val fetchResult = try {
            Result.success(fetch())
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }

        if (fetchResult.isSuccess) {
            saveFetchResult(fetchResult.getOrThrow())
            emit(Result.success(query().first()))
        } else {
            if (data != null) {
                emit(Result.success(data))
            } else {
                emit(Result.failure(fetchResult.exceptionOrNull()!!))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun signUp(email: String, password: String, userData: User): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Starting signup for email: $email")

                val authResponse = SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password

                    this.data = buildJsonObject {
                        put("phone_number", userData.phone_number ?: "")
                        put("full_name", userData.full_name ?: "")
                        put("role", userData.role)
                    }
                }

                val user = authResponse ?: throw Exception("Sign up successful but no user returned")
                val userId = user.id

                Log.d("AuthRepository", "Auth signup successful, userId: $userId")

                val createdUser = userData.copy(id = userId)
                userDao.insertUser(createdUser)
                Result.success(createdUser)

            } catch (e: Exception) {
                Log.e("AuthRepository", "Signup failed", e)
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun signOut(): Result<Boolean> {
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

    suspend fun getCurrentUser(): User? {
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        return if (session != null) {
            getUserProfile(session.user!!.id).first().getOrNull()
        } else {
            null
        }
    }

    fun getUserProfile(userId: String): Flow<Result<User?>> {
        return networkBoundResource(
            query = { userDao.getUser(userId) },
            fetch = { SupabaseClient.client.postgrest["profiles"].select { filter { eq("id", userId) } }.decodeSingle() },
            saveFetchResult = { user -> if (user != null) userDao.insertUser(user) }
        )
    }

    suspend fun resetPassword(email: String): Result<Unit> {
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

    suspend fun hasValidSession(): Boolean {
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
