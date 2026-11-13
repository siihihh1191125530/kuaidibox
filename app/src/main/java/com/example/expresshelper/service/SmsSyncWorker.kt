package com.example.expresshelper.service

import android.Manifest
import android.content.Context
import androidx.work.*
import com.example.expresshelper.data.sms.SmsEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit

class SmsSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val engine = EntryPointAccessors.fromApplication(applicationContext, SmsEntryPoint::class.java).engine()
        if (!engine.has(Manifest.permission.READ_SMS)) return Result.success()
        return try { engine.scan(); Result.success() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: SecurityException) { Result.success() }
        catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
    }
    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("sms-recovery", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SmsSyncWorker>(1, TimeUnit.HOURS).build())
        }
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("sms-recovery-now", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SmsSyncWorker>().setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build())
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsEntryPoint { fun engine(): SmsEngine }
