package com.ror.nms2go.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.ror.nms2go.utils.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    @param:ApplicationContext private val context: Context
) : Analytics {

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    override fun logEvent(name: String, vararg params: Pair<String, Any?>) {
        val attributes = params.toMap()
        AppLog.d(TAG, "$name $attributes")
        firebaseAnalytics.logEvent(name, attributes.toBundle())
    }

    private companion object {
        const val TAG = "FirebaseAnalytics"
    }
}

private fun Map<String, Any?>.toBundle(): Bundle = Bundle().apply {
    forEach { (key, value) ->
        when (value) {
            null -> putString(key, null)
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Double -> putDouble(key, value)
            is Boolean -> putBoolean(key, value)
            is Bundle -> putBundle(key, value)
            else -> putString(key, value.toString())
        }
    }
}
