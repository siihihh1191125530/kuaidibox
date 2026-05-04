package com.example.expresshelper.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.expresshelper.core.util.NotificationUtils
import com.example.expresshelper.data.sms.parser.ExpressCodeParser
import com.example.expresshelper.domain.repository.ExpressRepository
import com.example.expresshelper.presentation.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 短信通知监听服务
 */
@AndroidEntryPoint
class ExpressNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var expressRepository: ExpressRepository

    @Inject
    lateinit var expressCodeParser: ExpressCodeParser

    @Inject
    lateinit var notificationUtils: NotificationUtils

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            if (title.isEmpty() && text.isEmpty()) return

            val content = "$title $text"

            CoroutineScope(Dispatchers.IO).launch {
                processNotification(content, sbn.postTime)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理通知失败", e)
        }
    }

    private suspend fun processNotification(content: String, timestamp: Long) {
        try {
            val expressInfo = expressCodeParser.parseSms("", content, timestamp)

            if (expressInfo != null) {
                expressRepository.deleteDuplicateExpress()
                expressRepository.insertExpress(expressInfo)
                WidgetUpdater.update(this@ExpressNotificationListenerService)
                notificationUtils.showExpressNotification(expressInfo.code, expressInfo.source)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理短信通知失败", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    companion object {
        private const val TAG = "ExpressNotifListener"
    }
}