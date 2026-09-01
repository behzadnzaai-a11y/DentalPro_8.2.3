package com.example.ui.components

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ============================================================================
 *  موتور هندسهٔ شماتیک دندان — پورت دقیق نسخهٔ ۴ از docs/tooth-schematics.html
 * ============================================================================
 *
 * این فایل «عیناً» همان موتوری است که در نمونهٔ HTML توافق شد و تأیید گرفت.
 * هیچ عدد یا فرمولی بازنویسی یا ساده‌سازی نشده است؛ فقط از JavaScript به
 * Kotlin برگردانده شده تا اپلیکیشن دقیقاً همان تصویری را بکشد که در مرورگر
 * دیدید. تغییر هر ثابتی در این فایل، شماتیک را از نمونهٔ تأییدشده دور می‌کند.
 *
 * قرارداد هندسی (همان نسخهٔ HTML):
 *  • مبدأ y=0 روی نوک کاسپ / لبهٔ اینسیزال است و همه‌چیز میلی‌متر واقعی است.
 *  • برای فک بالا محور y وارونه می‌شود (ریشه رو به بالا).
 *  • برای سمت چپ (کوادرانت ۲،۳،۶،۷) محور x آینه می‌شود.
 *  • محور ریشه [rootCx] در تراز فورکا پیوسته است (بدون پرش C⁰).
 *  • نیم‌عرض ریشه [rootHw] با «بیشینهٔ نرم» تضمین می‌کند ریشه هرگز از کانال
 *    باریک‌تر نشود؛ یعنی هر ریشه همیشه دقیقاً روی کانال‌های خودش می‌نشیند.
 *  • فورکا از محل واقعی جدا شدن پاکت دو ریشهٔ مجاور ساخته می‌شود، نه از یک
 *    شکل ثابت — پس شکل طبیعی خودِ دندان است.
 */

// ————————————————————————————————————————————————————————————————————————
// ۱) داده‌های آناتومی — کانال‌ها
// ————————————————————————————————————————————————————————————————————————

/** یک کانال در جدول آناتومی. [isFixed] یعنی «ثابت»؛ قابل حذف نیست. */
data class SchematicCanal(
    val name: String,
    val minLen: Double,
    val maxLen: Double,
    val avg: Double,
    val refPoint: String,
    val mafFile: String,
    val isFixed: Boolean,
    val note: String? = null
)

private fun c(
    name: String, mn: Double, mx: Double, avg: Double,
    ref: String, maf: String, fixed: Boolean, note: String? = null
) = SchematicCanal(name, mn, mx, avg, ref, maf, fixed, note)

