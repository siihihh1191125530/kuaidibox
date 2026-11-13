package com.example.expresshelper

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expresshelper.core.database.AppDatabase
import com.example.expresshelper.data.local.entity.ExpressEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class DatabaseSafetyTest {
    @Test fun differentSendersDoNotSuppressEachOther() = runBlocking {
        val dao = db.expressDao()
        assertTrue(dao.ingest(item(), "first") > 0)
        assertTrue(dao.ingest(item().copy(smsSender = "107"), "second") > 0)
        assertEquals(2, dao.getAllExpressForWidget().size)
    }
    @Test fun deletingLegacyRecordsCannotResurrectThem() = runBlocking {
        val dao = db.expressDao()
        val original = item()
        val key = com.example.expresshelper.data.sms.SmsFingerprint.of(original.smsSender, original.smsBody, original.code)
        val id = dao.insertExpress(original)
        dao.deleteById(id)
        assertEquals(-1L, dao.ingest(original, key))
    }
    @Test fun bulkCleanupAlsoProtectsLegacyRecords() = runBlocking {
        val dao = db.expressDao()
        val original = item()
        val key = com.example.expresshelper.data.sms.SmsFingerprint.of(original.smsSender, original.smsBody, original.code)
        val id = dao.insertExpress(original)
        dao.markAsPicked(id, 100)
        dao.deletePickedBefore(200)
        assertEquals(-1L, dao.ingest(original, key))
        val second = item("9999", "取件码9999")
        dao.insertExpress(second)
        dao.deleteAll()
        assertEquals(-1L, dao.ingest(second, com.example.expresshelper.data.sms.SmsFingerprint.of(second.smsSender, second.smsBody, second.code)))
    }
    @Test fun concurrentInputsProduceOneRecord() = runBlocking {
        val dao = db.expressDao()
        kotlinx.coroutines.coroutineScope {
            repeat(20) { launch(kotlinx.coroutines.Dispatchers.IO) { dao.ingest(item(), "same") } }
        }
        assertEquals(1, dao.getAllExpressForWidget().size)
    }
    private lateinit var db: AppDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() { db.close() }
    private fun item(code: String = "1234", body: String = "取件码1234", time: Long = 1_000_000) =
        ExpressEntity(code = code, source = "菜鸟", smsSender = "106", smsBody = body, receivedTime = time)
    @Test fun duplicateCleanupPreservesUniqueParcels() = runBlocking {
        val dao = db.expressDao()
        dao.insertExpress(item()); dao.insertExpress(item()); dao.insertExpress(item("9999", "取件码9999"))
        dao.deleteDuplicateExpress()
        assertEquals(2, dao.getAllExpressForWidget().size)
        dao.deleteDuplicateExpress()
        assertEquals(2, dao.getAllExpressForWidget().size)
    }
    @Test fun recoveryDoesNotResurrectDeletedOrPicked() = runBlocking {
        val dao = db.expressDao()
        val id = dao.ingest(item(), "hash")
        dao.markAsPicked(id)
        assertEquals(-1L, dao.ingest(item(time = 1_001_000), "hash"))
        assertEquals(1, dao.getExpressById(id)!!.status)
        dao.deleteById(id)
        assertEquals(-1L, dao.ingest(item(), "hash"))
        assertTrue(dao.getAllExpressForWidget().isEmpty())
        assertTrue(dao.ingest(item(time = 90_000_000), "hash") > 0)
    }
    @Test fun oldDataAndDifferentParcelsArePreserved() = runBlocking {
        val dao = db.expressDao()
        dao.insertExpress(item())
        assertEquals(-1L, dao.ingest(item(), "hash"))
        assertTrue(dao.ingest(item(body = "丰巢取件码1234"), "different") > 0)
        assertEquals(2, dao.getAllExpressForWidget().size)
    }
}
