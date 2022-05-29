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
    @SerializedName("ENERC_KCAL") var energy: Double,
    //Energy
    @SerializedName("PROCNT") var protein: Double,
    //Protein
    @SerializedName("FAT") var fat: Double,
    //Total lipid (fat)
    @SerializedName("CHOCDF") var carbs: Double,
    //Carbohydrate, by difference
    @SerializedName("FIBTG") var fiber: Double
    //Fiber, total dietary
)
