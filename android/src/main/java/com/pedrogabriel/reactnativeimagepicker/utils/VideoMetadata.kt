package com.pedrogabriel.reactnativeimagepicker.utils

import android.media.MediaMetadataRetriever
import java.io.IOException
import java.lang.AutoCloseable

internal class CustomMediaMetadataRetriever : MediaMetadataRetriever(), AutoCloseable {
  @Throws(IOException::class)
  override fun close() {
    release()
  }
}

class VideoMetadata : Metadata() {

  override fun getDateTime(): String? {
    TODO("Not yet implemented")
  }

  override fun getWidth(): Int {
    TODO("Not yet implemented")
  }

  override fun getHeight(): Int {
    TODO("Not yet implemented")
  }
}
