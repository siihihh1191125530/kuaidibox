package com.example.expresshelper.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * 小组件更新工具类
 */
object WidgetUpdater {

    /**
     * 更新所有小组件
     */
    fun update(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ExpressWidgetProvider::class.java))

        if (widgetIds.isEmpty()) return

        // 发送广播更新所有小组件
        val intent = Intent(context, ExpressWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        }
        context.sendBroadcast(intent)
    }
}