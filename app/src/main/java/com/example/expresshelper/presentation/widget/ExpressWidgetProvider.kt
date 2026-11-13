package com.example.expresshelper.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.expresshelper.R
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class ExpressWidgetProvider : AppWidgetProvider() {

    companion object {
        private val updateLock = Mutex()
        const val ACTION_NEXT = "com.example.expresshelper.ACTION_NEXT"
        const val ACTION_TOGGLE = "com.example.expresshelper.ACTION_TOGGLE"
        const val EXTRA_CURRENT_INDEX = "current_index"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val actions = setOf(AppWidgetManager.ACTION_APPWIDGET_UPDATE, ACTION_NEXT, ACTION_TOGGLE,
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED)
        if (intent.action !in actions) { super.onReceive(context, intent); return }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000) { updateLock.withLock {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, ExpressWidgetProvider::class.java))
                val selected = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (intent.action == ACTION_TOGGLE) {
                    val dao = EntryPointAccessors.fromApplication(context.applicationContext, ExpressWidgetEntryPoint::class.java).expressDao()
                    val id = intent.getLongExtra("express_id", -1)
                    dao.getExpressById(id)?.let { if (it.status == 0) dao.markAsPicked(id) else dao.markAsPending(id) }
                }
                val prefs = context.getSharedPreferences("widget_pages", Context.MODE_PRIVATE)
                for (id in ids) {
                    val index = if (id == selected && intent.action == ACTION_NEXT) intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
                        else prefs.getInt("page_$id", 0)
                    prefs.edit().putInt("page_$id", index).apply()
                    updateAppWidget(context, manager, id, index)
                }
                } }
            } catch (_: Exception) {
                android.util.Log.w("ExpressWidget", "Widget update failed")
            } finally { pending.finish() }
        }
    }

    private suspend fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        currentIndex: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_express_list)

        // 获取小组件快递数据（待取 + 1天内已取）
        val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val widgetList = try {
            run {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ExpressWidgetEntryPoint::class.java
                )
                entryPoint.expressDao().getForWidgetWithRetention(oneDayAgo)
            }
        } catch (e: Exception) {
            emptyList()
        }

        if (widgetList.isEmpty()) {
            // 隐藏指示器，显示暂无待取
            views.setViewVisibility(R.id.indicator_container, View.GONE)
            views.setTextViewText(R.id.tv_code, "暂无待取")
            views.setTextColor(R.id.tv_code, android.graphics.Color.parseColor("#4CAF50"))
            views.setTextViewText(R.id.tv_counter, "")
            views.setTextViewText(R.id.tv_time, "")
            views.setTextViewText(R.id.tv_station, "")
        } else {
            val displayIndex = currentIndex.coerceAtLeast(0) % widgetList.size
            val item = widgetList[displayIndex]
            val nextIndex = (displayIndex + 1) % widgetList.size

            // 显示指示器容器
            views.setViewVisibility(R.id.indicator_container, View.VISIBLE)

            // 显示数据
            views.setTextViewText(R.id.tv_code, item.code)
            views.setTextColor(R.id.tv_code, android.graphics.Color.parseColor("#000000"))
            views.setTextViewText(R.id.tv_counter, "${displayIndex + 1}/${widgetList.size}")

            // 显示时间
            val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.receivedTime))
            views.setTextViewText(R.id.tv_time, timeStr)

            // 显示驿站名称/快递公司
            views.setTextViewText(R.id.tv_station, item.source)

            // 根据状态显示指示器颜色
            val indicatorRes = if (item.status == 0) {
                R.drawable.indicator_circle_red
            } else {
                R.drawable.indicator_circle_green
            }
            views.setInt(R.id.indicator, "setImageResource", indicatorRes)

            // 点击取件码 - 切换下一条
            val nextIntent = Intent(context, ExpressWidgetProvider::class.java).apply {
                action = ACTION_NEXT
                putExtra(EXTRA_CURRENT_INDEX, nextIndex)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_code, nextPendingIntent)

            // 点击圆形指示器 - 切换状态
            val toggleIntent = Intent(context, ExpressWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
                putExtra("express_id", item.id)
                putExtra(EXTRA_CURRENT_INDEX, displayIndex)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 1,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.indicator_container, togglePendingIntent)
        }

        val open = PendingIntent.getActivity(context, appWidgetId,
            Intent(context, com.example.expresshelper.presentation.main.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.tv_station, open)
        if (widgetList.isEmpty()) views.setOnClickPendingIntent(R.id.tv_code, open)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExpressWidgetEntryPoint {
    fun expressDao(): com.example.expresshelper.data.local.dao.ExpressDao
}