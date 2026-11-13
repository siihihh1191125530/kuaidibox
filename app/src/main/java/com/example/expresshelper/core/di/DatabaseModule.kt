package com.example.expresshelper.core.di

import android.content.Context
import androidx.room.Room
import com.example.expresshelper.core.database.AppDatabase
import com.example.expresshelper.data.local.dao.ExpressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS sms_receipts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, contentKey TEXT NOT NULL, receivedTime INTEGER NOT NULL)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_receipts_contentKey_receivedTime ON sms_receipts(contentKey, receivedTime)")
                }
            }


    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "express_helper.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideExpressDao(database: AppDatabase): ExpressDao {
        return database.expressDao()
    }
}
