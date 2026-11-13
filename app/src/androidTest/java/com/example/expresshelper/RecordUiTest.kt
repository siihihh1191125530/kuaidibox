package com.example.expresshelper

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.expresshelper.data.local.entity.ExpressEntity
import com.example.expresshelper.presentation.main.MainActivity
import com.example.expresshelper.presentation.manage.ManageExpressActivity
import com.example.expresshelper.presentation.widget.ExpressWidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordUiTest {
    @Test fun copyPickRestoreAndDelete() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val dao = EntryPointAccessors.fromApplication(context, ExpressWidgetEntryPoint::class.java).expressDao()
        val code = "QA-UI-358"
        val id = dao.insertExpress(ExpressEntity(code = code, source = "测试驿站", smsSender = "UI-QA",
            smsBody = "取件码$code", receivedTime = System.currentTimeMillis() + 10_000))
        fun awaitText() {
            val deadline = System.currentTimeMillis() + 5000
            while (true) {
                try { onView(withText(code)).check(matches(isDisplayed())); return }
                catch (e: Throwable) { if (System.currentTimeMillis() > deadline) throw e; Thread.sleep(100) }
            }
        }
        suspend fun awaitStatus(expected: Int?) {
            val deadline = System.currentTimeMillis() + 5000
            while (dao.getExpressById(id)?.status != expected && System.currentTimeMillis() < deadline) Thread.sleep(100)
            assertEquals(expected, dao.getExpressById(id)?.status)
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                awaitText()
                onView(withText(code)).perform(click())
                instrumentation.runOnMainSync {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    assertEquals(code, clipboard.primaryClip!!.getItemAt(0).text.toString())
                }
                onView(allOf(withId(R.id.btnMarkPicked), isDescendantOfA(withChild(withText(code))))).perform(click())
                onView(withText("确定")).perform(click())
                awaitStatus(1)
            }
            ActivityScenario.launch(ManageExpressActivity::class.java).use {
                onView(withId(R.id.tabPicked)).perform(click())
                awaitText()
                onView(allOf(withId(R.id.btnToggle), isDescendantOfA(withChild(withText(code))))).perform(click())
                awaitStatus(0)
                onView(withId(R.id.tabPending)).perform(click())
                awaitText()
                onView(withText(code)).perform(longClick())
                onView(withText("删除")).perform(click())
                awaitStatus(null)
            }
        } finally { dao.deleteById(id) }
    }
}
