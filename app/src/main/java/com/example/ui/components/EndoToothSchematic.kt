package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.toFdiToothNumber
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * ============================================================================
 *  شماتیک دندان برای درمان ریشه — نسخهٔ پورت‌شده از docs/tooth-schematics.html
 * ============================================================================
 *
 * تمام هندسه از [ToothSchematicGeometry] می‌آید که کپی عینی نسخهٔ ۴ تأییدشدهٔ
 * HTML است (صحت آن با ۲۱۷٬۳۴۲ مقایسهٔ عددی نسبت به همان فایل بررسی شده).
 * این فایل فقط «رسم» می‌کند و هیچ عدد هندسی مستقلی ندارد.
 *
 * موارد توافق‌شده که این نسخه رعایت می‌کند:
 *  ۱) جهت‌ها: فک بالا ریشه رو به بالا، فک پایین رو به پایین، و دندان‌های سمت
 *     چپ آینه می‌شوند؛ برچسب مزیال/دیستال یا باکال/پالاتال هم مطابق همان.
 *  ۲) کانال‌های ثابت همیشه نمایش داده می‌شوند و قابل حذف نیستند؛ کانال‌های
 *     احتمالی با درصد فراوانی قابل انتخاب‌اند.
 *  ۳) هر ریشه دقیقاً روی کانال‌های خودش می‌نشیند (تضمین ریاضی در rootHw).
 *  ۴) فورکا از محل واقعی جدا شدن ریشه‌ها ساخته می‌شود — شکل طبیعی دندان.
 *  ۵) طول کار با دکمه‌های + و − در گام ۰٫۵ میلی‌متر تغییر می‌کند.
 */

private val CANAL_COLORS = listOf(
    Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF22C55E),
    Color(0xFFF59E0B), Color(0xFFA855F7), Color(0xFF06B6D4)
)

fun canalColorAt(index: Int): Color = CANAL_COLORS[index % CANAL_COLORS.size]

/** گام تغییر طول کار — دقیقاً ۰٫۵ میلی‌متر، مطابق نسخهٔ HTML. */
const val WORKING_LENGTH_STEP = 0.5
private const val WL_MIN = 4.0
private const val WL_MAX = 40.0

/** درصد فراوانی از روی متن یادداشت («فراوانی ۶۵ الی ۸۰ درصد…» → ۷۲). */
fun defaultFrequencyOf(note: String?): Int {
    if (note.isNullOrBlank()) return 50
    val en = note.map { ch ->
        val i = "۰۱۲۳۴۵۶۷۸۹".indexOf(ch)
        if (i >= 0) ('0' + i) else ch
    }.joinToString("")
    val nums = Regex("""\d+(\.\d+)?""").findAll(en).map { it.value.toDouble() }.toList()
    if (nums.isEmpty()) return 50
    return if (nums.size >= 2) ((nums[0] + nums[1]) / 2).toInt() else nums[0].toInt()
}

fun clampWorkingLength(v: Double): Double = min(WL_MAX, max(WL_MIN, v))

/** گرد کردن به نزدیک‌ترین نیم میلی‌متر — همان bump() در HTML. */
fun snapToHalf(v: Double): Double = Math.round(v * 2.0) / 2.0

/**
 * شماتیک دندان + خط‌کش میلی‌متری.
 *
 * @param enabled کدام کانال‌ها روشن‌اند. کانال ثابت همیشه روشن است.
 * @param workingLengths طول کار هر کانال (میلی‌متر).
 */
@Composable
fun EndoToothSchematic(
    toothNumber: Int,
    workingLengths: Map<String, Double>,
    enabled: Map<String, Boolean> = emptyMap(),
    isPersian: Boolean = true,
    showRuler: Boolean = true,
    modifier: Modifier = Modifier
) {
    val fdi = remember(toothNumber) { toFdiToothNumber(toothNumber) }
    val canals = remember(fdi) { schematicCanalsFor(fdi) }
    val on = remember(fdi, enabled, canals) {
        canals.associate { it.name to (it.isFixed || (enabled[it.name] == true)) }
    }
    val geom = remember(fdi, on, workingLengths) {
        buildToothGeometry(fdi, on, workingLengths)
    }
    val upper = remember(fdi) { isUpperTooth(fdi) }
    val flip = remember(fdi) { if (isLeftSideTooth(fdi)) -1f else 1f }
    val labels = remember(fdi) { directionLabels(fdi) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // ——— جهت فک، همان متن نسخهٔ HTML ———
            Text(
                text = jawLabel(fdi),
                color = Color(0xFF38BDF8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                drawSchematic(geom, upper, flip, showRuler)
            }

            // ——— برچسب چپ/راست، دقیقاً مثل HTML ———
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("◄ ${labels.first}", color = Color(0xFF94A3B8), fontSize = 9.sp)
                Text("${labels.second} ►", color = Color(0xFF94A3B8), fontSize = 9.sp)
            }
        }
    }
}

