package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** یک لحظهٔ زمانی که هر ثانیه تازه می‌شود. */
data class ClockTick(
    val hour24: Int,
    val minute: Int,
    val second: Int
) {
    /** ساعت ۱۲ساعته؛ نیمه‌شب و ظهر به‌جای ۰ به‌صورت ۱۲ نشان داده می‌شوند. */
    val hour12: Int get() = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }

    val isBeforeNoon: Boolean get() = hour24 < 12
}

/**
 * هر ثانیه یک [ClockTick] تازه تولید می‌کند.
 *
 * اولین مقدار بلافاصله (بدون تأخیر) ساخته می‌شود تا بنر هرگز خالی نماند، و
 * تأخیرِ هر دور دقیقاً تا مرز ثانیهٔ بعدی حساب می‌شود تا عقربهٔ ثانیه‌شمار
 * به‌مرور از ساعت سیستم عقب نیفتد.
 */
@Composable
fun rememberClockTick(): State<ClockTick> = produceState(initialValue = currentTick()) {
    while (true) {
        val now = System.currentTimeMillis()
        delay(1000L - (now % 1000L))
        value = currentTick()
    }
}

private fun currentTick(): ClockTick {
    val cal = Calendar.getInstance()
    return ClockTick(
        hour24 = cal.get(Calendar.HOUR_OF_DAY),
        minute = cal.get(Calendar.MINUTE),
        second = cal.get(Calendar.SECOND)
    )
}

/** «۰۹:۰۵:۰۳» — همیشه دو رقمی، با ارقام فارسی در حالت فارسی. */
fun formatClockTime(tick: ClockTick, isPersian: Boolean): String {
    fun two(n: Int) = n.toString().padStart(2, '0')
    val raw = "${two(tick.hour12)}:${two(tick.minute)}:${two(tick.second)}"
    return if (isPersian) raw.toPersianDigitsLocal() else raw
}

/**
 * ساعت دیجیتال با عرضِ کاملاً پایدار: هر کاراکتر (رقم/دونقطه) در یک جعبهٔ
 * هم‌عرض رندر می‌شود، پس با عوض‌شدن ثانیه‌ها نه پهنای هر رقم و نه پهنای کل
 * سطر تغییر نمی‌کند و ساعت اصلاً تکان نمی‌خورد — مستقل از اینکه فونت انتخابی
 * ارقام tabular داشته باشد یا نه.
 *
 * چرا این روش؟ راه قبلی (FontFamily.Monospace) برای ارقام فارسی «۰..۹» کار
 * نمی‌کرد؛ چون Monospace گلیف فارسی ندارد و آن ارقام به فونت متغیرالعرضِ
 * سیستم fallback می‌شدند و کل سطر می‌لرزید. اینجا هر رقم در سلولی با عرض ثابت
 * قرار می‌گیرد، پس حتی fallback هم لرزش ایجاد نمی‌کند.
 */
