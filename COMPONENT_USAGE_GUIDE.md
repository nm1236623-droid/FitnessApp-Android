# 🎨 Fitness App UI 組件快速使用指南

## 📦 可用組件總覽

### 1. 卡片類組件

#### PremiumCard - 高級漸變卡片
```kotlin
import com.example.fitness.ui.theme.PremiumCard
import com.example.fitness.ui.theme.AppColors

PremiumCard(
    modifier = Modifier.fillMaxWidth(),
    gradient = AppColors.GradientPrimary, // 或 GradientSuccess, GradientWarning
    elevation = 3.dp,
    onClick = { /* 可選的點擊事件 */ }
) {
    // 卡片內容
    Text(
        text = "標題",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}
```

#### GlassCard - 玻璃態卡片
```kotlin
import com.example.fitness.ui.theme.GlassCard

GlassCard(
    modifier = Modifier.fillMaxWidth(),
    backgroundColor = MaterialTheme.colorScheme.surface
) {
    Text("現代化玻璃效果卡片")
}
```

### 2. 按鈕類組件

#### PremiumButton - 主要操作按鈕
```kotlin
import com.example.fitness.ui.theme.PremiumButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

PremiumButton(
    text = "開始訓練",
    onClick = { /* 操作 */ },
    icon = Icons.Default.FitnessCenter, // 可選圖標
    gradient = AppColors.GradientPrimary,
    enabled = true
)
```

#### OutlinedPremiumButton - 次要操作按鈕
```kotlin
import com.example.fitness.ui.theme.OutlinedPremiumButton

OutlinedPremiumButton(
    text = "取消",
    onClick = { /* 操作 */ },
    icon = Icons.Default.Close,
    borderColor = MaterialTheme.colorScheme.error
)
```

### 3. 數據展示組件

#### StatCard - 統計卡片
```kotlin
import com.example.fitness.ui.theme.StatCard

Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    StatCard(
        modifier = Modifier.weight(1f),
        title = "今日消耗",
        value = "520",
        subtitle = "卡路里",
        icon = Icons.Default.LocalFireDepartment,
        gradient = AppColors.GradientSuccess
    )
    
    StatCard(
        modifier = Modifier.weight(1f),
        title = "訓練時長",
        value = "45",
        subtitle = "分鐘",
        icon = Icons.Default.Timer,
        gradient = AppColors.GradientPrimary
    )
}
```

#### CircularProgressIndicator - 環形進度
```kotlin
import com.example.fitness.ui.theme.CircularProgressIndicator

CircularProgressIndicator(
    progress = 0.75f, // 0.0 到 1.0
    size = 120.dp,
    strokeWidth = 12.dp,
    gradient = AppColors.GradientSuccess,
    label = "完成度",
    value = "75%"
)
```

### 4. 交互組件

#### PremiumChip - 選擇標籤
```kotlin
import com.example.fitness.ui.theme.PremiumChip

var selectedChip by remember { mutableStateOf("胸部") }

FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    listOf("胸部", "背部", "腿部", "肩膀").forEach { part ->
        PremiumChip(
            text = part,
            selected = selectedChip == part,
            onClick = { selectedChip = part },
            selectedGradient = AppColors.GradientPrimary
        )
    }
}
```

### 5. 輔助組件

#### EmptyState - 空狀態
```kotlin
import com.example.fitness.ui.theme.EmptyState

EmptyState(
    emoji = "📊",
    title = "尚無訓練記錄",
    subtitle = "開始你的第一次訓練吧！",
    actionButton = {
        PremiumButton(
            text = "開始訓練",
            onClick = { /* 導航到訓練頁面 */ }
        )
    }
)
```

#### PremiumDivider - 分隔線
```kotlin
import com.example.fitness.ui.theme.PremiumDivider

Column {
    Text("區塊 1")
    PremiumDivider(modifier = Modifier.padding(vertical = 16.dp))
    Text("區塊 2")
}
```

