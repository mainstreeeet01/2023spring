package com.example.myapplication.util

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitModule {
    private lateinit var retrofit: Retrofit

    fun getRetrofitInstance(): Retrofit {

        val interceptor = HttpLoggingInterceptor {
            Log.d("TAG", it)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder().apply {
            addInterceptor(interceptor)
        }.build()

        return Retrofit.Builder()
            .baseUrl("http://192.168.0.14:443")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}