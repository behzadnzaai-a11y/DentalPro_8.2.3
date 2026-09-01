package com.example.ui.screens

import com.example.data.Treatment

/**
 * ============================================================================
 *  دسته‌بندی نوع درمان — تنها مرجع تشخیص «ترمیمی» و «عصب‌کشی» در کل برنامه
 * ============================================================================
 * پیش‌تر هر صفحه با `title.contains(...)` خودش تصمیم می‌گرفت و نتیجه این بود که
 * کارت «سطوح درگیر ترمیم» زیرِ عصب‌کشی هم می‌آمد و برعکس. از این پس هر جا لازم
 * است بدانیم یک درمان ترمیمی است یا اندو، از همین دو تابع استفاده می‌شود.
 *
 * تشخیص هم روی عنوان فارسی و هم روی معادل‌های لاتین (که در قالب‌های درمان و
 * دیتای وارداتی دیده می‌شوند) کار می‌کند.
 */

/** کلیدواژه‌های درمان‌های ترمیمی (Restorative / Operative Dentistry). */
private val RESTORATIVE_KEYWORDS = listOf(
    // فارسی
    "ترمیم", "پرکردن", "پر کردن", "کامپوزیت", "آمالگام", "امالگام",
    "گلاس آینومر", "گلاس‌آینومر", "سیلانت", "فیشورسیلانت", "فیشور سیلانت",
    "اینله", "آنله", "بیلدآپ", "بیلد آپ", "بازسازی تاج", "پوسیدگی",
    // لاتین
    "restoration", "restorative", "filling", "composite", "amalgam",
    "glass ionomer", "sealant", "inlay", "onlay", "build-up", "buildup",
    "caries", "operative"
)

/** کلیدواژه‌های درمان‌های اندو / عصب‌کشی (Endodontics). */
private val ENDODONTIC_KEYWORDS = listOf(
    // فارسی
    "عصب کشی", "عصب‌کشی", "عصبکشی", "درمان ریشه", "روت کانال", "اندو",
    "پالپکتومی", "پالپوتومی", "پالپ", "رتریتمنت", "درمان مجدد ریشه",
    "آپیکواکتومی", "اپکسیفیکیشن", "اپکسوژنزیس", "کانال",
    // لاتین
    "root canal", "rct", "endo", "endodontic", "pulpectomy", "pulpotomy",
    "pulp", "retreatment", "apicoectomy", "apexification", "apexogenesis"
)

private fun String.matchesAny(keywords: List<String>): Boolean {
    val haystack = this.lowercase().trim()
    if (haystack.isBlank()) return false
    return keywords.any { haystack.contains(it.lowercase()) }
}

/**
 * آیا این درمان ترمیمی است؟ (کارت «سطوح درگیر ترمیم» فقط برای همین‌ها)
 * عنوان و یادداشت هر دو بررسی می‌شوند تا درمان‌های دست‌ساز هم پوشش داده شوند.
 */
fun isRestorativeTreatment(title: String, notes: String = ""): Boolean {
    // اندو مقدم است: «ترمیم تاجی پس از عصب‌کشی» یک درمان اندو محسوب می‌شود.
    if (title.matchesAny(ENDODONTIC_KEYWORDS)) return false
    return title.matchesAny(RESTORATIVE_KEYWORDS) || notes.matchesAny(RESTORATIVE_KEYWORDS)
}

/**
 * آیا این درمان عصب‌کشی/اندو است؟ (شماتیک دندان، خط‌کش طول کار و کانال‌ها فقط
 * برای همین‌ها نمایش داده می‌شود)
 */
fun isEndodonticTreatment(title: String, notes: String = ""): Boolean =
    title.matchesAny(ENDODONTIC_KEYWORDS) || notes.matchesAny(ENDODONTIC_KEYWORDS)

/** میان‌بُرهای راحت روی خودِ موجودیت درمان. */
val Treatment.isRestorative: Boolean get() = isRestorativeTreatment(title, notes)
val Treatment.isEndodontic: Boolean get() = isEndodonticTreatment(title, notes)

/**
 * شماره‌های دندانِ ثبت‌شده روی یک درمان به‌صورت عدد.
 * ورودی می‌تواند «۱۶», "16, 26" یا "16 - 26" باشد.
 */
fun parseToothNumbers(selectedTeeth: String): List<Int> {
    if (selectedTeeth.isBlank()) return emptyList()
    val normalized = selectedTeeth
        .map { ch ->
            when (ch) {
                in '۰'..'۹' -> ('0' + (ch - '۰'))
                in '٠'..'٩' -> ('0' + (ch - '٠'))
                else -> ch
            }
        }
        .joinToString("")
    return Regex("\\d{1,2}")
        .findAll(normalized)
        .mapNotNull { it.value.toIntOrNull() }
        .filter { it in 1..85 }
        .distinct()
        .toList()
}

/**
 * تبدیل شماره‌گذاری Universal (۱ تا ۳۲) به FDI، چون جدول آناتومی کانال‌ها
 * (EndoAnatomyManager) با FDI کار می‌کند ولی کاربر ممکن است Universal وارد کند.
 * اگر عدد ورودی خودش یک شماره معتبر FDI باشد، دست‌نخورده برمی‌گردد.
 */
fun toFdiToothNumber(tooth: Int): Int {
    // شماره‌های معتبر FDI: دائمی 11..48 و شیری 51..85
    val quadrant = tooth / 10
    val position = tooth % 10
    val looksLikeFdi = position in 1..8 && (quadrant in 1..4 || quadrant in 5..8)
    if (looksLikeFdi) return tooth

    return when (tooth) {
        in 1..8 -> 18 - (tooth - 1)      // 1..8   → 18..11 (بالا راست)
        in 9..16 -> 21 + (tooth - 9)     // 9..16  → 21..28 (بالا چپ)
        in 17..24 -> 38 - (tooth - 17)   // 17..24 → 38..31 (پایین چپ)
        in 25..32 -> 41 + (tooth - 25)   // 25..32 → 41..48 (پایین راست)
        else -> tooth
    }
}
