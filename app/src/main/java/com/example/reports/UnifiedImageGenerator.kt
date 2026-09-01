package com.example.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.util.Base64
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * UnifiedImageGenerator — تنها تابعِ رندرِ تصویر در برنامه (جز کارتِ بانکی که رندرِ
 * اختصاصی دارد). همهٔ خروجی‌ها از [generateUnifiedImage] رد می‌شوند؛ هیچ‌جا نباید
 * مستقیم Bitmap/Canvas/TextPaint ساخته شود. هر ژنراتور فقط یک [UnifiedDocument]
 * توصیفی می‌سازد؛ عرض/مقیاس از [ExportDocKind]، رنگ از [ExportThemeSpec] و قلم از
 * [AppFonts] می‌آید. دو پاس دارد (اندازه‌گیری سپس رندر) تا ارتفاع از محتوا بیاید و متن بریده نشود.
 */
object UnifiedImageGenerator {

    // ------------------------------------------------------------------
    //  مدل سند
    // ------------------------------------------------------------------

    /** حالت بصری سند؛ رنگ‌های هر حالت از تم فعال مشتق می‌شود. */
    enum class DocTone {
        /** کارت تیرهٔ نئونی برای ارسال در چت (پیش‌فرض). */
        NeonDark,

        /** کاغذ روشن رسمی برای چاپ/بایگانی (پذیرش، رضایت‌نامه، نسخه، …). */
        PaperLight,

        /** کاغذ با هدر هشداری — اخطار تاخیر و اخطاریه‌های حقوقی. */
        PaperAlert
    }

    /** سلسله‌مراتب ظاهری متن. */
    enum class Emphasis { Quiet, Body, Strong, Accent, Danger, Warning, Success }

    /** رنگ معنایی کادر تأکید — عمداً از تم مستقل است (معاملهٔ پزشکی/مالی). */
    enum class CalloutTone { Info, Warning, Danger, Success }

    /** تراز عنوان سند. */
    enum class TitleAlign { Start, Center }

    /** یک سطر «برچسب ↔ مقدار». */
    data class KeyRow(
        val label: String,
        val value: String,
        val emphasize: Boolean = false,
        val monospace: Boolean = false
    )

    /** ستون جدول. */
    data class TableColumn(
        val title: String,
        val weight: Float = 1f,
        val center: Boolean = false
    )

    /** ستون تیتردار در بلوک اطلاعات دونیم‌شده. */
    data class InfoColumn(val title: String, val lines: List<String>)

    /** بلوک محتوایی — کل تنوع خروجی‌ها فقط با همین بلوک‌ها بیان می‌شود. */
    sealed interface DocBlock {
        data class Section(val title: String, val emphasis: Emphasis = Emphasis.Accent) : DocBlock
        data class Lines(val items: List<String>, val emphasis: Emphasis = Emphasis.Body) : DocBlock
        data class Paragraph(val text: String, val emphasis: Emphasis = Emphasis.Body, val boxed: Boolean = false) : DocBlock
        data class KeyValues(val rows: List<KeyRow>, val boxed: Boolean = true) : DocBlock
        data class Table(
            val columns: List<TableColumn>,
            val rows: List<List<String>>,
            /** رنگ معنایی هر ردیف (مثلاً هزینه = قرمز، پرداخت = سبز). */
            val rowEmphasis: List<Emphasis> = emptyList()
        ) : DocBlock
        data class Callout(
            val text: String,
            val tone: CalloutTone = CalloutTone.Info,
            val title: String? = null,
            val monospace: Boolean = false
        ) : DocBlock
        data class InfoColumns(val right: InfoColumn, val left: InfoColumn) : DocBlock
        data class SignatureBlock(
            val signatureBase64: String? = null,
            val legalNote: String? = null,
            val stampLabel: String? = null
        ) : DocBlock
        data class VerificationTrio(
            val fingerprintNote: String,
            val signatureNote: String,
            val stampTitle: String,
            val stampSubtitle: String,
            val signatureBase64: String? = null
        ) : DocBlock
        data class Banner(val text: String, val subText: String? = null) : DocBlock
    }

    /** سند کاملی که به رندرر داده می‌شود. */
    data class UnifiedDocument(
        val kind: ExportDocKind,
        val title: String,
        val fileNamePrefix: String,
        val tone: DocTone = DocTone.NeonDark,
        val subtitle: String? = null,
        val clinicName: String? = null,
        val doctorName: String? = null,
        val metaRight: String? = null,
        val metaLeft: String? = null,
        val titleAlign: TitleAlign = TitleAlign.Start,
        val topIconResId: Int? = null,
        val blocks: List<DocBlock> = emptyList(),
        val footNote: String? = null,
        val showBrand: Boolean = true
    )

    /** حذف بلوک‌های بی‌محتوا تا ارتفاع، محتوای واقعی بماند. */
    private fun List<DocBlock>.compact(): List<DocBlock> = filter { block ->
        when (block) {
            is DocBlock.Lines -> block.items.any { it.isNotBlank() }
            is DocBlock.KeyValues -> block.rows.isNotEmpty()
            is DocBlock.Table -> block.rows.isNotEmpty()
            is DocBlock.InfoColumns -> block.right.lines.isNotEmpty() || block.left.lines.isNotEmpty()
            else -> true
        }
    }

    // ------------------------------------------------------------------
    //  رندرر یکپارچه
    // ------------------------------------------------------------------

