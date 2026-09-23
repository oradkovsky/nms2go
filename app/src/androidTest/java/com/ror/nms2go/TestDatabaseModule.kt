package com.ror.nms2go

import android.content.Context
import com.ror.nms2go.data.AppDatabase
import com.ror.nms2go.data.OrderDao
import com.ror.nms2go.data.SenderDao
import com.ror.nms2go.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Swaps the sender table for [FakeSenderDao] in UI tests so sender state is a
 * synchronous in-memory flow. Orders keep the real database.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideSenderDao(): SenderDao = FakeSenderDao()

    @Provides
    fun provideOrderDao(database: AppDatabase): OrderDao = database.orderDao()
}
