package com.example.expresshelper

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.expresshelper.presentation.widget.ExpressWidgetEntryPoint
import com.example.expresshelper.presentation.widget.ExpressWidgetProvider
import com.example.expresshelper.presentation.widget.WidgetUpdater
import com.example.expresshelper.data.local.entity.ExpressEntity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real system widget host, RemoteViews and PendingIntent interaction; no launcher dependencies. */
@RunWith(AndroidJUnit4::class)
class WidgetIntegrationTest {
    @Test fun twoWidgetsFlipIndependentlyAndToggle() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, ExpressWidgetProvider::class.java)
        val dao = EntryPointAccessors.fromApplication(context, ExpressWidgetEntryPoint::class.java).expressDao()
        val now = System.currentTimeMillis() + 10_000
        val first = dao.insertExpress(ExpressEntity(code = "QA-101", source = "测试", smsSender = "WIDGET-QA", smsBody = "test", receivedTime = now))
        val second = dao.insertExpress(ExpressEntity(code = "QA-102", source = "测试", smsSender = "WIDGET-QA", smsBody = "test2", receivedTime = now - 1))
        var host: AppWidgetHost? = null
        lateinit var one: AppWidgetHostView
        lateinit var two: AppWidgetHostView
        try {
            instrumentation.runOnMainSync {
                host = AppWidgetHost(context, 20777)
                host!!.startListening()
                fun create(): AppWidgetHostView {
                    val id = host!!.allocateAppWidgetId()
                    assertTrue("Grant widget bind access to test target before running", manager.bindAppWidgetIdIfAllowed(id, component))
                    return host!!.createView(context, id, manager.getAppWidgetInfo(id))
                }
                one = create(); two = create()
            }
            WidgetUpdater.update(context)
            fun code(view: AppWidgetHostView): String {
                var result = ""
                instrumentation.runOnMainSync { result = view.findViewById<TextView>(R.id.tv_code)?.text?.toString().orEmpty() }
                return result
            }
            fun awaitCode(view: AppWidgetHostView, expected: String) {
                val deadline = System.currentTimeMillis() + 8000
                while (code(view) != expected && System.currentTimeMillis() < deadline) Thread.sleep(100)
                assertEquals(expected, code(view))
            }
            awaitCode(one, "QA-101"); awaitCode(two, "QA-101")
            instrumentation.runOnMainSync { one.findViewById<TextView>(R.id.tv_code).performClick() }
            awaitCode(one, "QA-102"); assertEquals("QA-101", code(two))
            instrumentation.runOnMainSync { one.findViewById<android.view.View>(R.id.indicator_container).performClick() }
            val deadline = System.currentTimeMillis() + 8000
            while (dao.getExpressById(second)!!.status != 1 && System.currentTimeMillis() < deadline) Thread.sleep(100)
            assertEquals(1, dao.getExpressById(second)!!.status)
            assertEquals(0, dao.getExpressById(first)!!.status)
        } finally {
            instrumentation.runOnMainSync { host?.stopListening(); host?.deleteHost() }
            dao.deleteById(first); dao.deleteById(second)
            WidgetUpdater.update(context)
        }
    }
}
