package com.example.myapplication.data.repository

import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.myapplication.data.models.User
import com.example.myapplication.di.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    private val USER_ID_KEY = stringPreferencesKey("user_id")

    suspend fun saveUserRole(role: String) {
        dataStore.edit { prefs ->
            prefs[USER_ROLE_KEY] = role
        }
    }

    suspend fun saveUserId(userId: String) {
        dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
        }
    }

    suspend fun getCachedUserRole(): String? {
        return dataStore.data.map { it[USER_ROLE_KEY] }.first()
    }

    suspend fun getCachedUserId(): String? {
        return dataStore.data.map { it[USER_ID_KEY] }.first()
    }

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
                
                // Cache the role and ID
                saveUserRole(userData.role ?: "tenant")
                saveUserId(userId)
                
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
                
                // Immediately try to refresh local cache from whatever we have
                val user = getUserFromMetadata()
                user?.let {
                    saveUserRole(it.role ?: "tenant")
                    saveUserId(it.id)
                }

                // Also try fresh profile if online
                getUserProfile()
                
                Result.success(true)
            } catch (e: Exception) {
                Log.e("AuthRepository", "SignIn failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun signOut(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.signOut()
                dataStore.edit { 
                    it.remove(USER_ROLE_KEY) 
                    it.remove(USER_ID_KEY)
                }
                Result.success(true)
            } catch (e: Exception) {
                Log.e("AuthRepository", "SignOut failed", e)
                Result.failure(e)
            }
        }
    }


    suspend fun uploadProfilePhoto(uri: Uri): Result<String> {
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
    }

    /**
     * Fetches the user profile from the database. 
     * This is the source of truth for the user's role and data.
     */
    suspend fun getUserProfile(): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.loadFromStorage()
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                    ?: return@withContext Result.failure(Exception("No active session"))

                val authUser = session.user
                    ?: return@withContext Result.failure(Exception("Session user is null"))

                // Try fetching profile from DB
                val profile = SupabaseClient.client.postgrest["profiles"]
                    .select { filter { eq("id", authUser.id) } }
                    .decodeSingle<User>()
                
                // If successful, cache the role and ID
                profile.role?.let { saveUserRole(it) }
                saveUserId(profile.id)
                
                Result.success(profile)
            } catch (e: Exception) {
                Log.w("AuthRepository", "getUserProfile (DB) failed, might be offline", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Extracts user data from the Auth session metadata.
     * This is useful when offline because GoTrue persists metadata locally.
     */
    suspend fun getUserFromMetadata(): User? {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.loadFromStorage()
                val session = SupabaseClient.client.auth.currentSessionOrNull() ?: return@withContext null
                val authUser = session.user ?: return@withContext null
                
                val meta = authUser.userMetadata
                val role = meta?.get("role")?.toString()?.trim('"') ?: "tenant"
                val fullName = meta?.get("full_name")?.toString()?.trim('"')
                val phone = meta?.get("phone_number")?.toString()?.trim('"')

                val user = User(
                    id = authUser.id,
                    email = authUser.email,
                    phone_number = phone,
                    id_number = null,
                    role = role,
                    full_name = fullName,
                    created_at = authUser.createdAt?.toString()
                )
                
                // Always try to keep cache updated when we see valid data
                saveUserId(user.id)
                saveUserRole(user.role)
                
                user
            } catch (e: Exception) {
                Log.e("AuthRepository", "getUserFromMetadata failed", e)
                null
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