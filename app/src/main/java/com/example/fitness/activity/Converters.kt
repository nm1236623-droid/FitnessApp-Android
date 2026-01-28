package com.example.fitness.activity

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.util.Date

object Converters {
    private val gson = Gson()

    // Instant <-> Long helpers
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    // Legacy Date helpers
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // Simple List<String> converter using Gson
    fun fromStringList(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    fun listToString(list: List<String>?): String? = gson.toJson(list ?: emptyList<String>())
}
