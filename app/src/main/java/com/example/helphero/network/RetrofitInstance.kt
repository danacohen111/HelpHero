package com.example.helphero.network

import com.example.helphero.api.NominatimApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.helphero.BuildConfig

object RetrofitInstance {
    private val BASE_URL: String = BuildConfig.NOMINATIM_URL

    val api: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}