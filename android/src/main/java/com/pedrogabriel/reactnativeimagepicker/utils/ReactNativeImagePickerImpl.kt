package com.pedrogabriel.reactnativeimagepicker.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.facebook.react.bridge.*
import java.io.File
import java.util.Collections
import java.util.concurrent.Executors

class ReactNativeImagePickerImpl(private val reactContext: ReactApplicationContext) : ActivityEventListener {
  private var fileUri: Uri? = null
  private var cameraCaptureURI: Uri? = null
  private var callback: Callback? = null
  private var promise: Promise? = null
  private lateinit var options: Options

  companion object {
    // Códigos públicos para apps consumidores interagirem com o picker
    const val REQUEST_LAUNCH_IMAGE_CAPTURE = 13001
    const val REQUEST_LAUNCH_VIDEO_CAPTURE = 13002
    const val REQUEST_LAUNCH_LIBRARY = 13003
  }

  init {
    reactContext.addActivityEventListener(this)
    val map = WritableNativeMap()

    map.putString("mediaType", "photo")
    map.putInt("selectionLimit", 5)
    map.putBoolean("includeBase64", true)
    map.putBoolean("includeExtra", false)
    map.putString("videoQuality", "high")
    map.putDouble("quality", 0.8)
    map.putInt("maxWidth", 1024)
    map.putInt("maxHeight", 768)
    map.putBoolean("saveToPhotos", true)
    map.putInt("durationLimit", 30)
    map.putString("cameraType", "front")
    map.putString("assetRepresentationMode", "current")

    // Para arrays:
    val mimeTypesArray = WritableNativeArray().apply {
      pushString("image/jpeg")
      pushString("image/png")
    }
    map.putArray("restrictMimeTypes", mimeTypesArray)
    this.options = Options(map)
  }

