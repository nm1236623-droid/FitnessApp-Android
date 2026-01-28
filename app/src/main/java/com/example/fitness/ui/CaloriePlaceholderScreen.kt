package com.example.fitness.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Temporary placeholder screen.
 * The app previously navigated to a calorie feature that isn't implemented yet.
 */
@Composable
fun CaloriePlaceholderScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("熱量計算", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("此功能尚未完成，敬請期待。", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
