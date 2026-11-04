package com.omnishield.utility.core.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

/**
 * Air-gapped Offline QR Code Generator Utility.
 * Encodes text, Wi-Fi credentials, passwords, and cryptographic payloads
 * completely offline with selectable Error Correction Levels.
 */
object QrCodeUtils {

    enum class QrErrorCorrection {
        LOW, MEDIUM, QUARTILE, HIGH
    }

    /**
     * Generates a square QR Code Bitmap from the provided content string.
     */
    fun generateQrBitmap(
        content: String,
        dimensionPx: Int = 600,
        errorCorrection: QrErrorCorrection = QrErrorCorrection.HIGH,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Result<Bitmap> = runCatching {
        if (content.isBlank()) {
            throw IllegalArgumentException("QR content cannot be blank")
        }

        val zxingEcLevel = when (errorCorrection) {
            QrErrorCorrection.LOW -> ErrorCorrectionLevel.L
            QrErrorCorrection.MEDIUM -> ErrorCorrectionLevel.M
            QrErrorCorrection.QUARTILE -> ErrorCorrectionLevel.Q
            QrErrorCorrection.HIGH -> ErrorCorrectionLevel.H
        }

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, zxingEcLevel)
            put(EncodeHintType.MARGIN, 2) // 2 modules quiet zone
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, dimensionPx, dimensionPx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
            }
        }

        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    /**
     * Formats Wi-Fi credentials into standard Wi-Fi QR schema:
     * WIFI:T:WPA;S:MySSID;P:MyPassword;;
     */
    fun formatWifiSchema(
        ssid: String,
        password: String,
        securityType: String = "WPA"
    ): String {
        return "WIFI:T:$securityType;S:$ssid;P:$password;;"
    }
}
