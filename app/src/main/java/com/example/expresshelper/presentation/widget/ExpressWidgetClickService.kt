package com.example.expresshelper.presentation.widget

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.expresshelper.domain.model.ExpressStatus
import com.example.expresshelper.domain.repository.ExpressRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 小组件点击处理服务
 */
@AndroidEntryPoint
class ExpressWidgetClickService : Service() {

    @Inject
    lateinit var expressRepository: ExpressRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ExpressWidgetProvider.ACTION_TOGGLE -> {
                val expressId = intent.getLongExtra("express_id", -1)
                if (expressId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val express = expressRepository.getExpressById(expressId)
                        if (express != null) {
                            when (express.status) {
                                ExpressStatus.PENDING -> expressRepository.markAsPicked(expressId)
                                ExpressStatus.PICKED -> expressRepository.markAsPending(expressId)
                                ExpressStatus.EXPIRED -> { /* ignore */ }
                            }
                        }
                        WidgetUpdater.update(this@ExpressWidgetClickService)
                        stopSelf(startId)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }
}