package com.example.expresshelper.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.expresshelper.data.local.dao.ExpressDao
import com.example.expresshelper.data.local.entity.ExpressEntity

@Database(
    entities = [ExpressEntity::class, com.example.expresshelper.data.local.entity.SmsReceipt::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expressDao(): ExpressDao
}
