package com.ror.nms2go

import androidx.test.platform.app.InstrumentationRegistry

/**
 * Helper for visual UI test delays.
 * - Without gradle param: no delay (fast CI).
 * - With -PvisualDelay=true (or -PvisualDelayMs=3000): 2s delay after each screen action.
 * Enabled via testInstrumentationRunnerArguments["visualDelay"] + BuildConfig fields.
 */
object TestVisuals {
    private const val DEFAULT_DELAY_MS = 2000L

    fun isEnabled(): Boolean {
        // Prefer instrumentation arguments (works for connected tests)
        val argsDelay = try {
            InstrumentationRegistry.getArguments().getString("visualDelay")?.toBoolean()
        } catch (_: Exception) {
            null
        }
        if (argsDelay != null) return argsDelay
        // Fallback to BuildConfig (generated from gradle -PvisualDelay)
        return try {
            BuildConfig.VISUAL_TEST_DELAY
        } catch (_: Exception) {
            false
        }
    }

    fun delayMs(): Long {
        val argsMs = try {
            InstrumentationRegistry.getArguments().getString("visualDelayMs")?.toLongOrNull()
        } catch (_: Exception) {
            null
        }
        if (argsMs != null) return argsMs
        return try {
            BuildConfig.VISUAL_TEST_DELAY_MS
        } catch (_: Exception) {
            DEFAULT_DELAY_MS
        }
    }

    fun afterAction() {
        if (isEnabled()) {
            try {
                Thread.sleep(delayMs())
            } catch (_: InterruptedException) {
                // ignore
            }
        }
    }

    fun afterSetContent() = afterAction()
}