/** جدول ANATOMY — کپی عینی از نسخهٔ HTML. */
private val ANATOMY: Map<Int, List<SchematicCanal>> = mapOf(
    11 to listOf(c("C", 21.0, 26.0, 23.5, "Incisal Edge", "#40", true, "همیشه حاضر")),
    12 to listOf(
        c("C", 20.0, 25.0, 22.5, "Incisal Edge", "#35", true, "همیشه حاضر"),
        c("P", 20.0, 25.0, 22.5, "Incisal Edge", "#35", false, "فراوانی ۱ الی ۳ درصد موارد در ایران")
    ),
    13 to listOf(
        c("C", 23.0, 30.0, 26.5, "Cusp Tip", "#45", true, "همیشه حاضر"),
        c("L", 23.0, 30.0, 26.5, "Cusp Tip", "#45", false, "فراوانی کانال دوم ۱ الی ۲ درصد موارد در ایران")
    ),
    14 to listOf(
        c("B", 19.0, 23.0, 21.0, "Buccal Cusp", "#25", true, "همیشه حاضر"),
        c("P", 19.0, 23.0, 21.0, "Palatal Cusp", "#25", false, "فراوانی ۸۵ درصد موارد در ایران"),
        c("C3", 18.0, 22.0, 20.0, "Cusp Tip", "#25", false, "فراوانی ۲ الی ۳ درصد موارد در ایران")
    ),
    15 to listOf(
        c("C", 19.0, 24.0, 21.5, "Cusp Tip", "#30", true, "همیشه حاضر"),
        c("P", 19.0, 24.0, 21.5, "Palatal Cusp", "#30", false, "فراوانی ۲۵ درصد موارد در ایران")
    ),
    16 to listOf(
        c("MB1", 19.0, 22.0, 20.5, "MB Cusp", "#30", true, "همیشه حاضر"),
        c("MB2", 18.0, 21.0, 19.5, "MB Cusp", "#25", false, "فراوانی ۶۵ الی ۸۰ درصد موارد در ایران"),
        c("DB", 18.0, 21.0, 19.5, "DB Cusp", "#25", true, "همیشه حاضر"),
        c("P", 20.0, 24.0, 22.0, "Palatal Cusp", "#40", true, "همیشه حاضر (طولانی‌ترین کانال)")
    ),
    17 to listOf(
        c("MB1", 18.0, 22.0, 20.0, "MB Cusp", "#30", true, "همیشه حاضر"),
        c("MB2", 17.0, 21.0, 19.0, "MB Cusp", "#25", false, "فراوانی ۳۰ الی ۴۰ درصد موارد در ایران"),
        c("DB", 18.0, 22.0, 20.0, "DB Cusp", "#25", true, "همیشه حاضر"),
        c("P", 18.0, 22.0, 20.0, "Palatal Cusp", "#40", true, "همیشه حاضر")
    ),
    18 to listOf(
        c("MB", 19.0, 22.0, 19.0, "MB Cusp", "#30", true),
        c("DB", 19.0, 22.0, 19.0, "DB Cusp", "#30", true),
        c("P", 19.0, 22.0, 19.5, "Palatal Cusp", "#30", true)
    ),
    31 to listOf(
        c("B", 19.0, 23.0, 21.0, "Incisal Edge", "#35", true, "همیشه حاضر"),
        c("L", 19.0, 23.0, 21.0, "Incisal Edge", "#35", false, "فراوانی ۲۰ الی ۳۵ درصد موارد در ایران")
    ),
    32 to listOf(
        c("B", 20.0, 24.0, 22.0, "Incisal Edge", "#35", true, "همیشه حاضر"),
        c("L", 20.0, 24.0, 22.0, "Incisal Edge", "#35", false, "فراوانی ۱۵ الی ۳۰ درصد موارد در ایران")
    ),
    33 to listOf(
        c("C", 22.0, 28.0, 25.0, "Cusp Tip", "#45", true, "همیشه حاضر"),
        c("L", 22.0, 28.0, 25.0, "Cusp Tip", "#45", false, "فراوانی ۲ الی ۵ درصد موارد در ایران")
    ),
    34 to listOf(
        c("B", 20.0, 24.0, 22.0, "Buccal Cusp", "#30", true, "همیشه حاضر"),
        c("L", 20.0, 24.0, 22.0, "Lingual Cusp", "#30", false, "فراوانی ۲۵ الی ۳۰ درصد موارد در ایران")
    ),
    35 to listOf(
        c("C", 20.0, 25.0, 22.5, "Buccal Cusp", "#30", true, "همیشه حاضر"),
        c("L", 20.0, 25.0, 22.5, "Lingual Cusp", "#30", false, "فراوانی ۱۰ الی ۱۵ درصد موارد در ایران")
    ),
    36 to listOf(
        c("MB", 20.0, 23.0, 21.5, "MB Cusp", "#30", true, "همیشه حاضر (بخشی از ریشه مزیال)"),
        c("ML", 20.0, 23.0, 21.5, "ML Cusp", "#25", true, "همیشه حاضر"),
        c("D", 19.0, 23.0, 21.0, "Distal Cusp", "#40", true, "همیشه حاضر"),
        c("DB", 18.0, 22.0, 20.0, "Distal Cusp", "#25", false, "فراوانی ۲۵ الی ۳۵ درصد موارد در ایران (چهارمین کانال)")
    ),
    37 to listOf(
        c("MB", 18.0, 22.0, 20.0, "MB Cusp", "#30", true, "همیشه حاضر"),
        c("ML", 18.0, 22.0, 20.0, "ML Cusp", "#25", true, "همیشه حاضر"),
        c("D", 18.0, 22.0, 20.0, "Distal Cusp", "#40", true, "همیشه حاضر"),
        c("C-Shape", 18.0, 22.0, 20.0, "Distal Cusp", "#35", false, "فراوانی ۵ الی ۸ درصد موارد در ایران"),
        c("DB", 17.0, 21.0, 19.0, "Distal Cusp", "#25", false, "فراوانی ۱۰ الی ۲۰ درصد موارد در ایران")
    ),
    38 to listOf(
        c("MB", 17.0, 22.0, 19.5, "MB Cusp", "#30", true),
        c("ML", 17.0, 22.0, 19.5, "ML Cusp", "#25", true),
        c("D", 17.0, 22.0, 19.0, "Distal Cusp", "#40", true)
    ),
    51 to listOf(c("C", 14.0, 20.0, 16.0, "Incisal Edge", "#30", true)),
    52 to listOf(c("C", 13.0, 19.0, 15.0, "Incisal Edge", "#30", true)),
    53 to listOf(c("C", 15.0, 21.0, 18.0, "Cusp Tip", "#35", true)),
    54 to listOf(
        c("MB", 13.0, 18.0, 15.0, "MB Cusp", "#25", true),
        c("DB", 13.0, 18.0, 15.0, "DB Cusp", "#25", true),
        c("P", 13.0, 18.0, 16.0, "Palatal Cusp", "#30", true)
    ),
    55 to listOf(
        c("MB", 14.0, 19.0, 16.0, "MB Cusp", "#25", true),
        c("DB", 14.0, 19.0, 16.0, "DB Cusp", "#25", true),
        c("P", 14.0, 19.0, 17.0, "Palatal Cusp", "#30", true)
    ),
    71 to listOf(c("C", 12.0, 18.0, 14.0, "Incisal Edge", "#25", true)),
    72 to listOf(c("C", 12.5, 18.5, 14.5, "Incisal Edge", "#25", true)),
    73 to listOf(c("C", 14.0, 20.0, 17.0, "Cusp Tip", "#30", true)),
    74 to listOf(
        c("M", 13.0, 18.0, 15.0, "Mesial Cusp", "#25", true),
        c("D", 13.0, 18.0, 15.0, "Distal Cusp", "#25", true)
    ),
    75 to listOf(
        c("M", 14.0, 19.0, 16.5, "Mesial Cusp", "#25", true),
        c("D", 14.0, 19.0, 16.5, "Distal Cusp", "#25", true)
    )
)