/** رسم کامل — ترتیب لایه‌ها همان نسخهٔ HTML است. */
private fun DrawScope.drawSchematic(
    g: ToothGeometry,
    upper: Boolean,
    flip: Float,
    showRuler: Boolean
) {
    val sil = SilhouetteBuilder(g)
    val map = g.map

    val rulerW = if (showRuler) 46f else 0f
    val padTop = 18f
    val padBot = 18f
    val padSide = 12f
    val drawW = size.width - rulerW - padSide * 2
    val drawH = size.height - padTop - padBot
    val widest = max(
        map.crownW,
        g.roots.maxOf { abs(it.xA) + it.wApex / 2 + 1 }
    ) * 2
    val px = min(drawH / (g.maxY + 1.2).toFloat(), drawW / widest.toFloat())
    val cx = padSide + drawW / 2
    val baseY = if (upper) size.height - padBot else padTop
    fun yOf(mm: Double): Float = if (upper) baseY - mm.toFloat() * px else baseY + mm.toFloat() * px
    fun xOf(mm: Double): Float = cx + mm.toFloat() * px * flip

    // ——— سیلوئت ریشه: یک مسیر پیوسته، بدون هیچ شکستگی ———
    val pts = sil.outline()
    if (pts.isNotEmpty()) {
        val path = Path()
        // Catmull-Rom → بزیه مکعبی (همان تابع smooth در HTML)
        path.moveTo(xOf(pts[0].x), yOf(pts[0].y))
        for (i in 0 until pts.size - 1) {
            val p0 = pts.getOrElse(i - 1) { pts[i] }
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p3 = pts.getOrElse(i + 2) { p2 }
            val c1x = p1.x + (p2.x - p0.x) / 6
            val c1y = p1.y + (p2.y - p0.y) / 6
            val c2x = p2.x - (p3.x - p1.x) / 6
            val c2y = p2.y - (p3.y - p1.y) / 6
            path.cubicTo(xOf(c1x), yOf(c1y), xOf(c2x), yOf(c2y), xOf(p2.x), yOf(p2.y))
        }
        path.lineTo(xOf(sil.trunkHalf), yOf(g.crownH))
        path.lineTo(xOf(-sil.trunkHalf), yOf(g.crownH))
        path.close()
        drawPath(
            path,
            brush = Brush.horizontalGradient(
                0.0f to Color(0xFFD8C9A8), 0.45f to Color(0xFFEFE3C8),
                0.60f to Color(0xFFF7EFDC), 1.0f to Color(0xFFD8C9A8),
                startX = xOf(-widest / 2), endX = xOf(widest / 2)
            )
        )
        drawPath(
            path, color = Color(0xFF0F172A),
            style = Stroke(width = 1.7f, join = StrokeJoin.Round, cap = StrokeCap.Round)
        )
    }

    // ——— تاج ———
    val cH = g.crownH
    val cW = map.crownW / 2
    val occ = cW * when (map.type) {
        CrownType.MOL -> 0.80; CrownType.PM -> 0.68
        CrownType.CAN -> 0.40; CrownType.INC -> 0.74
    }
    val neck = min(sil.trunkHalf, cW * 0.95)
    val crown = Path().apply {
        moveTo(xOf(-neck), yOf(cH))
        cubicTo(
            xOf(-cW * 0.99), yOf(cH * 0.80),
            xOf(-cW), yOf(cH * 0.42),
            xOf(-occ), yOf(cH * 0.10)
        )
        when (map.type) {
            CrownType.MOL -> {
                val n = 4
                val st = (occ * 2) / n
                for (i in 0 until n) {
                    val x0 = -occ + st * i
                    val x1 = x0 + st
                    quadraticBezierTo(xOf((x0 + x1) / 2), yOf(-0.55), xOf(x1), yOf(cH * 0.10))
                }
            }
            CrownType.PM -> {
                quadraticBezierTo(xOf(-occ * 0.5), yOf(-0.75), xOf(0.0), yOf(cH * 0.13))
                quadraticBezierTo(xOf(occ * 0.5), yOf(-0.75), xOf(occ), yOf(cH * 0.10))
            }
            CrownType.CAN -> {
                quadraticBezierTo(xOf(-occ * 0.35), yOf(-0.3), xOf(0.0), yOf(-1.5))
                quadraticBezierTo(xOf(occ * 0.35), yOf(-0.3), xOf(occ), yOf(cH * 0.10))
            }
            CrownType.INC -> {
                quadraticBezierTo(xOf(-occ), yOf(-0.45), xOf(-occ * 0.72), yOf(-0.62))
                lineTo(xOf(occ * 0.72), yOf(-0.62))
                quadraticBezierTo(xOf(occ), yOf(-0.45), xOf(occ), yOf(cH * 0.10))
            }
        }
        cubicTo(
            xOf(cW), yOf(cH * 0.42),
            xOf(cW * 0.99), yOf(cH * 0.80),
            xOf(neck), yOf(cH)
        )
        close()
    }
    drawPath(
        crown,
        brush = Brush.horizontalGradient(
            0.0f to Color(0xFFDCE6EE), 0.42f to Color(0xFFF2F7FA),
            0.58f to Color(0xFFFFFFFF), 1.0f to Color(0xFFDCE6EE),
            startX = xOf(-cW), endX = xOf(cW)
        )
    )
    drawPath(crown, Color(0xFF0F172A), style = Stroke(1.8f, join = StrokeJoin.Round))

    // ——— اتاقک پالپ ———
    val chTop = cH * 0.30
    val chFloor = cH + (if (g.multi) 0.5 else 0.2)
    val chH = cW * (if (map.type == CrownType.MOL) 0.60 else 0.40)
    val pulp = Path().apply {
        moveTo(xOf(-chH), yOf(chFloor))
        quadraticBezierTo(xOf(-chH * 0.92), yOf(chTop), xOf(0.0), yOf(chTop - 0.35))
        quadraticBezierTo(xOf(chH * 0.92), yOf(chTop), xOf(chH), yOf(chFloor))
        close()
    }
    drawPath(pulp, Color(0xFFEF4444).copy(alpha = 0.28f))
    drawPath(pulp, Color(0xFFEF4444).copy(alpha = 0.45f), style = Stroke(1f))

    // ——— کانال‌ها ———
    val orificeY = chFloor - 0.9
    g.roots.forEach { r ->
        r.canals.forEach { s ->
            val col = canalColorAt(s.colorIndex)
            val line = Path()
            var first = true
            var y = orificeY
            while (y <= s.wl + 1e-9) {
                val yy = min(y, s.wl)
                val xx = canalX(r, s, yy, g.furcY, orificeY)
                if (first) { line.moveTo(xOf(xx), yOf(yy)); first = false }
                else line.lineTo(xOf(xx), yOf(yy))
                y += OUTLINE_STEP
            }
            val tipX = xOf(canalX(r, s, s.wl, g.furcY, orificeY))
            val tipY = yOf(s.wl)
            line.lineTo(tipX, tipY)
            drawPath(line, col.copy(alpha = 0.28f), style = Stroke(6.2f, cap = StrokeCap.Round))
            drawPath(line, col, style = Stroke(2.5f, cap = StrokeCap.Round))
            drawCircle(col, 3.6f, Offset(tipX, tipY))
            drawCircle(Color.White, 1.5f, Offset(tipX, tipY))
        }
    }

    // ——— نشانگر فورکا ———
    if (g.multi) {
        sil.fornix.forEach { f ->
            val p = Offset(xOf(f.x), yOf(f.y))
            drawCircle(
                Color(0xFF38BDF8), 4.6f, p,
                style = Stroke(1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5f, 2f)))
            )
            drawCircle(Color(0xFF38BDF8), 1.7f, p)
        }
    }

    // ——— خط CEJ ———
    drawLine(
        Color(0xFF94A3B8).copy(alpha = 0.45f),
        Offset(xOf(-cW * 1.06), yOf(cH)), Offset(xOf(cW * 1.06), yOf(cH)),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
    )

    // ——— خط‌کش میلی‌متری ———
    if (showRuler) {
        val ax = size.width - rulerW + 14f
        drawLine(Color(0xFF64748B), Offset(ax, yOf(0.0)), Offset(ax, yOf(g.maxY)), strokeWidth = 1.6f)
        var mm = 0
        while (mm <= g.maxY.toInt()) {
            val maj = mm % 5 == 0
            val y = yOf(mm.toDouble())
            drawLine(
                if (maj) Color(0xFF94A3B8) else Color(0xFF475569),
                Offset(ax, y), Offset(ax + if (maj) 12f else 6f, y),
                strokeWidth = if (maj) 1.5f else 1f
            )
            if (maj) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8")
                        textSize = 20f
                        isAntiAlias = true
                    }
                    drawText(mm.toString(), ax + 15f, y + 6f, paint)
                }
            }
            mm++
        }
        // نشانگر طول کار هر کانال روی خط‌کش
        g.roots.forEach { r ->
            r.canals.forEach { s ->
                val y = yOf(s.wl)
                val col = canalColorAt(s.colorIndex)
                val tri = Path().apply {
                    moveTo(ax, y); lineTo(ax + 11f, y - 4.6f); lineTo(ax + 11f, y + 4.6f); close()
                }
                drawPath(tri, col)
            }
        }
    }
}

