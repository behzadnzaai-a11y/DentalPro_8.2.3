package com.example.ui.screens

import com.example.data.Patient

/**
 * تشخیص «شیری یا دائمی» بر اساس سن بیمار، در سطح تک‌تک دندان‌ها.
 *
 * چرا اینجا و نه داخل صفحه‌ها: پیش از این هر جا به شکل متفاوتی حدس زده می‌شد —
 * چارت دندانی از روی [Patient.age] و جدول آناتومی کانال‌ها از روی متنِ عنوان درمان
 * (وجود کلمهٔ «شیری» در عنوان). نتیجه این بود که برای یک کودک ۶ ساله، چارت
 * دندان‌های شیری را نشان می‌داد ولی شماتیک عصب‌کشی آناتومی دندان دائمی را
 * می‌کشید. حالا هر دو از همین یک منبع تغذیه می‌شوند.
 *
 * منابع جدول رویش/افتادن (تاریخ مراجعه: ۲۰۲۶):
 *  - AAPD Reference Manual of Pediatric Dentistry — "Dental Growth and Development"
 *    (جدول رسمی: زمان شروع کلسیفیکاسیون، رویش و افتادن برای هر دندان، به تفکیک فک)
 *  - American Dental Association — Primary/Permanent Tooth Development chart
 *  - Muthu MS و همکاران، مرور نظام‌مند و متاآنالیز جهانی تنوع زمان رویش دندان‌های
 *    شیری، Arch Oral Biol 2024;158:105857
 *  - Vandana S و همکاران، همان برای دندان‌های دائمی، Am J Hum Biol 2024;36(8):e24060
 *
 * نکتهٔ مهم بالینی: این اعداد «میانگین جمعیتی» هستند. AAPD صراحتاً هشدار می‌دهد که
 * بسیاری از کودکانِ کاملاً طبیعی از این زمان‌بندی پیروی نمی‌کنند؛ رویش دندان شیری تا
 * ۴ تا ۵ ماه و دندان دائمی تا ۱۲ تا ۱۸ ماه زودتر یا دیرتر هم طبیعی است. بنابراین
 * خروجی این فایل فقط «پیش‌فرض هوشمند» چارت است و هرگز جای معاینه را نمی‌گیرد —
 * به همین دلیل کاربر همیشه می‌تواند وضعیت هر دندان را دستی تغییر دهد.
 */
enum class Dentition { PRIMARY, MIXED, PERMANENT }

/** وضعیت پیش‌بینی‌شدهٔ یک جایگاه دندانی در سن معین. */
enum class ToothPhase {
    /** هنوز رویش نکرده (نه شیری نه دائمی). */
    UNERUPTED,
    /** دندان شیری در دهان است. */
    PRIMARY,
    /** بازهٔ تعویض: ممکن است شیریِ لق یا دائمیِ در حال رویش باشد. */
    TRANSITIONAL,
    /** دندان دائمی در دهان است. */
    PERMANENT
}

/**
 * یک ردیف از جدول رویش، بر حسب «موقعیت» در ربع (۱ تا ۸ برای دائمی، ۱ تا ۵ برای شیری).
 *
 * @param primaryEruptM رویش دندان شیری، به ماه
 * @param exfoliateY افتادن دندان شیری، به سال
 * @param permanentEruptY رویش دندان دائمی جانشین، به سال
 */
private data class EruptionRow(
    val primaryEruptM: IntRange?,
    val exfoliateY: IntRange?,
    val permanentEruptY: ClosedFloatingPointRange<Double>
)

/**
 * جدول AAPD به تفکیک فک بالا/پایین. کلید = موقعیت دندان در ربع.
 *
 * موقعیت‌های ۱ تا ۵ در دندان شیری معادل: ثنایای میانی، ثنایای جانبی، نیش،
 * مولر اول شیری، مولر دوم شیری. جانشین دائمیِ مولرهای شیری، پرمولرها هستند
 * (موقعیت ۴ و ۵ دائمی)، و مولرهای دائمی (۶، ۷، ۸) جانشین ندارند — پشت قوس
 * رویش می‌کنند و همین است که دورهٔ مختلط را می‌سازد.
 */