@Composable
fun StableClockText(
    tick: ClockTick,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.MaterialTheme.typography.labelLarge,
    fontWeight: androidx.compose.ui.text.font.FontWeight = androidx.compose.ui.text.font.FontWeight.Bold
) {
    val text = formatClockTime(tick, isPersian)
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val resolved = style.merge(androidx.compose.ui.text.TextStyle(color = color, fontWeight = fontWeight))
    val density = androidx.compose.ui.platform.LocalDensity.current
    // پهنای سلول از عریض‌ترینِ هر ده رقم گرفته می‌شود (نه فقط «۰»)، وگرنه ارقامِ
    // پهن‌تر مثل ۴/۵/۷/۸ بریده می‌شدند. کمی padding هم اضافه می‌شود تا هیچ رقمی نچسبد.
    val digitSamples = if (isPersian) "۰۱۲۳۴۵۶۷۸۹" else "0123456789"
    val cellWidth = remember(digitSamples, resolved, density) {
        val maxW = digitSamples.maxOf { measurer.measure(text = it.toString(), style = resolved).size.width }
        with(density) { maxW.toDp() } + 1.dp
    }
    // ساعت همیشه چپ‌به‌راست چیده می‌شود (ساعت:دقیقه:ثانیه)، حتی در حالتِ فارسی/RTL؛
    // وگرنه ترتیبِ ساعت‌شمار و ثانیه‌شمار برعکس دیده می‌شد.
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = modifier,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            text.forEach { ch ->
                val isDigit = ch in '0'..'9' || ch in '۰'..'۹'
                androidx.compose.foundation.layout.Box(
                    modifier = if (isDigit) Modifier.width(cellWidth) else Modifier,
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = ch.toString(),
                        style = resolved,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/** «قبل از ظهر» / «بعد از ظهر» (یا AM/PM در حالت انگلیسی). */
fun formatMeridiem(tick: ClockTick, isPersian: Boolean): String = when {
    isPersian && tick.isBeforeNoon -> "قبل از ظهر"
    isPersian -> "بعد از ظهر"
    tick.isBeforeNoon -> "AM"
    else -> "PM"
}

/**
 * تاریخ امروز دقیقاً به قالب خواسته‌شده: «شنبه 10 مهر 1401».
 *
 * @param isPersian در حالت فارسی ارقام هم فارسی می‌شوند.
 */
fun formatJalaliBannerDate(timestamp: Long = System.currentTimeMillis(), isPersian: Boolean = true): String {
    val jalali = PersianCalendarHelper.timestampToJalali(timestamp)
    val weekday = PersianCalendarHelper.WEEKDAY_NAMES.getOrElse(
        PersianCalendarHelper.getDayOfWeekIndex(jalali.year, jalali.month, jalali.day)
    ) { "" }
    val monthName = PersianCalendarHelper.MONTH_NAMES.getOrElse(jalali.month - 1) { "" }
    val raw = "$weekday ${jalali.day} $monthName ${jalali.year}"
    return if (isPersian) raw.toPersianDigitsLocal() else raw
}

private fun String.toPersianDigitsLocal(): String =
    com.example.util.PersianNumberFormatter.toPersianDigits(this)

/**
 * ساعت آنالوگ که **روی لوگو** کشیده می‌شود.
 *
 * قاعدهٔ ظاهری خواسته‌شده:
 *  * هر بخشی از عقربه‌ها که **روی لوگو** می‌افتد با شفافیت ۳۰٪ کشیده می‌شود تا
 *    لوگو خوانا بماند.
 *  * هر بخشی که **بیرون از لوگو** است با شفافیت ۱۰۰٪ کشیده می‌شود تا ساعت
 *    کاملاً واضح باشد.
 *
 * برای همین هر عقربه دقیقاً دو بار رسم می‌شود: یک بار با برشِ «داخل دایرهٔ لوگو»
 * و یک بار با برشِ «بیرون دایرهٔ لوگو». چون هر دو بار مسیرِ یکسانی رسم می‌شود،
 * عقربه یک خط پیوسته دیده می‌شود که فقط شفافیتش عوض می‌شود.
 *
 * @param logoDiameter قطر لوگویی که این ساعت رویش می‌نشیند؛ مرزِ تغییر شفافیت.
 * @param content خودِ لوگو، که زیر عقربه‌ها رسم می‌شود.
 */
@Composable
fun AnalogClockOverlay(
    modifier: Modifier = Modifier,
    size: Dp,
    logoDiameter: Dp,
    handColor: Color = Color.White,
    secondHandColor: Color = Color(0xFFFF5252),
    insideLogoAlpha: Float = 0.30f,
    outsideLogoAlpha: Float = 1.00f,
    showTicks: Boolean = true,
    content: @Composable () -> Unit
) {
    val tick by rememberClockTick()

    Box(
        modifier = modifier.size(size),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        content()

        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = min(this.size.width, this.size.height) / 2f
            val logoRadius = logoDiameter.toPx() / 2f

            // مرزِ شفافیت: داخل این دایره ۳۰٪، بیرونش ۱۰۰٪
            val logoCircle = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = center.x - logoRadius,
                        top = center.y - logoRadius,
                        right = center.x + logoRadius,
                        bottom = center.y + logoRadius
                    )
                )
            }

            /**
             * یک بار داخل لوگو (کم‌رنگ) و یک بار بیرونش (پررنگ) رسم می‌کند.
             * چون مسیرِ رسم در هر دو یکی است، شکست یا پرش دیده نمی‌شود.
             */
            fun drawSplitByLogo(block: DrawScope.(alpha: Float) -> Unit) {
                clipPath(logoCircle, clipOp = ClipOp.Intersect) { block(insideLogoAlpha) }
                clipPath(logoCircle, clipOp = ClipOp.Difference) { block(outsideLogoAlpha) }
            }

            /** عقربه از مرکز تا نسبتی از شعاع، با زاویهٔ ساعتی (۰ = بالا). */
            fun DrawScope.hand(
                fractionOfRadius: Float,
                angleDegrees: Float,
                strokeWidth: Float,
                color: Color,
                alpha: Float,
                tailFraction: Float = 0.12f
            ) {
                val rad = Math.toRadians((angleDegrees - 90f).toDouble())
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                val tip = Offset(
                    center.x + cosA * radius * fractionOfRadius,
                    center.y + sinA * radius * fractionOfRadius
                )
                val tail = Offset(
                    center.x - cosA * radius * tailFraction,
                    center.y - sinA * radius * tailFraction
                )
                drawLine(
                    color = color,
                    start = tail,
                    end = tip,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                    alpha = alpha
                )
            }

            // نشانه‌های ساعت — فقط بیرون لوگو معنا دارند، ولی برای یکدستی
            // همان قاعدهٔ شفافیت روی آن‌ها هم اعمال می‌شود.
            if (showTicks) {
                drawSplitByLogo { alpha ->
                    for (i in 0 until 12) {
                        val angle = Math.toRadians((i * 30 - 90).toDouble())
                        val isQuarter = i % 3 == 0
                        val outer = radius * 0.97f
                        val inner = radius * if (isQuarter) 0.86f else 0.91f
                        drawLine(
                            color = handColor,
                            start = Offset(
                                center.x + cos(angle).toFloat() * inner,
                                center.y + sin(angle).toFloat() * inner
                            ),
                            end = Offset(
                                center.x + cos(angle).toFloat() * outer,
                                center.y + sin(angle).toFloat() * outer
                            ),
                            strokeWidth = if (isQuarter) 2.4f else 1.4f,
                            cap = StrokeCap.Round,
                            alpha = alpha * 0.9f
                        )
                    }
                    // حلقهٔ بیرونی قاب ساعت
                    drawCircle(
                        color = handColor,
                        radius = radius * 0.99f,
                        center = center,
                        style = Stroke(width = 1.2f),
                        alpha = alpha * 0.45f
                    )
                }
            }

            // زاویه‌ها — عقربه‌های ساعت و دقیقه پیوسته حرکت می‌کنند (پرشی نیستند)
            val secondAngle = tick.second * 6f
            val minuteAngle = tick.minute * 6f + tick.second * 0.1f
            val hourAngle = (tick.hour24 % 12) * 30f + tick.minute * 0.5f

            drawSplitByLogo { alpha ->
                hand(0.52f, hourAngle, 4.5f, handColor, alpha)
            }
            drawSplitByLogo { alpha ->
                hand(0.74f, minuteAngle, 3.0f, handColor, alpha)
            }
            drawSplitByLogo { alpha ->
                hand(0.84f, secondAngle, 1.8f, secondHandColor, alpha, tailFraction = 0.18f)
            }

            // پیچ مرکزی — همیشه روی لوگوست، پس با همان قاعده کم‌رنگ می‌شود
            drawSplitByLogo { alpha ->
                drawCircle(color = handColor, radius = 3.4f, center = center, alpha = alpha)
                drawCircle(color = secondHandColor, radius = 1.7f, center = center, alpha = alpha)
            }
        }
    }
}
