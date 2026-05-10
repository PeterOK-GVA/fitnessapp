package com.peter.fitness.di

import com.peter.fitness.core.ids.IdFactory
import com.peter.fitness.core.ids.RandomIdFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class IdFactoryModule {

    @Binds
    @Singleton
    abstract fun bindIdFactory(impl: RandomIdFactory): IdFactory
}