    /**
     * تنها نقطهٔ تولید تصویر سند.
     * @param theme تم فعال برنامه؛ از `viewModel.currentExportTheme()` گرفته می‌شود.
     */
    fun generateUnifiedImage(context: Context, document: UnifiedDocument, theme: ExportThemeSpec): File {
        val style = CardStyle(context, document, theme)
        val ops = ArrayList<Op>()

        ops += style.headerOp(document)
        document.blocks.compact().forEach { block ->
            ops += Op(document.kind.blockGap)
            ops += style.blockOp(block)
        }
        document.footNote?.takeIf { it.isNotBlank() }?.let { note ->
            ops += Op(document.kind.blockGap)
            ops += style.footNoteOp(note)
        }
        if (document.showBrand) {
            ops += Op((10 * style.s).roundToInt())
            ops += style.brandOp()
        }

        val contentTop = (34f * style.s).roundToInt()
        val contentBottom = (38f * style.s).roundToInt()
        val measuredHeight = contentTop + ops.sumOf { it.height } + contentBottom
        val height = max(measuredHeight, max(ExportDocKind.MIN_HEIGHT_PX, document.kind.heightPx))

        val bitmap = Bitmap.createBitmap(document.kind.widthPx, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        style.paintBackground(canvas, height)
        var y = contentTop
        ops.forEach { op ->
            op.draw(canvas, y)
            y += op.height
        }
        return ExportFiles.writeBitmap(context, bitmap, document.fileNamePrefix)
    }

    // ------------------------------------------------------------------
    //  ابزار داخلی
    // ------------------------------------------------------------------

    /** یک بلوک آمادهٔ رندر: ارتفاع + تابع ترسیم با مبدأ y. */
    private class Op(val height: Int, val draw: (Canvas, Int) -> Unit = { _, _ -> })

    /** جمع‌کنندهٔ عمودی زیر-بلوک‌ها. */
    private class OpBuilder {
        private val parts = ArrayList<Op>()
        private var total: Int = 0

        fun add(height: Int, draw: (Canvas, Int) -> Unit = { _, _ -> }) {
            if (height <= 0) return
            parts += Op(height, draw)
            total += height
        }

        fun add(op: Op) {
            if (op.height <= 0) return
            parts += op
            total += op.height
        }

        fun build(): Op {
            val snapshot = ArrayList(parts)
            val height = total
            return Op(height) { canvas, startY ->
                var y = startY
                snapshot.forEach { part ->
                    part.draw(canvas, y)
                    y += part.height
                }
            }
        }
    }

    /**
     * تمام تصمیم‌های ظاهری (رنگ، اندازه، فاصله، کنتراست) فقط اینجاست.
     */
    private class CardStyle(
        val context: Context,
        val doc: UnifiedDocument,
        val theme: ExportThemeSpec
    ) {
        /** ضریب هندسه (از عرض خروجی) و ضریب متن (هندسه × مقیاس فونت کاربر). */
        val s: Float = doc.kind.scale
        val fs: Float = s * theme.fontScaleMultiplier
        val width: Int = doc.kind.widthPx
        val margin: Int = doc.kind.margin
        val innerWidth: Int get() = width - margin * 2
        val radius: Float get() = 18f * s
        val border: Float get() = max(1.5f, 2.5f * s)

        val isPaper: Boolean = doc.tone != DocTone.NeonDark

        // ---- رنگ‌های مشتق‌شده از تم فعال ----
        val paperTop: Int = if (isPaper) lighten(theme.surfaceArgb, 0.94f) else theme.backgroundArgb
        val paperBottom: Int = if (isPaper) lighten(theme.backgroundArgb, 0.9f) else theme.surfaceArgb
        val onPaper: Int = readableOn(Color.WHITE, paperTop, 4.2f)
        val bodyColor: Int = if (isPaper) readableOn(darken(onPaper, 0.12f), paperTop, 5f) else Color.WHITE
        val mutedColor: Int = blend(bodyColor, paperTop, 0.42f)
        val faintColor: Int = blend(bodyColor, paperTop, 0.62f)
        val accent: Int = readableOn(theme.primaryArgb, paperTop, 3.3f)
        val zebraFill: Int = withAlpha(bodyColor, 0.05f)
        val hairline: Int = withAlpha(accent, 0.45f)
        val cardFill: Int = if (isPaper) lighten(paperTop, 0.55f) else withAlpha(Color.WHITE, 0.04f)
        val cardBorder: Int = withAlpha(if (isPaper) darken(onPaper, 0.5f) else accent, 0.32f)
        val headerStart: Int = if (doc.tone == DocTone.PaperAlert) ExportSemanticColors.DANGER_DARK else theme.gradientStartArgb
        val headerEnd: Int = if (doc.tone == DocTone.PaperAlert) ExportSemanticColors.DANGER else theme.primaryArgb
        val onHeader: Int = readableOn(Color.WHITE, headerStart, 4.5f)
        val onHeaderMuted: Int = blend(onHeader, headerStart, 0.28f)

        // ---- اندازه‌ها (px در عرض مرجع ۱۰۸۰) ----
        val sizeDisplay = 43f * fs
        val sizeClinic = 29f * fs
        val sizeMeta = 21f * fs
        val sizeSection = 27f * fs
        val sizeBody = 23.5f * fs
        val sizeLabel = 21.5f * fs
        val sizeValue = 24f * fs
        val sizeKeyEmphasis = 30f * fs
        val sizeSmall = 19f * fs
        val sizeCaption = 17f * fs
        val sizeTableHeader = 20.5f * fs
        val sizeTableCell = 21f * fs

        fun paint(
            role: AppFonts.Role,
            size: Float,
            color: Int,
            align: Paint.Align = Paint.Align.LEFT,
            text: String? = null
        ): TextPaint = AppFonts.textPaint(context, role, size, color, align, text = text)

        /**
         * قلم «ارقام» (مونواسپیس). فقط وقتی صدا زده شود که از پیش
         * `AppFonts.isMonoSafeText(text)` بررسی شده باشد؛ رشتهٔ نمونه فقط برای
         * تحمیل چهرهٔ مونواسپیس به رجیستری است. اگر فونت مونواسپیس نصب نباشد،
         * خودکار به همان بدنه (که ارقامش tabular است) می‌افتد.
         */
        fun monoPaint(size: Float, color: Int, align: Paint.Align = Paint.Align.LEFT): TextPaint =
            AppFonts.textPaint(context, AppFonts.Role.Number, size, color, align, text = "0")

        fun colorFor(emphasis: Emphasis): Int = when (emphasis) {
            Emphasis.Quiet -> mutedColor
            Emphasis.Body -> bodyColor
            Emphasis.Strong -> bodyColor
            Emphasis.Accent -> accent
            Emphasis.Danger -> readableOn(ExportSemanticColors.DANGER, paperTop, 4.2f)
            Emphasis.Warning -> readableOn(ExportSemanticColors.WARNING, paperTop, 4.2f)
            Emphasis.Success -> readableOn(ExportSemanticColors.SUCCESS, paperTop, 4.2f)
        }

        private fun boldFor(emphasis: Emphasis): Boolean =
            emphasis == Emphasis.Strong || emphasis == Emphasis.Accent ||
                emphasis == Emphasis.Danger || emphasis == Emphasis.Warning

        // --------------------------------------------------------------
        //  هدر
        // --------------------------------------------------------------
        fun headerOp(document: UnifiedDocument): Op {
            val rows = ArrayList<Op>()
            val icon = document.topIconResId?.let { decodeResourceBitmap(it) }
            val iconSize = 58f * s

            if (!document.clinicName.isNullOrBlank() || !document.doctorName.isNullOrBlank()) {
                val headerOnPanel = isPaper
                val clinicPaint = paint(AppFonts.Role.Title, sizeClinic, if (headerOnPanel) onHeader else accent)
                val doctorPaint = paint(AppFonts.Role.Body, sizeMeta, if (headerOnPanel) onHeaderMuted else mutedColor)
                val clinicText = document.clinicName.orEmpty()
                val doctorText = document.doctorName.orEmpty()
                val clinicLayout = layout(clinicText, clinicPaint, (innerWidth * 0.6f).roundToInt(), Layout.Alignment.ALIGN_NORMAL)
                val doctorLayout = layout(doctorText, doctorPaint, (innerWidth * 0.38f).roundToInt(), Layout.Alignment.ALIGN_OPPOSITE)
                val rowHeight = max(clinicLayout.height, doctorLayout.height)
                rows += Op(rowHeight) { canvas, y ->
                    if (icon != null) {
                        val dest = RectF(margin.toFloat(), y.toFloat(), margin + iconSize, y + iconSize)
                        val p = Paint(Paint.ANTI_ALIAS_FLAG)
                        p.colorFilter = PorterDuffColorFilter(if (headerOnPanel) onHeader else accent, PorterDuff.Mode.SRC_IN)
                        canvas.drawBitmap(icon, null, dest, p)
                    }
                    val textLeftShift = if (icon != null) iconSize + (12 * s) else 0f
                    canvas.save()
                    canvas.translate((width - margin - clinicLayout.width).toFloat(), y.toFloat())
                    clinicLayout.draw(canvas)
                    canvas.restore()
                    canvas.save()
                    canvas.translate((margin + textLeftShift), y.toFloat())
                    doctorLayout.draw(canvas)
                    canvas.restore()
                }
                rows += Op((10 * s).roundToInt())
            }

            val titleAlign = if (document.titleAlign == TitleAlign.Center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
            val titlePaint = paint(AppFonts.Role.Title, sizeDisplay, if (isPaper) onHeader else bodyColor)
            val titleLayout = layout(document.title, titlePaint, innerWidth, titleAlign)
            rows += Op(titleLayout.height + (10 * s).roundToInt()) { canvas, y ->
                canvas.save()
                canvas.translate(margin.toFloat(), y.toFloat())
                titleLayout.draw(canvas)
                canvas.restore()
            }

            document.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                val subPaint = paint(AppFonts.Role.Body, sizeMeta + 2f * fs, if (isPaper) onHeaderMuted else accent)
                val subLayout = layout(sub, subPaint, innerWidth, Layout.Alignment.ALIGN_NORMAL)
                rows += Op(subLayout.height + (6 * s).roundToInt()) { canvas, y ->
                    canvas.save()
                    canvas.translate(margin.toFloat(), y.toFloat())
                    subLayout.draw(canvas)
                    canvas.restore()
                }
            }

            if (!document.metaRight.isNullOrBlank() || !document.metaLeft.isNullOrBlank()) {
                val metaPaint = paint(AppFonts.Role.Body, sizeMeta, if (isPaper) onHeaderMuted else mutedColor)
                val halfW = (innerWidth / 2f - 12f * s).roundToInt()
                val rightLayout = document.metaRight?.let { layout(it, metaPaint, halfW, Layout.Alignment.ALIGN_NORMAL) }
                val leftLayout = document.metaLeft?.let { layout(it, metaPaint, halfW, Layout.Alignment.ALIGN_OPPOSITE) }
                rows += Op(max(rightLayout?.height ?: 0, leftLayout?.height ?: 0) + (4 * s).roundToInt()) { canvas, y ->
                    rightLayout?.let {
                        canvas.save()
                        canvas.translate((margin + innerWidth / 2 + 12 * s), y.toFloat())
                        it.draw(canvas)
                        canvas.restore()
                    }
                    leftLayout?.let {
                        canvas.save()
                        canvas.translate(margin.toFloat(), y.toFloat())
                        it.draw(canvas)
                        canvas.restore()
                    }
                }
            }

            val hasPanel = isPaper
            val topPad = if (hasPanel) (26 * s).roundToInt() else (4 * s).roundToInt()
            val bottomPad = if (hasPanel) (26 * s).roundToInt() else (12 * s).roundToInt()
            val panelHeight = topPad + rows.sumOf { it.height } + bottomPad

            val builder = OpBuilder()
            builder.add(panelHeight) { canvas, y ->
                if (hasPanel) {
                    val bg = Paint(Paint.ANTI_ALIAS_FLAG)
                    bg.shader = LinearGradient(
                        0f, y.toFloat(), width.toFloat(), (y + panelHeight).toFloat(),
                        intArrayOf(headerStart, headerEnd), null, Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(0f, y.toFloat(), width.toFloat(), (y + panelHeight).toFloat(), bg)
                } else {
                    val band = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
                    canvas.drawRect(0f, 0f, width.toFloat(), 8f * s, band)
                }
                var cy = y + topPad
                rows.forEach { row ->
                    row.draw(canvas, cy)
                    cy += row.height
                }
            }
            builder.add((16 * s).roundToInt())
            builder.add((2 * s).roundToInt()) { canvas, yy ->
                val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = hairline
                    strokeWidth = max(1f, 1.5f * s)
                }
                canvas.drawLine(margin.toFloat(), yy.toFloat(), (width - margin).toFloat(), yy.toFloat(), line)
            }
            builder.add((14 * s).roundToInt())
            return builder.build()
        }

        // --------------------------------------------------------------
        //  بلوک‌ها
        // --------------------------------------------------------------
        fun blockOp(block: DocBlock): Op = when (block) {
            is DocBlock.Section -> sectionOp(block)
            is DocBlock.Lines -> linesOp(block)
            is DocBlock.Paragraph -> paragraphOp(block)
            is DocBlock.KeyValues -> keyValuesOp(block)
            is DocBlock.Table -> tableOp(block)
            is DocBlock.Callout -> calloutOp(block)
            is DocBlock.InfoColumns -> infoColumnsOp(block)
            is DocBlock.SignatureBlock -> signatureOp(block)
            is DocBlock.VerificationTrio -> verificationOp(block)
            is DocBlock.Banner -> bannerOp(block)
        }

        private fun sectionOp(block: DocBlock.Section): Op {
            val builder = OpBuilder()
            builder.add(textOp(block.title, sizeSection, colorFor(block.emphasis), AppFonts.Role.Title))
            builder.add((8 * s).roundToInt()) { canvas, y ->
                val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorFor(block.emphasis)
                    strokeWidth = max(2f, 3f * s)
                }
                canvas.drawLine(margin.toFloat(), y.toFloat(), margin + 92f * s, y.toFloat(), line)
            }
            return builder.build()
        }

        private fun linesOp(block: DocBlock.Lines): Op {
            val builder = OpBuilder()
            // تأکید داخل فهرست = همان خانوادهٔ متن با وزن بولد (نه فونت تیتر)
            val role = if (boldFor(block.emphasis)) AppFonts.Role.Emphasized else AppFonts.Role.Body
            block.items.filter { it.isNotBlank() }.forEach { line ->
                builder.add(textOp(line, sizeBody, colorFor(block.emphasis), role, gap = (8 * s).roundToInt()))
            }
            return builder.build()
        }

        /** یک بلوک متن‌یک‌خطی: یک‌بار اندازه‌گیری و همان چیدمان در پاس رندر. */
        private fun textOp(
            text: String,
            size: Float,
            color: Int,
            role: AppFonts.Role,
            align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
            widthOverride: Int? = null,
            xOverride: Float? = null,
            gap: Int = 0
        ): Op {
            val paint = paint(role, size, color)
            val lay = layout(text, paint, widthOverride ?: innerWidth, align)
            val left = xOverride ?: margin.toFloat()
            return Op(lay.height + gap) { canvas, y ->
                canvas.save()
                canvas.translate(left, y.toFloat())
                lay.draw(canvas)
                canvas.restore()
            }
        }

        private fun paragraphOp(block: DocBlock.Paragraph): Op {
            val role = if (boldFor(block.emphasis)) AppFonts.Role.Emphasized else AppFonts.Role.Body
            val color = colorFor(block.emphasis)
            val pad = (18 * s).roundToInt()
            val builder = OpBuilder()
            if (block.boxed) {
                val paint = paint(role, sizeBody, color)
                val lay = layout(block.text, paint, innerWidth - pad * 2, Layout.Alignment.ALIGN_NORMAL)
                val height = lay.height + pad * 2
                builder.add(height) { canvas, y ->
                    val rect = RectF(margin.toFloat(), y.toFloat(), (width - margin).toFloat(), (y + height).toFloat())
                    canvas.drawRoundRect(rect, radius, radius, fillPaint(cardFill))
                    canvas.drawRoundRect(rect, radius, radius, strokePaint(cardBorder, border))
                    canvas.save()
                    canvas.translate((margin + pad).toFloat(), (y + pad).toFloat())
                    lay.draw(canvas)
                    canvas.restore()
                }
            } else {
                builder.add(textOp(block.text, sizeBody, color, role))
            }
            return builder.build()
        }

        private fun keyValuesOp(block: DocBlock.KeyValues): Op {
            val pad = (18 * s).roundToInt()
            val rowGap = (12 * s).roundToInt()
            val usable = if (block.boxed) innerWidth - pad * 2 else innerWidth
            val labelW = (usable * 0.42f).roundToInt().coerceAtLeast(80)
            val valueW = (usable - labelW - (16 * s).roundToInt()).coerceAtLeast(80)

            val labelPaint = paint(AppFonts.Role.Body, sizeLabel, mutedColor)
            val valuePaint = paint(AppFonts.Role.Body, sizeValue, bodyColor)
            val valueEmphasized = paint(AppFonts.Role.Emphasized, sizeKeyEmphasis, accent)
            val valueMono = monoPaint(sizeValue + 1f * fs, if (block.rows.any { it.emphasize }) accent else bodyColor)

            val labels = ArrayList<StaticLayout>(block.rows.size)
            val values = ArrayList<StaticLayout>(block.rows.size)
            val heights = ArrayList<Int>(block.rows.size)
            block.rows.forEach { row ->
                val label = layout(row.label, labelPaint, labelW, Layout.Alignment.ALIGN_NORMAL)
                val paintForValue = when {
                    // مقدار فقط وقتی مونواسپیس است که اصلاً فارسی در آن نباشد
                    row.monospace && AppFonts.isMonoSafeText(row.value) -> valueMono
                    row.emphasize -> valueEmphasized
                    else -> valuePaint
                }
                val value = layout(row.value, paintForValue, valueW, Layout.Alignment.ALIGN_OPPOSITE)
                labels += label
                values += value
                heights += max(label.height, value.height)
            }
            val bodyHeight = heights.sum() + rowGap * (block.rows.size - 1).coerceAtLeast(0)
            val totalHeight = if (block.boxed) bodyHeight + pad * 2 else bodyHeight
            val boxed = block.boxed

            return Op(totalHeight) { canvas, y ->
                if (boxed) {
                    val rect = RectF(margin.toFloat(), y.toFloat(), (width - margin).toFloat(), (y + totalHeight).toFloat())
                    canvas.drawRoundRect(rect, radius, radius, fillPaint(cardFill))
                    canvas.drawRoundRect(rect, radius, radius, strokePaint(cardBorder, border))
                }
                var cy = y + (if (boxed) pad else 0)
                labels.forEachIndexed { index, label ->
                    val rowHeight = heights[index]
                    if (index % 2 == 1) {
                        canvas.drawRect(margin.toFloat(), cy - (6 * s), (width - margin).toFloat(), cy + rowHeight + (6 * s), fillPaint(zebraFill))
                    }
                    canvas.save()
                    canvas.translate((width - margin - labelW).toFloat(), cy.toFloat())
                    label.draw(canvas)
                    canvas.restore()
                    canvas.save()
                    canvas.translate((margin + (if (boxed) pad else 0)).toFloat(), cy.toFloat())
                    values[index].draw(canvas)
                    canvas.restore()
                    if (index != labels.lastIndex) {
                        canvas.drawLine(margin + pad * 0.5f, cy + rowHeight + rowGap * 0.5f, width - margin - pad * 0.5f, cy + rowHeight + rowGap * 0.5f, strokePaint(withAlpha(hairline, 0.5f), max(1f, 0.8f * s)))
                    }
                    cy += rowHeight + rowGap
                }
            }
        }

        private fun tableOp(block: DocBlock.Table): Op {
            val headerBg = withAlpha(accent, if (isPaper) 0.14f else 0.2f)
            val headerTextColor = readableOn(accent, blend(paperTop, accent, 0.16f), 4.4f)
            val headerPaint = paint(AppFonts.Role.Title, sizeTableHeader, headerTextColor)
            val cellPaint = paint(AppFonts.Role.Body, sizeTableCell, bodyColor)
            val totalWeight = block.columns.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.001f)
            val colWidths = block.columns.map { (innerWidth * (it.weight / totalWeight)).roundToInt().coerceAtLeast(48) }

            fun alignFor(center: Boolean, isFirst: Boolean): Layout.Alignment = when {
                center -> Layout.Alignment.ALIGN_CENTER
                isFirst -> Layout.Alignment.ALIGN_NORMAL
                else -> Layout.Alignment.ALIGN_OPPOSITE
            }

            val headerCells = block.columns.mapIndexed { i, col ->
                layout(col.title, headerPaint, (colWidths[i] - 12 * s).roundToInt(), alignFor(col.center, i == 0))
            }
            val headerHeight = max(headerCells.maxOf { it.height }, (34 * s).roundToInt()) + (16 * s).roundToInt()

            val rowCells = ArrayList<List<StaticLayout>>(block.rows.size)
            val rowHeights = ArrayList<Int>(block.rows.size)
            fun paintForRow(rowIndex: Int): TextPaint {
                val emphasis = block.rowEmphasis.getOrNull(rowIndex) ?: return cellPaint
                return if (emphasis == Emphasis.Body || emphasis == Emphasis.Strong || emphasis == Emphasis.Quiet) {
                    cellPaint
                } else {
                    paint(AppFonts.Role.Body, sizeTableCell, colorFor(emphasis))
                }
            }
            // سلول‌های عددیِ لاتین با قلم ارقام رندر می‌شوند تا ستون‌های پول/تعداد
            // هم‌عرض بمانند؛ برای هر رنگ یک بار ساخته می‌شود (نه هر سلول).
            val monoByColor = HashMap<Int, TextPaint>()
            fun paintForCell(text: String, fallback: TextPaint): TextPaint {
                if (text.isEmpty() || !AppFonts.isMonoSafeText(text)) return fallback
                if (!text.any { it.isDigit() }) return fallback
                return monoByColor.getOrPut(fallback.color) { monoPaint(sizeTableCell, fallback.color) }
            }
            block.rows.forEachIndexed { rowIndex, row ->
                val rowPaint = paintForRow(rowIndex)
                val cells = block.columns.mapIndexed { i, col ->
                    val text = row.getOrElse(i) { "" }
                    layout(text, paintForCell(text, rowPaint), (colWidths[i] - 14 * s).roundToInt(), alignFor(col.center, i == 0))
                }
                rowCells += cells
                rowHeights += max(cells.maxOf { it.height }, (30 * s).roundToInt()) + (14 * s).roundToInt()
            }
            val totalHeight = (12 * s).roundToInt() + headerHeight + rowHeights.sum() + (10 * s).roundToInt()

            return Op(totalHeight) { canvas, y ->
                val left = margin.toFloat()
                val right = (width - margin).toFloat()
                val tableTop = y + (12 * s)
                canvas.drawRect(left, tableTop, right, tableTop + headerHeight, fillPaint(headerBg))
                var xRight = right
                headerCells.forEachIndexed { i, cell ->
                    val cellLeft = xRight - colWidths[i]
                    canvas.drawRect(cellLeft, tableTop, xRight, tableTop + headerHeight, strokePaint(hairline, max(1f, 1.1f * s)))
                    canvas.save()
                    canvas.translate(cellLeft + (7 * s), tableTop + (8 * s))
                    cell.draw(canvas)
                    canvas.restore()
                    xRight = cellLeft
                }
                var rowTop = tableTop + headerHeight
                rowCells.forEachIndexed { rowIndex, cells ->
                    val rowHeight = rowHeights[rowIndex]
                    if (rowIndex % 2 == 1) canvas.drawRect(left, rowTop, right, rowTop + rowHeight, fillPaint(zebraFill))
                    var rx = right
                    cells.forEachIndexed { i, cell ->
                        val cellLeft = rx - colWidths[i]
                        canvas.drawRect(cellLeft, rowTop, rx, rowTop + rowHeight, strokePaint(hairline, max(1f, s)))
                        canvas.save()
                        canvas.translate(cellLeft + (7 * s), rowTop + (7 * s))
                        cell.draw(canvas)
                        canvas.restore()
                        rx = cellLeft
                    }
                    rowTop += rowHeight
                }
                canvas.drawRect(left, tableTop, right, rowTop, strokePaint(accent, max(1.5f, 2.2f * s)))
            }
        }

