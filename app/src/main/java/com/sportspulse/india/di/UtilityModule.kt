package com.sportspulse.india.di

import com.sportspulse.india.core.data.api.GeminiApiClient
import com.sportspulse.india.core.data.rss.RssFeedParser
import com.sportspulse.india.core.domain.usecase.HaversineDistanceUseCase
import com.sportspulse.india.core.domain.usecase.HierarchyFilter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides utility/domain objects that don't fit into the other modules:
 *  - [GeminiApiClient]        — Gemini AI SDK wrapper
 *  - [RssFeedParser]          — Rome RSS parser
 *  - [HierarchyFilter]        — Sports event ranking use case
 *  - [HaversineDistanceUseCase] — Distance calculation
 */
@Module
@InstallIn(SingletonComponent::class)
object UtilityModule {

    @Provides
    @Singleton
    fun provideGeminiApiClient(): GeminiApiClient = GeminiApiClient()

    @Provides
    @Singleton
    fun provideRssFeedParser(): RssFeedParser = RssFeedParser()

    @Provides
    @Singleton
    fun provideHierarchyFilter(): HierarchyFilter = HierarchyFilter()

    @Provides
    @Singleton
    fun provideHaversineDistanceUseCase(): HaversineDistanceUseCase = HaversineDistanceUseCase()
}
