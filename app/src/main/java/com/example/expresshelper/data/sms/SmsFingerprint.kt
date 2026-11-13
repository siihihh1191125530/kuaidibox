package com.example.expresshelper.data.sms

import java.security.MessageDigest

object SmsFingerprint {
    fun of(sender: String, body: String, code: String): String = MessageDigest.getInstance("SHA-256")
        .digest("${sender.trim()}\u0000${body.trim()}\u0000$code".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
