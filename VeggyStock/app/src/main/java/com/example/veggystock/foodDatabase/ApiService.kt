package com.example.veggystock.foodDatabase

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun foodDatabase(@Url url: String): Response<Hints>

    @GET
    suspend fun foodAnalysis(@Url url: String): Response<Gson2>
}