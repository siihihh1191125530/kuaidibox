package com.example.expresshelper.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "express_info",
    indices = [Index(value = ["code", "received_time"], unique = false)]
)
data class ExpressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "code")
    val code: String,                    // 取件码

    @ColumnInfo(name = "source")
    val source: String,                  // 快递来源（菜鸟/丰巢等）

    @ColumnInfo(name = "location")
    val location: String? = null,        // 取件地点

    @ColumnInfo(name = "sms_sender")
    val smsSender: String,               // 短信发送者号码

    @ColumnInfo(name = "sms_body")
    val smsBody: String,                 // 短信原文

    @ColumnInfo(name = "received_time")
    val receivedTime: Long,              // 收到短信时间

    @ColumnInfo(name = "status")
    val status: Int = ExpressStatus.PENDING.value,  // 状态

    @ColumnInfo(name = "picked_time")
    val pickedTime: Long? = null,        // 标记已取时间

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

enum class ExpressStatus(val value: Int) {
    PENDING(0),      // 待取件
    PICKED(1),       // 已取件
    EXPIRED(2)       // 已过期
}
