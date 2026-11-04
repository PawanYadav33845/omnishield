package com.omnishield.utility.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import java.io.OutputStream

object PdfUtils {

    fun getPageCount(context: Context, uri: Uri): Int {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
        pfd.use {
            val renderer = PdfRenderer(it)
            val count = renderer.pageCount
            renderer.close()
            return count
        }
    }

    /**
     * Renders a crisp 2x super-sampled bitmap preview of a PDF page.
     */
    fun renderPagePreview(context: Context, uri: Uri, pageIndex: Int = 0): Bitmap? {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        pfd.use {
            val renderer = PdfRenderer(it)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                return null
            }
            val page = renderer.openPage(pageIndex)
            val scale = 2.0f
            val targetWidth = (page.width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            return bitmap
        }
    }

    /**
     * Merges multiple PDF documents into a unified output PDF stream with high-DPI supersampling.
     */
    fun mergePdfs(
        context: Context,
        uris: List<Uri>,
        outputStream: OutputStream
    ): Int {
        val doc = PdfDocument()
        var totalPagesWritten = 0
        val scale = 2.0f

        try {
            for (uri in uris) {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: continue
                pfd.use {
                    val renderer = PdfRenderer(it)
                    for (p in 0 until renderer.pageCount) {
                        val page = renderer.openPage(p)
                        val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, totalPagesWritten + 1).create()
                        val pdfPage = doc.startPage(pageInfo)

                        val targetWidth = (page.width * scale).toInt().coerceAtLeast(1)
                        val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                        pdfPage.canvas.save()
                        pdfPage.canvas.scale(1f / scale, 1f / scale)
                        pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfPage.canvas.restore()

                        doc.finishPage(pdfPage)
                        bitmap.recycle()
                        page.close()
                        totalPagesWritten++
                    }
                    renderer.close()
                }
            }

            doc.writeTo(outputStream)
            outputStream.flush()
        } finally {
            doc.close()
        }
        return totalPagesWritten
    }

    /**
     * Extracts a page range from an existing PDF document into a new PDF with high-DPI supersampling.
     */
    fun splitPdf(
        context: Context,
        uri: Uri,
        startPage1Based: Int,
        endPage1Based: Int,
        outputStream: OutputStream
    ): Int {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot read PDF.")

        val doc = PdfDocument()
        var pagesExtracted = 0
        val scale = 2.0f

        try {
            pfd.use {
                val renderer = PdfRenderer(it)
                val clampedStart = (startPage1Based - 1).coerceIn(0, renderer.pageCount - 1)
                val clampedEnd = (endPage1Based - 1).coerceIn(clampedStart, renderer.pageCount - 1)

                for (p in clampedStart..clampedEnd) {
                    val page = renderer.openPage(p)
                    val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, pagesExtracted + 1).create()
                    val pdfPage = doc.startPage(pageInfo)

                    val targetWidth = (page.width * scale).toInt().coerceAtLeast(1)
                    val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                    pdfPage.canvas.save()
                    pdfPage.canvas.scale(1f / scale, 1f / scale)
                    pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfPage.canvas.restore()

                    doc.finishPage(pdfPage)
                    bitmap.recycle()
                    page.close()
                    pagesExtracted++
                }
                renderer.close()
            }

            doc.writeTo(outputStream)
            outputStream.flush()
        } finally {
            doc.close()
        }
        return pagesExtracted
    }
}
