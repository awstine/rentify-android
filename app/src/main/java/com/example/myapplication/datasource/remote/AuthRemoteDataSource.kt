package com.example.myapplication.datasource.remote

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

interface AuthRemoteDataSource {
    suspend fun signUp(request: SignUpRequestDto): Result<UserResponseDto>
    suspend fun signIn(request: SignInRequestDto): Result<Boolean>
    suspend fun signOut(): Result<Boolean>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun hasValidSession(): Boolean
}

class AuthRemoteDataSourceImpl @Inject constructor(
    private val apiClient: SupabaseClient,
) : AuthRemoteDataSource {

    override suspend fun signUp(request: SignUpRequestDto): Result<UserResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Starting signup for email: ${request.email}")

                val authResponse = apiClient.auth.signUpWith(Email) {
                    this.email = request.email
                    this.password = request.password

                    this.data = buildJsonObject {
                        put("phone_number", request.userData.phoneNumber)
                        put("full_name", request.userData.fullName)
                        put("role", request.userData.role.name)
                    }
                }

                val user = authResponse ?: throw Exception("Sign up successful but no user returned")

                Log.d("AuthRepository", "Auth signup successful, userId: ${user.id}")

                val responseDto = UserResponseDto(
                    id = user.id,
                    email = user.email,
                    phoneNumber = request.userData.phoneNumber,
                    idNumber = null,
                    role = request.userData.role.value,
                    fullName = request.userData.fullName,
                    createdAt = user.createdAt?.toString(),
                    profileImageUrl = request.userData.profilePhotoUrl
                )
                Result.success(responseDto)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Signup failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun signIn(request: SignInRequestDto): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                apiClient.auth.signInWith(Email) {
                    this.email = request.email
                    this.password = request.password
                }
                Result.success(true)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Sign in failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun signOut(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                apiClient.auth.signOut()
                Result.success(true)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Sign out failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting to reset password for email: $email")
                apiClient.auth.resetPasswordForEmail(email)
                Log.d("AuthRepository", "Password reset email sent successfully.")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to send password reset email", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun hasValidSession(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                apiClient.auth.loadFromStorage()
                val session = apiClient.auth.currentSessionOrNull()
                session != null
            } catch (e: Exception) {
                Log.e("AuthRepository", "Session check failed", e)
                false
            }
        }
    }
}
