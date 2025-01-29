package com.example.helphero.models

import android.content.Context
import com.cloudinary.android.MediaManager
import com.example.helphero.BuildConfig

fun initCloudinary(context: Context) {
    val config: HashMap<String, String> = hashMapOf(
        "cloud_name" to BuildConfig.CLOUD_NAME,
        "api_key" to BuildConfig.API_KEY,
        "api_secret" to BuildConfig.API_SECRET
    )
    MediaManager.init(context, config)
}