private val UPPER_ERUPTION: Map<Int, EruptionRow> = mapOf(
    1 to EruptionRow(6..10, 7..8, 7.0..8.0),      // ثنایای میانی
    2 to EruptionRow(8..12, 8..9, 8.0..9.0),      // ثنایای جانبی
    3 to EruptionRow(16..20, 11..12, 11.0..12.0), // نیش
    4 to EruptionRow(11..18, 9..11, 10.0..11.0),  // مولر اول شیری → پرمولر اول
    5 to EruptionRow(20..30, 9..12, 10.0..12.0),  // مولر دوم شیری → پرمولر دوم
    6 to EruptionRow(null, null, 5.5..7.0),       // مولر اول دائمی («مولر ۶ سالگی»)
    7 to EruptionRow(null, null, 12.0..14.0),     // مولر دوم دائمی
    8 to EruptionRow(null, null, 17.0..30.0)      // مولر سوم
)

private val LOWER_ERUPTION: Map<Int, EruptionRow> = mapOf(
    1 to EruptionRow(5..8, 6..7, 6.0..7.0),
    2 to EruptionRow(7..10, 7..8, 7.0..8.0),
    3 to EruptionRow(16..20, 9..11, 9.0..11.0),
    4 to EruptionRow(11..18, 10..12, 10.0..12.0),
    5 to EruptionRow(20..30, 11..13, 11.0..13.0),
    6 to EruptionRow(null, null, 5.5..7.0),
    7 to EruptionRow(null, null, 12.0..14.0),
    8 to EruptionRow(null, null, 17.0..30.0)
)

/** مرزهای کلی دوره‌های دندانی (سال). */
private const val PRIMARY_ONLY_MAX_AGE = 5
private const val MIXED_MAX_AGE = 12

/**
 * سن مؤثر بیمار: تاریخ تولد بر عدد ذخیره‌شدهٔ سن مقدم است، چون `age` یک عدد
 * ثابت در دیتابیس است و با گذشت زمان کهنه می‌شود، ولی `birthDate` همیشه
 * سن درست را می‌دهد. اگر هیچ‌کدام معتبر نبود `null`.
 */
fun effectivePatientAge(patient: Patient?): Int? {
    if (patient == null) return null
    calculateJalaliAge(patient.birthDate)?.let { return it }
    return patient.age.takeIf { it in 1..120 }
}

/** دورهٔ دندانی بیمار. وقتی سن نامعلوم است، دائمی در نظر گرفته می‌شود (حالت امن برای بزرگسال). */
fun dentitionForAge(age: Int?): Dentition = when {
    age == null -> Dentition.PERMANENT
    age <= PRIMARY_ONLY_MAX_AGE -> Dentition.PRIMARY
    age <= MIXED_MAX_AGE -> Dentition.MIXED
    else -> Dentition.PERMANENT
}

fun dentitionForPatient(patient: Patient?): Dentition = dentitionForAge(effectivePatientAge(patient))

/** آیا چارت باید پیش‌فرض روی دندان‌های شیری باز شود؟ (فقط دورهٔ کاملاً شیری) */
fun defaultsToPrimaryChart(patient: Patient?): Boolean =
    dentitionForPatient(patient) == Dentition.PRIMARY

/** آیا این شمارهٔ FDI یک دندان شیری است (ربع‌های ۵ تا ۸)؟ */
fun isPrimaryToothNumber(tooth: Int): Boolean = tooth / 10 in 5..8

/** آیا این شمارهٔ FDI یک دندان دائمی است (ربع‌های ۱ تا ۴)؟ */
fun isPermanentToothNumber(tooth: Int): Boolean = tooth / 10 in 1..4

