package com.pedrogabriel.reactnativeimagepicker.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface

class ImageMetadata(uri: Uri, context: Context) : Metadata()  {
  init {
    try {
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val exif = ExifInterface(inputStream)
        val datetimeTag = exif.getAttribute(ExifInterface.TAG_DATETIME)

        // Extraia mais metadados aqui, se desejar
        if (datetimeTag != null) {
          datetime = getDateTimeInUTC(datetimeTag, "yyyy:MM:dd HH:mm:ss")
        }
      }
    } catch (e: Exception) {
      // Não propaga o erro — falha na leitura EXIF não deve impedir seleção
      Log.e("RNIP", "Could not load image metadata: ${e.message}")
    }
  }

  override fun getDateTime(): String? = datetime

  override fun getWidth(): Int = 0
  override fun getHeight(): Int = 0
}
