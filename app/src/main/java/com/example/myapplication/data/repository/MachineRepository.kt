package com.example.myapplication.data.repository

import com.example.myapplication.data.models.MachineSession
import com.example.myapplication.data.models.WashingMachine
import com.example.myapplication.di.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MachineRepository {

    suspend fun getMachinesForTenant(): Result<List<WashingMachine>> {
        return withContext(Dispatchers.IO) {
            try {
                // RLS should filter machines belonging to the property where the tenant has an active booking
                val machines = SupabaseClient.client.postgrest["washing_machines"]
                    .select()
                    .decodeList<WashingMachine>()
                Result.success(machines)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getActiveSessionsForTenant(tenantId: String): Result<List<MachineSession>> {
        return withContext(Dispatchers.IO) {
            try {
                val sessions = SupabaseClient.client.postgrest["machine_sessions"]
                    .select {
                        filter {
                            eq("tenant_id", tenantId)
                            eq("status", "active")
                        }
                    }
                    .decodeList<MachineSession>()
                Result.success(sessions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createSession(session: MachineSession): Result<MachineSession> {
        return withContext(Dispatchers.IO) {
            try {
                val createdSession = SupabaseClient.client.postgrest["machine_sessions"]
                    .insert(session)
                    .decodeSingle<MachineSession>()
                Result.success(createdSession)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
