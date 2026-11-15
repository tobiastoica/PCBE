package com.example.micromobility.di

import android.app.Application
import com.example.micromobility.di.AppContainer

class MicromobilityApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}