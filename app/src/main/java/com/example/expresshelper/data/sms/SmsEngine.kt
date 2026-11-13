package com.example.expresshelper.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.expresshelper.data.local.dao.ExpressDao
import com.example.expresshelper.data.local.entity.ExpressEntity
import com.example.expresshelper.data.sms.parser.ExpressCodeParser
import com.example.expresshelper.core.util.NotificationUtils
import com.example.expresshelper.presentation.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ExpressDao,
    private val parser: ExpressCodeParser,
    private val notifications: NotificationUtils
) {
    private val prefs = context.getSharedPreferences("sms_sync", Context.MODE_PRIVATE)
    private val scanLock = Mutex()
    fun has(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    fun status(): String = prefs.getString("status", "尚未补查短信")!!

    suspend fun ingest(sender: String, body: String, time: Long, notify: Boolean): Int = withContext(Dispatchers.IO) {
        var added = 0
        for (item in parser.parseAll(sender, body, time)) {
            val digest = SmsFingerprint.of(sender, body, item.code)
            val id = dao.ingest(ExpressEntity(code = item.code, source = item.source,
                location = item.location, smsSender = sender, smsBody = body, receivedTime = time), digest)
            if (id > 0) {
                added++
                if (notify) notifications.showExpressNotification(item.code, item.source)
            }
        }
        if (added > 0) WidgetUpdater.update(context)
        added
    }

    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        scanLock.withLock {
            if (!has(Manifest.permission.READ_SMS)) return@withLock 0
            val end = System.currentTimeMillis()
            // First grant imports at most seven days. Later scans overlap to tolerate delayed provider writes.
            val checkpoint = prefs.getLong("checkpoint", end - 7 * 86_400_000L)
            // Clock corrections must not make the query window empty until the old time catches up.
            val start = maxOf(0L, (if (checkpoint > end) end - 7 * 86_400_000L else checkpoint) - 300_000L)
            var added = 0
            try {
                val cursor = context.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf("address", "body", "date"), "date >= ? AND date <= ?",
                    arrayOf(start.toString(), end.toString()), "date ASC")
                    ?: error("短信库暂时不可用")
                cursor.use {
                    while (it.moveToNext()) {
                        added += ingest(it.getString(0).orEmpty(), it.getString(1).orEmpty(), it.getLong(2), false)
                    }
                }
                val stamp = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(end))
                prefs.edit().putLong("checkpoint", end).putString("status", "$stamp 补查完成，新增 $added 条").commit()
                added
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                prefs.edit().putString("status", "补查未完成，请检查短信权限后重试").apply()
                throw e
            }
        }
    }
}
