package com.example.fitness.coach.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Minimal QR code generator for a given string.
 * Uses ZXing core (no embedded views).
 */
object QrCode {
    fun generate(text: String, sizePx: Int = 800): Bitmap {
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        return matrix.toBitmap()
    }
}

private fun BitMatrix.toBitmap(): Bitmap {
    val width = width
    val height = height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

@Composable
fun rememberQrImage(text: String?, sizePx: Int = 800): ImageBitmap? {
    if (text.isNullOrBlank()) return null
    val bmp = remember(text, sizePx) { QrCode.generate(text, sizePx) }
    return remember(bmp) { bmp.asImageBitmap() }
}

