package com.example.myapplication.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface RetrofitInterface {

    @GET("data")
    suspend fun getPublicResponseData(): Response<List<PublicResponseData>>

    @GET("search")
    suspend fun getSearchData(@Query("query") query: String): Response<List<String>>
}