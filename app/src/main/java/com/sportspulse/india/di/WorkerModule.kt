package com.sportspulse.india.di

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides WorkManager [Configuration] using [HiltWorkerFactory].
 *
 * This replaces the default WorkManager initialisation so that Hilt can inject
 * dependencies into [androidx.work.Worker] subclasses annotated with [@HiltWorker].
 *
 * The app's [SportsPulseApplication] must implement [androidx.work.Configuration.Provider]
 * and return the configuration provided here.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(
        workerFactory: HiltWorkerFactory
    ): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
