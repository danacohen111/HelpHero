package com.example.helphero.models

import com.google.gson.annotations.SerializedName

data class NominatimResponse(
    @SerializedName("display_name") val displayName: String
)
