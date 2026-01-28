package com.example.fitness.food

import android.content.Context
import android.graphics.Bitmap

/**
 * Stub FoodRecognitionManager — returns an empty list.
 * TODO: replace with ML Kit implementation (ImageLabeling) after Gradle sync.
 */
class FoodRecognitionManager(private val context: Context) {
    suspend fun recognize(bitmap: Bitmap): List<String> {
        // placeholder implementation; integrate ML Kit here
        return emptyList()
    }
}
