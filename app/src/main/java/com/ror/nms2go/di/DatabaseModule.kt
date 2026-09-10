package com.ror.nms2go.di

import android.content.Context
import com.ror.nms2go.data.AppDatabase
import com.ror.nms2go.data.OrderDao
import com.ror.nms2go.data.SenderDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // Keep using AppDatabase.getInstance() semantics if already initialized,
        // but Hilt now owns the singleton. Reuse Room builder with migrations.
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideSenderDao(database: AppDatabase): SenderDao = database.senderDao()

    @Provides
    fun provideOrderDao(database: AppDatabase): OrderDao = database.orderDao()
}
