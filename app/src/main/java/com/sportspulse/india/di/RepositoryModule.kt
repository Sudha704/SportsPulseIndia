package com.sportspulse.india.di

import com.sportspulse.india.core.domain.repository.AlertRepository
import com.sportspulse.india.core.domain.repository.BroadcastRepository
import com.sportspulse.india.core.domain.repository.LocationRepository
import com.sportspulse.india.core.domain.repository.SportEventRepository
import com.sportspulse.india.core.domain.repository.VenueRepository
import com.sportspulse.india.features.alerts.data.AlertRepositoryImpl
import com.sportspulse.india.features.dashboard.data.BroadcastRepositoryImpl
import com.sportspulse.india.features.dashboard.data.SportEventRepositoryImpl
import com.sportspulse.india.features.venues.data.LocationRepositoryImpl
import com.sportspulse.india.features.venues.data.VenueRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds each repository *interface* to its concrete *implementation*.
 *
 * Using [@Binds] instead of [@Provides] avoids creating a wrapper method
 * and lets Hilt generate more efficient code.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSportEventRepository(
        impl: SportEventRepositoryImpl
    ): SportEventRepository

    @Binds
    @Singleton
    abstract fun bindBroadcastRepository(
        impl: BroadcastRepositoryImpl
    ): BroadcastRepository

    @Binds
    @Singleton
    abstract fun bindVenueRepository(
        impl: VenueRepositoryImpl
    ): VenueRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(
        impl: AlertRepositoryImpl
    ): AlertRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository
}