/** تبدیل شمارهٔ دندان دائمی به شیریِ متناظر (۱۱ → ۵۱). برای موقعیت ۶ تا ۸ معادلی ندارد. */
fun permanentToPrimaryNumber(tooth: Int): Int? {
    val q = tooth / 10
    val p = tooth % 10
    if (q !in 1..4 || p !in 1..5) return null
    return (q + 4) * 10 + p
}

/** تبدیل شمارهٔ دندان شیری به دائمیِ جانشین (۵۱ → ۱۱). */
fun primaryToPermanentNumber(tooth: Int): Int? {
    val q = tooth / 10
    val p = tooth % 10
    if (q !in 5..8 || p !in 1..5) return null
    return (q - 4) * 10 + p
}

private fun isUpperQuadrant(quadrant: Int) = quadrant == 1 || quadrant == 2 || quadrant == 5 || quadrant == 6

/**
 * وضعیت یک جایگاه دندانی در سن معین.
 *
 * ورودی می‌تواند شمارهٔ شیری یا دائمی باشد؛ در هر دو حالت «جایگاه» یکی است
 * (۵۱ و ۱۱ یک جایگاه‌اند) و پاسخ بر همان جایگاه داده می‌شود.
 *
 * منطق: بین «شروع افتادن شیری» و «پایان رویش دائمی» بازهٔ گذار است، چون در آن
 * فاصله واقعاً معلوم نیست کدام دندان در دهان است — و این دقیقاً همان چیزی است
 * که چارت مختلط باید نشان دهد.
 */
fun toothPhaseAtAge(tooth: Int, age: Int?): ToothPhase {
    if (age == null) return ToothPhase.PERMANENT
    val q = tooth / 10
    val p = tooth % 10
    if (p !in 1..8) return ToothPhase.PERMANENT
    val table = if (isUpperQuadrant(q)) UPPER_ERUPTION else LOWER_ERUPTION
    val row = table[p] ?: return ToothPhase.PERMANENT

    // دندان‌های بدون جانشین شیری (مولرهای دائمی ۶، ۷، ۸)
    if (row.primaryEruptM == null || row.exfoliateY == null) {
        return when {
            age < row.permanentEruptY.start -> ToothPhase.UNERUPTED
            age < row.permanentEruptY.endInclusive -> ToothPhase.TRANSITIONAL
            else -> ToothPhase.PERMANENT
        }
    }

    val primaryEruptYears = row.primaryEruptM.first / 12.0
    val exfoliationStart = row.exfoliateY.first.toDouble()
    val permanentDone = row.permanentEruptY.endInclusive

    return when {
        age < primaryEruptYears -> ToothPhase.UNERUPTED
        age < exfoliationStart -> ToothPhase.PRIMARY
        age < permanentDone -> ToothPhase.TRANSITIONAL
        else -> ToothPhase.PERMANENT
    }
}

/**
 * چارت پیشنهادی برای یک ربع در سن معین: برای هر جایگاه، شمارهٔ دندانی که
 * به‌احتمال زیاد در دهان است برگردانده می‌شود.
 *
 * در بازهٔ گذار، شمارهٔ **دائمی** برگردانده می‌شود چون درمانِ ثبت‌شده در آن سن
 * معمولاً روی دندان تازه‌رویش‌یافته است، ولی [toothPhaseAtAge] همچنان
 * TRANSITIONAL گزارش می‌کند تا رابط کاربری بتواند هر دو گزینه را نشان دهد.
 */
fun suggestedToothNumber(permanentTooth: Int, age: Int?): Int =
    when (toothPhaseAtAge(permanentTooth, age)) {
        ToothPhase.PRIMARY -> permanentToPrimaryNumber(permanentTooth) ?: permanentTooth
        else -> permanentTooth
    }

/**
 * ترتیب جایگاه‌های یک قوس، از سمت راست بیمار به چپ (همان ترتیبی که چارت رسم می‌شود).
 * برای فک بالا: ۱۸..۱۱ سپس ۲۱..۲۸ — و برای پایین: ۴۸..۴۱ سپس ۳۱..۳۸.
 */
