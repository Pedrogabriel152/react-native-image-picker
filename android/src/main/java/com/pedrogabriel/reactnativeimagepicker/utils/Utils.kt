package com.pedrogabriel.reactnativeimagepicker.utils


import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.List
import java.util.UUID
import com.pedrogabriel.reactnativeimagepicker.utils.ReactNativeImagePickerImpl

object Utils {
  const val FILE_NAME_PREFIX: String = "rn_image_picker_lib_temp_"

  const val ERR_CAMERA_UNAVAILABLE: String = "camera_unavailable"
  const val ERR_PERMISSION: String = "permission"
  const val ERR_OTHERS = "others"

  const val MEDIA_TYPE_PHOTO: String = "photo"
  const val MEDIA_TYPE_VIDEO: String = "video"

  const val CAMERA_PERMISSION_DESCRIPTION = "This library does not require Manifest.permission.CAMERA, if you add this permission in manifest then you have to obtain the same."

  @JvmStatic
  fun createFile(reactContext: Context, fileType: String): File? {
    try {
        val fileName: String = "$FILE_NAME_PREFIX${UUID.randomUUID()}.$fileType"

        val fileDir: File = reactContext.cacheDir;

        val file: File = File(fileDir, fileName)
        file.createNewFile()
        return file
    }catch (e: Exception){
      e.printStackTrace();
      return null
    }
  }

  @JvmStatic
  fun createUri(file: File, reactContext: Context): Uri{
    val authority: String = reactContext.applicationContext.packageName+".imagepickerprovider"
    return FileProvider.getUriForFile(reactContext,authority, file)
  }

