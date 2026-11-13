package com.example.expresshelper

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expresshelper.core.database.AppDatabase
import com.example.expresshelper.core.di.DatabaseModule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class MigrationTest {
    @Test fun versionOneDataSurvivesUpgrade() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE express_info (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, code TEXT NOT NULL, source TEXT NOT NULL, location TEXT, sms_sender TEXT NOT NULL, sms_body TEXT NOT NULL, received_time INTEGER NOT NULL, status INTEGER NOT NULL, picked_time INTEGER, created_at INTEGER NOT NULL)")
            old.execSQL("CREATE INDEX index_express_info_code_received_time ON express_info(code, received_time)")
            old.execSQL("INSERT INTO express_info VALUES (7, 'A-123', '菜鸟', NULL, '106', '取件码A-123', 1000000, 1, 1000010, 1000000)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(DatabaseModule.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val saved = db.expressDao().getExpressById(7)!!
            assertEquals("A-123", saved.code)
            assertEquals(1, saved.status)
            assertEquals(1000010L, saved.pickedTime)
            assertEquals(-1L, db.expressDao().ingest(saved.copy(id = 0), "old"))
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
