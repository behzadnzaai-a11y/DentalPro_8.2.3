package com.example.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Converts every PDF page to a separate PNG without truncating long reports.
 *
 * عرض رندرشده حالا از همان جدول [ExportDocKind] می‌آید (نه `page.width * 2`)؛
 * نتیجه: تصویر صفحات PDF دقیقاً هم‌عرض کاغذهای رسمیِ تولیدشده توسط رندرر
 * یکپارچه است و وقتی کنار هم در واتساپ/گالری دیده می‌شوند، اندازه‌شان
 * نامتناسب به‌نظر نمی‌رسد. نسبت A4 هم حفظ می‌شود.
 */
object PdfRendererHelper {

    /** عرض هدف = عرض کاغذهای رسمی (A4 در ۱۵۰DPI). */
    val targetWidthPx: Int get() = ExportDocKind.AdmissionForm.widthPx

    fun renderPdfToImageCards(
        context: Context,
        pdfFile: File
    ): List<File> {
        require(pdfFile.isFile && pdfFile.canRead()) { "PDF is missing or unreadable" }
        val outDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val operationId = UUID.randomUUID().toString().take(8)
        val result = mutableListOf<File>()

        try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    require(renderer.pageCount > 0) { "PDF contains no pages" }
                    for (index in 0 until renderer.pageCount) {
                        renderer.openPage(index).use { page ->
                            // یک صفحه در هر لحظه در حافظه؛ ارتفاع از همان نسبت
                            // A4 صفحه به‌دست می‌آید (نه عدد دلخواه ثابت).
                            val width = targetWidthPx
                            val height = (width.toFloat() * page.height / page.width).roundToInt()
                                .coerceAtLeast(ExportDocKind.MIN_HEIGHT_PX)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            try {
                                Canvas(bitmap).drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val outFile = File(
                                    outDir,
                                    "report_${operationId}_page_${(index + 1).toString().padStart(3, '0')}.png"
                                )
                                FileOutputStream(outFile).use { output ->
                                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                                        "Could not encode PDF page ${index + 1}"
                                    }
                                }
                                result += outFile
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
            return result
        } catch (error: Exception) {
            result.forEach(File::delete)
            throw error
        }
    }
}