private val MIRROR: Map<Int, Int> = mapOf(
    21 to 11, 22 to 12, 23 to 13, 24 to 14, 25 to 15, 26 to 16, 27 to 17, 28 to 18,
    41 to 31, 42 to 32, 43 to 33, 44 to 34, 45 to 35, 46 to 36, 47 to 37, 48 to 38,
    61 to 51, 62 to 52, 63 to 53, 64 to 54, 65 to 55,
    81 to 71, 82 to 72, 83 to 73, 84 to 74, 85 to 75
)

/** شمارهٔ متناظر در جدول (دندان‌های سمت چپ به قرینهٔ راست نگاشت می‌شوند). */
fun canonTooth(t: Int): Int = if (ANATOMY.containsKey(t)) t else (MIRROR[t] ?: 11)

/** فهرست کانال‌های آناتومیک یک دندان. */
fun schematicCanalsFor(tooth: Int): List<SchematicCanal> = ANATOMY[canonTooth(tooth)].orEmpty()

// ————————————————————————————————————————————————————————————————————————
// ۲) نقشهٔ ریشه‌ها
// ————————————————————————————————————————————————————————————————————————

/**
 * یک ریشه. [xFurc]/[xApex] فاصلهٔ افقی مرکز ریشه از محور در تراز فورکا و اپکس،
 * [wFurc]/[wApex] عرض ریشه در همان دو تراز، و [curve] انحنای آپیکال.
 */
data class RootSpec(
    val id: String,
    val label: String,
    val canals: List<String>,
    val xFurc: Double,
    val xApex: Double,
    val wFurc: Double,
    val wApex: Double,
    val curve: Double
)

enum class CrownType { INC, CAN, PM, MOL }

data class ToothMap(
    val plane: String,           // "MD" یا "BL"
    val crownW: Double,
    val crownH: Double,
    val furcBelowCej: Double,
    val type: CrownType,
    val roots: List<RootSpec>
)

private fun r(
    id: String, label: String, canals: List<String>,
    xFurc: Double, xApex: Double, wFurc: Double, wApex: Double, curve: Double
) = RootSpec(id, label, canals, xFurc, xApex, wFurc, wApex, curve)

