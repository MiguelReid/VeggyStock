package com.example.veggystock.foodDatabase

import com.google.gson.annotations.SerializedName

data class Hints(
    @SerializedName("hints") var listHints: List<Food>
)

data class Food(
    @SerializedName("food") var food: Gson
)

data class Gson(
    @SerializedName("label") var label: String,
    @SerializedName("image") var image: String,
    @SerializedName("foodId") var id: String,
    @SerializedName("nutrients") var nutrients: NutrientsGson
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

data class Gson2(
    @SerializedName("calories") var calories: Double,
    @SerializedName("healthLabels") var healthLabels: String
)
