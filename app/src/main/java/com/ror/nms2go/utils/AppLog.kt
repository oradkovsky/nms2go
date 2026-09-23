package com.ror.nms2go.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Single entry point for error logging.
 *
 * Normal app: logs to logcat AND reports to Crashlytics.
 * UI tests: logs to logcat only ([crashlyticsEnabled] is set to false by
 * HiltTestRunner, which backs every connected test).
 *
 * Crashlytics is additionally guarded by runCatching so an uninitialized
 * Crashlytics can never crash the app.
 */
object AppLog {

    @Volatile
    var crashlyticsEnabled: Boolean = true

    fun w(tag: String, message: String, error: Throwable) {
        Log.w(tag, message, error)
        if (crashlyticsEnabled) {
            runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
        }
    }
}