  fun launchCamera(optionsMap: ReadableMap, promise: Promise){
    try{
    if (!Utils.isCameraAvailable(reactContext)) {
      promise.reject(Utils.ERR_CAMERA_UNAVAILABLE, "Camera error")
      return;
    }
    val currentActivity = reactContext.currentActivity
    if (currentActivity == null) {
      promise.reject(Utils.ERR_OTHERS, "Activity error")
      return
    }

    if(!Utils.isCameraPermissionFulfilled(reactContext, currentActivity)){
      promise.reject(Utils.ERR_OTHERS, Utils.CAMERA_PERMISSION_DESCRIPTION)
      return
    }

    this.promise = promise
    this.options = Options(optionsMap);
   
    if (this.options.saveToPhotos == true && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !Utils.hasPermission(currentActivity)) {
      promise.reject(Utils.ERR_PERMISSION, "Access denied")
      return;
    }
 
    var requestCode: Int
    var file: File?
    var cameraIntent: Intent

    if(this.options.mediaType == Utils.MEDIA_TYPE_VIDEO){
      requestCode = REQUEST_LAUNCH_VIDEO_CAPTURE;
      cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE);
      cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, this.options.videoQuality);
      if (this.options.durationLimit > 0) {
        cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, this.options.durationLimit);
      }
      file = Utils.createFile(reactContext, "mp4")
      if(file == null){
        promise.reject(Utils.ERR_OTHERS, "File create failed")
        return
      }
      cameraCaptureURI = Utils.createUri(file, reactContext);
    }else {
      requestCode = REQUEST_LAUNCH_IMAGE_CAPTURE
      cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
      file = Utils.createFile(reactContext, "jpg")
      if(file == null){
        promise.reject(Utils.ERR_OTHERS, "File create failed")
        return
        
      }
      
      Log.d("DEBUG_TEST", "cameraIntent: $reactContext.applicationContext.packageName.imagepickerprovider ")
      cameraCaptureURI = Utils.createUri(file, reactContext)
    }
    if (this.options.useFrontCamera) {
      Utils.setFrontCamera(cameraIntent)
    }

    Log.d("DEBUG_TEST", "cameraCaptureURI: $cameraCaptureURI")

    fileUri = Uri.fromFile(file)
    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraCaptureURI)
    cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

    try {
      currentActivity.startActivityForResult(cameraIntent, requestCode)
    } catch (e: ActivityNotFoundException) {
      promise.reject(Utils.ERR_OTHERS, e.message)
      this.promise = null
    }}catch (e: ActivityNotFoundException) {
      Log.d("DEBUG_TEST", "cameraCaptureURI: $e")
      this.promise = null
    }
  }

  fun launchImageLibrary(optionsMap: ReadableMap, promise: Promise) {
    val currentActivity = reactContext.currentActivity
    if (currentActivity == null) {
      promise.reject(Utils.ERR_OTHERS, "Activity error")
      return
    }

    this.promise = promise
    this.options = Options(optionsMap)

    val opts = this.options
    val selectionLimit = opts.selectionLimit
    val isSingleSelect = selectionLimit == 1
    val isPhoto = opts.mediaType == Utils.MEDIA_TYPE_PHOTO
    val isVideo = opts.mediaType == Utils.MEDIA_TYPE_VIDEO

    val mediaType = when {
      isPhoto -> "image/*"
      isVideo -> "video/*"
      else -> "*/*"
    }
    var intent: Intent

    if(isPhoto){
      intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "image/*"
        addCategory(Intent.CATEGORY_OPENABLE)

        if (!isSingleSelect && selectionLimit > 1) {
          putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
      }
    }else if(isVideo){
      intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "video/*"
        addCategory(Intent.CATEGORY_OPENABLE)
        if (!isSingleSelect && selectionLimit > 1) {
          putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
      }
    }else {
      intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        if (!isSingleSelect && selectionLimit > 1) {
          putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
      }
    }

    try {
      currentActivity.startActivityForResult(
        Intent.createChooser(intent, "Selecione um arquivo"),
        REQUEST_LAUNCH_LIBRARY
      )
    } catch (e: ActivityNotFoundException) {
      promise.reject(Utils.ERR_OTHERS, e.message)
      this.promise = null
    }
  }

  private fun onAssetsObtained(fileUris: List<Uri>) {
    val executor = Executors.newSingleThreadExecutor()
    executor.submit {
      try {
        Log.d("DEBUG_TEST", "deu ruim $promise")
        promise?.resolve(Utils.getResponseMap(fileUris as java.util.List<Uri>, options, reactContext))
      } catch (exception: RuntimeException) {
        promise?.reject(Utils.ERR_OTHERS, exception.message)
      } finally {
        promise = null
      }
    }
  }

  override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
    val currentPromise = promise
    if (!Utils.isValidRequestCode(requestCode) || currentPromise == null) return

    if (resultCode != Activity.RESULT_OK) {
      if (requestCode == REQUEST_LAUNCH_IMAGE_CAPTURE) {
        fileUri?.let { Utils.deleteFile(it) }
      }
      try {
        this.promise?.resolve(Utils.getCancelMap())
      } catch (exception: RuntimeException) {
        this.promise?.reject(Utils.ERR_OTHERS, exception.message)
      }
      return
    }

    when (requestCode) {
      REQUEST_LAUNCH_IMAGE_CAPTURE -> {
        if (options.saveToPhotos == true) {
          cameraCaptureURI?.let { Utils.saveToPublicDirectory(it, reactContext, "photo") }
        }
        fileUri?.let { onAssetsObtained(Collections.singletonList(it)) }
      }

      REQUEST_LAUNCH_LIBRARY -> {
        onAssetsObtained(Utils.collectUrisFromData(data) as List<Uri>)
      }

      REQUEST_LAUNCH_VIDEO_CAPTURE -> {
        if (options.saveToPhotos == true) {
          cameraCaptureURI?.let { Utils.saveToPublicDirectory(it, reactContext, "video") }
        }
        fileUri?.let { onAssetsObtained(Collections.singletonList(it)) }
      }
    }
  }

  override fun onNewIntent(intent: Intent) = Unit
}
