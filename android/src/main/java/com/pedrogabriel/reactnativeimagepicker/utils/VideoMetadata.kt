package com.pedrogabriel.reactnativeimagepicker.utils

import java.lang.Integer.parseInt

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

import java.io.IOException

class CustomMediaMetadataRetriever : MediaMetadataRetriever(), AutoCloseable {
  @Throws(IOException::class)
  override fun close() {
    release()
  }
}

class VideoMetadata(uri: Uri, context: Context) : Metadata() {
  private var duration: Int = 0
  private var bitrate: Int = 0

  init {
    try {
      CustomMediaMetadataRetriever().use { metadataRetriever ->
        metadataRetriever.setDataSource(context, uri)

        val durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val bitrateStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        val datetimeStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)

        if (durationStr != null) duration = (durationStr.toFloat().toInt()) / 1000
        if (bitrateStr != null) bitrate = bitrateStr.toInt()

        if (datetimeStr != null) {
          // METADATA_KEY_DATE retorna no formato: "20211214T102646.000Z"
          val datetimeToFormat = datetimeStr.substringBefore('.') + "+GMT"
          datetime = getDateTimeInUTC(datetimeToFormat, "yyyyMMdd'T'HHmmss+zzz")
        }

        val widthStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)

        if (widthStr != null && heightStr != null) {
          val rotation = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
          val rotationInt = rotation?.toIntOrNull() ?: 0

          if (rotationInt == 90 || rotationInt == 270) {
            widthData = heightStr.toInt()
            heightData = widthStr.toInt()
          } else {
            widthData = widthStr.toInt()
            heightData = heightStr.toInt()
          }
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  fun getBitrate(): Int = bitrate
  fun getDuration(): Int = duration

  override fun getDateTime(): String? = datetime
  override fun getWidth(): Int = widthData
  override fun getHeight(): Int = heightData
}
