package com.ror.nms2go.di

import com.ror.nms2go.data.GmailRepository
import com.ror.nms2go.data.GmailSender
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface GmailModule {
    @Binds
    fun bindGmailSender(impl: GmailRepository): GmailSender
}
