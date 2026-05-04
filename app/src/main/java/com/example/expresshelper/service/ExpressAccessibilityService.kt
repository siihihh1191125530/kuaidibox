package com.example.expresshelper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
 * 无障碍服务
 * 通过辅助功能服务检测通知内容，获取快递短信
 */
@AndroidEntryPoint
class ExpressAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var expressRepository: ExpressRepository

    @Inject
    lateinit var expressCodeParser: ExpressCodeParser

    @Inject
    lateinit var notificationUtils: NotificationUtils

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "无障碍服务已连接")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            notificationTimeout = 100
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 过滤掉来自自己应用的窗口事件（可能是对话框）
        if (event.packageName == packageName) return

        val content = event.text?.joinToString(" ") ?: ""
        if (content.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                processContent(content, System.currentTimeMillis())
            }
        }
    }

    private suspend fun processContent(content: String, timestamp: Long) {
        try {
            // 清理7天前的已取快递
            val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            expressRepository.deletePickedBefore(sevenDaysAgo)

            val expressInfo = expressCodeParser.parseSms("", content, timestamp)

            if (expressInfo != null) {
                val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                val exists = expressRepository.existsPendingByCode(expressInfo.code, oneDayAgo)

                if (!exists) {
                    expressRepository.insertExpress(expressInfo)
                    WidgetUpdater.update(this@ExpressAccessibilityService)
                    notificationUtils.showExpressNotification(expressInfo.code, expressInfo.source)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理通知失败", e)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务中断")
    }

    companion object {
        private const val TAG = "ExpressAccessService"
    }
}