package com.pedrogabriel.reactnativeimagepicker

import android.app.Activity
import com.facebook.react.bridge.*
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.DocumentsContract
import android.content.Intent
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream


@ReactModule(name = ReactNativeImagePickerModule.NAME)
class ReactNativeImagePickerModule(reactContext: ReactApplicationContext) :
    NativeReactNativeImagePickerSpec(reactContext), ActivityEventListener {
    companion object {
        const val NAME = "ReactNativeImagePicker"
        private const val REQUEST_CODE_PICK = 1001
    }

    private var promise: Promise? = null

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    override fun launchImageLibrary(options: ReadableMap, promise: Promise) {
        val activity = currentActivity ?: run {
            promise?.reject("E_ACTIVITY_NULL", "Nenhuma Activity ativa")
            return
        }

        this.promise = promise

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
//      val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//        type = "*/*" // tipo genérico
//        addCategory(Intent.CATEGORY_OPENABLE)
//        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
//      }
        try {
            activity.startActivityForResult(Intent.createChooser(intent, "Selecionar arquivo"), REQUEST_CODE_PICK)
        } catch (e: Exception) {
          Log.d("DEBUG", "deu ruim")
            promise?.reject("E_PICKER_ERROR", e.message)
        }
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
