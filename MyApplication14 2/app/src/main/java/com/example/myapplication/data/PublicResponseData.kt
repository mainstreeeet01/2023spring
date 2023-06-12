package com.example.myapplication.data

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize


data class PublicResponseData(
    val category: String? = null,
    val title: String? = null,
    val desc: String? = null,
    val keywordStr: String?,
    val url: String? = null
) {
    val keywords: List<KeywordResponseData>?
        get() {
            val itemType = object : TypeToken<List<KeywordResponseData>>() {}.type
            val str = keywordStr?.replace("\\", "")?.replace("\"{", "{")?.replace("}\"", "}")
            return Gson().fromJson<List<KeywordResponseData>>(str, itemType)
        }
}

@Parcelize
data class KeywordResponseData(
    val keyword: String,
    val isDuplicate: Boolean
): Parcelable