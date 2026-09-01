package com.example.reports

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * ============================================================================
 *  تنها جدول ابعاد خروجی‌ها در کل برنامه
 * ============================================================================
 * قبلاً هر ژنراتور عرض دلخواه خودش را هارد‌کد کرده بود (۱۰۰۰ / ۱۰۸۰ / ۱۲۰۰ …)
 * و به‌همین‌دلیل نسبت ابعاد و چگالی متن بین کارت‌ها متفاوت بود. از این پس
 * **هر خروجی یک [ExportDocKind] دارد** و عرض/حاشیه/مقیاس فونت از همان یک‌جا
 * می‌آید.
 *
 * قاعدهٔ عرض‌ها:
 *  • مرجع طراحی = ۱۰۸۰px (عرض نمایش تصویر در پیش‌نمایش واتساپ/تلگرام/بله).
 *  • کارت‌های گفت‌وگومحور (اطلاع‌رسانی، صورت‌حساب، طرح درمان) روی همان ۱۰۸۰ می‌مانند.
 *  • کاغذهای رسمی تک‌ستونه ۱۲۴۰px = عرض A4 در ۱۵۰DPI ⇒ همان نسبت کاغذ.
 *  • جدول‌های پرتعداد (نسخه، فاکتور لابراتوار، دفتر حساب طرف‌حساب) ۱۴۸۰px تا
 *    خانه‌ها نشکنند/به‌هم نریزند.
 *  • کارت بانکی از قاعدهٔ «کاغذ» خارج است: نسبت استاندارد ISO/IEC 7810 ID-1
 *    (۸۵.۶×۵۳.۹۸ ⇒ ۱.۵۸۶) با ۱۰۰۰×۶۳۰ رعایت می‌شود.
 *
 * مهم: [scale] روی **همه** اندازه‌ها (فونت، حاشیه، فاصله‌ها، شعاع گوشه‌ها،
 * ضخامت خط) ضرب می‌شود، پس هر خروجی در عرض خودش دقیقاً همان نسبت‌بندی بصری
 * خروجی دیگر را دارد و ارتفاع همیشه از روی محتوا محاسبه می‌شود (نه ثابت).
 */
enum class ExportDocKind(

    /** نام فارسی که در لاگ/تست/دیباگ دیده می‌شود. */
    val label: String,

    /** عرض بوم به پیکسل — تنها منبع حقیقت برای این خروجی. */
    val widthPx: Int,

    /** حاشیهٔ کناری به نسبت عرض (برای همهٔ کارت‌ها یک عدد ⇒ هم‌ترازی بصری). */
    val marginRatio: Float = 0.056f,

    /**
     * ارتفاع ثابت به پیکسل. ۰ یعنی «ارتفاع از محتوا محاسبه شود» — که برای همهٔ
     * خروجی‌ها همان خواسته است؛ تنها کارت بانکی (تصویر با نسبت ازپیش‌محدد)
     * ارتفاع ثابت دارد.
     */
    val fixedHeightPx: Int = 0
) {
    // ---- کارت‌های گفت‌وگویی (واتساپ/تلگرام) ----
    InfoCard("کارت اطلاع‌رسانی", 1080),
    StatementCard("کارت صورت‌حساب / طرح درمان", 1080),

    // ---- کاغذهای رسمی بالینی و مالی ----
    PostOpCare("برگه مراقبت‌های پس از درمان", 1240),
    AdmissionForm("فرم پذیرش و تاریخچه پزشکی", 1240),
    LegalConsent("رضایت‌نامه آگاهانه و برائت‌نامه", 1240),
    InstallmentNotice("یادآوری قسط / اخطار تاخیر", 1240),

    // ---- خروجی‌های جدولی پهن ----
    Prescription("نسخهٔ دارویی (جدول ۵ ستونه)", 1480),
    LabInvoice("فاکتور رسمی سفارش لابراتوار", 1480),
    VendorLedger("صورتحساب طرف‌حساب / تأمین‌کننده", 1480),

    // ---- تنها خروجی‌ای که از رندرر یکپارچه رد نمی‌شود ----
    BankCard("کارت بانکی", 1000, marginRatio = 0.07f, fixedHeightPx = 630);

    /**
     * ضریب مقیاس این خروجی نسبت به مرجع ۱۰۸۰px. هر عددی که در رندرر به‌عنوان
     * اندازه استفاده می‌شود، در همین عدد ضرب می‌شود.
     */
    val scale: Float get() = widthPx / REFERENCE_WIDTH_PX.toFloat()

    /** حاشیهٔ کناری بر حسب پیکسل بومِ این خروجی. */
    val margin: Int get() = (widthPx * marginRatio).roundToInt()

    /** فاصلهٔ عمودی استاندارد بین بلوک‌ها (مقیاس‌خورده با عرض همین خروجی). */
    val blockGap: Int get() = (28f * scale).roundToInt()

    /** ارتفاع بوم؛ ۰ یعنی ارتفاع پویا از محتوا. */
    val heightPx: Int get() = fixedHeightPx

    companion object {
        /** عرض مرجع طراحی؛ تغییر این عدد تنها در صورت نیاز به بازطراحی کل سیستم. */
        const val REFERENCE_WIDTH_PX = 1080

        /** حداقل ارتفاع امن خروجی (جلوگیری از بوم صفر در صورت محتوای خالی). */
        const val MIN_HEIGHT_PX = 320
    }
}

/**
 * ابزار مشترک ذخیرهٔ فایل خروجی. همهٔ خروجی‌های تصویری (کارت‌ها و کارت بانکی)
 * در `cacheDir/shared` نوشته می‌شوند؛ دقیقاً همان مسیری که `file_paths.xml` به
 * FileProvider داده شده، بنابراین اشتراک‌گذاری بدون کپی‌کردنِ اضافه کار می‌کند.
 */
object ExportFiles {

    const val SHARE_DIR = "shared"

    fun newFile(context: Context, prefix: String, extension: String = "png"): File {
        val dir = File(context.applicationContext.cacheDir, SHARE_DIR).apply { mkdirs() }
        val safePrefix = prefix.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.ifBlank { "export" }
        return File(dir, "${safePrefix}_${System.currentTimeMillis()}.${extension.lowercase()}")
    }

    fun writeBitmap(context: Context, bitmap: Bitmap, prefix: String): File {
        val file = newFile(context, prefix)
        FileOutputStream(file).use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG encode failed" }
        }
        return file
    }
}
