package com.pedrogabriel.reactnativeimagepicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.pedrogabriel.reactnativeimagepicker.utils.ReactNativeImagePickerImpl


@ReactModule(name = ReactNativeImagePickerModule.NAME)
class ReactNativeImagePickerModule(reactContext: ReactApplicationContext) :
    NativeReactNativeImagePickerSpec(reactContext), ActivityEventListener {
    private lateinit var imagePickerModuleImpl: ReactNativeImagePickerImpl
    companion object {
        const val NAME = "ReactNativeImagePicker"
        private const val REQUEST_CODE_PICK = 1001

    }

    private var promise: Promise? = null

    init {
        reactContext.addActivityEventListener(this)
        imagePickerModuleImpl = ReactNativeImagePickerImpl(reactContext)
    }

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    override fun launchImageLibrary(options: ReadableMap, promise: Promise) {
        this.imagePickerModuleImpl.launchImageLibrary(options, promise)
    }

    @ReactMethod
    override fun launchCamera(options: ReadableMap, promise: Promise) {
        this.imagePickerModuleImpl.launchCamera(options, promise)
    }

    override fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == REQUEST_CODE_PICK && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            val currentPromise = promise
            promise = null // limpa

            if (uri != null && currentPromise != null) {
                try {
                    val inputStream = activity.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        currentPromise.reject("E_NO_STREAM", "Não foi possível abrir InputStream para ${uri}")
                        return
                    }

                    val exif = ExifInterface(inputStream)
                    val tags = listOf(
                        ExifInterface.TAG_DATETIME,
                        ExifInterface.TAG_MAKE,
                        ExifInterface.TAG_MODEL,
                        ExifInterface.TAG_FOCAL_LENGTH,
                        ExifInterface.TAG_GPS_LATITUDE,
                        ExifInterface.TAG_GPS_LONGITUDE,
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.TAG_EXPOSURE_TIME,
                        ExifInterface.TAG_ISO_SPEED_RATINGS
                    )

                    val exifData = WritableNativeMap()
                    for (tag in tags) {
                        exif.getAttribute(tag)?.let { value ->
                            exifData.putString(tag, value)
                        }
                    }

                    val result = Arguments.createMap()
                    result.putString("uri", uri.toString())
                    result.putMap("exif", exifData)

                    currentPromise.resolve(result)

                } catch (e: Exception) {
                    currentPromise.reject("E_EXIF_ERROR", "Erro ao ler metadados EXIF: ${e.message}")
                }
            } else {
                currentPromise?.reject("E_NO_FILE", "Nenhum arquivo selecionado")
            }
        }
    }


    override fun onNewIntent(intent: Intent) = Unit

    override fun multiply(a: Double, b: Double): Double {
        return a * b
    }
}
