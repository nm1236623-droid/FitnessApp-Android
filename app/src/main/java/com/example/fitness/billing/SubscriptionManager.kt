package com.example.fitness.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.fitness.coach.UserRole
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration // ★ 新版設定類別
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchaseParams // ★ 新版購買參數
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SubscriptionManager {
    // ★ 權限 ID：教練專屬功能需要此訂閱
    private const val ENTITLEMENT_ID = "premium_coach"

    // ★ 學員免費功能權限
    private const val TRAINEE_BASIC_FEATURES = "trainee_basic"

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _isTraineeBasic = MutableStateFlow(true) // 學員預設可使用基礎功能
    val isTraineeBasic: StateFlow<Boolean> = _isTraineeBasic.asStateFlow()

    fun initialize(context: Context, apiKey: String, userId: String?) {
        Purchases.debugLogsEnabled = true

        // ★★★ 修正 1: 使用 PurchasesConfiguration ★★★
        val builder = PurchasesConfiguration.Builder(context, apiKey)
        if (userId != null) {
            builder.appUserID(userId)
        }
        Purchases.configure(builder.build())

        checkSubscriptionStatus()
    }

    fun checkSubscriptionStatus() {
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                // 檢查教練專屬功能權限
                val isActive = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                _isPro.value = isActive

                // 檢查學員基礎功能（通常免費或包含在任何方案中）
                val hasTraineeBasic = customerInfo.entitlements[TRAINEE_BASIC_FEATURES]?.isActive == true
                _isTraineeBasic.value = hasTraineeBasic

                Log.d("Subscription", "User is Pro: $isActive, Trainee Basic: $hasTraineeBasic")
            }

            override fun onError(error: PurchasesError) {
                Log.e("Subscription", "Check status failed: ${error.message}")
                // 錯誤時給予基礎權限
                _isTraineeBasic.value = true
            }
        })
    }

    /**
     * 檢查使用者是否有權限使用特定功能
     * @param userRole 使用者角色（學員/教練）
     * @param isCoachFeature 是否為教練專屬功能
     * @return true 表示有權限使用
     */
    fun hasAccessToFeature(userRole: UserRole, isCoachFeature: Boolean): Boolean {
        return when {
            // 教練功能需要訂閱
            isCoachFeature && userRole == UserRole.COACH -> _isPro.value
            // 學員基礎功能免費
            !isCoachFeature && userRole == UserRole.TRAINEE -> _isTraineeBasic.value
            // 其他情況不允許
            else -> false
        }
    }

    /**
     * 檢查教練是否需要訂閱才能使用進階功能
     */
    fun shouldShowPaywall(userRole: UserRole): Boolean {
        return userRole == UserRole.COACH && !_isPro.value
    }

    // 取得商品列表
    fun getOfferings(onResult: (List<Package>) -> Unit) {
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                val packages = offerings.current?.availablePackages ?: emptyList()
                onResult(packages)
            }

            override fun onError(error: PurchasesError) {
                Log.e("Subscription", "Get offerings failed: ${error.message}")
                onResult(emptyList())
            }
        })
    }

    // ★★★ 修正 2: 購買邏輯與 Callback ★★★
    fun purchase(activity: Activity, packageToBuy: Package, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // 建立購買參數
        val params = PurchaseParams.Builder(activity, packageToBuy).build()

        Purchases.sharedInstance.purchase(params, object : PurchaseCallback {
            override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                if (customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true) {
                    _isPro.value = true
                    onSuccess()
                }
            }

            override fun onError(error: PurchasesError, userCancelled: Boolean) {
                if (!userCancelled) {
                    onError(error.message)
                }
            }
        })
    }
}