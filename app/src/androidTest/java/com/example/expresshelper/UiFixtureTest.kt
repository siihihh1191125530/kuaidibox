package com.example.expresshelper

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.expresshelper.service.SmsEntryPoint
import com.example.expresshelper.presentation.widget.ExpressWidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit opt-in fake data for screenshot review; never compiled into the app APK. */
@RunWith(AndroidJUnit4::class)
class UiFixtureTest {
    @Test fun fixtures() = runBlocking {
        val mode = InstrumentationRegistry.getArguments().getString("fixtures") ?: return@runBlocking
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dao = EntryPointAccessors.fromApplication(context, ExpressWidgetEntryPoint::class.java).expressDao()
        dao.getAllExpressForWidget().filter { it.smsSender == "UI-DEMO" }.forEach { dao.deleteById(it.id) }
        if (mode == "seed") {
            val engine = EntryPointAccessors.fromApplication(context, SmsEntryPoint::class.java).engine()
            val now = System.currentTimeMillis()
            engine.ingest("UI-DEMO", "【菜鸟驿站】包裹已到，取件码：A-12-3456。地址：花园小区东门。", now, false)
            engine.ingest("UI-DEMO", "【丰巢】快递已入柜，取件码：806291。地址：星河公寓一楼。", now - 3_600_000, false)
            engine.ingest("UI-DEMO", "【妈妈驿站】请凭取件码：3-2-086领取包裹。地址：中心路便利店。", now - 86_400_000, false)
        }
    }
}