/** جدول ROOTMAP — کپی عینی از نسخهٔ HTML. */
private val ROOTMAP: Map<Int, ToothMap> = mapOf(
    11 to ToothMap("MD", 8.6, 10.5, 0.0, CrownType.INC,
        listOf(r("R", "ریشه", listOf("C"), 0.0, 0.0, 6.2, 1.3, 0.5))),
    12 to ToothMap("MD", 7.0, 9.5, 0.0, CrownType.INC,
        listOf(r("R", "ریشه", listOf("C", "P"), 0.0, 0.0, 5.6, 1.3, 0.6))),
    13 to ToothMap("MD", 7.8, 10.5, 0.0, CrownType.CAN,
        listOf(r("R", "ریشه", listOf("C", "L"), 0.0, 0.0, 6.6, 1.5, 0.5))),
    14 to ToothMap("BL", 7.4, 8.6, 7.2, CrownType.PM, listOf(
        r("B", "باکال", listOf("B"), -1.5, -2.4, 2.5, 1.1, -0.2),
        r("C3", "میانی", listOf("C3"), 0.0, 0.0, 2.1, 1.0, 0.0),
        r("P", "پالاتال", listOf("P"), 1.5, 2.5, 2.5, 1.1, 0.2)
    )),
    15 to ToothMap("BL", 7.0, 8.4, 0.0, CrownType.PM,
        listOf(r("R", "ریشه", listOf("C", "P"), 0.0, 0.0, 5.2, 1.2, 0.3))),
    16 to ToothMap("BL", 10.4, 7.5, 3.8, CrownType.MOL, listOf(
        r("MB", "مزیوباکال", listOf("MB1", "MB2"), -2.9, -4.4, 3.2, 1.2, 0.35),
        r("DB", "دیستوباکال", listOf("DB"), -0.6, -1.3, 2.7, 1.1, 0.2),
        r("P", "پالاتال", listOf("P"), 2.8, 4.7, 3.4, 1.3, -0.45)
    )),
    17 to ToothMap("BL", 9.8, 7.3, 4.0, CrownType.MOL, listOf(
        r("MB", "مزیوباکال", listOf("MB1", "MB2"), -2.6, -3.7, 3.1, 1.2, 0.3),
        r("DB", "دیستوباکال", listOf("DB"), -0.5, -1.1, 2.6, 1.1, 0.15),
        r("P", "پالاتال", listOf("P"), 2.6, 4.0, 3.3, 1.3, -0.35)
    )),
    18 to ToothMap("BL", 9.0, 7.0, 4.2, CrownType.MOL, listOf(
        r("MB", "مزیوباکال", listOf("MB"), -2.2, -3.0, 2.9, 1.2, 0.3),
        r("DB", "دیستوباکال", listOf("DB"), -0.4, -0.9, 2.5, 1.1, 0.15),
        r("P", "پالاتال", listOf("P"), 2.3, 3.3, 3.0, 1.2, -0.3)
    )),
    31 to ToothMap("BL", 5.3, 9.0, 0.0, CrownType.INC,
        listOf(r("R", "ریشه", listOf("B", "L"), 0.0, 0.0, 4.4, 1.1, 0.4))),
    32 to ToothMap("BL", 5.9, 9.5, 0.0, CrownType.INC,
        listOf(r("R", "ریشه", listOf("B", "L"), 0.0, 0.0, 4.7, 1.1, 0.4))),
    33 to ToothMap("BL", 6.9, 11.0, 0.0, CrownType.CAN,
        listOf(r("R", "ریشه", listOf("C", "L"), 0.0, 0.0, 6.0, 1.4, 0.45))),
    34 to ToothMap("BL", 7.1, 8.6, 0.0, CrownType.PM,
        listOf(r("R", "ریشه", listOf("B", "L"), 0.0, 0.0, 5.0, 1.2, 0.4))),
    35 to ToothMap("BL", 7.2, 8.4, 0.0, CrownType.PM,
        listOf(r("R", "ریشه", listOf("C", "L"), 0.0, 0.0, 5.1, 1.2, 0.4))),
    36 to ToothMap("MD", 10.8, 7.6, 3.0, CrownType.MOL, listOf(
        r("M", "مزیال", listOf("MB", "ML"), -2.7, -3.8, 3.6, 1.3, 0.55),
        r("D", "دیستال", listOf("D", "DB"), 2.5, 3.5, 3.2, 1.2, -0.25)
    )),
    37 to ToothMap("MD", 10.2, 7.4, 3.2, CrownType.MOL, listOf(
        r("M", "مزیال", listOf("MB", "ML"), -2.5, -3.5, 3.5, 1.3, 0.5),
        r("D", "دیستال", listOf("D", "DB", "C-Shape"), 2.4, 3.3, 3.2, 1.2, -0.2)
    )),
    38 to ToothMap("MD", 9.4, 7.0, 3.4, CrownType.MOL, listOf(
        r("M", "مزیال", listOf("MB", "ML"), -2.2, -2.9, 3.3, 1.2, 0.45),
        r("D", "دیستال", listOf("D"), 2.1, 2.8, 3.0, 1.2, -0.2)
    )),
    51 to ToothMap("MD", 6.5, 6.2, 0.0, CrownType.INC,
        listOf(r("R", "ریشه", listOf("C"), 0.0, 0.0, 4.6, 1.1, 0.4))),
    52 to ToothMap("MD", 5.1, 5.6, 0.0, CrownType.INC,
        listOf(r("R", "ریشه", listOf("C"), 0.0, 0.0, 4.0, 1.0, 0.4))),
    53 to ToothMap("MD", 6.9, 6.5, 0.0, CrownType.CAN,
        listOf(r("R", "ریشه", listOf("C"), 0.0, 0.0, 4.8, 1.2, 0.4))),
    54 to ToothMap("BL", 7.1, 5.1, 1.6, CrownType.MOL, listOf(
        r("MB", "مزیوباکال", listOf("MB"), -2.4, -3.6, 2.3, 1.0, 0.4),
        r("DB", "دیستوباکال", listOf("DB"), -0.4, -1.0, 2.0, 0.9, 0.2),
        r("P", "پالاتال", listOf("P"), 2.3, 3.7, 2.4, 1.0, -0.4)
    )),
    55 to ToothMap("BL", 8.2, 5.7, 1.8, CrownType.MOL, listOf(
        r("MB", "مزیوباکال", listOf("MB"), -2.6, -3.9, 2.5, 1.0, 0.4),
        r("DB", "دیستوباکال", listOf("DB"), -0.4, -1.1, 2.1, 0.9, 0.2),
        r("P", "پالاتال", listOf("P"), 2.5, 4.0, 2.6, 1.1, -0.4)
    )),
    71 to ToothMap("MD", 4.2, 5.0, 0.0, CrownType.INC,
        listOf(r("R", "ریشه", listOf("C"), 0.0, 0.0, 3.4, 0.9, 0.4))),
    72 to ToothMap("MD", 4.6, 5.2, 0.0, CrownType.INC,
        listOf(r("R", "ریشه", listOf("C"), 0.0, 0.0, 3.6, 0.9, 0.4))),
    73 to ToothMap("MD", 5.6, 6.0, 0.0, CrownType.CAN,
        listOf(r("R", "ریشه", listOf("C"), 0.0, 0.0, 4.4, 1.1, 0.4))),
    74 to ToothMap("MD", 7.9, 5.7, 1.5, CrownType.MOL, listOf(
        r("M", "مزیال", listOf("M"), -2.6, -4.0, 2.6, 1.1, 0.5),
        r("D", "دیستال", listOf("D"), 2.5, 3.8, 2.4, 1.0, -0.3)
    )),
    75 to ToothMap("MD", 9.4, 5.9, 1.7, CrownType.MOL, listOf(
        r("M", "مزیال", listOf("M"), -3.0, -4.5, 2.8, 1.1, 0.5),
        r("D", "دیستال", listOf("D"), 2.8, 4.2, 2.6, 1.0, -0.3)
    ))
)

