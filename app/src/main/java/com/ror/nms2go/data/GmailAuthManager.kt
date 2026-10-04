package com.ror.nms2go.data

import android.content.Context
import com.ror.nms2go.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Result of a Gmail OAuth round-trip.
 */
sealed interface GmailAuthResult {
    data class Authorized(val accessToken: String) : GmailAuthResult
    data class Failed(val message: String) : GmailAuthResult
}

/**
 * Single owner of Gmail OAuth coordination. Any ViewModel that needs a token
 * calls [authorize]: it emits [authorizationRequests] (observed by MainActivity,
 * which runs the Google auth UI) and suspends without blocking a thread until
 * [onAuthorizationResult] / [onAuthorizationFailure] delivers the outcome.
 * A newer request supersedes a still-pending one.
 */
@Singleton
class GmailAuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val lock = Any()
    private var pending: CompletableDeferred<GmailAuthResult>? = null

    private val _authorizationRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val authorizationRequests: SharedFlow<Unit> = _authorizationRequests.asSharedFlow()

    suspend fun authorize(): GmailAuthResult {
        val request = CompletableDeferred<GmailAuthResult>()
        synchronized(lock) {
            pending?.cancel()
            pending = request
        }
        _authorizationRequests.tryEmit(Unit)
        return request.await()
    }

    fun onAuthorizationResult(accessToken: String?) {
        val result = if (accessToken.isNullOrBlank()) {
            GmailAuthResult.Failed(context.getString(R.string.auth_no_token))
        } else {
            GmailAuthResult.Authorized(accessToken)
        }
        complete(result)
    }

    fun onAuthorizationFailure(message: String) {
        complete(GmailAuthResult.Failed(message))
    }

    private fun complete(result: GmailAuthResult) {
        synchronized(lock) {
            pending?.complete(result)
            pending = null
        }
    }
}
