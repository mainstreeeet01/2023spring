package com.example.myapplication.data

import com.example.myapplication.util.RetrofitModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class Repo {

    private val api = RetrofitModule.getRetrofitInstance().create(RetrofitInterface::class.java)

    suspend fun getPublicResponseData(): List<PublicResponseData>? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val response = api.getPublicResponseData()
                return@withContext if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            }
            null
        }
    }

    suspend fun getSearchData(query: String): List<String>? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val response = api.getSearchData(query)
                 response.body().takeIf { response.isSuccessful }
            }.getOrNull()
        }
    }
}