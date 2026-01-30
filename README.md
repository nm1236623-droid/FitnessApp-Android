# 🏋️ FitnessApp - 智慧健身紀錄助手 (Android)

> **結合 Google Gemini AI 與雲端同步的個人化健身教練**
> *目前狀態：Google Play 封測階段 (Closed Testing)*

## 📖 專案簡介 (Overview)
這是一款專為健身愛好者設計的 Android 原生應用程式。鑑於市面上健身 App 介面過於繁雜，我利用 **Java/Kotlin** 與 **Android Studio** 開發了這款強調「直觀操作」與「智慧分析」的工具。不僅能記錄訓練數據，更整合了 **Google Gemini API**，能根據使用者的數據提供即時的飲食與運動建議。

## ✨ 核心功能 (Key Features)

### 🤖 AI 智慧教練 (AI Integration)
* **Gemini API 串接**：整合 Google 生成式 AI，根據使用者的體重、運動頻率與目標，自動生成個人化的飲食菜單與訓練優化建議。
* **智慧問答**：使用者可針對健身問題進行提問，App 會即時回傳專業建議。

### ☁️ 雲端數據同步 (Cloud Sync)
* **Firebase Firestore**：實作即時資料庫 (Real-time Database)，確保使用者的訓練紀錄、體重變化能即時備份至雲端。
* **跨裝置支援**：無論更換手機或重新安裝，登入後數據自動還原，無縫接軌。

### 🔐 安全會員系統 (User Auth)
* **Firebase Authentication**：整合 Google Sign-In 與 Email 註冊登入功能，提供金融級別的資安保護。
* **權限管理**：確保每位使用者的資料隱私，防止未經授權的存取。

### 📊 視覺化圖表與追蹤 (Analytics)
* **進度追蹤**：將每日訓練量與體重變化繪製成圖表，讓進步看得見。
* **歷史紀錄**：完整的日曆視圖，方便回顧過去的訓練細節。

### 🛠️ 其他實務功能
* **訂閱制整合 (RevenueCat)**：實作應用程式內購 (IAP) 功能，區分免費與付費會員權限。
* **廣告變現 (AdMob)**：整合 Google 廣告播送機制。
* **圖片載入優化 (Coil)**：使用現代化圖片載入庫，確保介面流暢不卡頓。

---

## 📱 應用程式截圖 (Screenshots)

| 登入畫面 | 主頁面 | AI 建議 | 食物辨識 |
|:---:|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/d021bb88-4b15-448e-88a9-38e41746dc3b" width="220"/> | <img src="https://github.com/user-attachments/assets/a560622a-4ac6-4a0f-b7a7-5bbd276336b0" width="220"/> | <img src="https://github.com/user-attachments/assets/735fce5f-9ad3-42ce-87b2-b83c61f81dc9" width="220"/> | <img src="https://github.com/user-attachments/assets/877bc061-dafb-4969-a7a4-31bf2236173a](https://github.com/user-attachments/assets/ea3f1fc2-f31d-4d08-beaa-0cca89184d0a" width="220"/> 

---

## 🛠️ 技術堆疊 (Tech Stack)

* **語言 (Languages)**: Kotlin
* **開發工具 (IDE)**: Android Studio Ladybug
* **架構 (Architecture)**: MVVM / Object-Oriented Design (OOD)
* **後端服務 (Backend)**: Google Firebase (Auth, Firestore, Storage)
* **AI 模型 (AI Model)**: Google Gemini Pro API
* **關鍵函式庫 (Libraries)**:
    * `Retrofit / OkHttp`: 網路請求處理
    * `Coil`: 異步圖片加載
    * `MPAndroidChart`: 數據圖表繪製
    * `RevenueCat`: 訂閱與金流管理
* **版本控制 (Version Control)**: Git & GitHub

## 📬 聯絡作者 (Contact)
* **Developer**: Sid (陳佑軒)
* **Email**: 513210271@m365.fju.edu.tw
* **Role**: 輔仁大學軟體工程學系 | 皇輝科技 網管工程師

---
*感謝您的閱讀！如果您對這個專案感興趣，歡迎查看原始碼或與我聯繫。*
