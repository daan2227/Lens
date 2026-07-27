package com.lens.camera.gallery

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

data class Capture(val file: File) {
    val id: String get() = file.name
}

/**
 * Persists captures to app-private storage so the in-app gallery survives process
 * death and app restarts. [exportToGallery] additionally copies a capture into the
 * device's public Pictures/LENS folder via MediaStore (the native equivalent of the
 * web version's browser "download").
 */
class CaptureStore(private val context: Context) {

    private val dir: File = File(context.filesDir, "captures").apply { mkdirs() }

    fun listAll(): List<Capture> =
        dir.listFiles { f -> f.isFile && f.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { Capture(it) }
            ?: emptyList()

    fun save(bitmap: Bitmap): Capture {
        val file = File(dir, "LENS_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return Capture(file)
    }

    fun loadBitmap(capture: Capture): Bitmap? = BitmapFactory.decodeFile(capture.file.path)

    fun delete(capture: Capture) {
        capture.file.delete()
    }

    /** Copies the capture into the public Pictures/LENS gallery folder. Returns the new content Uri. */
    fun exportToGallery(capture: Capture): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, capture.file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LENS")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            capture.file.inputStream().use { input -> input.copyTo(out) }
        } ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }
}
