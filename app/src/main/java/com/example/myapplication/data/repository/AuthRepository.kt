package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.models.User
import com.example.myapplication.di.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

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
        return getUserProfile().getOrNull()
    }

    suspend fun getUserProfile(): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // ** THE FIX IS HERE **
                // Ensure the session is loaded from storage before trying to access it.
                SupabaseClient.client.auth.loadFromStorage()
                
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                if (session == null) {
                     return@withContext Result.failure(Exception("No active session"))
                }
                
                val authUser = session.user
                if (authUser == null) {
                     return@withContext Result.failure(Exception("Session user is null"))
                }

                // 1. Try fetching profile from DB
                try {
                    val profile = SupabaseClient.client.postgrest["profiles"]
                        .select { filter { eq("id", authUser.id) } }
                        .decodeSingle<User>()
                    return@withContext Result.success(profile)
                } catch (e: Exception) {
                    Log.w("AuthRepo", "Profile fetch failed. This usually means the row is missing. Attempting self-repair...", e)
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
