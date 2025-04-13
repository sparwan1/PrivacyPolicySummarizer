package com.example.privacypolicysummarizer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap

fun Drawable.toBitmapSafely(): Bitmap {
    return if (this is BitmapDrawable) {
        this.bitmap
    } else {
        createBitmap(this.intrinsicWidth.coerceAtLeast(1), this.intrinsicHeight.coerceAtLeast(1)).also { bitmap ->
            val canvas = Canvas(bitmap)
            this.setBounds(0, 0, canvas.width, canvas.height)
            this.draw(canvas)
        }
    }
}