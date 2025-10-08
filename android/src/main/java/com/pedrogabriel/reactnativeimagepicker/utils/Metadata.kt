package com.pedrogabriel.reactnativeimagepicker.utils


import android.annotation.SuppressLint
import android.util.Log;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


abstract class Metadata {
  protected var datetime: String? = null
  protected var height: Int = 0
  protected var width: Int = 0

  abstract fun getDateTime(): String?

  abstract fun getWidth(): Int

  abstract fun getHeight(): Int

  /**
   * Converts a timestamp to a UTC timestamp
   *
   * @param value  - timestamp
   * @param format - input format
   * @return formatted timestamp
   */
  @SuppressLint("KotlinNullnessAnnotation")
  @Nullable
  protected fun getDateTimeInUTC(value: String?, format: String?): String? {
    try {
      val datetime: Date? = SimpleDateFormat(format, Locale.US).parse(value)
      val formatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

      if (datetime != null) {
        return formatter.format(datetime)
      }

      return null
    } catch (e: Exception) {
      // This error does not bubble up to RN as we don't want failed datetime parsing to prevent selection
      Log.e("RNIP", "Could not parse image datetime to UTC: " + e.message)
      return null
    }
  }
}
