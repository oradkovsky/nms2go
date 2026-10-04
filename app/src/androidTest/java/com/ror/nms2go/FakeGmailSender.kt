package com.ror.nms2go

import com.ror.nms2go.data.GmailSender
import com.ror.nms2go.di.GmailModule
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records Gmail delivery in UI tests instead of hitting the network.
 * Set [failWith] to throw from [sendMessage], simulating a send failure.
 * Must be [reset] in every test's @Before - the Hilt singleton outlives a
 * single test.
 */
@Singleton
class FakeGmailSender @Inject constructor() : GmailSender {
    data class SentMessage(val to: String, val subject: String)

    val sentMessages = mutableListOf<SentMessage>()
    var failWith: String? = null

    fun reset() {
        sentMessages.clear()
        failWith = null
    }

    override fun sendMessage(to: String, subject: String, htmlBody: String, accessToken: String) {
        failWith?.let { throw IOException(it) }
        sentMessages += SentMessage(to, subject)
    }
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [GmailModule::class]
)
interface FakeGmailModule {
    @Binds
    fun bindGmailSender(fake: FakeGmailSender): GmailSender
}
