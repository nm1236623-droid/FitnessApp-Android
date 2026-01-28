package com.example.fitness.booking

import java.time.Instant
import java.time.LocalDateTime

/**
 * 課程類型
 */
enum class CourseType {
    ONLINE,     // 線上課程
    OFFLINE,    // 線下課程
    HYBRID      // 混合模式
}

/**
 * 課程狀態
 */
enum class CourseStatus {
    SCHEDULED,  // 已排程
    ONGOING,    // 進行中
    COMPLETED,  // 已完成
    CANCELLED   // 已取消
}

/**
 * 預約狀態
 */
enum class BookingStatus {
    PENDING,    // 待確認
    CONFIRMED,  // 已確認
    CANCELLED,  // 已取消
    COMPLETED   // 已完成
}

/**
 * 課程模型
 */
data class Course(
    val id: String,
    val coachId: String,
    val coachName: String,
    val title: String,
    val description: String,
    val type: CourseType,
    val duration: Int,          // 分鐘
    val maxParticipants: Int,
    val currentParticipants: Int = 0,
    val price: Double,
    val currency: String = "TWD",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String? = null,
    val onlineLink: String? = null,
    val status: CourseStatus = CourseStatus.SCHEDULED,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val createdAt: Instant = Instant.now()
)

/**
 * 預約記錄
 */
data class Booking(
    val id: String,
    val courseId: String,
    val userId: String,
    val userName: String,
    val userEmail: String? = null,
    val userPhone: String? = null,
    val status: BookingStatus = BookingStatus.PENDING,
    val notes: String? = null,
    val bookedAt: Instant = Instant.now(),
    val confirmedAt: Instant? = null,
    val cancelledAt: Instant? = null,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val amount: Double = 0.0
)

/**
 * 付款狀態
 */
enum class PaymentStatus {
    PENDING,    // 待付款
    PAID,       // 已付款
    REFUNDED,   // 已退款
    FAILED      // 付款失敗
}

/**
 * 教練排程時段
 */
data class CoachSchedule(
    val id: String,
    val coachId: String,
    val dayOfWeek: Int,         // 1-7 (Monday-Sunday)
    val startTime: String,      // HH:mm format
    val endTime: String,        // HH:mm format
    val isAvailable: Boolean = true,
    val recurrent: Boolean = true
)
