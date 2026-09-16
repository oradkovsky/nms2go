package com.ror.nms2go.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.ror.nms2go.BuildConfig

/**
 * Applies a test tag only in debug builds so release binaries contain no test tags.
 * Inline + BuildConfig.DEBUG allows R8 to strip the string literal and testTag call from release.
 */
inline fun Modifier.debugTestTag(tag: String): Modifier =
    if (BuildConfig.DEBUG) testTag(tag) else this