fun rootMapFor(tooth: Int): ToothMap = ROOTMAP[canonTooth(tooth)] ?: ROOTMAP[11]!!

// ————————————————————————————————————————————————————————————————————————
// ۳) کمکی‌های جهت — «بالا/پایین» و «چپ/راست»
// ————————————————————————————————————————————————————————————————————————

/** فک بالا: کوادرانت ۱،۲،۵،۶ — ریشه رو به بالا کشیده می‌شود. */
fun isUpperTooth(t: Int): Boolean {
    val q = t / 10
    return q == 1 || q == 2 || q == 5 || q == 6
}

/** سمت چپ بیمار: کوادرانت ۲،۳،۶،۷ — تصویر آینه می‌شود. */
fun isLeftSideTooth(t: Int): Boolean {
    val q = t / 10
    return q == 2 || q == 3 || q == 6 || q == 7
}

fun isAnteriorTooth(t: Int): Boolean = (t % 10) in 1..3

/** برچسب دو سر تصویر بر اساس صفحهٔ نمایش و سمت دندان. */
fun directionLabels(tooth: Int): Pair<String, String> {
    val md = rootMapFor(tooth).plane == "MD"
    val flipPositive = !isLeftSideTooth(tooth)
    val left = if (md) (if (flipPositive) "دیستال" else "مزیال")
    else (if (flipPositive) "باکال" else "پالاتال/لینگوال")
    val right = if (md) (if (flipPositive) "مزیال" else "دیستال")
    else (if (flipPositive) "پالاتال/لینگوال" else "باکال")
    return left to right
}

fun jawLabel(tooth: Int): String =
    if (isUpperTooth(tooth)) "فک بالا — ریشه رو به بالا" else "فک پایین — ریشه رو به پایین"

// ————————————————————————————————————————————————————————————————————————
// ۴) موتور هندسه — تابع خالص
// ————————————————————————————————————————————————————————————————————————

/** کانال پس از محاسبه: طول کار، آفست از محور ریشه، و مختصات دهانه. */
class CanalGeom(
    val spec: SchematicCanal,
    val colorIndex: Int
) {
    var wl: Double = 0.0
    var off: Double = 0.0
    var orificeX: Double = 0.0
    val name: String get() = spec.name
}

/** ریشه پس از محاسبه. مقادیر `_`دار همان‌های نسخهٔ HTML هستند. */
class RootGeom(val spec: RootSpec) {
    val canals = mutableListOf<CanalGeom>()
    var xF: Double = 0.0
    var xA: Double = 0.0
    var wFurc: Double = spec.wFurc
    var wApex: Double = spec.wApex
    var apexY: Double = 0.0
    var cH: Double = 0.0
    val curve: Double get() = spec.curve
}

class ToothGeometry(
    val map: ToothMap,
    val roots: List<RootGeom>,
    val crownH: Double,
    val furcY: Double,
    val maxY: Double,
    val multi: Boolean
)

/** بیشینهٔ نرم: همیشه ≥ max(a,b) ولی بدون گوشهٔ تیز. */
fun smax(a: Double, b: Double, k: Double): Double =
    0.5 * (a + b + sqrt((a - b) * (a - b) + k * k))

private fun smoothstep(t: Double): Double = t * t * (3 - 2 * t)

