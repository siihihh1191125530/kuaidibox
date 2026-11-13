package com.example.expresshelper.data.local.dao

import androidx.room.*
import com.example.expresshelper.data.local.entity.ExpressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpressDao {

    // 获取所有待取快递（Flow响应式）- status = 0 表示待取
    @Query("SELECT * FROM express_info WHERE status = 0 ORDER BY received_time DESC")
    fun getPendingExpress(): Flow<List<ExpressEntity>>

    // 获取所有快递（分页）
    @Query("SELECT * FROM express_info ORDER BY received_time DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllExpress(limit: Int, offset: Int = 0): List<ExpressEntity>

    // 获取所有快递（用于管理界面，Flow响应式）
    @Query("SELECT * FROM express_info ORDER BY received_time DESC")
    fun getAllExpressForManage(): Flow<List<ExpressEntity>>

    // 根据ID获取快递
    @Query("SELECT * FROM express_info WHERE id = :id")
    suspend fun getExpressById(id: Long): ExpressEntity?

    // 插入快递信息
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpress(express: ExpressEntity): Long

    // 批量插入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(expressList: List<ExpressEntity>)

    // 标记已取 - status = 1 表示已取
    @Query("UPDATE express_info SET status = 1, picked_time = :pickedTime WHERE id = :id")
    suspend fun markAsPicked(id: Long, pickedTime: Long = System.currentTimeMillis())

    // 标记未取 - status = 0 表示未取
    @Query("UPDATE express_info SET status = 0, picked_time = NULL WHERE id = :id")
    suspend fun markAsPending(id: Long)

    // 删除快递
    @Delete
    suspend fun deleteExpress(express: ExpressEntity)

    // 根据ID删除
    @Query("DELETE FROM express_info WHERE id = :id")
    suspend fun deleteByIdRaw(id: Long)

    @Transaction
    suspend fun deleteById(id: Long) {
        getExpressById(id)?.let { remember(it) }
        deleteByIdRaw(id)
    }

    suspend fun remember(item: ExpressEntity) {
        val key = com.example.expresshelper.data.sms.SmsFingerprint.of(item.smsSender, item.smsBody, item.code)
        if (!hasReceipt(key, item.receivedTime - 120_000, item.receivedTime + 120_000)) {
            insertReceipt(com.example.expresshelper.data.local.entity.SmsReceipt(contentKey = key, receivedTime = item.receivedTime))
        }
    }

    // 删除已取快递（清理超过指定天数的记录）
    @Query("DELETE FROM express_info WHERE status = 1 AND picked_time < :beforeTime")
    suspend fun deletePickedBeforeRaw(beforeTime: Long)

    @Query("SELECT * FROM express_info WHERE status = 1 AND picked_time < :beforeTime")
    suspend fun pickedBefore(beforeTime: Long): List<ExpressEntity>

    @Transaction
    suspend fun deletePickedBefore(beforeTime: Long) {
        pickedBefore(beforeTime).forEach { remember(it) }
        deletePickedBeforeRaw(beforeTime)
    }

    // 获取待取数量
    @Query("SELECT COUNT(*) FROM express_info WHERE status = 0")
    fun getPendingCount(): Flow<Int>

    // 获取已取数量
    @Query("SELECT COUNT(*) FROM express_info WHERE status = 1")
    fun getPickedCount(): Flow<Int>

    // 检查是否已存在（防止重复：同一取件码12小时内）
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM express_info
            WHERE code = :code
            AND received_time > :afterTime
        )
    """)
    suspend fun existsPendingByCode(code: String, afterTime: Long): Boolean

    // 检查是否已存在（同一取件码，不限制时间）
    @Query("SELECT COUNT(*) FROM express_info WHERE code = :code")
    suspend fun countByCode(code: String): Int

    // Only exact duplicates. Never group unrelated parcels by pickup code alone.
    @Query("DELETE FROM express_info WHERE id NOT IN (SELECT MIN(id) FROM express_info GROUP BY code, sms_sender, sms_body, received_time, status)")
    suspend fun deleteDuplicateExpress()

    @Query("SELECT EXISTS(SELECT 1 FROM sms_receipts WHERE contentKey = :key AND receivedTime BETWEEN :start AND :end)")
    suspend fun hasReceipt(key: String, start: Long, end: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM express_info WHERE code = :code AND sms_body = :body AND sms_sender = :sender AND received_time BETWEEN :start AND :end)")
    suspend fun hasLegacy(code: String, body: String, sender: String, start: Long, end: Long): Boolean

    @Insert
    suspend fun insertReceipt(receipt: com.example.expresshelper.data.local.entity.SmsReceipt)

    @Transaction
    suspend fun ingest(entity: ExpressEntity, key: String): Long {
        val start = entity.receivedTime - 120_000L
        val end = entity.receivedTime + 120_000L
        if (hasReceipt(key, start, end)) return -1
        insertReceipt(com.example.expresshelper.data.local.entity.SmsReceipt(contentKey = key, receivedTime = entity.receivedTime))
        if (hasLegacy(entity.code, entity.smsBody, entity.smsSender, start, end)) return -1
        return insertExpress(entity)
    }

    // 获取所有快递（用于小组件，直接返回列表）
    @Query("SELECT * FROM express_info ORDER BY received_time DESC")
    suspend fun getAllExpressForWidget(): List<ExpressEntity>

    // 获取待取快递（用于小组件，只显示待取）
    @Query("SELECT * FROM express_info WHERE status = 0 ORDER BY received_time DESC")
    suspend fun getPendingForWidget(): List<ExpressEntity>

    // 获取小组件快递列表（待取 + 1天内已取）
    @Query("SELECT * FROM express_info WHERE status = 0 OR (status = 1 AND picked_time > :oneDayAgo) ORDER BY received_time DESC")
    suspend fun getForWidgetWithRetention(oneDayAgo: Long): List<ExpressEntity>

    // 清空所有数据
    @Query("DELETE FROM express_info")
    suspend fun deleteAllRaw()

    @Transaction
    suspend fun deleteAll() {
        getAllExpressForWidget().forEach { remember(it) }
        deleteAllRaw()
    }
}
