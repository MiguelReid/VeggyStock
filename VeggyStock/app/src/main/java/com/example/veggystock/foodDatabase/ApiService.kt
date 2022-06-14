package com.example.veggystock.foodDatabase

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * La primera funcion es de donde cojo el foodId que necesito para la segunda funcion
 * donde los tags de salud como el de vegano esta disponible
 */

interface ApiService {
    @GET
    suspend fun foodDatabase(@Url url: String): Response<Hints>

    @GET
    suspend fun foodAnalysis(@Url url: String): Response<Gson2>
}