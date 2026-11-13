package com.example.expresshelper

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.expresshelper.service.SmsEntryPoint
import com.example.expresshelper.presentation.widget.ExpressWidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmsIntegrationTest {
    @Test fun receiveParsePersistDeduplicateAndDelete() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = EntryPointAccessors.fromApplication(context, SmsEntryPoint::class.java).engine()
        val dao = EntryPointAccessors.fromApplication(context, ExpressWidgetEntryPoint::class.java).expressDao()
        val time = System.currentTimeMillis()
        val body = "【菜鸟驿站】测试包裹已到，取件码：A-12-3456。地址：花园小区东门。"
        assertEquals(1, engine.ingest("TEST-$time", body, time, false))
        assertEquals(0, engine.ingest("TEST-$time", body, time, false))
        val row = dao.getAllExpressForWidget().first { it.smsSender == "TEST-$time" }
        assertEquals("A-12-3456", row.code)
        assertEquals("花园小区东门", row.location)
        dao.markAsPicked(row.id)
        assertEquals(0, engine.ingest("TEST-$time", body, time + 1000, false))
        assertEquals(1, dao.getExpressById(row.id)!!.status)
        dao.deleteById(row.id)
        assertEquals(0, engine.ingest("TEST-$time", body, time, false))
        assertNull(dao.getExpressById(row.id))
        assertEquals(0, engine.ingest("TEST", "验证码 123456，请勿泄露", time, false))
    }
}