/**
 * ساخت هندسهٔ کامل دندان.
 *
 * @param on کدام کانال‌ها روشن‌اند (ثابت‌ها همیشه true)
 * @param wl طول کار هر کانال برحسب میلی‌متر؛ نبودِ مقدار یعنی میانگین آناتومیک
 */
fun buildToothGeometry(
    tooth: Int,
    on: Map<String, Boolean>,
    wl: Map<String, Double>
): ToothGeometry {
    val map = rootMapFor(tooth)
    val specs = schematicCanalsFor(tooth).filter { on[it.name] ?: it.isFixed }
    val crownH = map.crownH
    val orderIndex = schematicCanalsFor(tooth).withIndex().associate { (i, s) -> s.name to i }

    // هر ریشه فقط کانال‌های خودش را برمی‌دارد؛ ریشهٔ بدون کانال حذف می‌شود.
    var roots = map.roots.map { rs ->
        RootGeom(rs).also { g ->
            specs.filter { rs.canals.contains(it.name) }
                .forEach { g.canals.add(CanalGeom(it, orderIndex[it.name] ?: 0)) }
        }
    }.filter { it.canals.isNotEmpty() }.toMutableList()

    // کانالی که به هیچ ریشه‌ای نخورد (نام غیرمنتظره) گم نمی‌شود.
    val claimed = roots.flatMap { g -> g.canals.map { it.name } }.toSet()
    val orphans = specs.filter { !claimed.contains(it.name) }
    if (orphans.isNotEmpty()) {
        if (roots.isEmpty()) roots = mutableListOf(RootGeom(map.roots[0]))
        orphans.forEachIndexed { i, o ->
            roots[i % roots.size].canals.add(CanalGeom(o, orderIndex[o.name] ?: 0))
        }
    }
    // هیچ‌وقت دندانِ بدون ریشه رسم نمی‌شود. رابط کاربری اجازه نمی‌دهد کانال
    // ثابت خاموش شود، ولی اگر تماس‌گیرندهٔ دیگری همهٔ کانال‌ها را خاموش بفرستد
    // باز هم باید یک ریشهٔ سالم (با کانال ثابتِ اول) دیده شود، نه تصویر خالی.
    if (roots.isEmpty()) {
        val fallbackRoot = map.roots.first()
        val fallbackCanal = schematicCanalsFor(tooth).firstOrNull { it.isFixed }
            ?: schematicCanalsFor(tooth).firstOrNull()
        roots = mutableListOf(
            RootGeom(fallbackRoot).also { g ->
                if (fallbackCanal != null) {
                    g.canals.add(CanalGeom(fallbackCanal, orderIndex[fallbackCanal.name] ?: 0))
                }
            }
        )
    }
    roots.sortBy { it.spec.xFurc }

    // تعداد ریشه از تعداد کانال پیروی می‌کند؛ ریشه‌های باقی‌مانده جمع‌تر می‌شوند.
    val shrink = if (roots.size == 1) 0.0 else roots.size.toDouble() / map.roots.size
    roots.forEach { g ->
        if (roots.size == 1) { g.xF = 0.0; g.xA = 0.0 }
        else {
            g.xF = g.spec.xFurc * (0.62 + 0.38 * shrink)
            g.xA = g.spec.xApex * (0.62 + 0.38 * shrink)
        }
    }
    // وقتی چند ریشه به یکی فرو می‌ریزد، آن یکی باید عرض ریشهٔ ادغام‌شده بگیرد
    // وگرنه دندان مثل سوزن دیده می‌شود.
    if (roots.size < map.roots.size) {
        val k = min(1.9, sqrt(map.roots.size.toDouble() / roots.size))
        roots.forEach { g ->
            g.wFurc *= k
            g.wApex *= min(k, 1.35)
        }
    }

    val multi = roots.size > 1
    val furcY = crownH + (if (multi) map.furcBelowCej else 0.6)
    val apical = 0.75
    roots.forEach { g ->
        val n = g.canals.size
        g.canals.forEachIndexed { i, s ->
            s.wl = max(6.0, wl[s.name] ?: s.spec.avg)
            s.off = if (n <= 1) 0.0 else (i - (n - 1) / 2.0) * min(0.95, g.wFurc * 0.30)
        }
        g.apexY = (g.canals.maxOf { it.wl }) + apical
        g.apexY = max(g.apexY, furcY + 2.0)
        // محور ریشه دست‌کم روی ۳٫۲ میلی‌متر باز می‌شود تا واگرایی ناگهانی نشود.
        g.cH = furcY - max(furcY - crownH, 3.2)
    }
    // دهانهٔ کانال روی کف اتاقک پالپ
    roots.forEach { g ->
        g.canals.forEach { s -> s.orificeX = (rootCx(g, furcY, furcY) + s.off) * 0.72 }
    }
    val maxY = roots.maxOf { it.apexY }
    return ToothGeometry(map, roots, crownH, furcY, maxY, multi)
}

