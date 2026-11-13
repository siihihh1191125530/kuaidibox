package com.example.expresshelper

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ExpressHelperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.expresshelper.service.SmsSyncWorker.schedule(this)
    }
}