#### SuccessAnimation - 成功動畫
```kotlin
import com.example.fitness.ui.theme.SuccessAnimation

if (operationSuccess) {
    SuccessAnimation(
        message = "訓練計畫已保存！"
    )
}
```

## 🎨 顏色方案使用

```kotlin
import com.example.fitness.ui.theme.AppColors

// 漸層背景
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(
            brush = Brush.linearGradient(AppColors.GradientPrimary)
        )
)

// 可用的漸層
AppColors.GradientPrimary   // Indigo → Purple (主要)
AppColors.GradientSecondary // Cyan → Blue (次要)
AppColors.GradientSuccess   // Green漸層 (成功)
AppColors.GradientWarning   // Amber漸層 (警告)
AppColors.GradientError     // Red漸層 (錯誤)
```

## 🔧 實際應用示例

### 示例 1: 優化訓練計畫卡片

**之前:**
```kotlin
Card {
    Column {
        Text(planName)
        Text("${exercises.size} 個動作")
    }
}
```

**之後:**
```kotlin
PremiumCard(
    gradient = AppColors.GradientPrimary,
    onClick = { onPlanClick() }
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = planName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${exercises.size} 個動作",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White
        )
    }
}
```

### 示例 2: 優化統計概覽

**之前:**
```kotlin
Row {
    Column {
        Text("今日消耗")
        Text("520 卡路里")
    }
    Column {
        Text("訓練時長")
        Text("45 分鐘")
    }
}
```

**之後:**
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    StatCard(
        modifier = Modifier.weight(1f),
        title = "今日消耗",
        value = "520",
        subtitle = "卡路里",
        icon = Icons.Default.LocalFireDepartment,
        gradient = AppColors.GradientSuccess
    )
    
    StatCard(
        modifier = Modifier.weight(1f),
        title = "訓練時長",
        value = "45",
        subtitle = "分鐘",
        icon = Icons.Default.Timer,
        gradient = AppColors.GradientPrimary
    )
}
```

### 示例 3: 優化操作按鈕

**之前:**
```kotlin
Button(onClick = { startWorkout() }) {
    Text("開始訓練")
}
```

**之後:**
```kotlin
PremiumButton(
    text = "開始訓練",
    onClick = { startWorkout() },
    icon = Icons.Default.FitnessCenter,
    modifier = Modifier.fillMaxWidth()
)
```

## 💡 設計最佳實踐

### 1. 漸層使用場景
- **Primary**: 主要操作、重要信息
- **Success**: 完成狀態、成就展示
- **Warning**: 提醒、待處理事項
- **Error**: 錯誤提示、危險操作

### 2. 間距系統
```kotlin
Arrangement.spacedBy(8.dp)  // 小間距
Arrangement.spacedBy(12.dp) // 中等間距
Arrangement.spacedBy(16.dp) // 大間距
Arrangement.spacedBy(24.dp) // 區塊間距
```

### 3. 圓角使用
```kotlin
MaterialTheme.shapes.small      // 8.dp - 按鈕、輸入框
MaterialTheme.shapes.medium     // 16.dp - 卡片
MaterialTheme.shapes.large      // 20.dp - 大卡片
MaterialTheme.shapes.extraLarge // 28.dp - 特殊元素
```

### 4. 陰影階梯
```kotlin
elevation = 0.dp  // 無陰影
elevation = 2.dp  // 輕微陰影
elevation = 4.dp  // 標準陰影
elevation = 8.dp  // 強調陰影
```

## 🚀 快速遷移檢查清單

- [ ] 替換普通 `Card` 為 `PremiumCard` 或 `GlassCard`
- [ ] 替換 `Button` 為 `PremiumButton`
- [ ] 添加統計數據的 `StatCard`
- [ ] 空狀態使用 `EmptyState` 組件
- [ ] 標籤選擇使用 `PremiumChip`
- [ ] 進度顯示使用 `CircularProgressIndicator`
- [ ] 成功反饋使用 `SuccessAnimation`
- [ ] 統一使用 `AppColors` 漸層色

---

**提示**: 所有組件都支持 Modifier 參數，可以靈活調整大小、位置和行為。

