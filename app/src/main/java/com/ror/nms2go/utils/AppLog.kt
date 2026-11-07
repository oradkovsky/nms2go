package com.ror.nms2go.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ror.nms2go.BuildConfig

/**
 * Single entry point for logging.
 *
 * Debug logs are written to logcat only in debug builds.
 * Warnings always log to logcat and report to Crashlytics in normal app runs.
 * UI tests: logs to logcat only ([crashlyticsEnabled] is set to false by
 * HiltTestRunner, which backs every connected test).
 *
 * Crashlytics is additionally guarded by runCatching so an uninitialized
 * Crashlytics can never crash the app.
 */
object AppLog {

    @Volatile
    var crashlyticsEnabled: Boolean = true

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun w(tag: String, message: String, error: Throwable) {
        Log.w(tag, message, error)
        if (crashlyticsEnabled) {
            runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
        }
    }
}