/**
 * محور ریشه — در تراز فورکا کاملاً پیوسته (بدون پرش).
 * بالای فورکا با smoothstep از محور دندان باز می‌شود، پایین آن واگرایی +
 * انحنای آپیکال. مشتق در دو سر صفر است تا شکستگی نیفتد.
 */
fun rootCx(r: RootGeom, y: Double, furcY: Double): Double {
    if (y <= furcY) {
        val t = min(1.0, max(0.0, (y - r.cH) / max(0.001, furcY - r.cH)))
        return r.xF * smoothstep(t)
    }
    val t = min(1.0, (y - furcY) / max(0.001, r.apexY - furcY))
    val e = smoothstep(t)
    return r.xF + (r.xA - r.xF) * e + r.curve * sin(Math.PI * t).pow(1.3)
}

/** نیم‌عرض ریشه — هرگز باریک‌تر از کانال‌های داخلش نمی‌شود. */
fun rootHw(r: RootGeom, y: Double, furcY: Double): Double {
    val t = min(1.0, max(0.0, (y - furcY) / max(0.001, r.apexY - furcY)))
    val e = smoothstep(t)
    val hw = (r.wFurc / 2) + ((r.wApex / 2) - (r.wFurc / 2)) * e.pow(1.15)
    val need = (r.canals.maxOfOrNull { abs(it.off * (1 - 0.35 * t)) } ?: 0.0) + 0.62
    return smax(hw, need, 0.30)
}

/**
 * مسیر کانال. آفست از دهانه تا فورکا با smoothstep رشد می‌کند و چون [rootCx]
 * خودش پیوسته است، مسیر کانال هم در تراز فورکا هیچ شکستگی ندارد.
 */
fun canalX(r: RootGeom, s: CanalGeom, y: Double, furcY: Double, orificeY: Double): Double {
    val ta = min(1.0, max(0.0, (y - furcY) / max(0.001, r.apexY - furcY)))
    val axis = rootCx(r, y, furcY)
    if (y >= furcY) return axis + s.off * (1 - 0.35 * ta)
    val t = min(1.0, max(0.0, (y - orificeY) / max(0.001, furcY - orificeY)))
    val e = smoothstep(t)
    return s.orificeX + (axis + s.off - s.orificeX) * e
}

// ————————————————————————————————————————————————————————————————————————
// ۵) سیلوئت — اجتماع پاکت ریشه‌ها + فورکای طبیعی
// ————————————————————————————————————————————————————————————————————————

/** ثابت‌های ترسیم — عیناً از نسخهٔ HTML. */
const val OUTLINE_STEP = 0.30
const val FURC_ND = 2.2
const val FURC_MINSLOPE = 0.60
const val FURC_CLR = 0.58

data class MmPoint(val x: Double, val y: Double)

/** نقطهٔ فورنیکس (سقف گرد فورکا) بین دو ریشهٔ مجاور. */
data class Fornix(val x: Double, val y: Double)

class SilhouetteBuilder(private val g: ToothGeometry) {
    private val furcY = g.furcY
    private val roots = g.roots

    fun rawL(r: RootGeom, y: Double) = rootCx(r, y, furcY) - rootHw(r, y, furcY)
    fun rawR(r: RootGeom, y: Double) = rootCx(r, y, furcY) + rootHw(r, y, furcY)

    /** گذر گردن دندان روی دست‌کم ۲ میلی‌متر پخش می‌شود تا پله نشود. */
    private val trunkTop = furcY - max(furcY - g.crownH, 2.0)
    private fun uTrunk(y: Double): Double {
        val t = min(1.0, max(0.0, (y - trunkTop) / max(0.001, furcY - trunkTop)))
        return t * t * (3 - 2 * t)
    }

    val trunkHalf: Double = max(
        g.map.crownW * 0.40 * 0.92,
        max(abs(rawL(roots.first(), furcY)), abs(rawR(roots.last(), furcY)))
    )

    /** ارتفاع جدا شدن هر دو ریشهٔ مجاور = اولین y که پاکت‌ها از هم فاصله می‌گیرند. */
    val splitY: List<Double> = buildList {
        for (i in 0 until roots.size - 1) {
            val a = roots[i]; val b = roots[i + 1]
            var ys = furcY
            val lim = min(a.apexY, b.apexY)
            var y = furcY
            while (y <= lim) {
                ys = y
                if (rawR(a, y) <= rawL(b, y)) break
                y += 0.04
            }
            add(max(furcY, min(ys, lim - 0.4)))
        }
    }

    val fornix: List<Fornix> = splitY.mapIndexed { i, y ->
        Fornix((rawR(roots[i], y) + rawL(roots[i + 1], y)) / 2, y)
    }

