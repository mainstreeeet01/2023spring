package com.example.myapplication

import android.os.Parcelable
import com.example.myapplication.data.KeywordResponseData
import kotlinx.parcelize.Parcelize


@Parcelize
data class ListItemData(
    val title: String?,
    val keywords: List<KeywordResponseData>? = null,
    val category: String? = null,
    val subTitle: String? = null,
    val url: String? = null
): Parcelable {
    val keywordStr: String?
        get() = keywords?.joinToString { it.keyword }
}