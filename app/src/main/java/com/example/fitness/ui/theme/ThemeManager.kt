package com.example.fitness.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

/**
 * 主題模式
 */
enum class ThemeMode {
    LIGHT,      // 淺色主題
    DARK,       // 深色主題
    AUTO        // 自動（跟隨系統）
}

/**
 * 顏色方案
 */
enum class ColorScheme {
    NEON_BLUE,      // 霓虹藍（預設）
    PURPLE,         // 紫色
    GREEN,          // 綠色
    ORANGE,         // 橙色
    RED,            // 紅色
    CUSTOM          // 自訂
}

/**
 * 主題管理器
 */
class ThemeManager(private val context: Context) {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val COLOR_SCHEME_KEY = stringPreferencesKey("color_scheme")
        private val AUTO_DARK_START_HOUR_KEY = stringPreferencesKey("auto_dark_start")
        private val AUTO_DARK_END_HOUR_KEY = stringPreferencesKey("auto_dark_end")

        // 單例
        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // 當前主題模式
    var currentThemeMode by mutableStateOf(ThemeMode.AUTO)
        private set

    // 當前顏色方案
    var currentColorScheme by mutableStateOf(ColorScheme.NEON_BLUE)
        private set

    /**
     * 獲取主題模式
     */
    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        val mode = prefs[THEME_MODE_KEY] ?: ThemeMode.AUTO.name
        try {
            ThemeMode.valueOf(mode).also { currentThemeMode = it }
        } catch (e: Exception) {
            ThemeMode.AUTO
        }
    }

    /**
     * 獲取顏色方案
     */
    val colorScheme: Flow<ColorScheme> = context.themeDataStore.data.map { prefs ->
        val scheme = prefs[COLOR_SCHEME_KEY] ?: ColorScheme.NEON_BLUE.name
        try {
            ColorScheme.valueOf(scheme).also { currentColorScheme = it }
        } catch (e: Exception) {
            ColorScheme.NEON_BLUE
        }
    }

    /**
     * 設置主題模式
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
        currentThemeMode = mode
    }

    /**
     * 設置顏色方案
     */
    suspend fun setColorScheme(scheme: ColorScheme) {
        context.themeDataStore.edit { prefs ->
            prefs[COLOR_SCHEME_KEY] = scheme.name
        }
        currentColorScheme = scheme
    }

    /**
     * 設置自動深色模式時間範圍
     */
    suspend fun setAutoDarkModeHours(startHour: Int, endHour: Int) {
        context.themeDataStore.edit { prefs ->
            prefs[AUTO_DARK_START_HOUR_KEY] = startHour.toString()
            prefs[AUTO_DARK_END_HOUR_KEY] = endHour.toString()
        }
    }

    /**
     * 判斷當前是否應該使用深色模式
     */
    fun shouldUseDarkMode(systemInDarkTheme: Boolean): Boolean {
        return when (currentThemeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.AUTO -> systemInDarkTheme
        }
    }

    /**
     * 根據顏色方案獲取主色調
     */
    fun getPrimaryColor(): androidx.compose.ui.graphics.Color {
        return when (currentColorScheme) {
            ColorScheme.NEON_BLUE -> TechColors.NeonBlue
            ColorScheme.PURPLE -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
            ColorScheme.GREEN -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
            ColorScheme.ORANGE -> androidx.compose.ui.graphics.Color(0xFFFF9800)
            ColorScheme.RED -> androidx.compose.ui.graphics.Color(0xFFF44336)
            ColorScheme.CUSTOM -> TechColors.NeonBlue // 預設值
        }
    }
}
