package com.pedrogabriel.reactnativeimagepicker.utils

import com.facebook.react.bridge.ReadableMap
import android.text.TextUtils
import com.pedrogabriel.reactnativeimagepicker.utils.Utils

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
        mediaType = options.getString("mediaType") ?: Utils.MEDIA_TYPE_PHOTO
        restrictMimeTypes = options.getArray("restrictMimeTypes")?.toArrayList()?.map { it.toString() }?.toTypedArray() ?: null
        selectionLimit = if (options.hasKey("selectionLimit")) {
            options.getInt("selectionLimit")
        } else {
          1 
        }
        includeBase64 = if (options.hasKey("includeBase64")) {
          options.getBoolean("includeBase64")
        } else {
          false
        }
        includeExtra = if (options.hasKey("includeExtra")) {
          options.getBoolean("includeExtra")
        } else {
          false
        }

        val videoQualityString =  if (options.hasKey("videoQuality")) {
          options.getString("videoQuality")
        }else {
          "low"
        }
        
        if (!videoQualityString.isNullOrEmpty() && videoQualityString.lowercase() != "high") {
          videoQuality = 0
        }

        if (options.hasKey("conversionQuality")) {
          conversionQuality = (options.getDouble("conversionQuality") * 100).toInt()
        }

        val assetRepresentationMode = if (options.hasKey("assetRepresentationMode")) {
          options.getString("assetRepresentationMode")
        }else {
          null
        }
        
        if (!assetRepresentationMode.isNullOrEmpty() && assetRepresentationMode.lowercase() == "current") {
          convertToJpeg = false
        }

        if (options.getString("cameraType") == "front") {
          useFrontCamera = true
        }

        quality = (options.getDouble("quality") * 100).toInt()
        maxHeight = options.getInt("maxHeight") ?: 2000
        maxWidth = options.getInt("maxWidth") ?: 2000
        saveToPhotos = if (options.hasKey("saveToPhotos")) {
          options.getBoolean("saveToPhotos")
        } else {
          false
        }
        durationLimit = if(options.hasKey("durationLimit")) {
          options.getInt("durationLimit")
        } else {
          0
        }
  }
}
