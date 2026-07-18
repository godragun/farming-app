package com.project.farmingapp.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for efficient image compression and processing.
 * All operations are background-thread safe.
 */
object ImageCompressor {
    
    /**
     * Compress image to specified dimensions.
     * Prevents loading full-resolution images into memory.
     *
     * @param inputPath Path to original image
     * @param outputPath Path to save compressed image
     * @param maxWidth Maximum width (default 1024px)
     * @param maxHeight Maximum height (default 720px)
     * @param quality JPEG quality 0-100 (default 85)
     * @return Success status
     */
    suspend fun compressImage(
        inputPath: String,
        outputPath: String,
        maxWidth: Int = 1024,
        maxHeight: Int = 720,
        quality: Int = 85
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputFile = File(inputPath)
            if (!inputFile.exists()) {
                LogUtil.cameraError("Input image not found: $inputPath")
                return@withContext false
            }
            
            LogUtil.cameraDebug("Starting compression: $inputPath → size: ${inputFile.length()} bytes")
            
            // Decode with inSampleSize to reduce memory usage
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(inputPath, options)
            
            val sampleSize = calculateInSampleSize(options.outWidth, options.outHeight, maxWidth, maxHeight)
            options.inSampleSize = sampleSize
            options.inJustDecodeBounds = false
            
            val bitmap = BitmapFactory.decodeFile(inputPath, options) ?: run {
                LogUtil.cameraError("Failed to decode bitmap: $inputPath")
                return@withContext false
            }
            
            // Compress and save
            FileOutputStream(outputPath).use { fos ->
                val compressed = Bitmap.createScaledBitmap(
                    bitmap,
                    minOf(bitmap.width, maxWidth),
                    minOf(bitmap.height, maxHeight),
                    true
                )
                compressed.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                bitmap.recycle()
                compressed.recycle()
            }
            
            val outputFile = File(outputPath)
            LogUtil.cameraDebug("Compression complete: ${outputFile.length()} bytes (${((outputFile.length().toFloat() / inputFile.length()) * 100).toInt()}% of original)")
            true
        } catch (e: Exception) {
            LogUtil.cameraError("Compression failed", e)
            false
        }
    }
    
    /**
     * Get file size in human-readable format
     */
    fun getFileSizeReadable(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Int {
        var inSampleSize = 1
        
        if (height > maxHeight || width > maxWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}
