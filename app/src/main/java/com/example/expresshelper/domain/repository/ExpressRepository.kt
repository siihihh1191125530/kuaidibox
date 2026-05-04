package com.example.expresshelper.domain.repository

import com.example.expresshelper.domain.model.ExpressInfo
import com.example.expresshelper.domain.model.ExpressStatus
import kotlinx.coroutines.flow.Flow

interface ExpressRepository {

    fun getPendingExpress(): Flow<List<ExpressInfo>>

    fun getAllExpressForManage(): Flow<List<ExpressInfo>>

    suspend fun getExpressById(id: Long): ExpressInfo?

    suspend fun insertExpress(express: ExpressInfo): Long

    suspend fun markAsPicked(id: Long)

    suspend fun markAsPending(id: Long)

    suspend fun deleteById(id: Long)

    fun getPendingCount(): Flow<Int>

    fun getPickedCount(): Flow<Int>

    suspend fun existsPendingByCode(code: String, afterTime: Long): Boolean

    suspend fun getPendingExpressList(): List<ExpressInfo>

    suspend fun deleteAll()

    suspend fun deletePickedBefore(beforeTime: Long)

    suspend fun deleteDuplicateExpress()
}
