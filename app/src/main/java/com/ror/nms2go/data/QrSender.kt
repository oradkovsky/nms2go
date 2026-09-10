package com.ror.nms2go.data

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONArray
import org.json.JSONObject

data class QrSender(
    val company: String = "",
    val email: String = "",
    val receiver: String = "",
    val parser: String = ""
)

object QrCodec {

    private const val KEY_ITEMS = "items"
    private const val KEY_COMPANY = "company"
    private const val KEY_EMAIL = "email"
    private const val KEY_RECEIVER = "receiver"
    private const val KEY_PARSER = "parser"

    fun encode(senders: List<SenderEntity>): String =
        encodeItems(senders.map { QrSender(it.companyName, it.email, it.receiverEmail, it.parser) })

    fun encodeItems(items: List<QrSender>): String {
        val root = JSONObject()
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put(KEY_COMPANY, item.company)
                    .put(KEY_EMAIL, item.email)
                    .put(KEY_RECEIVER, item.receiver)
                    .put(KEY_PARSER, item.parser)
            )
        }
        root.put(KEY_ITEMS, array)
        return root.toString()
    }

    fun decode(text: String): List<QrSender>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return try {
            if (trimmed.startsWith("[")) {
                decodeArray(JSONArray(trimmed))
            } else {
                val root = JSONObject(trimmed)
                val array = root.optJSONArray(KEY_ITEMS)
                    ?: return null
                decodeArray(array)
            }
        } catch (e: Exception) {
            Log.w("QrCodec", "QR decode failed", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }

    private fun decodeArray(array: JSONArray): List<QrSender> {
        val result = mutableListOf<QrSender>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val item = QrSender(
                company = obj.optString(KEY_COMPANY).trim(),
                email = obj.optString(KEY_EMAIL).trim(),
                receiver = obj.optString(KEY_RECEIVER).trim(),
                parser = obj.optString(KEY_PARSER).trim()
            )
            if (item.email.isNotBlank() || item.receiver.isNotBlank()) {
                result += item
            }
        }
        return result
    }
}