package com.example.fitness.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

// 用於存儲部位分析的數據模型
data class PartAnalysisEntry(
    val date: String, // 格式: yyyy-MM-dd
    val exerciseWeights: Map<String, Float> // 動作名稱 -> 最大重量
)

class PartAnalysisRepository(private val context: Context) {
    private val gson = Gson()
    private val fileName = "part_analysis_data.json"

    private val _data = MutableStateFlow<List<PartAnalysisEntry>>(emptyList())
    val data = _data.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            try {
                val json = file.readText()
                val type = object : TypeToken<List<PartAnalysisEntry>>() {}.type
                val loaded: List<PartAnalysisEntry> = gson.fromJson(json, type)
                _data.value = loaded
            } catch (e: Exception) {
                e.printStackTrace()
                _data.value = emptyList()
            }
        }
    }

    private fun saveData(list: List<PartAnalysisEntry>) {
        try {
            val json = gson.toJson(list)
            File(context.filesDir, fileName).writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 新增或更新某一日期的動作重量紀錄
     * 如果當天已有紀錄，會比較新舊重量，取較大者 (PR)
     */
    suspend fun addOrUpdateRecord(date: String, newWeights: Map<String, Float>) {
        val currentList = _data.value.toMutableList()
        val index = currentList.indexOfFirst { it.date == date }

        if (index != -1) {
            // 如果今天已經有紀錄，合併數據 (保留較重的紀錄)
            val existingEntry = currentList[index]
            val mergedWeights = existingEntry.exerciseWeights.toMutableMap()

            newWeights.forEach { (name, weight) ->
                val oldWeight = mergedWeights[name] ?: 0f
                if (weight > oldWeight) {
                    mergedWeights[name] = weight
                }
            }
            currentList[index] = existingEntry.copy(exerciseWeights = mergedWeights)
        } else {
            // 如果今天沒有紀錄，新增一筆
            currentList.add(PartAnalysisEntry(date, newWeights))
        }

        // 排序 (按日期) 並儲存
        val sortedList = currentList.sortedBy { it.date }
        _data.value = sortedList
        saveData(sortedList)
    }
}