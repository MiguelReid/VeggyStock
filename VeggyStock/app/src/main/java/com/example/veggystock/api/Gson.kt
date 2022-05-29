package com.example.veggystock.api

import com.google.gson.annotations.SerializedName

data class Gson(
    @SerializedName("label") var label: String,
    @SerializedName("image") var image: String,
    @SerializedName("nutrients") var nutrients: List<NutrientsGson>
)

data class HintsGson(
    @SerializedName("hints") var listHints: List<Gson>
)

data class NutrientsGson(
    @SerializedName("ENERC_KCAL") var enercKcal: Double,
    @SerializedName("PROCNT") var procnt: Double,
    @SerializedName("FAT") var fat: Double,
    @SerializedName("CHOCDF") var chocdf: Double,
    @SerializedName("FIBTG") var fibtg: Double
    )
