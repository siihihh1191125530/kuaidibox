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
    suspend fun deleteById(id: Long)

    // 删除已取快递（清理超过指定天数的记录）
    @Query("DELETE FROM express_info WHERE status = 1 AND picked_time < :beforeTime")
    suspend fun deletePickedBefore(beforeTime: Long)

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

    // 删除重复的快递记录（保留ID最小的那条）
    @Query("""
        DELETE FROM express_info
        WHERE id NOT IN (
            SELECT MIN(id) FROM express_info
            WHERE code IN (
                SELECT code FROM express_info
                GROUP BY code
                HAVING COUNT(*) > 1
            )
            GROUP BY code
        )
    """)
    suspend fun deleteDuplicateExpress()

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
    suspend fun deleteAll()
}
