package com.example.veggystock.foodDatabase

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * First function is where I get the foodId that I will be needing for the second function,
 * where the health tags (like vegan) are available
 */

interface ApiService {
    @GET
    suspend fun foodDatabase(@Url url: String): Response<Hints>

    @GET
    suspend fun foodAnalysis(@Url url: String): Response<Gson2>
}