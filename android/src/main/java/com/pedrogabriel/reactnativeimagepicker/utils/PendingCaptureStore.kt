package com.pedrogabriel.reactnativeimagepicker.utils

import android.content.Context
import android.content.SharedPreferences
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists the in-flight camera capture so it can be recovered when Android kills the
 * app process while the camera app is in the foreground (common on Samsung devices).
 */
class PendingCaptureStore(context: Context) {
  data class Pending(
    val requestCode: Int,
    val filePath: String,
    val optionsJson: String,
    val tag: String?,
    val timestamp: Long,
    val resultCode: Int?
  )

  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun save(requestCode: Int, filePath: String, options: ReadableMap, tag: String?) {
    prefs.edit()
      .putInt(KEY_REQUEST_CODE, requestCode)
      .putString(KEY_FILE_PATH, filePath)
      .putString(KEY_OPTIONS, JSONObject(options.toHashMap() as Map<*, *>).toString())
      .putString(KEY_TAG, tag)
      .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
      .remove(KEY_RESULT_CODE)
      .commit()
  }

  fun markResult(resultCode: Int) {
    if (!prefs.contains(KEY_FILE_PATH)) return
    prefs.edit().putInt(KEY_RESULT_CODE, resultCode).commit()
  }

  fun load(): Pending? {
    val filePath = prefs.getString(KEY_FILE_PATH, null) ?: return null
    return Pending(
      requestCode = prefs.getInt(KEY_REQUEST_CODE, 0),
      filePath = filePath,
      optionsJson = prefs.getString(KEY_OPTIONS, null) ?: "{}",
      tag = prefs.getString(KEY_TAG, null),
      timestamp = prefs.getLong(KEY_TIMESTAMP, 0L),
      resultCode = if (prefs.contains(KEY_RESULT_CODE)) prefs.getInt(KEY_RESULT_CODE, 0) else null
    )
  }

  fun pendingFilePath(): String? = prefs.getString(KEY_FILE_PATH, null)

  fun clear() {
    prefs.edit().clear().commit()
  }

  companion object {
    private const val PREFS_NAME = "rn_image_picker_pending"
    private const val KEY_REQUEST_CODE = "requestCode"
    private const val KEY_FILE_PATH = "filePath"
    private const val KEY_OPTIONS = "options"
    private const val KEY_TAG = "tag"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_RESULT_CODE = "resultCode"

    const val MAX_AGE_MS: Long = 24L * 60 * 60 * 1000

    fun isUsable(pending: Pending): Boolean {
      val file = File(pending.filePath)
      val fresh = System.currentTimeMillis() - pending.timestamp < MAX_AGE_MS
      return fresh && file.exists() && file.length() > 0
    }

    /** Rebuilds a ReadableMap from the JSON persisted in [save]. */
    fun optionsFromJson(json: String): ReadableMap {
      return try {
        jsonToMap(JSONObject(json))
      } catch (e: Exception) {
        Arguments.createMap()
      }
    }

    private fun jsonToMap(obj: JSONObject): WritableMap {
      val map = Arguments.createMap()
      val keys = obj.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        when (val value = obj.get(key)) {
          JSONObject.NULL -> map.putNull(key)
          is Boolean -> map.putBoolean(key, value)
          is Int -> map.putDouble(key, value.toDouble())
          is Long -> map.putDouble(key, value.toDouble())
          is Double -> map.putDouble(key, value)
          is Number -> map.putDouble(key, value.toDouble())
          is String -> map.putString(key, value)
          is JSONObject -> map.putMap(key, jsonToMap(value))
          is JSONArray -> {
            val arr = Arguments.createArray()
            for (i in 0 until value.length()) {
              when (val item = value.get(i)) {
                is String -> arr.pushString(item)
                is Boolean -> arr.pushBoolean(item)
                is Number -> arr.pushDouble(item.toDouble())
                else -> arr.pushString(item.toString())
              }
            }
            map.putArray(key, arr)
          }
          else -> map.putString(key, value.toString())
        }
      }
      return map
    }
  }
}
