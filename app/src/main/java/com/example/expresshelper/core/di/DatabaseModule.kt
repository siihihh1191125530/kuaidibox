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
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideExpressDao(database: AppDatabase): ExpressDao {
        return database.expressDao()
    }
}
