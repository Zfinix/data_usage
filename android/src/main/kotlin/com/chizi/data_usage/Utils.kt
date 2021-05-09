package com.chizi.data_usage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import java.io.ByteArrayOutputStream

object Utils {
    @JvmStatic
    fun drawableToBase64(icon: Drawable?): ByteArray? {
        return try {
            if (icon != null) {
                val bitmap = getBitmapFromDrawable(icon)
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                byteArrayOutputStream.toByteArray()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("DATA_USAGE", e.toString())
            null
        }
    }

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    @JvmStatic
    fun <T> getValueOrDefault(value: T?, defaultValue: T): T {
        return value ?: defaultValue
    }
}