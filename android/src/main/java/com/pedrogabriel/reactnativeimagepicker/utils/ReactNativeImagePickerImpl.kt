package com.pedrogabriel.reactnativeimagepicker.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.facebook.react.bridge.*
import java.io.File
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ReactNativeImagePickerImpl(private val reactContext: ReactApplicationContext) : ActivityEventListener {
  private var fileUri: Uri? = null
  private var cameraCaptureURI: Uri? = null
  private var promise: Promise? = null
  private var options: Options = Options(Arguments.createMap())
  private val pendingStore = PendingCaptureStore(reactContext)

  companion object {
    // Códigos públicos para apps consumidores interagirem com o picker
    const val REQUEST_LAUNCH_IMAGE_CAPTURE = 13001
    const val REQUEST_LAUNCH_VIDEO_CAPTURE = 13002
    const val REQUEST_LAUNCH_LIBRARY = 13003

    // Um único worker para processar as mídias: evita criar uma thread nova por chamada
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
  }

  init {
    reactContext.addActivityEventListener(this)
  }

  fun launchCamera(optionsMap: ReadableMap, promise: Promise) {
    try {
      if (!Utils.isCameraAvailable(reactContext)) {
        promise.reject(Utils.ERR_CAMERA_UNAVAILABLE, "Camera error")
        return
      }
      val currentActivity = reactContext.currentActivity
      if (currentActivity == null) {
        promise.reject(Utils.ERR_OTHERS, "Activity error")
        return
      }

      if (!Utils.isCameraPermissionFulfilled(reactContext, currentActivity)) {
        promise.reject(Utils.ERR_OTHERS, Utils.CAMERA_PERMISSION_DESCRIPTION)
        return
      }

      val opts = Options(optionsMap)

      if (opts.saveToPhotos == true && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !Utils.hasPermission(currentActivity)) {
        promise.reject(Utils.ERR_PERMISSION, "Access denied")
        return
      }

      // Uma captura anterior que nunca foi recuperada é descartada ao iniciar outra
      discardPendingCapture()
      Utils.cleanOldTempFiles(reactContext, null)

      val requestCode: Int
      val cameraIntent: Intent
      val file: File?

      if (opts.mediaType == Utils.MEDIA_TYPE_VIDEO) {
        requestCode = REQUEST_LAUNCH_VIDEO_CAPTURE
        cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, opts.videoQuality)
        if (opts.durationLimit > 0) {
          cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, opts.durationLimit)
        }
        file = Utils.createFile(reactContext, "mp4")
      } else {
        requestCode = REQUEST_LAUNCH_IMAGE_CAPTURE
        cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        file = Utils.createFile(reactContext, "jpg")
      }

      if (file == null) {
        promise.reject(Utils.ERR_OTHERS, "File create failed")
        return
      }

      val captureUri = Utils.createUri(file, reactContext)
      if (opts.useFrontCamera) {
        Utils.setFrontCamera(cameraIntent)
      }
      cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
      cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

      this.promise = promise
      this.options = opts
      this.cameraCaptureURI = captureUri
      this.fileUri = Uri.fromFile(file)

      // Persistido antes de sair do app: se o Android matar o processo enquanto a câmera
      // estiver aberta, getPendingResult() consegue recuperar a foto depois.
      pendingStore.save(requestCode, file.absolutePath, optionsMap, opts.tag)

      try {
        currentActivity.startActivityForResult(cameraIntent, requestCode)
      } catch (e: ActivityNotFoundException) {
        discardPendingCapture()
        this.promise = null
        promise.reject(Utils.ERR_OTHERS, e.message)
      }
    } catch (e: Throwable) {
      discardPendingCapture()
      this.promise = null
      promise.reject(Utils.ERR_OTHERS, e.message)
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
    Utils.cleanOldTempFiles(reactContext, pendingStore.pendingFilePath())

    val opts = this.options
    val allowMultiple = opts.selectionLimit != 1
    val isPhoto = opts.mediaType == Utils.MEDIA_TYPE_PHOTO
    val isVideo = opts.mediaType == Utils.MEDIA_TYPE_VIDEO

    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      when {
        isPhoto -> type = "image/*"
        isVideo -> type = "video/*"
        else -> {
          type = "*/*"
          putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        }
      }
      if (allowMultiple) {
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
      }
    }

    try {
      currentActivity.startActivityForResult(
        Intent.createChooser(intent, "Selecione um arquivo"),
        REQUEST_LAUNCH_LIBRARY
      )
    } catch (e: ActivityNotFoundException) {
      this.promise = null
      promise.reject(Utils.ERR_OTHERS, e.message)
    }
  }

  /**
   * Recupera a última captura da câmera que não foi entregue porque o processo do app
   * foi encerrado enquanto a câmera estava aberta. Resolve null se não houver nada.
   */
  fun getPendingResult(promise: Promise) {
    // Se ainda há uma captura em andamento neste processo, ela será entregue pela promise original
    if (this.promise != null) {
      promise.resolve(null)
      return
    }

    val pending = pendingStore.load()
    if (pending == null) {
      promise.resolve(null)
      return
    }

    val cancelled = pending.resultCode != null && pending.resultCode != Activity.RESULT_OK
    if (cancelled || !PendingCaptureStore.isUsable(pending)) {
      discardPendingCapture()
      promise.resolve(null)
      return
    }

    executor.execute {
      try {
        val pendingOptions = Options(PendingCaptureStore.optionsFromJson(pending.optionsJson))
        val file = File(pending.filePath)
        if (pendingOptions.saveToPhotos == true) {
          val mediaType = if (pending.requestCode == REQUEST_LAUNCH_VIDEO_CAPTURE) "video" else "photo"
          Utils.saveToPublicDirectory(Utils.createUri(file, reactContext), reactContext, mediaType)
        }
        val response = Utils.getResponseMap(Collections.singletonList(Uri.fromFile(file)) as java.util.List<Uri>, pendingOptions, reactContext)
        val map = Arguments.createMap()
        map.merge(response)
        if (pending.tag != null) map.putString("tag", pending.tag) else map.putNull("tag")
        map.putBoolean("recovered", true)
        pendingStore.clear()
        promise.resolve(map)
      } catch (e: Throwable) {
        discardPendingCapture()
        promise.reject(Utils.ERR_OTHERS, e.message)
      }
    }
  }

  private fun discardPendingCapture() {
    pendingStore.load()?.let { File(it.filePath).delete() }
    pendingStore.clear()
  }

  private fun onAssetsObtained(fileUris: List<Uri>, clearPending: Boolean) {
    val currentPromise = promise ?: return
    val currentOptions = options
    executor.execute {
      try {
        currentPromise.resolve(Utils.getResponseMap(fileUris as java.util.List<Uri>, currentOptions, reactContext))
      } catch (e: Throwable) {
        // Throwable para incluir OutOfMemoryError: rejeita em vez de derrubar o processo
        currentPromise.reject(Utils.ERR_OTHERS, e.message)
      } finally {
        if (clearPending) pendingStore.clear()
        if (promise === currentPromise) promise = null
      }
    }
  }

  override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
    if (!Utils.isValidRequestCode(requestCode)) return
    val isCapture = requestCode == REQUEST_LAUNCH_IMAGE_CAPTURE || requestCode == REQUEST_LAUNCH_VIDEO_CAPTURE
    val currentPromise = promise

    if (currentPromise == null) {
      // Processo recriado: guarda o resultado para getPendingResult()
      if (isCapture) {
        if (resultCode == Activity.RESULT_OK) pendingStore.markResult(resultCode) else discardPendingCapture()
      }
      return
    }

    if (resultCode != Activity.RESULT_OK) {
      if (isCapture) discardPendingCapture()
      promise = null
      currentPromise.resolve(Utils.getCancelMap())
      return
    }

    when (requestCode) {
      REQUEST_LAUNCH_IMAGE_CAPTURE, REQUEST_LAUNCH_VIDEO_CAPTURE -> {
        val uri = fileUri
        if (uri == null) {
          discardPendingCapture()
          promise = null
          currentPromise.reject(Utils.ERR_OTHERS, "Capture file lost")
          return
        }
        if (options.saveToPhotos == true) {
          val mediaType = if (requestCode == REQUEST_LAUNCH_VIDEO_CAPTURE) "video" else "photo"
          cameraCaptureURI?.let { Utils.saveToPublicDirectory(it, reactContext, mediaType) }
        }
        onAssetsObtained(Collections.singletonList(uri), clearPending = true)
      }

      REQUEST_LAUNCH_LIBRARY -> {
        onAssetsObtained(Utils.collectUrisFromData(data) as List<Uri>, clearPending = false)
      }
    }
  }

  override fun onNewIntent(intent: Intent) = Unit
}
