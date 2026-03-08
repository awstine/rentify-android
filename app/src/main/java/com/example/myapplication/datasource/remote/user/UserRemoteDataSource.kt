package com.example.myapplication.datasource.remote.user

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.myapplication.datasource.remote.Role
import com.example.myapplication.datasource.remote.UserDto
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface UserRemoteDataSource {
    suspend fun uploadProfilePhoto(uri: Uri): Result<String>

    suspend fun getUserProfile(): Result<UserDto>

    suspend fun getUserFromMetadata(): UserDto?
}

class UserRemoteDataSourceImpl
    @Inject
    constructor(
        private val apiService: SupabaseClient,
        @ApplicationContext private val context: Context,
    ) : UserRemoteDataSource {
        override suspend fun uploadProfilePhoto(uri: Uri): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    // Ensure session exists
                    apiService.auth.loadFromStorage()
                    val session =
                        apiService.auth.currentSessionOrNull()
                            ?: throw Exception("No active session")

                    val userId = session.user?.id ?: throw Exception("User not found in session")

                    // Read image bytes
                    val bytes =
                        context
                            .contentResolver
                            .openInputStream(uri)
                            ?.readBytes()
                            ?: throw Exception("Failed to read image")

                    // Path in bucket
                    val path = "$userId/avatar.jpg"

                    // Upload (overwrite allowed)
                    apiService.storage
                        .from("avatars")
                        .upload(
                            path = path,
                            data = bytes,
                            upsert = true,
                        )

                    // Get public URL
                    val publicUrl =
                        apiService.storage
                            .from("avatars")
                            .publicUrl(path)

                    // Save URL to profiles table
                    apiService.postgrest["profiles"]
                        .update({
                            set("profile_image_url", publicUrl)
                        }) {
                            filter { eq("id", userId) }
                        }

                    Result.success(publicUrl)
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Profile photo upload failed", e)
                    Result.failure(e)
                }
            }

        override suspend fun getUserProfile(): Result<UserDto> {
            return withContext(Dispatchers.IO) {
                try {
                    // Ensure the session is loaded from storage before trying to access it.
                    apiService.auth.loadFromStorage()

                    val session =
                        apiService.auth.currentSessionOrNull()
                            ?: return@withContext Result.failure(Exception("No active session"))

                    val authUser =
                        session.user
                            ?: return@withContext Result.failure(Exception("Session user is null"))

                    // 1. Try fetching profile from DB
                    try {
                        val profile =
                            apiService.postgrest["profiles"]
                                .select { filter { eq("id", authUser.id) } }
                                .decodeSingle<UserDto>()
                        return@withContext Result.success(profile)
                    } catch (e: Exception) {
                        Log.w("AuthRepo", "Profile fetch failed.", e)
                    }

                    // 2. Construct Fallback User from Metadata
                    val meta = authUser.userMetadata
                    val role = meta?.get("role")?.toString()?.trim('"') ?: "tenant"
                    val fullName = meta?.get("full_name")?.toString()?.trim('"')
                    val phone = meta?.get("phone_number")?.toString()?.trim('"')

                    val fallbackUser =
                        UserDto(
                            role = Role.fromValue(role),
                            userId = authUser.id,
                            profilePhotoUrl = "",
                            phoneNumber = phone ?: "",
                            fullName = fullName ?: "",
                        )

                    // 3. Attempt Self-Repair: Insert the missing profile row.
                    // This is crucial for RLS policies on other tables (like 'rooms') to work.
                    try {
                        Log.d("AuthRepo", "Attempting to upsert missing profile for user ${authUser.id}")
                        apiService.postgrest["profiles"].upsert(fallbackUser)
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

        override suspend fun getUserFromMetadata(): UserDto? {
            return withContext(Dispatchers.IO) {
                try {
                    apiService.auth.loadFromStorage()
                    val session = apiService.auth.currentSessionOrNull() ?: return@withContext null
                    val authUser = session.user ?: return@withContext null

                    val meta = authUser.userMetadata
                    val role = meta?.get("role")?.toString()?.trim('"') ?: "tenant"
                    val fullName = meta?.get("full_name")?.toString()?.trim('"')
                    val phone = meta?.get("phone_number")?.toString()?.trim('"')

                    val user =
                        UserDto(
                            role = Role.fromValue(role),
                            userId = authUser.id,
                            profilePhotoUrl = "",
                            phoneNumber = phone ?: "",
                            fullName = fullName ?: "",
                        )

                    user
                } catch (e: Exception) {
                    Log.e("AuthRepository", "getUserFromMetadata failed", e)
                    null
                }
            }
        }
    }
