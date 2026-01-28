# 營養記錄功能整合指南

## 概述

本指南說明如何將營養記錄功能整合到您的 Fitness App 中。所有組件都已準備就緒，包括響應式布局、圖表、數據持久化和測試功能。

## 文件結構

```
ui/
├── NutritionRecordsScreen.kt          # 基本營養記錄頁面
├── ResponsiveNutritionScreen.kt      # 響應式營養記錄頁面
├── NutritionCharts.kt                 # 營養數據圖表組件
├── NutritionPreferences.kt            # 用戶偏好設定和數據持久化
├── ComprehensiveNutritionTest.kt      # 綜合測試頁面
├── NutritionAnalytics.kt              # 營養數據分析組件
├── NutritionTheme.kt                  # 自定義主題
├── NavigationUpdate.kt                # 導航更新組件
└── HomeScreen_Nutrition.kt           # 帶營養功能的HomeScreen版本
```

## 整合步驟

### 1. 添加依賴項

在 `app/build.gradle` 中添加 DataStore 依賴：

```kotlin
dependencies {
    implementation "androidx.datastore:datastore-preferences:1.0.0"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.2"
}
```

### 2. 導航整合

在 HomeScreen.kt 的 when 條件中添加營養記錄路由：

```kotlin
"nutrition" -> ResponsiveNutritionRecordsScreen(
    activityRepository = activityRepository,
    onBack = { selectTab("plans") }
)
```

並在底部導航欄添加營養記錄標籤：

```kotlin
NavigationBarItem(
    selected = selected == "nutrition",
    onClick = { selectTab("nutrition") },
    icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = "Nutrition Records") },
    label = {},
    alwaysShowLabel = false
)
```

### 3. 主要組件使用

#### 響應式營養記錄頁面

```kotlin
ResponsiveNutritionRecordsScreen(
    activityRepository = activityRepository,
    onBack = { /* 返回邏輯 */ }
)
```

#### 營養圖表

```kotlin
NutritionChartsScreen(
    activityRepository = activityRepository
)
```

#### 偏好設定

```kotlin
NutritionPreferencesScreen(
    onBack = { /* 返回邏輯 */ }
)
```

#### 綜合測試

```kotlin
ComprehensiveNutritionTestScreen(
    activityRepository = activityRepository,
    onBack = { /* 返回邏輯 */ }
)
```

## 功能特性

### 📱 響應式布局
- **手機** (< 600dp): 垂直滾動布局
- **平板** (600-840dp): 單欄寬布局 + 側邊統計面板
- **大平板** (> 840dp): 兩欄對稱布局

### 📊 圖表類型
- **折線圖**: 卡路里趨勢分析
- **圓餅圖**: 營養素分布
- **柱狀圖**: 週對比分析
- **進度條**: 目標達成率

### 💾 數據持久化
- **每日目標**: 卡路里、蛋白質、碳水化合物、脂肪
- **通知設定**: 提醒時間和開關
- **顯示偏好**: 主題模式、週摘要顯示
- **數據設定**: 自動同步、匯出格式

### 🧪 測試功能
- **組件測試**: 各個UI組件的功能測試
- **響應式測試**: 不同螢幕尺寸適應性
- **整合測試**: 完整工作流程驗證
- **性能測試**: 組件渲染性能

## 主題和樣式

使用 `NutritionTheme` 包裹組件以獲得一致的霓虹風格：

```kotlin
NutritionTheme {
    // 您的營養相關組件
}
```

### 顏色方案
- **NeonBlue**: 主要操作和標題
- **NeonGreen**: 成功狀態和積極數據
- **NeonOrange**: 警告和注意事項
- **NeonPurple**: 次要信息和輔助功能
- **NeonYellow**: 提示和建議

## 數據流

```
ActivityLogRepository → NutritionAnalytics → UI Components
                                    ↓
DataStore ← NutritionPreferencesManager ← User Settings
```

## 測試建議

1. **單元測試**: 測試 `NutritionPreferencesManager` 的數據持久化
2. **UI測試**: 使用 `ComposableTestRule` 測試各個組件
3. **整合測試**: 使用 `ComprehensiveNutritionTestScreen` 進行完整測試
4. **響應式測試**: 在不同模擬器尺寸上測試布局適應性

## 性能優化

1. **懶加載**: 使用 `LazyColumn` 處理大量數據
2. **狀態提升**: 適當使用 `remember` 和 `derivedStateOf`
3. **圖表優化**: 限制數據點數量，使用 Canvas 繪製
4. **緩存策略**: DataStore 自動處理偏好設定緩存

## 常見問題

### Q: 如何自定義營養目標？
A: 使用 `NutritionPreferencesScreen` 或直接調用 `NutritionPreferencesManager` 的更新方法。

### Q: 圖表數據如何更新？
A: 圖表組件使用模擬數據，您可以替換為實際的 `ActivityLogRepository` 數據。

### Q: 如何添加新的圖表類型？
A: 在 `NutritionCharts.kt` 中添加新的 Composable 函數，並更新主題顏色。

### Q: 響應式布局如何調整？
A: 修改 `ResponsiveNutritionScreen.kt` 中的斷點值和布局邏輯。

## 後續開發建議

1. **實際數據整合**: 連接真實的營養數據源
2. **雲端同步**: 整合 Firebase Firestore
3. **圖表交互**: 添加點擊、縮放等交互功能
4. **導出功能**: 實現數據匯出為 CSV/PDF
5. **多語言支援**: 國際化所有文字內容

## 聯絡支援

如有技術問題或需要進一步協助整合，請參考相關文件或聯繫開發團隊。

---

**注意**: 本指南基於當前版本的組件，如有更新請參考最新的代碼文件。