private fun archPositions(upper: Boolean): List<Int> {
    val right = if (upper) 1 else 4
    val left = if (upper) 2 else 3
    return (8 downTo 1).map { right * 10 + it } + (1..8).map { left * 10 + it }
}

/**
 * فهرست شماره‌های دندانِ یک قوس برای نمایش در چارت.
 *
 * - PERMANENT: همان ۱۶ جایگاه دائمی.
 * - PRIMARY: فقط ۱۰ جایگاه شیری (مولرهای دائمی در دورهٔ شیری وجود ندارند).
 * - MIXED: برای هر جایگاه، بسته به سن، شمارهٔ شیری یا دائمی انتخاب می‌شود؛
 *   جایگاه‌هایی که هنوز رویش نکرده‌اند حذف می‌شوند تا چارت دندانِ ناموجود
 *   نشان ندهد. این همان چارت ترکیبیِ استاندارد دورهٔ مختلط است.
 */
fun archForMode(mode: Dentition, age: Int?, upper: Boolean): List<String> {
    val positions = archPositions(upper)
    return when (mode) {
        Dentition.PERMANENT -> positions.map { it.toString() }
        Dentition.PRIMARY -> positions.mapNotNull { permanentToPrimaryNumber(it)?.toString() }
        Dentition.MIXED -> positions.mapNotNull { perm ->
            when (toothPhaseAtAge(perm, age)) {
                ToothPhase.UNERUPTED -> null
                ToothPhase.PRIMARY -> permanentToPrimaryNumber(perm)?.toString() ?: perm.toString()
                else -> perm.toString()
            }
        }
    }
}

/** برچسب فارسی وضعیت، برای نمایش در راهنمای چارت. */
fun toothPhaseLabel(phase: ToothPhase): String = when (phase) {
    ToothPhase.UNERUPTED -> "رویش‌نکرده"
    ToothPhase.PRIMARY -> "شیری"
    ToothPhase.TRANSITIONAL -> "در حال تعویض"
    ToothPhase.PERMANENT -> "دائمی"
}

/** توضیح دورهٔ دندانی بیمار برای نوار بالای چارت. */
fun dentitionSummaryLabel(age: Int?): String = when {
    age == null -> "سن ثبت نشده — چارت دائمی"
    age <= PRIMARY_ONLY_MAX_AGE -> "$age ساله — دورهٔ دندان‌های شیری"
    age <= MIXED_MAX_AGE -> "$age ساله — دورهٔ دندانی مختلط (شیری و دائمی)"
    else -> "$age ساله — دورهٔ دندان‌های دائمی"
}

/**
 * آیا این درمان به دندان شیری مربوط است؟
 * اول از روی شمارهٔ دندان (قطعی‌ترین نشانه)، و اگر دندانی ثبت نشده بود از روی
 * عنوان درمان. برای درمانی که هیچ نشانه‌ای ندارد `null` برمی‌گردد تا در فیلتر
 * به اشتباه در یکی از دو گروه جا نگیرد.
 */
fun treatmentDentition(selectedTeeth: String, title: String): Dentition? {
    val teeth = parseToothNumbers(selectedTeeth).map { toFdiToothNumber(it) }
    if (teeth.isNotEmpty()) {
        val anyPrimary = teeth.any { isPrimaryToothNumber(it) }
        val anyPermanent = teeth.any { isPermanentToothNumber(it) }
        return when {
            anyPrimary && !anyPermanent -> Dentition.PRIMARY
            anyPermanent && !anyPrimary -> Dentition.PERMANENT
            anyPrimary && anyPermanent -> Dentition.MIXED
            else -> null
        }
    }
    val t = title.lowercase()
    val primaryHint = title.contains("شیری") || title.contains("پالپوتومی") ||
            title.contains("اطفال") || t.contains("pediatric") || t.contains("ssc")
    return if (primaryHint) Dentition.PRIMARY else null
}
