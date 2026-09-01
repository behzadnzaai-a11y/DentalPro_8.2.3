package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ============================================================================
 *  نمودار شماتیک سطوح دندان (Tooth Surface Chart) — مخصوص ترمیم‌ها
 * ============================================================================
 * همان فرم تصویری کلاسیکِ پرونده دندانپزشکی: یک مربع که به پنج ناحیه تقسیم شده
 * است — چهار ذوزنقهٔ پیرامونی (مزیال، دیستال، باکال، لینگوال) و یک مربع مرکزی
 * (اکلوزال برای دندان‌های خلفی / اینسیزال برای دندان‌های قدامی).
 *
 * هر ناحیه با لمس انتخاب/لغو می‌شود و ناحیه‌های انتخاب‌شده هاشور رنگی می‌گیرند،
 * دقیقاً مثل علامت‌گذاری ترمیم روی چارت کاغذی.
 *
 * جهت‌ها بر اساس نیم‌فک (quadrant) آینه می‌شوند تا «مزیال» همیشه سمت خط وسط
 * دهان باشد — همان قراردادی که در چارت بالینی رعایت می‌شود.
 */

/** نواحی قابل انتخاب روی نمودار. کدها با همان کدهای ذخیره‌شده در یادداشت درمان یکی است. */
enum class ToothSurfaceZone(val code: String) {
    MESIAL("M"),
    DISTAL("D"),
    BUCCAL("B"),
    LINGUAL("L"),
    CENTER("O")   // اکلوزال یا اینسیزال بسته به نوع دندان
}

private val SURFACE_FILL = Color(0xFF3B82F6)
private val SURFACE_FILL_STRONG = Color(0xFF2563EB)

@Composable
fun ToothSurfaceDiagram(
    toothNumber: Int?,
    selectedCodes: Set<String>,
    onToggle: (String) -> Unit,
    readOnly: Boolean = false,
    isPersian: Boolean = true,
    modifier: Modifier = Modifier
) {
    // دندان‌های قدامی (1 تا 3) لبهٔ اینسیزال دارند نه سطح اکلوزال
    val isAnterior = remember(toothNumber) {
        val pos = (toothNumber ?: 16) % 10
        pos in 1..3
    }
    // نیم‌فک راست بیمار → مزیال در سمت راست تصویر؛ نیم‌فک چپ → آینه
    val mirrored = remember(toothNumber) {
        val q = (toothNumber ?: 16) / 10
        q == 2 || q == 3 || q == 6 || q == 7
    }

    val centerCode = if (isAnterior) "I" else "O"
    // کد مرکزی در داده می‌تواند O یا I باشد؛ هر دو را «مرکز انتخاب‌شده» می‌شماریم
    val centerSelected = "O" in selectedCodes || "I" in selectedCodes

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(132.dp)
                    .then(
                        if (readOnly) Modifier
                        else Modifier.pointerInput(mirrored, isAnterior) {
                            detectTapGestures { offset ->
                                val zone = zoneAt(offset, size.width.toFloat(), size.height.toFloat())
                                if (zone != null) {
                                    val code = when (zone) {
                                        ToothSurfaceZone.CENTER -> centerCode
                                        ToothSurfaceZone.MESIAL -> if (mirrored) "D" else "M"
                                        ToothSurfaceZone.DISTAL -> if (mirrored) "M" else "D"
                                        else -> zone.code
                                    }
                                    onToggle(code)
                                }
                            }
                        }
                    )
            ) {
                drawSurfaceChart(
                    selected = buildSet {
                        // ترجمهٔ کدهای منطقی به نواحی تصویری (با درنظرگرفتن آینه)
                        if ("M" in selectedCodes) add(if (mirrored) ToothSurfaceZone.DISTAL else ToothSurfaceZone.MESIAL)
                        if ("D" in selectedCodes) add(if (mirrored) ToothSurfaceZone.MESIAL else ToothSurfaceZone.DISTAL)
                        if ("B" in selectedCodes) add(ToothSurfaceZone.BUCCAL)
                        if ("L" in selectedCodes) add(ToothSurfaceZone.LINGUAL)
                        if (centerSelected) add(ToothSurfaceZone.CENTER)
                    },
                    centerLabelIsIncisal = isAnterior
                )
            }

            // برچسب‌های حروف روی نواحی — Canvas متن نمی‌کشد، پس روی آن Overlay می‌کنیم.
            // این Box لمس را نمی‌گیرد تا کلیک‌ها به Canvas زیرین برسند.
            val mesialLabel = if (mirrored) "D" else "M"
            val distalLabel = if (mirrored) "M" else "D"
            Box(modifier = Modifier.size(132.dp)) {
                ZoneLabel(mesialLabel, Modifier.align(Alignment.CenterEnd).padding(end = 7.dp))
                ZoneLabel(distalLabel, Modifier.align(Alignment.CenterStart).padding(start = 7.dp))
                ZoneLabel("B", Modifier.align(Alignment.TopCenter).padding(top = 7.dp))
                ZoneLabel("L", Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp))
                ZoneLabel(centerCode, Modifier.align(Alignment.Center))
            }
        }

        Text(
            text = if (isPersian) {
                if (isAnterior) "نمای اینسیزال — روی هر سطح بزنید" else "نمای اکلوزال — روی هر سطح بزنید"
            } else {
                if (isAnterior) "Incisal view — tap a surface" else "Occlusal view — tap a surface"
            },
            fontSize = 8.5.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun ZoneLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF475569),
        modifier = modifier
    )
}

