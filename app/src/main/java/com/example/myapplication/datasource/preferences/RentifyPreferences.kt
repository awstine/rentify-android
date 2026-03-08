package com.example.myapplication.datasource.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import javax.inject.Inject

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

internal const val dataStoreFileName = "rentify.preferences_pb"

interface RentifyPreferences {
    suspend fun saveUserRole(role: String)
    suspend fun getUserRole(): Flow<String>
    suspend fun saveUserId (id: String)
    suspend fun getUserId(): Flow<String>
}

class RentifyPreferencesImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
): RentifyPreferences{
    override suspend fun saveUserRole(role: String) {
        dataStore.edit {
            it[USER_ROLE_KEY] = role
        }
    }

    override suspend fun getUserRole(): Flow<String> {
        return dataStore.data.map {
            it[USER_ROLE_KEY] ?: ""
        }
    }

    override suspend fun saveUserId(id: String) {
        dataStore.edit {
            it[USER_ID_KEY] = id
        }
    }

    override suspend fun getUserId(): Flow<String> {
        return dataStore.data.map {
            it[USER_ID_KEY] ?: ""
        }
    }


    companion object {
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

}