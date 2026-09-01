package com.example.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ReportColumn(val title: String, val weight: Float = 1f)
data class ReportSummaryLine(val label: String, val value: String, val emphasize: Boolean = false)

/**
 * Reusable, self-contained PDF builder for every printable document in the
 * app (invoices, statements, treatment plans, and all financial/clinical
 * reports). Draws A4 pages using the native android.graphics.pdf API — no
 * extra library/dependency needed. Right-aligned Persian-first layout with
 * automatic pagination.
 *
 * یکپارچگی با خروجی‌های تصویری:
 *  • قلم: همان [AppFonts] که در کارت‌ها و UI استفاده می‌شود (دیگر فونت پیش‌فرض
 *    سیستم نیست؛ وگرنه PDF و PNG دو ظاهر متفاوت داشتند).
 *  • مقیاس فونت کاربر: [ExportThemeSpec.fontScaleMultiplier] روی همهٔ اندازه‌ها
 *    اعمال می‌شود (قبلاً فقط روی دو-سه Paint اعمال می‌شد).
 *  • رنگ: متن بدنه برای چاپ عمداً سرمه‌ایِ رسمی است، اما خط سربرگ و هدر جدول
 *    از رنگ تم فعال ([ExportThemeSpec.primaryArgb]) گرفته می‌شود.
 *
 * Usage:
 *   PdfReportBuilder(clinicName, doctorName, "صورت‌حساب بیمار", accentColor = themeColorInt)
 *       .withParagraph("...")
 *       .withTable(columns, rows)
 *       .withSummaryLine("جمع کل", "۱٬۲۰۰٬۰۰۰ تومان", emphasize = true)
 *       .withSignatureLines()
 *       .build(context, "statement")
 */
