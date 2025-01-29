package com.example.helphero.base

import android.app.Application
import com.example.helphero.models.initCloudinary

class HelpHeroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initCloudinary(this)
    }
}