  @JvmStatic
  fun saveToPublicDirectory(uri: Uri, context: Context, mediaType: String) {
    val resolver: ContentResolver = context.contentResolver
    var mediaStoreUri: Uri? = null
    val fileDetails: ContentValues = ContentValues()

    if (mediaType == "video") {
      fileDetails.put(MediaStore.Video.Media.DISPLAY_NAME, UUID.randomUUID().toString())
      fileDetails.put(MediaStore.Video.Media.MIME_TYPE, resolver.getType(uri))
      mediaStoreUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, fileDetails)
    } else {
      fileDetails.put(MediaStore.Images.Media.DISPLAY_NAME, UUID.randomUUID().toString());
      fileDetails.put(MediaStore.Images.Media.MIME_TYPE, resolver.getType(uri));
      mediaStoreUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fileDetails);
    }

    if (mediaStoreUri != null) {
      copyUri(uri, mediaStoreUri, resolver)
    }
  }

  @JvmStatic
  fun copyUri(fromUri: Uri, toUri: Uri, resolver: ContentResolver) {
    try {
      resolver.openOutputStream(toUri).use { os ->
        resolver.openInputStream(fromUri).use { `is` ->
          if (os == null || `is` == null) return
          val buffer = ByteArray(8192)
          var bytesRead: Int
          while (`is`.read(buffer).also { bytesRead = it } != -1) {
            os.write(buffer, 0, bytesRead)
          }
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  @JvmStatic
  fun getAppSpecificStorageUri(sharedStorageUri: Uri?, context: Context): Uri? {
    if (sharedStorageUri == null) return null
    val contentResolver = context.contentResolver
    var fileType = getFileTypeFromMime(contentResolver.getType(sharedStorageUri))

    if (fileType == null) {
      contentResolver.query(sharedStorageUri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (nameIndex != -1) {
            val fileName = cursor.getString(nameIndex)
            val lastDotIndex = fileName.lastIndexOf('.')
            if (lastDotIndex != -1) {
              fileType = fileName.substring(lastDotIndex + 1)
            }
          }
        }
      }
    }

    val destFile = createFile(context, fileType ?: "jpg") ?: return null
    val toUri = Uri.fromFile(destFile)
    copyUri(sharedStorageUri, toUri, contentResolver)
    return toUri
  }

  @JvmStatic
  fun isCameraAvailable(reactContext: Context): Boolean {
    val pm = reactContext.packageManager
    return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
  }

  /**
   * Opening front camera is not officially supported in android, the below hack is obtained from various online sources
   */
  @JvmStatic
  fun setFrontCamera(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
      intent.putExtra("android.intent.extras.CAMERA_FACING", CameraCharacteristics.LENS_FACING_FRONT)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
      }
    } else {
      intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
    }
  }

  @JvmStatic
  fun getImageDimensions(uri: Uri, reactContext: Context): IntArray {
    try {
      reactContext.contentResolver.openInputStream(uri).use { inputStream ->
        if (inputStream == null) return intArrayOf(0, 0)

        val orientation = getOrientation(uri, reactContext)

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)

        return if (needToSwapDimension(orientation)) {
          intArrayOf(options.outHeight, options.outWidth)
        } else {
          intArrayOf(options.outWidth, options.outHeight)
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
      return intArrayOf(0, 0)
    }
  }

  @JvmStatic
  fun hasPermission(activity: Activity): Boolean {
    val writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    return writePermission == PackageManager.PERMISSION_GRANTED
  }

  @JvmStatic
  fun getBase64String(uri: Uri, reactContext: Context): String? {
    try {
      reactContext.contentResolver.openInputStream(uri).use { inputStream ->
        ByteArrayOutputStream().use { output ->
          if (inputStream == null) return null
          val buffer = ByteArray(8192)
          var bytesRead: Int
          while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
          }
          val bytes = output.toByteArray()
          return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
      return null
    }
  }

  private fun needToSwapDimension(orientation: String?): Boolean {
    return orientation == ExifInterface.ORIENTATION_ROTATE_90.toString() ||
      orientation == ExifInterface.ORIENTATION_ROTATE_270.toString()
  }

  private fun shouldConvertToJpeg(mimeType: String?, options: Options): Boolean {
    return options.convertToJpeg && mimeType != null && (mimeType == "image/heic" || mimeType == "image/heif")
  }

  /**
   * Resize image and/or convert it from HEIC/HEIF to JPEG
   * When decoding a jpg to bitmap all exif meta data will be lost, so make sure to copy orientation exif to new file else image might have wrong orientations
   */
  @JvmStatic
  fun resizeOrConvertImage(uri: Uri?, context: Context, options: Options): Uri? {
    try {
      if (uri == null) return null
      val origDimens = getImageDimensions(uri, context)
      var mimeType = getMimeType(uri, context)

      val targetQuality: Int

      if (!shouldResizeImage(origDimens[0], origDimens[1], options)) {
        if (shouldConvertToJpeg(mimeType, options)) {
          mimeType = "image/jpeg"
          targetQuality = options.conversionQuality
        } else {
          return uri
        }
      } else {
        targetQuality = options.quality
      }

      val newDimens = getImageDimensBasedOnConstraints(origDimens[0], origDimens[1], options)

      context.contentResolver.openInputStream(uri).use { imageStream ->
        if (imageStream == null) return uri
        var b = BitmapFactory.decodeStream(imageStream)
        val originalOrientation = getOrientation(uri, context)

        b = if (needToSwapDimension(originalOrientation)) {
          Bitmap.createScaledBitmap(b, newDimens[1], newDimens[0], true)
        } else {
          Bitmap.createScaledBitmap(b, newDimens[0], newDimens[1], true)
        }

        val file = createFile(context, getFileTypeFromMime(mimeType))
        if (file == null) return uri

        context.contentResolver.openOutputStream(Uri.fromFile(file)).use { os ->
          if (os == null) return uri
          b.compress(getBitmapCompressFormat(mimeType), targetQuality, os)
        }

        setOrientation(file, originalOrientation, context)
        deleteFile(uri)
        return Uri.fromFile(file)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      return uri
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun getOrientation(uri: Uri, context: Context): String? {
    context.contentResolver.openInputStream(uri).use { `is` ->
      if (`is` == null) return null
      val exifInterface = ExifInterface(`is`)
      return exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)
    }
  }

  /**
   * ExifInterface.saveAttributes is costly operation so don't set exif for unnecessary orientations
   */
  @JvmStatic
  @Throws(IOException::class)
  fun setOrientation(file: File, orientation: String?, context: Context) {
    if (orientation == ExifInterface.ORIENTATION_NORMAL.toString() || orientation == ExifInterface.ORIENTATION_UNDEFINED.toString()) {
      return
    }
    val exifInterface = ExifInterface(file)
    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, orientation)
    exifInterface.saveAttributes()
  }

  @JvmStatic
  fun getImageDimensBasedOnConstraints(origWidth: Int, origHeight: Int, options: Options): IntArray {
    var width = origWidth
    var height = origHeight

    if (options.maxWidth == 0 || options.maxHeight == 0) {
      return intArrayOf(width, height)
    }

    if (options.maxWidth < width) {
      height = ((options.maxWidth.toFloat() / width) * height).toInt()
      width = options.maxWidth
    }

    if (options.maxHeight < height) {
      width = ((options.maxHeight.toFloat() / height) * width).toInt()
      height = options.maxHeight
    }

    return intArrayOf(width, height)
  }

  @JvmStatic
  fun getFileSize(uri: Uri, context: Context): Double {
    try {
      context.contentResolver.openFileDescriptor(uri, "r").use { f ->
        if (f == null) return 0.0
        return f.statSize.toDouble()
      }
    } catch (e: Exception) {
      e.printStackTrace()
      return 0.0
    }
  }

  @JvmStatic
  fun shouldResizeImage(origWidth: Int, origHeight: Int, options: Options): Boolean {
    if ((options.maxWidth == 0 || options.maxHeight == 0) && options.quality == 100) {
      return false
    }

    if (options.maxWidth >= origWidth && options.maxHeight >= origHeight && options.quality == 100) {
      return false
    }

    return true
  }

  @JvmStatic
  fun getBitmapCompressFormat(mimeType: String?): Bitmap.CompressFormat {
    return when (mimeType) {
      "image/jpeg" -> Bitmap.CompressFormat.JPEG
      "image/png" -> Bitmap.CompressFormat.PNG
      else -> Bitmap.CompressFormat.JPEG
    }
  }

  @JvmStatic
  fun getFileTypeFromMime(mimeType: String?): String {
    if (mimeType == null) {
      return "jpg"
    }
    return when (mimeType) {
      "image/jpeg" -> "jpg"
      "image/png" -> "png"
      "image/gif" -> "gif"
      else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
    }
  }

  @JvmStatic
  fun deleteFile(uri: Uri) {
    try {
      File(uri.path ?: "").delete()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Since library users can have many modules in their project, we should respond to onActivityResult only for our request.
   */
  @JvmStatic
  fun isValidRequestCode(requestCode: Int): Boolean {
    return when (requestCode) {
      ReactNativeImagePickerImpl.REQUEST_LAUNCH_IMAGE_CAPTURE,
      ReactNativeImagePickerImpl.REQUEST_LAUNCH_VIDEO_CAPTURE,
      ReactNativeImagePickerImpl.REQUEST_LAUNCH_LIBRARY -> true
      else -> false
    }
  }

  /**
   * This library does not require Manifest.permission.CAMERA permission, but if user app declares as using this permission which is not granted, then attempting to use ACTION_IMAGE_CAPTURE|ACTION_VIDEO_CAPTURE will result in a SecurityException.
   * https://issuetracker.google.com/issues/37063818
   */
  @JvmStatic
  fun isCameraPermissionFulfilled(context: Context, activity: Activity): Boolean {
    try {
      val declaredPermissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        .requestedPermissions ?: return true

      if (Arrays.asList(*declaredPermissions).contains(Manifest.permission.CAMERA)
        && ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
      ) {
        return false
      }

      return true
    } catch (e: PackageManager.NameNotFoundException) {
      e.printStackTrace()
      return true
    }
  }

  @JvmStatic
  fun isImageType(uri: Uri, context: Context): Boolean {
    return isContentType("image/", uri, context)
  }

  @JvmStatic
  fun isVideoType(uri: Uri, context: Context): Boolean {
    return isContentType("video/", uri, context)
  }

  /**
   * Verifies the content types of a file URI. A helper function for isVideoType and isImageType
   *
   * @param contentMimeType - "video/" or "image/"
   * @param uri             - file uri
   * @param context         - react context
   * @return a boolean to determine if file is of specified content type i.e. image or video
   */
  @JvmStatic
  fun isContentType(contentMimeType: String, uri: Uri, context: Context): Boolean {
    val mimeType = getMimeType(uri, context)
    return mimeType?.contains(contentMimeType) ?: false
  }

  @JvmStatic
  fun getMimeType(uri: Uri, context: Context): String? {
    val scheme = uri.scheme
    return when (scheme) {
      "file" -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
      "content" -> {
        val contentResolver = context.contentResolver
        val contentResolverMimeType = contentResolver.getType(uri)
        if (contentResolverMimeType.isNullOrBlank()) {
          getMimeTypeForContent(uri, context)
        } else {
          contentResolverMimeType
        }
      }
      else -> "Unknown"
    }
  }

  @JvmStatic
  @Nullable
  fun getMimeTypeForContent(uri: Uri, context: Context): String? {
    val fileName = getFileNameForContent(uri, context)
    var fileType = "Unknown"
    val lastDotIndex = fileName.lastIndexOf('.')
    if (lastDotIndex != -1) {
      fileType = fileName.substring(lastDotIndex + 1)
    }
    return fileType
  }

  @JvmStatic
  fun getFileName(uri: Uri, context: Context): String {
    return when (uri.scheme) {
      "file" -> uri.lastPathSegment ?: "Unknown"
      "content" -> getFileNameForContent(uri, context)
      else -> "Unknown"
    }
  }

  @JvmStatic
  fun getOriginalFilePath(uri: Uri, context: Context): String? {
    var originPath: String? = null
    var workingUri = uri
    if (uri.scheme?.contains("content") == true) {
      originPath = getFilePathFromContent(uri, context)
      getAppSpecificStorageUri(uri, context)?.let { workingUri = it }
    } else {
      originPath = uri.toString()
    }
    return originPath
  }

  private fun getFilePathFromContent(uri: Uri, context: Context): String? {
    val proj = arrayOf(MediaStore.Images.Media.DATA)
    context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
      val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
      if (index == -1) {
        return null
      }
      if (cursor.moveToFirst()) {
        return cursor.getString(index)
      }
    }
    return null
  }

  private fun getFileNameForContent(uri: Uri, context: Context): String {
    val contentResolver = context.contentResolver
    var cursor: Cursor? = null
    var fileName = uri.lastPathSegment ?: "Unknown"
    try {
      cursor = contentResolver.query(uri, null, null, null, null)
      if (cursor != null && cursor.moveToFirst()) {
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1) {
          fileName = cursor.getString(nameIndex)
        }
      }
    } finally {
      cursor?.close()
    }
    return fileName
  }

  @JvmStatic
  fun collectUrisFromData(data: Intent?): List<Uri> {
    if (data == null) return Collections.emptyList<Uri>() as List<Uri>
    if (data.clipData == null) {
      data.data?.let { return Collections.singletonList(it) as List<Uri> }
      return Collections.emptyList<Uri>() as List<Uri>
    }

    val clipData: ClipData = data.clipData!!
    val fileUris: MutableList<Uri> = ArrayList(clipData.itemCount)

    for (i in 0 until clipData.itemCount) {
      fileUris.add(clipData.getItemAt(i).uri)
    }

    return fileUris as List<Uri>
  }

  @JvmStatic
  fun getImageResponseMap(uri: Uri, appSpecificUri: Uri, options: Options, context: Context): ReadableMap {
    val imageMetadata = ImageMetadata(appSpecificUri, context)
    val dimensions = getImageDimensions(appSpecificUri, context)

    val fileName = getFileName(uri, context)
    val originalPath = getOriginalFilePath(uri, context)

    val map: WritableMap = Arguments.createMap()
    map.putString("uri", appSpecificUri.toString())
    map.putDouble("fileSize", getFileSize(appSpecificUri, context))
    map.putString("fileName", fileName)
    map.putInt("width", dimensions[0])
    map.putInt("height", dimensions[1])
    map.putString("type", getMimeType(appSpecificUri, context))
    map.putString("originalPath", originalPath)

    if (options.includeBase64 == true) {
      getBase64String(appSpecificUri, context)?.let { map.putString("base64", it) }
    }

    if (options.includeExtra == true) {
      // Add more extra data here ...
      map.putString("timestamp", imageMetadata.getDateTime())
      map.putString("id", fileName)
    }

    return map
  }

  @JvmStatic
  fun getVideoResponseMap(uri: Uri, appSpecificUri: Uri, options: Options, context: Context): ReadableMap {
    val map: WritableMap = Arguments.createMap()
    val videoMetadata = VideoMetadata(appSpecificUri, context)

    val fileName = getFileName(uri, context)
    val originalPath = getOriginalFilePath(uri, context)

    map.putString("uri", appSpecificUri.toString())
    map.putDouble("fileSize", getFileSize(appSpecificUri, context))
    map.putInt("duration", videoMetadata.getDuration())
    map.putInt("bitrate", videoMetadata.getBitrate())
    map.putString("fileName", fileName)
    map.putString("type", getMimeType(appSpecificUri, context))
    map.putInt("width", videoMetadata.getWidth())
    map.putInt("height", videoMetadata.getHeight())
    map.putString("originalPath", originalPath)

    if (options.includeExtra == true) {
      // Add more extra data here ...
      map.putString("timestamp", videoMetadata.getDateTime())
      map.putString("id", fileName)
    }

    return map
  }

  @JvmStatic
  @Throws(RuntimeException::class)
  fun getResponseMap(fileUris: List<Uri>, options: Options, context: Context): ReadableMap {
    val assets: WritableArray = Arguments.createArray()

    for (i in fileUris.indices) {
      val uri = fileUris[i]

      var appSpecificUrl = uri
      if (uri.scheme?.contains("content") == true) {
        getAppSpecificStorageUri(uri, context)?.let { appSpecificUrl = it }
      }

      if (isImageType(uri, context)) {
        appSpecificUrl = resizeOrConvertImage(appSpecificUrl, context, options) ?: appSpecificUrl
        assets.pushMap(getImageResponseMap(uri, appSpecificUrl, options, context))
      } else if (isVideoType(uri, context)) {
        if (uri.scheme?.contains("content") == true) {
          getAppSpecificStorageUri(uri, context)?.let { appSpecificUrl = it }
        }
        assets.pushMap(getVideoResponseMap(uri, appSpecificUrl, options, context))
      } else {
        throw RuntimeException("Unsupported file type")
      }
    }

    val response: WritableMap = Arguments.createMap()
    response.putArray("assets", assets)

    return response
  }

  @JvmStatic
  fun getErrorMap(errCode: String, errMsg: String?): ReadableMap {
    val map: WritableMap = Arguments.createMap()
    map.putString("errorCode", errCode)
    if (errMsg != null) {
      map.putString("errorMessage", errMsg)
    }
    return map
  }

  @JvmStatic
  fun getCancelMap(): ReadableMap {
    val map: WritableMap = Arguments.createMap()
    map.putBoolean("didCancel", true)
    return map
  }
}