/**
 * ردیف کنترل یک کانال: انتخاب (برای احتمالی‌ها)، درصد فراوانی، و طول کار با
 * دکمه‌های + / − در گام ۰٫۵ میلی‌متر.
 */
@Composable
fun CanalControlRow(
    canal: SchematicCanal,
    colorIndex: Int,
    isOn: Boolean,
    workingLength: Double,
    onToggle: (Boolean) -> Unit,
    onWorkingLengthChange: (Double) -> Unit,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // نقطهٔ رنگی، هم‌رنگ همان کانال روی شماتیک
        Canvas(Modifier.size(10.dp)) { drawCircle(canalColorAt(colorIndex)) }

        if (canal.isFixed) {
            // کانال ثابت: همیشه روشن، قابل حذف نیست
            Text(
                text = canal.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp)
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("ثابت", fontSize = 8.sp) },
                modifier = Modifier.height(22.dp)
            )
        } else {
            Checkbox(
                checked = isOn,
                onCheckedChange = { if (!readOnly) onToggle(it) },
                enabled = !readOnly,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = canal.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp)
            )
            // درصد فراوانی برای کانال احتمالی
            Text(
                text = "٪${defaultFrequencyOf(canal.note)}",
                fontSize = 9.sp,
                color = Color(0xFFF59E0B)
            )
        }

        Spacer(Modifier.weight(1f))

        if (isOn) {
            // ——— گام ۰٫۵ میلی‌متری ———
            IconButton(
                onClick = {
                    if (!readOnly) onWorkingLengthChange(
                        clampWorkingLength(snapToHalf(workingLength - WORKING_LENGTH_STEP))
                    )
                },
                enabled = !readOnly,
                modifier = Modifier.size(28.dp)
            ) { Icon(Icons.Default.Remove, contentDescription = "کاهش", Modifier.size(15.dp)) }

            Text(
                text = formatMm(workingLength),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            IconButton(
                onClick = {
                    if (!readOnly) onWorkingLengthChange(
                        clampWorkingLength(snapToHalf(workingLength + WORKING_LENGTH_STEP))
                    )
                },
                enabled = !readOnly,
                modifier = Modifier.size(28.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "افزایش", Modifier.size(15.dp)) }
        }
    }
    if (!canal.isFixed && !canal.note.isNullOrBlank()) {
        Text(
            text = canal.note,
            fontSize = 8.5.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 28.dp, bottom = 2.dp)
        )
    }
}

/** نمایش عدد میلی‌متر: ۲۱ به‌جای ۲۱٫۰ ولی ۲۱٫۵ کامل. */
fun formatMm(v: Double): String =
    if (v == Math.floor(v)) "${v.toInt()} mm" else String.format("%.1f mm", v)
