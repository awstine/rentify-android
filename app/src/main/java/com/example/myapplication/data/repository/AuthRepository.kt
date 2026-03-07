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
    suspend fun getUserProfile(): Result<User>
    suspend fun uploadProfilePhoto(uri: Uri): Result<String>
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


    override suspend fun uploadProfilePhoto(uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure session exists
                SupabaseClient.client.auth.loadFromStorage()
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                    ?: throw Exception("No active session")

                val userId = session.user?.id ?: throw Exception("User not found in session")

                // Read image bytes
                val bytes = SupabaseClient.appContext
                    .contentResolver
                    .openInputStream(uri)
                    ?.readBytes()
                    ?: throw Exception("Failed to read image")

                // Path in bucket
                val path = "$userId/avatar.jpg"

                // Upload (overwrite allowed)
                SupabaseClient.client.storage
                    .from("avatars")
                    .upload(
                        path = path,
                        data = bytes,
                        upsert = true
                    )

                // Get public URL
                val publicUrl = SupabaseClient.client.storage
                    .from("avatars")
                    .publicUrl(path)

                // Save URL to profiles table
                SupabaseClient.client.postgrest["profiles"]
                    .update({
                        set("profileImageUrl", publicUrl)
                    }) {
                        filter { eq("id", userId) }
                    }

                Result.success(publicUrl)

            } catch (e: Exception) {
                Log.e("AuthRepository", "Profile photo upload failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getUserProfile(): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure the session is loaded from storage before trying to access it.
                SupabaseClient.client.auth.loadFromStorage()

                val session = SupabaseClient.client.auth.currentSessionOrNull()
                    ?: return@withContext Result.failure(Exception("No active session"))

                val authUser = session.user
                    ?: return@withContext Result.failure(Exception("Session user is null"))

                // 1. Try fetching profile from DB
                try {
                    val profile = SupabaseClient.client.postgrest["profiles"]
                        .select { filter { eq("id", authUser.id) } }
                        .decodeSingle<User>()
                    return@withContext Result.success(profile)
                } catch (e: Exception) {
                    Log.w("AuthRepo", "Profile fetch failed.", e)
                }

                // 2. Construct Fallback User from Metadata
                val meta = authUser.userMetadata
                val role = meta?.get("role")?.toString()?.trim('"') ?: "tenant"
                val fullName = meta?.get("full_name")?.toString()?.trim('"')
                val phone = meta?.get("phone_number")?.toString()?.trim('"')

                val fallbackUser = User(
                    id = authUser.id,
                    email = authUser.email,
                    phone_number = phone,
                    id_number = null,
                    role = role,
                    full_name = fullName,
                    created_at = authUser.createdAt?.toString()
                )

                // 3. Attempt Self-Repair: Insert the missing profile row.
                // This is crucial for RLS policies on other tables (like 'rooms') to work.
                try {
                    Log.d("AuthRepo", "Attempting to upsert missing profile for user ${authUser.id}")
                    SupabaseClient.client.postgrest["profiles"].upsert(fallbackUser)
                    Log.d("AuthRepo", "Self-repair successful! Profile created.")
                } catch (e: Exception) {
                    Log.e("AuthRepo", "Self-repair failed. RLS on 'profiles' might block this, or other DB error.", e)
                }

                // Return the user data, which is now backed by a DB row.
                Result.success(fallbackUser)

            } catch (e: Exception) {
                Log.e("AuthRepository", "getUserProfile fatal error", e)
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