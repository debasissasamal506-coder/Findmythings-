package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageStorageHelper {

    fun createCameraTempUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, "images").apply { mkdirs() }
        val tempFile = File(cacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, tempFile)
    }

    suspend fun persistImageToInternalStorage(context: Context, sourceUri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val imagesDir = File(context.filesDir, "item_images").apply { mkdirs() }
                val targetFile = File(imagesDir, "item_${System.currentTimeMillis()}.jpg")

                val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                if (inputStream == null) return@withContext null

                // Decode with inSampleSize to safeguard memory for large camera images
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                var sampleSize = 1
                val maxDimension = 1600
                while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val freshInputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                val bitmap = BitmapFactory.decodeStream(freshInputStream, null, decodeOptions)
                freshInputStream?.close()

                if (bitmap != null) {
                    FileOutputStream(targetFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    }
                    bitmap.recycle()
                    Uri.fromFile(targetFile).toString()
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    fun deleteInternalImage(uriString: String?) {
        if (uriString == null) return
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