        private fun calloutOp(block: DocBlock.Callout): Op {
            val (fg, bg) = calloutColors(block.tone)
            val pad = (18 * s).roundToInt()
            val contentW = innerWidth - pad * 2
            val titleLayout = block.title?.let {
                layout(it, paint(AppFonts.Role.Title, sizeSection, fg), contentW, Layout.Alignment.ALIGN_NORMAL)
            }
            val size = if (block.monospace) sizeValue + 2f * fs else sizeBody
            val bodyPaint = if (block.monospace && AppFonts.isMonoSafeText(block.text)) {
                monoPaint(size, fg)
            } else {
                paint(AppFonts.Role.Body, size, fg)
            }
            val textLayout = layout(block.text, bodyPaint, contentW, Layout.Alignment.ALIGN_NORMAL)
            val gap = if (titleLayout != null) (8 * s).roundToInt() else 0
            val height = pad * 2 + (titleLayout?.height ?: 0) + gap + textLayout.height

            return Op(height) { canvas, y ->
                val rect = RectF(margin.toFloat(), y.toFloat(), (width - margin).toFloat(), (y + height).toFloat())
                canvas.drawRoundRect(rect, radius, radius, fillPaint(bg))
                canvas.drawRoundRect(rect, radius, radius, strokePaint(fg, max(1.5f, 2.2f * s)))
                var cy = y + pad
                titleLayout?.let {
                    canvas.save()
                    canvas.translate((margin + pad).toFloat(), cy.toFloat())
                    it.draw(canvas)
                    canvas.restore()
                    cy += it.height + gap
                }
                canvas.save()
                canvas.translate((margin + pad).toFloat(), cy.toFloat())
                textLayout.draw(canvas)
                canvas.restore()
            }
        }

