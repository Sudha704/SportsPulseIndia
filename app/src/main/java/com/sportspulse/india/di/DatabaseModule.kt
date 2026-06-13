package com.sportspulse.india.di

import android.content.Context
import androidx.room.Room
import com.sportspulse.india.core.data.db.SportsPulseDatabase
import com.sportspulse.india.core.data.db.dao.AlertDao
import com.sportspulse.india.core.data.db.dao.BroadcastDao
import com.sportspulse.india.core.data.db.dao.SportEventDao
import com.sportspulse.india.core.data.db.dao.VenueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the singleton Room database.
     *
     * [fallbackToDestructiveMigration] is used during development.
     * Replace with proper [Migration] objects before production release.
     */
    @Provides
    @Singleton
    fun provideSportsPulseDatabase(
        @ApplicationContext context: Context
    ): SportsPulseDatabase =
        Room.databaseBuilder(
            context,
            SportsPulseDatabase::class.java,
            SportsPulseDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    // ─────────────────────────────────────────────────────────────────────────
    // DAO providers — each function obtains the DAO from the singleton DB.
    // ─────────────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideSportEventDao(db: SportsPulseDatabase): SportEventDao =
        db.sportEventDao()

    @Provides
    @Singleton
    fun provideVenueDao(db: SportsPulseDatabase): VenueDao =
        db.venueDao()

    @Provides
    @Singleton
    fun provideBroadcastDao(db: SportsPulseDatabase): BroadcastDao =
        db.broadcastDao()

    @Provides
    @Singleton
    fun provideAlertDao(db: SportsPulseDatabase): AlertDao =
        db.alertDao()
}
