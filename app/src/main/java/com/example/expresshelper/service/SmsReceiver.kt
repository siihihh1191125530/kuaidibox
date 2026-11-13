package com.example.expresshelper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.expresshelper.data.sms.SmsEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    @Inject lateinit var engine: SmsEngine
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_000) {
                    messages.groupBy { it.originatingAddress.orEmpty() }.forEach { (sender, parts) ->
                        engine.ingest(sender, parts.joinToString("") { it.messageBody.orEmpty() },
                            System.currentTimeMillis(), true)
                    }
                }
            } catch (_: Exception) {
                // Inbox recovery retries without logging private SMS content.
                SmsSyncWorker.enqueue(context)
            } finally { pending.finish() }
        }
    }
}
