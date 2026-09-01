package com.example.reports

import androidx.compose.ui.graphics.toArgb
import com.example.ui.theme.GlobalFontScaleSteps
import com.example.ui.theme.NeonPalette
import com.example.ui.theme.ThemeNeonTeal

/**
 * یکپارچه‌ترین حالت ممکن: تنها «اسنپشات تم» برای همهٔ خروجی‌ها (کارت‌های PNG،
 * PDFها و تصویر صفحات). مقادیر همیشه از تم فعال + پلهٔ سایز فونت کاربر ساخته
 * می‌شوند تا هیچ خروجی‌ای رنگ/اندازهٔ پیش‌فرضِ ثابتی نبیند.
 *
 * نکات مهم:
 *  • [current] آخرین مقدار نصب‌شده توسط اپ را برمی‌گرداند. [com.example.ui.DentalViewModel]
 *    با هر تغییر تم یا سایز فونت، آن را به‌روز می‌کند؛ بنابراین حتی فراخوانی‌هایی
 *    که از بک‌گراند/Worker (بدون دسترسی به ViewModel) اجرا می‌شوند هم تم درست
 *    را می‌گیرند و نه یک رنگ ثابت.
 *  • عمداً دیگر `Default`ِ تمِ طلایی وجود ندارد؛ باگ قبلی دقیقاً از همین‌جا
 *    می‌آمد: پارامتر `themeSpec` پیش‌فرض داشت و بعضی call-site‌ها پاسش
 *    نمی‌دادند ⇒ کارت نسخه و کارت یادآوری همیشه Amber (طلایی) رندر می‌شدند.
 */
data class ExportThemeSpec(
    val primaryArgb: Int,
    val secondaryArgb: Int,
    val tertiaryArgb: Int,
    val backgroundArgb: Int,
    val surfaceArgb: Int,
    val surfaceVariantArgb: Int,
    val primaryContainerArgb: Int,
    val onPrimaryContainerArgb: Int,
    val onPrimaryArgb: Int,
    val glowArgb: Int,
    val gradientStartArgb: Int,
    val gradientEndArgb: Int,
    val fontScaleMultiplier: Float = 1.0f
) {
    companion object {

        /** پله‌ای که در تنظیمات اپ «حالت عادی» حساب می‌شود (مقیاس = ۱.۰). */
        const val NORMAL_FONT_SCALE_INDEX = 9

        /**
         * @param fontScaleIndex پلهٔ سراسری سایز فونت از تنظیمات (۰..۱۹).
         *   پیش‌فرض = [NORMAL_FONT_SCALE_INDEX] تا اگر فراخوانی آن را نداند،
         *   حداقل اندازهٔ «معمولی» را بگیرد نه ۸۵٪ (که مقادیر قبلی بود).
         */
        fun fromPalette(palette: NeonPalette, fontScaleIndex: Int = NORMAL_FONT_SCALE_INDEX): ExportThemeSpec {
            val scale = GlobalFontScaleSteps.getOrElse(fontScaleIndex.coerceIn(0, 19)) { 1.0f }
            return ExportThemeSpec(
                primaryArgb = palette.primary.toArgb(),
                secondaryArgb = palette.secondary.toArgb(),
                tertiaryArgb = palette.tertiary.toArgb(),
                backgroundArgb = palette.background.toArgb(),
                surfaceArgb = palette.surface.toArgb(),
                surfaceVariantArgb = palette.surfaceVariant.toArgb(),
                primaryContainerArgb = palette.primaryContainer.toArgb(),
                onPrimaryContainerArgb = palette.onPrimaryContainer.toArgb(),
                onPrimaryArgb = palette.onPrimary.toArgb(),
                glowArgb = palette.glow.toArgb(),
                gradientStartArgb = palette.gradientStart.toArgb(),
                gradientEndArgb = palette.gradientEnd.toArgb(),
                fontScaleMultiplier = scale
            )
        }

        /** آخرین تم نصب‌شده (پیش‌فرض: تم مرجع برنامه). */
        @Volatile
        private var installed: ExportThemeSpec = fromPalette(ThemeNeonTeal, NORMAL_FONT_SCALE_INDEX)

        /** توسط [com.example.ui.DentalViewModel] در هر تغییر تم/فونت صدا زده می‌شود. */
        fun install(spec: ExportThemeSpec) {
            installed = spec
        }

        /**
         * تم پیش‌فرض برای مسیری که ViewModel ندارد (Worker، پرینت سرویس، پیش‌نمایش).
         * همیشه همان تمی است که کاربر در اپ انتخاب کرده.
         */
        fun current(): ExportThemeSpec = installed
    }
}
