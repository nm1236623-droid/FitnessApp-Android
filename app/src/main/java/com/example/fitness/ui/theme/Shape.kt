package com.example.fitness.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 統一的形狀系統 - 現代化圓角設計
 */
val Shapes = Shapes(
    // 極小圓角 - 用於小元素
    extraSmall = RoundedCornerShape(4.dp),

    // 小圓角 - 用於按鈕、輸入框
    small = RoundedCornerShape(8.dp),

    // 中等圓角 - 用於卡片
    medium = RoundedCornerShape(16.dp),

    // 大圓角 - 用於大卡片、對話框
    large = RoundedCornerShape(20.dp),

    // 超大圓角 - 用於特殊強調元素
    extraLarge = RoundedCornerShape(28.dp)
)

