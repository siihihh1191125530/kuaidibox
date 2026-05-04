package com.example.expresshelper.domain.model

data class ExpressInfo(
    val id: Long = 0,
    val code: String,
    val source: String,
    val location: String? = null,
    val smsSender: String,
    val smsBody: String,
    val receivedTime: Long,
    val status: ExpressStatus = ExpressStatus.PENDING,
    val pickedTime: Long? = null
)

enum class ExpressStatus {
    PENDING,
    PICKED,
    EXPIRED
}
