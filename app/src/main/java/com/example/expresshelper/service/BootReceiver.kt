package com.example.expresshelper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.expresshelper.presentation.widget.WidgetUpdater

/**
 * 开机启动接收器
 * 用于在设备重启后更新小组件
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "设备启动完成，更新小组件")
            WidgetUpdater.update(context)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
