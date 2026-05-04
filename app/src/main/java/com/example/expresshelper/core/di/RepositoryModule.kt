package com.example.expresshelper.core.di

import com.example.expresshelper.data.repository.ExpressRepositoryImpl
import com.example.expresshelper.domain.repository.ExpressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpressRepository(
        expressRepositoryImpl: ExpressRepositoryImpl
    ): ExpressRepository
}
