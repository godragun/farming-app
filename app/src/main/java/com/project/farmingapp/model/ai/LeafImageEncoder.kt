package com.project.farmingapp.model.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Shrinks a photo and Base64-encodes it for the Roboflow workflow.
 *
 * Camera photos can be 12+ MP. Decoding one at full size needs ~48 MB and can throw
 * [OutOfMemoryError] (an Error, not an Exception - it would crash the app), so the image is
 * sub-sampled while decoding and the decode is retried at a smaller size if memory runs out.
 */
object LeafImageEncoder {

    private const val MAX_SIDE = 640

    @Throws(LeafScanError::class)
    fun encodeBase64(file: File): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw LeafScanError.EmptyImage

        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= MAX_SIDE) sample *= 2

        var attempt = 0
        while (true) {
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bmp: Bitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
                    ?: throw LeafScanError.EmptyImage
                val bos = ByteArrayOutputStream()
                try {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, bos)
                } finally {
                    bmp.recycle()
                }
                return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
            } catch (e: OutOfMemoryError) {
                if (++attempt > 3) throw LeafScanError.EmptyImage
                sample *= 2
            }
        }
    }
}
