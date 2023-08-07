package com.example.mappi_kt.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtils {
    @SuppressLint("Recycle")
    fun rotateImageIfRequired(context: Context, bitmap: Bitmap, imageUri: Uri?): Bitmap {
        val exif = imageUri?.let { uri ->
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.let { ExifInterface(it) }
        }

        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }

        return rotatedBitmap
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        return baos.toByteArray()
    }

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }
}
