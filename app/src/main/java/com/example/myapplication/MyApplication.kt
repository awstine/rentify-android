package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.SupabaseClient
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SupabaseClient.init(this)
    }
}