        private fun infoColumnsOp(block: DocBlock.InfoColumns): Op {
            val pad = (18 * s).roundToInt()
            val gap = (24 * s).roundToInt()
            val colW = ((innerWidth - pad * 2 - gap) / 2).coerceAtLeast(120)
            val titlePaint = paint(AppFonts.Role.Title, sizeLabel, accent)
            val linePaint = paint(AppFonts.Role.Body, sizeValue, bodyColor)

            fun measure(column: InfoColumn): Pair<StaticLayout, StaticLayout> {
                val title = layout(column.title, titlePaint, colW, Layout.Alignment.ALIGN_NORMAL)
                val body = layout(column.lines.filter { it.isNotBlank() }.joinToString("\n"), linePaint, colW, Layout.Alignment.ALIGN_NORMAL)
                return title to body
            }

            val right = measure(block.right)
            val left = measure(block.left)
            fun columnHeight(pair: Pair<StaticLayout, StaticLayout>): Int = pair.first.height + (6 * s).roundToInt() + pair.second.height
            val height = max(columnHeight(right), columnHeight(left)) + pad * 2

            return Op(height) { canvas, y ->
                val rect = RectF(margin.toFloat(), y.toFloat(), (width - margin).toFloat(), (y + height).toFloat())
                canvas.drawRoundRect(rect, radius, radius, fillPaint(cardFill))
                canvas.drawRoundRect(rect, radius, radius, strokePaint(cardBorder, border))
                canvas.drawLine(rect.centerX(), y + pad * 0.6f, rect.centerX(), y + height - pad * 0.6f, strokePaint(cardBorder, max(1f, s)))
                var cy = y + pad
                canvas.save()
                canvas.translate((width - margin - pad - colW).toFloat(), cy.toFloat())
                right.first.draw(canvas)
                canvas.restore()
                cy += right.first.height + (6 * s).roundToInt()
                canvas.save()
                canvas.translate((width - margin - pad - colW).toFloat(), cy.toFloat())
                right.second.draw(canvas)
                canvas.restore()
                var ly = y + pad
                canvas.save()
                canvas.translate((margin + pad).toFloat(), ly.toFloat())
                left.first.draw(canvas)
                canvas.restore()
                ly += left.first.height + (6 * s).roundToInt()
                canvas.save()
                canvas.translate((margin + pad).toFloat(), ly.toFloat())
                left.second.draw(canvas)
                canvas.restore()
            }
        }

