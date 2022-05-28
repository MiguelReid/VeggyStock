package com.example.veggystock.api

import com.google.gson.annotations.SerializedName

data class Gson(
    @SerializedName("label") var label: String,
    @SerializedName("image") var image: String,
    @SerializedName("nutrients") var nutrients: String,
)

data class hitsGson(
    @SerializedName("hints") var listHints: List<Gson>
)
