package com.pedrogabriel.reactnativeimagepicker.utils

import com.facebook.react.bridge.ReadableMap
import android.text.TextUtils

class Options(options: ReadableMap) {
  var selectionLimit: Int = 0
  var includeBase64: Boolean? = null
  var includeExtra: Boolean? = null
  var videoQuality: Int = 1
  var quality: Int = 0
  var conversionQuality: Int = 92
  var convertToJpeg: Boolean = true
  var maxWidth: Int = 0
  var maxHeight: Int = 0
  var saveToPhotos: Boolean? = null
  var durationLimit: Int = 0
  var useFrontCamera: Boolean = false
  var mediaType: String? = null
  var restrictMimeTypes: Array<String?>? = null

  init {
        mediaType = options.getString("mediaType")
        restrictMimeTypes = options.getArray("restrictMimeTypes")?.toArrayList()?.map { it.toString() }?.toTypedArray()
        selectionLimit = options.getInt("selectionLimit")
        includeBase64 = options.getBoolean("includeBase64")
        includeExtra = options.getBoolean("includeExtra")

        val videoQualityString = options.getString("videoQuality")
        if (!videoQualityString.isNullOrEmpty() && videoQualityString.lowercase() != "high") {
          videoQuality = 0
        }

        if (options.hasKey("conversionQuality")) {
          conversionQuality = (options.getDouble("conversionQuality") * 100).toInt()
        }

        val assetRepresentationMode = options.getString("assetRepresentationMode")
        if (!assetRepresentationMode.isNullOrEmpty() && assetRepresentationMode.lowercase() == "current") {
          convertToJpeg = false
        }

        if (options.getString("cameraType") == "front") {
          useFrontCamera = true
        }

        quality = (options.getDouble("quality") * 100).toInt()
        maxHeight = options.getInt("maxHeight")
        maxWidth = options.getInt("maxWidth")
        saveToPhotos = options.getBoolean("saveToPhotos")
        durationLimit = options.getInt("durationLimit")
  }
}
