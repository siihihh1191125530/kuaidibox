package com.example.expresshelper

import android.Manifest
import android.app.Application
import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expresshelper.core.database.AppDatabase
import com.example.expresshelper.core.util.KeywordsPreferences
import com.example.expresshelper.core.util.NotificationUtils
import com.example.expresshelper.data.sms.SmsEngine
import com.example.expresshelper.data.sms.parser.ExpressCodeParser
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SmsRecoveryTest {
    private lateinit var context: Application
    private lateinit var db: AppDatabase
    private lateinit var engine: SmsEngine
    private lateinit var provider: Inbox
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("sms_sync", 0).edit().clear().commit()
        shadowOf(context).grantPermissions(Manifest.permission.READ_SMS)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        engine = SmsEngine(context, db.expressDao(), ExpressCodeParser(KeywordsPreferences(context)), NotificationUtils(context))
        provider = Inbox()
        ShadowContentResolver.registerProviderInternal("sms", provider)
    }
    @After fun close() { db.close() }
    @Test fun repeatedScansOnlyImportOnce() = runBlocking {
        assertEquals(1, engine.scan())
        assertEquals(0, engine.scan())
        assertEquals(1, db.expressDao().getAllExpressForWidget().size)
        assertTrue(engine.status().contains("新增 0 条"))
    }
    @Test fun permissionRevocationSkipsProvider() = runBlocking {
        shadowOf(context).denyPermissions(Manifest.permission.READ_SMS)
        assertEquals(0, engine.scan())
        assertEquals(0, provider.queries)
    }
    @Test fun failureDoesNotAdvanceCheckpoint() = runBlocking {
        val prefs = context.getSharedPreferences("sms_sync", 0)
        prefs.edit().putLong("checkpoint", 123).commit()
        provider.fail = true
        try { engine.scan(); fail("Expected provider failure") } catch (_: IllegalStateException) { }
        assertEquals(123L, prefs.getLong("checkpoint", 0))
        assertTrue(engine.status().contains("未完成"))
    }
    @Test fun clockRollbackStillScansRecentHistory() = runBlocking {
        context.getSharedPreferences("sms_sync", 0).edit().putLong("checkpoint", System.currentTimeMillis() + 86_400_000).commit()
        assertEquals(1, engine.scan())
        assertTrue(provider.start < provider.end)
    }
    class Inbox : ContentProvider() {
        var fail = false
        var queries = 0
        var start = 0L
        var end = 0L
        val received = System.currentTimeMillis() - 1000
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
            queries++
            if (fail) error("simulated failure")
            start = args!![0].toLong(); end = args[1].toLong()
            return MatrixCursor(arrayOf("address", "body", "date")).apply {
                if (received in start..end) {
                    addRow(arrayOf("106", "【丰巢】取件码：678901", received))
                    addRow(arrayOf("106", "验证码123456", received))
                }
            }
        }
        override fun onCreate() = true
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
    }
}
