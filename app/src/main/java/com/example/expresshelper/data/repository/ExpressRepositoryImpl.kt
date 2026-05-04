package com.example.expresshelper.data.repository

import com.example.expresshelper.data.local.dao.ExpressDao
import com.example.expresshelper.data.local.entity.ExpressEntity
import com.example.expresshelper.data.local.entity.ExpressStatus
import com.example.expresshelper.domain.model.ExpressInfo
import com.example.expresshelper.domain.repository.ExpressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpressRepositoryImpl @Inject constructor(
    private val expressDao: ExpressDao
) : ExpressRepository {

    override fun getPendingExpress(): Flow<List<ExpressInfo>> {
        return expressDao.getPendingExpress().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllExpressForManage(): Flow<List<ExpressInfo>> {
        return expressDao.getAllExpressForManage().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getExpressById(id: Long): ExpressInfo? {
        return expressDao.getExpressById(id)?.toDomain()
    }

    override suspend fun insertExpress(express: ExpressInfo): Long {
        return expressDao.insertExpress(express.toEntity())
    }

    override suspend fun markAsPicked(id: Long) {
        expressDao.markAsPicked(id)
    }

    override suspend fun markAsPending(id: Long) {
        expressDao.markAsPending(id)
    }

    override suspend fun deleteById(id: Long) {
        expressDao.deleteById(id)
    }

    override fun getPendingCount(): Flow<Int> {
        return expressDao.getPendingCount()
    }

    override fun getPickedCount(): Flow<Int> {
        return expressDao.getPickedCount()
    }

    override suspend fun existsPendingByCode(code: String, afterTime: Long): Boolean {
        return expressDao.existsPendingByCode(code, afterTime)
    }

    override suspend fun getPendingExpressList(): List<ExpressInfo> {
        return expressDao.getAllExpressForWidget().map { it.toDomain() }
    }

    override suspend fun deleteAll() {
        expressDao.deleteAll()
    }

    override suspend fun deletePickedBefore(beforeTime: Long) {
        expressDao.deletePickedBefore(beforeTime)
    }

    override suspend fun deleteDuplicateExpress() {
        expressDao.deleteDuplicateExpress()
    }

    // Extension functions for mapping
    private fun ExpressEntity.toDomain(): ExpressInfo {
        val domainStatus = when (status) {
            0 -> com.example.expresshelper.domain.model.ExpressStatus.PENDING
            1 -> com.example.expresshelper.domain.model.ExpressStatus.PICKED
            else -> com.example.expresshelper.domain.model.ExpressStatus.EXPIRED
        }
        return ExpressInfo(
            id = id,
            code = code,
            source = source,
            location = location,
            smsSender = smsSender,
            smsBody = smsBody,
            receivedTime = receivedTime,
            status = domainStatus,
            pickedTime = pickedTime
        )
    }

    private fun ExpressInfo.toEntity(): ExpressEntity {
        val entityStatus = when (status) {
            com.example.expresshelper.domain.model.ExpressStatus.PENDING -> 0
            com.example.expresshelper.domain.model.ExpressStatus.PICKED -> 1
            com.example.expresshelper.domain.model.ExpressStatus.EXPIRED -> 2
        }
        return ExpressEntity(
            id = id,
            code = code,
            source = source,
            location = location,
            smsSender = smsSender,
            smsBody = smsBody,
            receivedTime = receivedTime,
            status = entityStatus,
            pickedTime = pickedTime
        )
    }
}
