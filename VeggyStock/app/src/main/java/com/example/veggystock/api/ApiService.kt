package com.example.veggystock.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {

    @GET
    suspend fun getData(@Url url: String): Response<Gson>
}