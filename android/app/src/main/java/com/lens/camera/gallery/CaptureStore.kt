package com.lens.camera.gallery

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.LruCache
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class Capture(val file: File) {
    val id: String get() = file.name
    val isVideo: Boolean get() = file.extension == "mp4"
}

/** Gallery grid tiles only need a small preview; this keeps decoding fast and cheap. */
const val THUMBNAIL_MAX_DIMENSION = 320

/** Full-screen viewer target: plenty of detail for a phone screen, even zoomed in a few times. */
const val VIEWER_MAX_DIMENSION = 2048

/**
 * Persists captures to app-private storage so the in-app gallery survives process
 * death and app restarts. [exportToGallery] additionally copies a capture into the
 * device's public Pictures/LENS folder via MediaStore (the native equivalent of the
 * web version's browser "download").
 */
class CaptureStore(private val context: Context) {

    private val dir: File = File(context.filesDir, "captures").apply { mkdirs() }

    // Decoded bitmaps are cached (keyed by capture + target size) so swiping back and
    // forth in the gallery doesn't re-decode the same JPEG from disk repeatedly.
    private val bitmapCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun listAll(): List<Capture> =
        dir.listFiles { f -> f.isFile && (f.extension == "jpg" || f.extension == "mp4") }
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

    /** A fresh file path for a hyperlapse recording in progress; wrap the result in [Capture]
     *  once it's finished encoding and ready to show in the gallery. */
    fun newVideoFile(): File = File(dir, "LENS_${System.currentTimeMillis()}.mp4")

    /**
     * Decodes a capture downsampled to roughly [maxDimension] on its longest side instead of
     * full sensor resolution: loading a 50MP frame at full size for a small gallery thumbnail
     * (or even the full-screen viewer) is slow to decode and holds far more memory than the
     * screen can ever show, which is what made gallery scrolling/swiping laggy. Pass
     * Int.MAX_VALUE for an undownsampled decode (e.g. before exporting or sharing).
     */
    fun loadBitmap(capture: Capture, maxDimension: Int = Int.MAX_VALUE): Bitmap? {
        val cacheKey = "${capture.id}:$maxDimension"
        bitmapCache.get(cacheKey)?.let { return it }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(capture.file.path, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        if (width > maxDimension || height > maxDimension) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / sampleSize >= maxDimension && halfHeight / sampleSize >= maxDimension) {
                sampleSize *= 2
            }
        }

        val decoded = BitmapFactory.decodeFile(
            capture.file.path,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return null

        bitmapCache.put(cacheKey, decoded)
        return decoded
    }

    /**
     * A still frame pulled from a video capture for use as its gallery thumbnail, downsampled
     * the same way [loadBitmap] downsamples photos.
     */
    fun loadVideoThumbnail(capture: Capture, maxDimension: Int): Bitmap? {
        val cacheKey = "${capture.id}:video:$maxDimension"
        bitmapCache.get(cacheKey)?.let { return it }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(capture.file.path)
            val frame = retriever.frameAtTime ?: return null
            val longSide = maxOf(frame.width, frame.height)
            val scaled = if (longSide > maxDimension) {
                val scale = maxDimension.toFloat() / longSide
                Bitmap.createScaledBitmap(
                    frame,
                    (frame.width * scale).toInt().coerceAtLeast(1),
                    (frame.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                frame
            }
            bitmapCache.put(cacheKey, scaled)
            scaled
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    fun delete(capture: Capture) {
        capture.file.delete()
        bitmapCache.remove("${capture.id}:$THUMBNAIL_MAX_DIMENSION")
        bitmapCache.remove("${capture.id}:$VIEWER_MAX_DIMENSION")
        bitmapCache.remove("${capture.id}:${Int.MAX_VALUE}")
        bitmapCache.remove("${capture.id}:video:$THUMBNAIL_MAX_DIMENSION")
        bitmapCache.remove("${capture.id}:video:$VIEWER_MAX_DIMENSION")
    }

    /** Copies the capture into the public Pictures/LENS (or Movies/LENS for video) gallery
     *  folder. Returns the new content Uri. */
    fun exportToGallery(capture: Capture): Uri? {
        val resolver = context.contentResolver
        val isVideo = capture.isVideo
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
        val folder = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, capture.file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/LENS")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            capture.file.inputStream().use { input -> input.copyTo(out) }
        } ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    /** A content:// Uri suitable for handing to a share Intent (readable by the receiving app only). */
    fun shareUri(capture: Capture): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", capture.file)
}
