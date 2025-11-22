package com.grupo2.ashley.core.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class ImageCompressor @Inject constructor() {

    /**
     * Comprime una imagen para reducir su tamaño de archivo.
     * @param imageBytes Bytes de la imagen original
     * @param maxSizeKb Tamaño máximo en KB (predeterminado: 500 KB)
     * @param maxWidth Ancho máximo en píxeles (predeterminado: 1024)
     * @param maxHeight Alto máximo en píxeles (predeterminado: 1024)
     * @return Bytes de la imagen comprimida
     */
    fun compress(
        imageBytes: ByteArray,
        maxSizeKb: Int = 500,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024
    ): ByteArray {
        try {
            // Decode the image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false

            // Decode with inSampleSize
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                ?: return imageBytes

            // Further resize if needed
            val scaledBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                val scale = minOf(
                    maxWidth.toFloat() / bitmap.width,
                    maxHeight.toFloat() / bitmap.height
                )
                val width = (bitmap.width * scale).toInt()
                val height = (bitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else {
                bitmap
            }

            // Compress to JPEG with quality adjustment
            var quality = 90
            var compressedBytes: ByteArray
            do {
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                compressedBytes = outputStream.toByteArray()
                quality -= 10
            } while (compressedBytes.size > maxSizeKb * 1024 && quality > 10)

            if (scaledBitmap != bitmap) {
                bitmap.recycle()
            }
            scaledBitmap.recycle()

            return compressedBytes
        } catch (e: Exception) {
            // If compression fails, return original
            return imageBytes
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