/** تشخیص اینکه لمس کاربر در کدام ناحیه افتاده است. */
private fun zoneAt(offset: Offset, w: Float, h: Float): ToothSurfaceZone? {
    val pad = w * 0.08f
    val left = pad
    val top = pad
    val right = w - pad
    val bottom = h - pad
    if (offset.x < left || offset.x > right || offset.y < top || offset.y > bottom) return null

    val innerLeft = left + (right - left) * 0.30f
    val innerRight = left + (right - left) * 0.70f
    val innerTop = top + (bottom - top) * 0.30f
    val innerBottom = top + (bottom - top) * 0.70f

    if (offset.x in innerLeft..innerRight && offset.y in innerTop..innerBottom) {
        return ToothSurfaceZone.CENTER
    }
    // بیرون از مربع مرکزی: با مقایسهٔ فاصلهٔ نسبی از لبه‌ها تصمیم می‌گیریم
    val distLeft = offset.x - left
    val distRight = right - offset.x
    val distTop = offset.y - top
    val distBottom = bottom - offset.y
    return when (minOf(distLeft, distRight, distTop, distBottom)) {
        distRight -> ToothSurfaceZone.MESIAL   // سمت راست تصویر
        distLeft -> ToothSurfaceZone.DISTAL    // سمت چپ تصویر
        distTop -> ToothSurfaceZone.BUCCAL
        else -> ToothSurfaceZone.LINGUAL
    }
}

private fun DrawScope.drawSurfaceChart(
    selected: Set<ToothSurfaceZone>,
    centerLabelIsIncisal: Boolean
) {
    val w = size.width
    val h = size.height
    val pad = w * 0.08f
    val left = pad
    val top = pad
    val right = w - pad
    val bottom = h - pad

    val innerLeft = left + (right - left) * 0.30f
    val innerRight = left + (right - left) * 0.70f
    val innerTop = top + (bottom - top) * 0.30f
    val innerBottom = top + (bottom - top) * 0.70f

    val outline = Color(0xFF64748B)
    val emptyFill = Color(0xFFF8FAFC)

    fun zonePath(zone: ToothSurfaceZone): Path = Path().apply {
        when (zone) {
            ToothSurfaceZone.BUCCAL -> {          // ذوزنقه بالا
                moveTo(left, top); lineTo(right, top)
                lineTo(innerRight, innerTop); lineTo(innerLeft, innerTop); close()
            }
            ToothSurfaceZone.LINGUAL -> {         // ذوزنقه پایین
                moveTo(left, bottom); lineTo(right, bottom)
                lineTo(innerRight, innerBottom); lineTo(innerLeft, innerBottom); close()
            }
            ToothSurfaceZone.DISTAL -> {          // ذوزنقه چپ
                moveTo(left, top); lineTo(left, bottom)
                lineTo(innerLeft, innerBottom); lineTo(innerLeft, innerTop); close()
            }
            ToothSurfaceZone.MESIAL -> {          // ذوزنقه راست
                moveTo(right, top); lineTo(right, bottom)
                lineTo(innerRight, innerBottom); lineTo(innerRight, innerTop); close()
            }
            ToothSurfaceZone.CENTER -> {          // مربع مرکزی
                moveTo(innerLeft, innerTop); lineTo(innerRight, innerTop)
                lineTo(innerRight, innerBottom); lineTo(innerLeft, innerBottom); close()
            }
        }
    }

    ToothSurfaceZone.entries.forEach { zone ->
        val path = zonePath(zone)
        val isOn = zone in selected
        drawPath(
            path,
            color = if (isOn) SURFACE_FILL.copy(alpha = 0.85f) else emptyFill,
            style = Fill
        )
        drawPath(
            path,
            color = if (isOn) SURFACE_FILL_STRONG else outline,
            style = Stroke(width = if (isOn) 2.2f else 1.4f)
        )
    }

    // قاب بیرونی پررنگ‌تر برای خوانایی
    drawRect(
        color = Color(0xFF334155),
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
        style = Stroke(width = 2f)
    )

    // برای دندان‌های قدامی، ناحیهٔ مرکزی به‌جای «اکلوزال» لبهٔ اینسیزال است؛
    // با یک خط قطری آن را متمایز می‌کنیم تا با نمای خلفی اشتباه نشود.
    if (centerLabelIsIncisal) {
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(innerLeft, innerBottom),
            end = Offset(innerRight, innerTop),
            strokeWidth = 1.2f
        )
    }
}