class PdfReportBuilder(
    private val clinicName: String,
    private val doctorName: String,
    private val reportTitle: String,
    private val subtitle: String? = null,
    private val accentColor: Int = Color.rgb(15, 23, 42),
    private val themeSpec: ExportThemeSpec = ExportThemeSpec.current()
) {
    // Official printable documents use a formal, high-contrast classic navy palette,
    // independent of the app's internal UI neon/dark theme.
    private val formalNavyColor: Int = Color.rgb(15, 23, 42)
    private val formalSlateColor: Int = Color.rgb(30, 41, 59)
    private val fontScale: Float = themeSpec.fontScaleMultiplier
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 36f

    private var columns: List<ReportColumn>? = null
    private var rows: List<List<String>> = emptyList()
    private val summaryLines = mutableListOf<ReportSummaryLine>()
    private var showSignatureLines = false
    private var bodyParagraph: String? = null

    fun withTable(columns: List<ReportColumn>, rows: List<List<String>>) = apply {
        this.columns = columns
        this.rows = rows
    }

    fun withParagraph(text: String) = apply { bodyParagraph = text }

    fun withSummaryLine(label: String, value: String, emphasize: Boolean = false) = apply {
        summaryLines.add(ReportSummaryLine(label, value, emphasize))
    }

    fun withSignatureLines() = apply { showSignatureLines = true }

    // ---- قلم‌ها: همیشه از AppFonts (همان فونت کارت‌ها و UI) ----
    // نقش‌ها: Title = خانوادهٔ نمایشی (تیتر/سرستون)، Emphasized = جمع‌های برجسته،
    // Body = متن، Number = ارقامِ لاتین (مونواسپیس اگر نصب باشد).
    private fun font(
        context: Context,
        role: AppFonts.Role,
        sizePt: Float,
        color: Int,
        align: Paint.Align,
        text: String? = null
    ): TextPaint = AppFonts.textPaint(context, role, sizePt * fontScale, color, align, text = text)

    private fun clean(context: Context, text: String): String = AppFonts.sanitize(context, text)

    fun build(context: Context, fileNamePrefix: String = "report"): File {
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = drawHeader(context, canvas, isFirstPage = true)

        fun finishAndStartNewPage() {
            drawFooter(context, canvas, pageNumber)
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = drawHeader(context, canvas, isFirstPage = false)
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - margin - 30f) {
                finishAndStartNewPage()
            }
        }

        val titlePaint = font(context, AppFonts.Role.Title, 15f, Color.BLACK, Paint.Align.RIGHT)
        canvas.drawText(clean(context, reportTitle), pageWidth - margin, y, titlePaint)
        y += 20f * fontScale
        subtitle?.let {
            val sp = font(context, AppFonts.Role.Body, 10.5f, Color.DKGRAY, Paint.Align.RIGHT)
            canvas.drawText(clean(context, it), pageWidth - margin, y, sp)
            y += 18f * fontScale
        }
        y += 6f

        bodyParagraph?.let { text ->
            val tp = font(context, AppFonts.Role.Body, 10.5f, Color.rgb(30, 30, 30), Paint.Align.RIGHT)
            // طول از همان رشتهٔ sanitizeشده گرفته می‌شود: حذف ایموجی طول رشته را کم می‌کند.
            val body = clean(context, text)
            val layout = StaticLayout.Builder.obtain(body, 0, body.length, tp, (pageWidth - margin * 2).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(2f, PERSIAN_LINE_SPACING_MULT)
                .build()
            ensureSpace(layout.height.toFloat() + 12f)
            canvas.save()
            canvas.translate(margin, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + 16f
        }

        val cols = columns
        if (cols != null) {
            y = drawTableHeader(context, canvas, cols, y)
            val rowHeight = 20f * fontScale
            for (row in rows) {
                if (y + rowHeight > pageHeight - margin - 30f) {
                    finishAndStartNewPage()
                    y = drawTableHeader(context, canvas, cols, y)
                }
                y = drawTableRow(context, canvas, cols, row, y, rowHeight)
            }
            y += 12f
        }

        if (summaryLines.isNotEmpty()) {
            ensureSpace(summaryLines.size * 20f * fontScale + 20f)
            val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 16f
            summaryLines.forEach { line ->
                val labelPaint = font(
                    context,
                    if (line.emphasize) AppFonts.Role.Emphasized else AppFonts.Role.Body,
                    if (line.emphasize) 12.5f else 10.5f,
                    if (line.emphasize) formalNavyColor else Color.DKGRAY,
                    Paint.Align.RIGHT
                )
                val valuePaint = font(
                    context,
                    AppFonts.Role.Number,
                    if (line.emphasize) 12.5f else 10.5f,
                    if (line.emphasize) formalNavyColor else Color.rgb(20, 20, 20),
                    Paint.Align.LEFT
                )
                canvas.drawText(clean(context, line.label), pageWidth - margin, y, labelPaint)
                canvas.drawText(clean(context, line.value), margin, y, valuePaint)
                y += 20f * fontScale
            }
        }

        if (showSignatureLines) {
            ensureSpace(70f)
            y += 34f
            val p = Paint().apply { color = Color.GRAY; strokeWidth = 1f }
            canvas.drawLine(margin, y, margin + 150f, y, p)
            canvas.drawLine(pageWidth - margin - 150f, y, pageWidth - margin, y, p)
            val labelPaint = font(context, AppFonts.Role.Body, 9f, Color.GRAY, Paint.Align.CENTER)
            canvas.drawText(clean(context, "امضای بیمار / مراجعه‌کننده"), margin + 75f, y + 14f, labelPaint)
            canvas.drawText(clean(context, "مهر و امضای مطب"), pageWidth - margin - 75f, y + 14f, labelPaint)
        }

        drawFooter(context, canvas, pageNumber)
        document.finishPage(page)

        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${fileNamePrefix}_${UUID.randomUUID().toString().take(8)}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawHeader(context: Context, canvas: Canvas, isFirstPage: Boolean): Float {
        var y = margin + 8f
        val namePaint = font(context, AppFonts.Role.Title, 14f, formalNavyColor, Paint.Align.RIGHT)
        canvas.drawText(clean(context, clinicName), pageWidth - margin, y, namePaint)
        y += 16f * fontScale
        val docPaint = font(context, AppFonts.Role.Body, 9.5f, Color.DKGRAY, Paint.Align.RIGHT)
        canvas.drawText(clean(context, doctorName), pageWidth - margin, y, docPaint)
        if (!isFirstPage) {
            val contPaint = font(context, AppFonts.Role.Body, 9f, Color.GRAY, Paint.Align.LEFT)
            canvas.drawText("(ادامه)", margin, y, contPaint)
        }
        y += 14f
        val linePaint = Paint().apply { color = themeSpec.primaryArgb; strokeWidth = 2f }
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 20f
        return y
    }

    private fun drawFooter(context: Context, canvas: Canvas, pageNumber: Int) {
        val p = font(context, AppFonts.Role.Body, 8f, Color.GRAY, Paint.Align.CENTER)
        canvas.drawText("صفحه $pageNumber  —  تولید شده توسط دنتال‌پرو", (pageWidth / 2).toFloat(), pageHeight - 18f, p)
    }

    private fun drawTableHeader(context: Context, canvas: Canvas, cols: List<ReportColumn>, yStart: Float): Float {
        val y = yStart
        val headerBg = Paint().apply {
            color = UnifiedImageGenerator.blend(Color.rgb(241, 245, 249), themeSpec.primaryArgb, 0.10f)
        }
        canvas.drawRect(margin, y - 13f, pageWidth - margin, y + 6f, headerBg)
        val totalWeight = cols.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.001f)
        val usableWidth = pageWidth - margin * 2
        var xRight = pageWidth - margin
        val paint = font(context, AppFonts.Role.Title, 9.5f, formalSlateColor, Paint.Align.RIGHT)
        for (col in cols) {
            val colWidth = usableWidth * (col.weight / totalWeight)
            canvas.drawText(clean(context, col.title), xRight - 4f, y, paint)
            xRight -= colWidth
        }
        val newY = y + 15f * fontScale
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        canvas.drawLine(margin, newY - 11f, pageWidth - margin, newY - 11f, linePaint)
        return newY
    }

    private fun drawTableRow(context: Context, canvas: Canvas, cols: List<ReportColumn>, row: List<String>, yStart: Float, rowHeight: Float): Float {
        val totalWeight = cols.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.001f)
        val usableWidth = pageWidth - margin * 2
        var xRight = pageWidth - margin
        val textPaint = font(context, AppFonts.Role.Body, 9f, Color.rgb(35, 35, 35), Paint.Align.RIGHT)
        val numberPaint = font(context, AppFonts.Role.Number, 9f, Color.rgb(35, 35, 35), Paint.Align.RIGHT)
        for ((i, col) in cols.withIndex()) {
            val colWidth = usableWidth * (col.weight / totalWeight)
            val text = clean(context, row.getOrElse(i) { "" })
            // ارقام و مبالغ با قلم عددی رندر می‌شوند تا ستون‌ها در PDF بچکند
            // قاعدهٔ یکتا در AppFonts: مونواسپیس فقط برای رشتهٔ کاملاً لاتین
            val paint = if (AppFonts.isMonoSafeText(text) && text.any { it.isDigit() }) numberPaint else textPaint
            canvas.drawText(truncateToWidth(text, paint, colWidth - 8f), xRight - 4f, yStart, paint)
            xRight -= colWidth
        }
        val linePaint = Paint().apply { color = Color.argb(35, 0, 0, 0); strokeWidth = 0.5f }
        canvas.drawLine(margin, yStart + 6f, pageWidth - margin, yStart + 6f, linePaint)
        return yStart + rowHeight
    }

    private fun truncateToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > maxWidth) t = t.dropLast(1)
        return "$t…"
    }
}
