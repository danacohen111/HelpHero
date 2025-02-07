package com.example.helphero.api

import com.example.helphero.models.NominatimResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
    @GET("search")
    fun searchLocation(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5
    ): Call<List<NominatimResponse>>
}

