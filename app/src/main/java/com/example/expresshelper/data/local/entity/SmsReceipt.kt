package com.example.expresshelper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Kept when a parcel is deleted so recovery cannot resurrect it. Contains no SMS text. */
@Entity(tableName = "sms_receipts", indices = [Index(value = ["contentKey", "receivedTime"])])
data class SmsReceipt(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentKey: String, val receivedTime: Long)
