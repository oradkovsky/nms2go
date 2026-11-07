package com.ror.nms2go.di

import com.ror.nms2go.analytics.Analytics
import com.ror.nms2go.analytics.FirebaseAnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalytics(impl: FirebaseAnalyticsTracker): Analytics
}
