package com.example.myapplication

import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.KeywordResponseData
import com.example.myapplication.data.PublicResponseData
import com.example.myapplication.data.Repo
import ir.mahozad.android.PieChart
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch


class MainViewModel : ViewModel() {

    private val repo by lazy { Repo() }

    private val _bubbleItems by lazy { MutableStateFlow(listOf<KeywordResponseData>()) }
    val bubbleItems get() = _bubbleItems.asStateFlow()

    private val _searchBubbleItems by lazy { MutableStateFlow(listOf<KeywordResponseData>()) }
    val searchBubbleItems get() = _searchBubbleItems.asStateFlow()

    private val _chartItems by lazy { MutableLiveData(listOf<PieChart.Slice>()) }
    val chartItems: LiveData<List<PieChart.Slice>> get() = _chartItems

    val resultItems by lazy { MutableStateFlow(listOf<PublicResponseData>()) }

    var keywordCategoryData = listOf<KeywordCategoryData>()

    val searchResult by lazy { MutableStateFlow(listOf<String>()) }

    private val _listItems by lazy {
        MutableStateFlow(listOf<ListItemData>())
    }
    val listItems get() = _listItems.asStateFlow()

    private val _title by lazy { MutableStateFlow("") }
    val title get() = _title.asStateFlow()

    private val _keyword by lazy { MutableStateFlow("") }
    val keyword get() = _keyword.asStateFlow()

    private val _event by lazy { MutableLiveData<ListItemData>() }
    val event: LiveData<ListItemData> get() = _event

    private val _initEvent by lazy { MutableLiveData<List<KeywordResponseData>>() }
    val initEvent: LiveData<List<KeywordResponseData>> get() = _initEvent

    private val _moveToWeb by lazy { MutableLiveData<String>() }
    val moveToWeb: LiveData<String> get() = _moveToWeb

    fun getData() {
        viewModelScope.launch {
            val result = repo.getPublicResponseData()

            if (!result.isNullOrEmpty()) {
                resultItems.emit(result)

                val resultMap = mutableMapOf<String, MutableList<String?>>()
                val keywordCategoryDataList = mutableListOf<KeywordCategoryData>()
                val keywordList = mutableListOf<KeywordResponseData>()

                result.forEach { data ->
                    data.keywords?.forEach { keyword ->
                        if (resultMap.contains(keyword.keyword)) {
                            resultMap[keyword.keyword]?.add(data.category)
                        } else {
                            resultMap[keyword.keyword] = mutableListOf(data.category)
                        }

                        if (!keywordList.any { it.keyword == keyword.keyword }) {
                            keywordList.add(keyword)
                        }
                    }
                }

                val sortedList = keywordList.filter { it.keyword.isNotEmpty() }.sortedBy { it.keyword }

                _bubbleItems.emit(sortedList)

                resultMap.forEach { (t, u) ->
                    keywordCategoryDataList.add(KeywordCategoryData(keyword = t, category = u))
                }

                keywordCategoryData = keywordCategoryDataList

                _initEvent.postValue(sortedList)
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            _keyword.emit("")
            _title.emit("")
            searchResult.emit(emptyList())
        }
    }

    fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        viewModelScope.launch {
            _keyword.emit(s.toString())
        }

        Log.d("aaa", s.toString())
        s.toString().takeIf { it != "null" && it.isNotEmpty() }?.let { query ->
            viewModelScope.launch {
                val result = repo.getSearchData(query)
                result.takeIf { it != null }?.let {
                    searchResult.emit(it)
                }
            }
        }
    }

    fun init(title: String) {
        viewModelScope.launch {
            _title.emit(title)
        }

        val filterList = keywordCategoryData.filter { it.keyword == title }
        if (filterList.isNotEmpty()) {
            val resultMap =  mutableMapOf<String?, Int>()
            filterList[0].category?.forEach {
                resultMap[it] = (resultMap[it] ?: 0) + 1
            }

            val resultList = mutableListOf<PieChart.Slice>()
            fun generateRandomColor(): Int {
                fun component() = (0..255).random()
                return Color.rgb(component(), component(), component())
            }

            resultMap.forEach { (t, u) ->
                val slice = PieChart.Slice(u.toFloat() / (filterList[0].category?.size ?: 1), color = generateRandomColor(), legend = t ?: "")
                resultList.add(slice)
            }

            _chartItems.postValue(resultList)
        }

        resultItems.value.filter { it.keywords?.any { it.keyword == title } == true }.map {
            ListItemData(it.title, it.keywords, it.category, it.desc, it.url)
        }.also {
            viewModelScope.launch {
                _listItems.emit(it)
            }
        }
    }

    fun onClickBubble(title: String) {
        init(title)
    }

    fun onClickListItem(item: ListItemData) {
        _event.postValue(item)
    }

    fun setSearchBubbleItems(word: String) {
        viewModelScope.launch {
            bubbleItems.value.filter { if (word.isEmpty()) true else it.keyword.contains(word) }.also {
                _searchBubbleItems.emit(it)
            }
        }
    }

    fun onClickSite(url: String) {
        _moveToWeb.postValue(url)
    }
}

data class KeywordCategoryData(
    val keyword: String?,
    val category: List<String?>?
)