        private fun signatureOp(block: DocBlock.SignatureBlock): Op {
            val builder = OpBuilder()
            if (!block.legalNote.isNullOrBlank()) {
                val paint = paint(AppFonts.Role.Body, sizeBody, mutedColor)
                val lay = layout(block.legalNote, paint, innerWidth, Layout.Alignment.ALIGN_NORMAL)
                builder.add(lay.height + (12 * s).roundToInt()) { canvas, y ->
                    canvas.save()
                    canvas.translate(margin.toFloat(), y.toFloat())
                    lay.draw(canvas)
                    canvas.restore()
                }
            }
            val sig = decodeSignature(block.signatureBase64)
            if (sig != null) {
                val boxHeight = (140f * s).roundToInt()
                val maxW = innerWidth * 0.42f
                val factor = min(maxW / sig.width, boxHeight.toFloat() / sig.height)
                val drawW = (sig.width * factor)
                builder.add(boxHeight + (16 * s).roundToInt()) { canvas, y ->
                    val dest = RectF(margin.toFloat(), y.toFloat(), margin + drawW, (y + boxHeight).toFloat())
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true; isDither = true }
                    canvas.drawBitmap(sig, null, dest, p)
                    canvas.drawLine(margin.toFloat(), y + boxHeight + (8 * s), margin + drawW, y + boxHeight + (8 * s), strokePaint(hairline, max(1f, s)))
                }
            }
            block.stampLabel?.takeIf { it.isNotBlank() }?.let { stamp ->
                val stampPaint = paint(AppFonts.Role.Title, sizeLabel, accent, Paint.Align.RIGHT)
                builder.add((sizeLabel * 1.9f).roundToInt()) { canvas, y ->
                    canvas.drawText(AppFonts.sanitize(context, stamp), (width - margin).toFloat(), y + sizeLabel * 1.4f, stampPaint)
                }
            }
            return builder.build()
        }

        private fun verificationOp(block: DocBlock.VerificationTrio): Op {
            val gap = (16 * s).roundToInt()
            val boxW = (innerWidth - gap * 2) / 3f
            val boxH = (215f * s).roundToInt()
            val labelPaint = paint(AppFonts.Role.Title, sizeLabel, bodyColor)
            val hintPaint = paint(AppFonts.Role.Body, sizeCaption, mutedColor)
            val hintCenterPaint = paint(AppFonts.Role.Body, sizeCaption, mutedColor, Paint.Align.CENTER)
            val sig = decodeSignature(block.signatureBase64)
            val titles = listOf(block.fingerprintNote, block.signatureNote, block.stampTitle)

            return Op(boxH) { canvas, y ->
                val rects = listOf(
                    RectF((width - margin) - boxW, y.toFloat(), (width - margin).toFloat(), (y + boxH).toFloat()),
                    RectF((width - margin) - boxW * 2 - gap, y.toFloat(), (width - margin) - boxW - gap, (y + boxH).toFloat()),
                    RectF(margin.toFloat(), y.toFloat(), margin + boxW, (y + boxH).toFloat())
                )
                rects.forEachIndexed { index, rect ->
                    canvas.drawRoundRect(rect, radius * 0.7f, radius * 0.7f, fillPaint(cardFill))
                    canvas.drawRoundRect(rect, radius * 0.7f, radius * 0.7f, strokePaint(cardBorder, border))
                    val titleLayout = layout(titles[index], labelPaint, (rect.width() - 12 * s).roundToInt(), Layout.Alignment.ALIGN_CENTER)
                    canvas.save()
                    canvas.translate(rect.left + (6 * s), rect.top + (10 * s))
                    titleLayout.draw(canvas)
                    canvas.restore()
                    when (index) {
                        0 -> {
                            val top = rect.top + titleLayout.height + (20 * s)
                            val oval = RectF(rect.centerX() - 50f * s, top, rect.centerX() + 50f * s, rect.bottom - (16 * s))
                            val dashed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = mutedColor
                                style = Paint.Style.STROKE
                                strokeWidth = max(1.2f, 1.8f * s)
                                pathEffect = DashPathEffect(floatArrayOf(9f * s, 8f * s), 0f)
                            }
                            canvas.drawOval(oval, dashed)
                            canvas.drawText(AppFonts.sanitize(context, "محل اثر انگشت"), rect.centerX(), oval.centerY() + (6f * s), hintCenterPaint)
                        }
                        1 -> {
                            if (sig != null) {
                                val dest = RectF(rect.left + (12 * s), rect.top + titleLayout.height + (16 * s), rect.right - (12 * s), rect.bottom - (14 * s))
                                canvas.drawBitmap(sig, null, dest, Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
                            } else {
                                canvas.drawText(AppFonts.sanitize(context, "امضا ثبت نشده است"), rect.centerX(), rect.centerY() + (6f * s), hintCenterPaint)
                            }
                        }
                        else -> {
                            val sub = layout(block.stampSubtitle, hintPaint, (rect.width() - 14 * s).roundToInt(), Layout.Alignment.ALIGN_CENTER)
                            canvas.save()
                            canvas.translate(rect.left + (7 * s), rect.top + titleLayout.height + (24 * s))
                            sub.draw(canvas)
                            canvas.restore()
                            val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = accent
                                style = Paint.Style.STROKE
                                strokeWidth = max(1.5f, 2.2f * s)
                            }
                            val frameRect = RectF(rect.centerX() - 58f * s, rect.centerY() + (4 * s), rect.centerX() + 58f * s, rect.bottom - (22 * s))
                            canvas.drawRoundRect(frameRect, 10f * s, 10f * s, frame)
                        }
                    }
                }
            }
        }

        private fun bannerOp(block: DocBlock.Banner): Op {
            val pad = (14 * s).roundToInt()
            val bg = if (isPaper) darken(theme.gradientStartArgb, 0.2f) else withAlpha(accent, 0.16f)
            val onBanner = readableOn(if (isPaper) Color.WHITE else bodyColor, bg, 4.2f)
            val mainPaint = paint(AppFonts.Role.Body, sizeSmall, onBanner)
            val subPaint = paint(AppFonts.Role.Body, sizeCaption, blend(onBanner, bg, 0.25f))
            val main = layout(block.text, mainPaint, innerWidth - pad * 2, Layout.Alignment.ALIGN_CENTER)
            val sub = block.subText?.let { layout(it, subPaint, innerWidth - pad * 2, Layout.Alignment.ALIGN_CENTER) }
            val height = pad * 2 + main.height + (sub?.height ?: 0)
            return Op(height) { canvas, y ->
                val rect = RectF(margin.toFloat(), y.toFloat(), (width - margin).toFloat(), (y + height).toFloat())
                canvas.drawRoundRect(rect, radius * 0.55f, radius * 0.55f, fillPaint(bg))
                canvas.save()
                canvas.translate((margin + pad).toFloat(), (y + pad).toFloat())
                main.draw(canvas)
                canvas.restore()
                sub?.let {
                    canvas.save()
                    canvas.translate((margin + pad).toFloat(), (y + pad + main.height).toFloat())
                    it.draw(canvas)
                    canvas.restore()
                }
            }
        }

        // --------------------------------------------------------------
        //  پاورقی
        // --------------------------------------------------------------
        fun footNoteOp(note: String): Op {
            val paint = paint(AppFonts.Role.Body, sizeSmall, mutedColor)
            val lay = layout(note, paint, innerWidth, Layout.Alignment.ALIGN_CENTER)
            val builder = OpBuilder()
            builder.add((12 * s).roundToInt()) { canvas, y ->
                canvas.drawLine(margin.toFloat(), y.toFloat(), (width - margin).toFloat(), y.toFloat(), strokePaint(hairline, max(1f, 1.2f * s)))
            }
            builder.add(lay.height + (10 * s).roundToInt()) { canvas, y ->
                canvas.save()
                canvas.translate(margin.toFloat(), (y + 8 * s).toFloat())
                lay.draw(canvas)
                canvas.restore()
            }
            return builder.build()
        }

        fun brandOp(): Op {
            val paint = paint(AppFonts.Role.Body, sizeCaption, faintColor, Paint.Align.CENTER)
            return Op((sizeCaption * 2f).roundToInt()) { canvas, y ->
                canvas.drawText("تولید شده توسط دنتال‌پرو", width / 2f, y + sizeCaption * 1.3f, paint)
            }
        }

        fun paintBackground(canvas: Canvas, height: Int) {
            val paint = Paint()
            paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), intArrayOf(paperTop, paperBottom), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            if (!isPaper) {
                val glow = Paint(Paint.ANTI_ALIAS_FLAG)
                glow.shader = LinearGradient(
                    0f, 0f, 0f, height * 0.35f,
                    withAlpha(theme.glowArgb, 0.16f), withAlpha(theme.glowArgb, 0f), Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height * 0.35f, glow)
            }
        }

        // --------------------------------------------------------------
        //  helpers
        // --------------------------------------------------------------
        private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        private fun strokePaint(color: Int, strokeWidth: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }

        /** چیدمان استاندارد متن: RTL-aware + فاصلهٔ خط فارسی + pad. */
        fun layout(
            rawText: String,
            paint: TextPaint,
            availWidth: Int,
            align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
            maxLines: Int = Int.MAX_VALUE,
            ellipsize: Boolean = false
        ): StaticLayout {
            val text = AppFonts.sanitize(context, rawText).ifBlank { " " }
            val w = availWidth.coerceAtLeast(16)
            val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, w)
                .setAlignment(align)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setLineSpacing(PERSIAN_LINE_SPACING_ADD_PX * s, PERSIAN_LINE_SPACING_MULT)
                .setIncludePad(true)
            if (maxLines < Int.MAX_VALUE) builder.setMaxLines(maxLines)
            if (ellipsize) builder.setEllipsize(TextUtils.TruncateAt.END)
            return builder.build()
        }

        private fun decodeResourceBitmap(resId: Int): Bitmap? = try {
            BitmapFactory.decodeResource(context.resources, resId)
        } catch (t: Throwable) {
            null
        }

        private fun decodeSignature(raw: String?): Bitmap? {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) return null
            return try {
                if (value.startsWith("file://") || (value.startsWith("/") && !value.contains(','))) {
                    BitmapFactory.decodeFile(value.removePrefix("file://"))
                } else {
                    val pure = if (value.contains(",")) value.substringAfter(",") else value
                    val bytes = Base64.decode(pure, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } catch (t: Throwable) {
                null
            }
        }

        private fun calloutColors(tone: CalloutTone): Pair<Int, Int> = when (tone) {
            CalloutTone.Info -> accent to withAlpha(accent, 0.10f)
            CalloutTone.Warning -> readableOn(ExportSemanticColors.WARNING, paperTop, 4.2f) to withAlpha(ExportSemanticColors.WARNING, 0.12f)
            CalloutTone.Danger -> readableOn(ExportSemanticColors.DANGER, paperTop, 4.2f) to withAlpha(ExportSemanticColors.DANGER, 0.12f)
            CalloutTone.Success -> readableOn(ExportSemanticColors.SUCCESS, paperTop, 4.2f) to withAlpha(ExportSemanticColors.SUCCESS, 0.12f)
        }
    }

    // ------------------------------------------------------------------
    //  رنگ‌های معنایی + ریاضیات رنگ
    // ------------------------------------------------------------------

    internal fun luminance(color: Int): Float {
        fun channel(v: Int): Float {
            val c = v / 255f
            return if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055) / 1.055).toDouble(), 2.4).toFloat()
        }
        return 0.2126f * channel(Color.red(color)) + 0.7152f * channel(Color.green(color)) + 0.0722f * channel(Color.blue(color))
    }

    internal fun contrastRatio(a: Int, b: Int): Float {
        val la = luminance(a)
        val lb = luminance(b)
        val hi = max(la, lb)
        val lo = min(la, lb)
        return (hi + 0.05f) / (lo + 0.05f)
    }

    internal fun blend(from: Int, to: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * f).roundToInt(),
            (Color.red(from) + (Color.red(to) - Color.red(from)) * f).roundToInt(),
            (Color.green(from) + (Color.green(to) - Color.green(from)) * f).roundToInt(),
            (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * f).roundToInt()
        )
    }

    internal fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((255f * alpha.coerceIn(0f, 1f)).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))

    internal fun lighten(color: Int, amount: Float): Int = blend(color, Color.WHITE, amount)

    internal fun darken(color: Int, amount: Float): Int = blend(color, Color.BLACK, amount)

    /**
     * اگر رنگ روی پس‌زمینه کنتراست کافی نداشته باشد، به‌تدریج به سیاه/سفید
     * می‌شود؛ در نتیجه خروجی در هر تمی خوانا می‌ماند و هویت رنگی تم هم حفظ می‌شود.
     */
    internal fun readableOn(color: Int, background: Int, min: Float = 4.5f): Int {
        if (contrastRatio(color, background) >= min) return color
        val target = if (luminance(background) > 0.5f) Color.BLACK else Color.WHITE
        var best = color
        var step = 0.1f
        while (step <= 1f) {
            best = blend(color, target, step)
            if (contrastRatio(best, background) >= min) break
            step += 0.05f
        }
        return best
    }
}

/**
 * رنگ‌های معنایی که برخلاف بقیهٔ رنگ‌ها از تم پیروی **نمی‌کنند**: خطر/هشدار/
 * موفقیت در یک نرم‌افزار بالینی معنا دارند و نباید با عوض‌شدن تم عوض شوند.
 * تنها همین لیست (و نه هیچ ژنراتوری) مجاز به تعریف رنگ ثابت است.
 */
object ExportSemanticColors {
    /** قرمز خطر — اخطار تاخیر، خونریزی، بدهی معوق. */
    val DANGER: Int = Color.rgb(0xDC, 0x26, 0x26)

    /** قرمز تیرهٔ هدر اخطاریه‌ها. */
    val DANGER_DARK: Int = Color.rgb(0x7F, 0x1D, 0x1D)

    /** کهربایی هشدار — سوابق پزشکی، نکات احتیاطی. */
    val WARNING: Int = Color.rgb(0xB4, 0x53, 0x09)

    /** سبز موفقیت — تسویه شده، تأیید شده. */
    val SUCCESS: Int = Color.rgb(0x04, 0x78, 0x57)
}