    /** دیوارهٔ کنده‌کاری‌شده هرگز به کانال نزدیک‌تر از CLR نمی‌شود. */
    private fun canalLim(r: RootGeom, y: Double, dir: Int): Double? {
        var lim: Double? = null
        r.canals.forEach { s ->
            if (y > s.wl + 0.6) return@forEach
            val cx = canalX(r, s, y, furcY, r.cH)
            val v = cx - dir * FURC_CLR
            lim = if (lim == null) v else if (dir > 0) min(lim!!, v) else max(lim!!, v)
        }
        return lim
    }

    /**
     * دیوارهٔ داخلی از فورنیکس با پروفیل جذری باز می‌شود: مشتق افقی در نوک
     * بی‌نهایت است، پس دو دیواره در فورنیکس بر هم مماس‌اند و به‌جای گوشهٔ تیز V
     * یک سقف کاملاً گرد ساخته می‌شود — یعنی شکل طبیعی فورکا.
     */
    private fun notch(r: RootGeom, x: Double, f: Fornix?, y: Double, dir: Int): Double {
        if (f == null) return x
        val u = min(1.0, max(0.0, (y - f.y) / FURC_ND))
        if (u >= 1) return x
        val far = dir * (x - f.x)
        val base = far * sqrt(u)
        val open = max(base, FURC_MINSLOPE * (y - f.y))
        val o = (1 - u) * open + u * far
        var w = f.x + dir * o
        val lim = canalLim(r, y, dir)
        if (lim != null) w = if (dir > 0) min(w, lim) else max(w, lim)
        return w
    }

    fun edgeL(r: RootGeom, y: Double, outer: Boolean, f: Fornix?): Double {
        val v = rawL(r, y)
        return if (outer) {
            if (y < furcY) (-trunkHalf) + (v - (-trunkHalf)) * uTrunk(y) else v
        } else notch(r, v, f, y, +1)
    }

    fun edgeR(r: RootGeom, y: Double, outer: Boolean, f: Fornix?): Double {
        val v = rawR(r, y)
        return if (outer) {
            if (y < furcY) (trunkHalf) + (v - (trunkHalf)) * uTrunk(y) else v
        } else notch(r, v, f, y, -1)
    }

    /** نمونه‌برداری تطبیقی: نزدیک سقف فورکا گام ریز می‌شود تا زاویه‌دار نشود. */
    private fun stepAt(y: Double, focus: Double?): Double {
        if (focus == null) return OUTLINE_STEP
        val d = abs(y - focus)
        return if (d < FURC_ND) OUTLINE_STEP * (0.18 + 0.82 * min(1.0, d / FURC_ND)) else OUTLINE_STEP
    }

    private fun samples(y0: Double, y1: Double, focus: Double?, f: (Double) -> MmPoint): List<MmPoint> {
        val out = mutableListOf<MmPoint>()
        val dir = if ((y1 - y0).sign == 0.0) 1.0 else (y1 - y0).sign
        val span = abs(y1 - y0)
        var travelled = 0.0
        var y = y0
        var guard = 0
        while (travelled < span && ++guard < 20000) {
            out.add(f(y))
            val st = min(stepAt(y, focus), span - travelled)
            if (st <= 1e-9) break
            travelled += st
            y = y0 + dir * travelled
        }
        out.add(f(y1))
        return out
    }

    /**
     * دور زدن کامل سیلوئت: از یقه سمت چپ، پایین ریشه‌ها، نوک گرد آپیکال و
     * بازگشت. هیچ نقطه‌ای دستکاری نمی‌شود، پس جایی برای شکستگی خط نمی‌ماند.
     */
    fun outline(): List<MmPoint> {
        val path = mutableListOf<MmPoint>()
        roots.forEachIndexed { i, r ->
            val top = if (i == 0) g.crownH else splitY[i - 1]
            val bot = if (i == roots.size - 1) g.crownH else splitY[i]
            val outerL = i == 0
            val outerR = i == roots.size - 1
            val fl = if (i > 0) fornix[i - 1] else null
            val fr = if (i < roots.size - 1) fornix[i] else null

            path += samples(top, r.apexY, fl?.y) { y -> MmPoint(edgeL(r, y, outerL, fl), y) }

            // نوک گرد آپیکال — گام زاویه‌ای بر حسب شعاع واقعی
            val ax = rootCx(r, r.apexY, furcY)
            val aw = rootHw(r, r.apexY, furcY)
            val na = max(6.0, Math.ceil((Math.PI * aw) / OUTLINE_STEP)).toInt()
            for (k in 0..na) {
                val a = Math.PI - (Math.PI * k / na)
                path.add(MmPoint(ax + aw * Math.cos(a), r.apexY + aw * 0.85 * sin(a)))
            }

            path += samples(r.apexY, bot, fr?.y) { y -> MmPoint(edgeR(r, y, outerR, fr), y) }
        }
        return path
    }
}
