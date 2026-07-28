package com.lens.camera.hyperlapse

import android.graphics.Bitmap
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/**
 * Encodes a sequence of still frames into an accelerated MP4 ("hyperlapse"): frames are
 * captured seconds or minutes apart but written back at a fixed [frameRate], so playback runs
 * much faster than real time.
 *
 * Uses MediaCodec in buffer (non-Surface) input mode with the universally-supported flexible
 * YUV420 format (all codecs support it since API 22 — our minSdk is 24). A MediaCodec input
 * Surface only reliably accepts GPU-rendered content; drawing Bitmaps into it via
 * Surface.lockCanvas() is explicitly documented as unsupported, which is why this encodes via
 * MediaCodec.getInputImage() and a manual RGB->YUV420 conversion instead.
 */
class HyperlapseEncoder(
    outputFile: File,
    private val width: Int,
    private val height: Int,
    private val frameRate: Int = 30
) {
    private val codec: MediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        start()
    }
    private val muxer = MediaMuxer(outputFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private val bufferInfo = MediaCodec.BufferInfo()
    private var trackIndex = -1
    private var muxerStarted = false
    private var frameCount = 0L

    /** Feeds one frame, scaling it to the encoder's fixed size first if needed. Call from a
     *  background thread — this does a full RGB->YUV420 conversion per pixel. */
    fun addFrame(bitmap: Bitmap) {
        val scaled = if (bitmap.width == width && bitmap.height == height) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
        val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex >= 0) {
            val image = codec.getInputImage(inputIndex)
            if (image != null) {
                writeYuv(image, scaled)
                val presentationTimeUs = frameCount * 1_000_000L / frameRate
                codec.queueInputBuffer(inputIndex, 0, width * height * 3 / 2, presentationTimeUs, 0)
                frameCount++
            } else {
                codec.queueInputBuffer(inputIndex, 0, 0, 0, 0)
            }
        }
        if (scaled !== bitmap) scaled.recycle()
        drainEncoder(endOfStream = false)
    }

    /** Signals end of input, flushes remaining output, and finalizes the file. Call once,
     *  from a background thread; safe to call even if no frames were ever added. */
    fun finish() {
        val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex >= 0) {
            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        drainEncoder(endOfStream = true)
        try {
            codec.stop()
        } catch (e: Exception) {
            // Codec never produced output (e.g. zero frames) — nothing to flush.
        }
        codec.release()
        if (muxerStarted) {
            try {
                muxer.stop()
            } catch (e: Exception) {
                // No samples were ever written.
            }
        }
        muxer.release()
    }

    private fun drainEncoder(endOfStream: Boolean) {
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                outputIndex >= 0 -> {
                    val encodedData = codec.getOutputBuffer(outputIndex)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    /** BT.601 "video range" RGB->YUV420, the standard fixed-point Android conversion. */
    private fun writeYuv(image: Image, bitmap: Bitmap) {
        val w = image.width
        val h = image.height
        val argb = IntArray(w * h)
        bitmap.getPixels(argb, 0, w, 0, 0, w, h)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val yRowStride = yPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride

        for (row in 0 until h) {
            val rowBase = row * w
            for (col in 0 until w) {
                val pixel = argb[rowBase + col]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yBuffer.put(row * yRowStride + col, y.coerceIn(0, 255).toByte())
            }
        }

        var row = 0
        while (row < h) {
            var col = 0
            while (col < w) {
                val pixel = argb[row * w + col]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                val chromaRow = row / 2
                val chromaCol = col / 2
                uBuffer.put(chromaRow * uRowStride + chromaCol * uPixelStride, u.coerceIn(0, 255).toByte())
                vBuffer.put(chromaRow * vRowStride + chromaCol * vPixelStride, v.coerceIn(0, 255).toByte())
                col += 2
            }
            row += 2
        }
    }

    companion object {
        private const val TIMEOUT_US = 10_000L
    }
}
