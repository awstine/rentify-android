package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClient {

    lateinit var appContext: Context
        private set

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
            
            install(Postgrest)
            install(Auth)
            install(Functions)
            install(Realtime)
            install(Storage)
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}