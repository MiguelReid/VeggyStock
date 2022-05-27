package com.example.veggystock.api

import com.google.gson.annotations.SerializedName

data class Gson(
    @SerializedName("hdurl") var url: String,
)
