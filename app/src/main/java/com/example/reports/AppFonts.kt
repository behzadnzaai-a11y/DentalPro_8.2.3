package com.example.reports

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import java.io.File

/**
 * AppFonts — تنها منبعِ تایپوگرافی در کلِ برنامه (UI، PNG، PDF، کارتِ بانکی).
 * هیچ‌جا نباید مستقیم Typeface.DEFAULT/MONOSPACE ست شود.
 *
 * سیستمِ سه‌لایه:
 *   • Body      = Vazirmatn Regular (متنِ بدنه/لیبل/جدول؛ ارقامِ tabular → ترازِ ستونِ پول)
 *   • Emphasized= Vazirmatn Bold    (تأکید/جمعِ کل)
 *   • Title     = Estedad Bold      (عنوان/هدر/نامِ دارندهٔ کارت)
 *   • Number    = JetBrains Mono    (فقط رشته‌های کاملاً لاتین؛ [isMonoSafeText] هر
 *                                    متنِ فارسی/عربی را به فونتِ بدنه برمی‌گرداند)
 *
 * افزودن/تعویضِ فونت فقط با گذاشتنِ فایل در `assets/fonts/` (انتخابِ خانواده بر
 * اساسِ نامِ فایل: [PREFERRED_FAMILY_KEYS]/[DISPLAY_FAMILY_KEYS]/[NUMBER_FAMILY_KEYS]).
 * نبودِ هر خانواده به لایهٔ بالاتر fallback می‌کند؛ پوشهٔ خالی ⇒ فونتِ سیستم و
 * [hasCustomFont]=false. مجوزهای OFL در `LICENSES/`.
 */
object AppFonts {

    /** پوشهٔ فونت‌ها در assets. */
    const val ASSET_DIR = "fonts"

    /**
     * پسوندهای مجاز. `txt` عمداً پذیرفته می‌شود تا فایل‌های TTF که با فرمت
     * متنی تحویل داده شده‌اند، بدون دخالت دستی کار کنند — با این حال محتوای
     * فایل هم بررسی می‌شود (به [looksLikeFont] نگاه کنید) تا مثلاً `OFL.txt`
     * که فقط کنار فونت‌ها گذاشته می‌شود، به‌عنوان قلم اشتباه گرفته نشود.
     */
    private val ALLOWED_EXTENSIONS = listOf("ttf", "otf", "txt")

    /** امضاهای معتبر ابتدای یک فایل فونت sfnt/TTF/OTF. */
    private val FONT_MAGIC_PREFIXES = listOf(
        byteArrayOf(0x00, 0x01, 0x00, 0x00),
        "true".toByteArray(),
        "OTTO".toByteArray(),
        "ttcf".toByteArray()
    )

    /**
     * کلیدهای [Role.Body] و [Role.Emphasized]. اولین خانواده‌ای که در
     * `assets/fonts` پیدا شود برنده است. برای عوض‌کردن فونت اصلی متن، همین
     * لیست را ویرایش کنید (یا فایل‌های خانوادهٔ جدید را بگذارید).
     */
    val PREFERRED_FAMILY_KEYS: List<String> = listOf(
        "vazirmatn",
        "samim",
        "sahel",
        "shabnam",
        "estedad",
        "iran_sans",
        "dana",
        "parastoo"
    )

    /**
     * کلیدهای [Role.Title] (عنوان‌ها و هدرها). اگر هیچ‌کدام نصب نباشد، تیترها
     * با بولدِ همان خانوادهٔ بدنه رندر می‌شوند.
     */
    val DISPLAY_FAMILY_KEYS: List<String> = listOf(
        "estedad",
        "morabba",
        "gulzar_display"
    )

    /**
     * کلیدهای [Role.Number] — فونت هم‌عرض برای ارقام/لاتین. اگر نصب نباشد،
     * ارقام با همان بدنه رندر می‌شوند (Vazirmatn خودش tabular است).
     */
    val NUMBER_FAMILY_KEYS: List<String> = listOf(
        "jetbrainsmono",
        "roboto-mono",
        "robotomono",
        "ibmplexmono",
        "spacemono",
        "mono",
        "tabular",
        "digits"
    )

    /** کلیدهایی که یک فایل را «مونواسپیس/ارقام» می‌شناسند. */
    private val MONO_KEYWORDS = listOf("mono", "tabular", "digits", "numbers", "farsidata")

    /** کلیدهای وزن‌دار. */
    private val BOLD_KEYWORDS = listOf("bold", "heavy", "black", "extrabold", "semibold", "medium")
    private val LIGHT_KEYWORDS = listOf("light", "thin", "hairline")
    private val ITALIC_KEYWORDS = listOf("italic", "oblique")

    /** چهره‌های مجاز که هر ژنراتور می‌تواند بخواهد. */
    enum class Role {
        /** متن بدنه، لیبل‌ها، سلول جدول، پاراگراف‌ها. */
        Body,

        /** تأکیدِ داخل متن (سنگین‌تر، ولی همان خانوادهٔ بدنه تا وسط پاراگراف خانواده عوض نشود). */
        Emphasized,

        /** عنوان سند/بخش و هدر جدول — خانوادهٔ نمایشی (Title/Display). */
        Title,

        /** عدد، مبلغ، شماره کارت، IBAN، تاریخ لاتین — مونواسپیس اگر رشته لاتین باشد. */
        Number
    }

    /** نتیجهٔ یک بار اسکن و بارگذاری فونت‌ها. */
    class FontSet internal constructor(
        /** چهرهٔ معمولیِ خانوادهٔ بدنه. */
        val regular: Typeface?,
        /** چهرهٔ بولدِ خانوادهٔ بدنه. */
        val bold: Typeface?,
        /** خانوادهٔ تیترها (ممکن است null باشد ⇒ به [bold] می‌افتد). */
        val displayRegular: Typeface?,
        val displayBold: Typeface?,
        /** خانوادهٔ ارقام/لاتین (ممکن است null باشد ⇒ به [regular] می‌افتد). */
        val mono: Typeface?,
        val monoBold: Typeface?,
        val loadedFileNames: List<String>,
        val bodyFamily: String? = null,
        val displayFamily: String? = null,
        val numberFamily: String? = null
    ) {
        val hasCustomFont: Boolean
            get() = regular != null || bold != null || displayRegular != null ||
                displayBold != null || mono != null || monoBold != null

        /** خانوادهٔ نمایشیِ جداگانه نصب شده؟ (اگر false باشد تیتر با بولدِ بدنه رندر می‌شود) */
        val hasDisplayFace: Boolean get() = displayRegular != null || displayBold != null

        /** فونت هم‌عرض ارقام نصب شده؟ */
        val hasMonoFace: Boolean get() = mono != null || monoBold != null

        /** برای دیباگ/تست: چه چیزی واقعاً لود شده است. */
        fun describe(): String = if (!hasCustomFont) {
            "system-default (no embedded font found in assets/${AppFonts.ASSET_DIR})"
        } else {
            "body=${bodyFamily ?: "-"} display=${displayFamily ?: bodyFamily ?: "-"} " +
                "number=${numberFamily ?: bodyFamily ?: "-"} files=${loadedFileNames.joinToString()}"
        }

        /** آیا این رشته می‌تواند با فونت مونواسپیس رندر شود؟ (بی‌متنِ عربی/فارسی) */
        private fun monoApplies(text: String?): Boolean =
            hasMonoFace && (text == null || AppFonts.isMonoSafeText(text))

        private fun faceFor(role: Role, text: String?): Typeface? = when (role) {
            Role.Body -> regular ?: bold ?: displayRegular
            Role.Emphasized -> bold ?: regular ?: displayBold
            Role.Title -> displayBold ?: displayRegular ?: bold ?: regular
            Role.Number -> if (monoApplies(text)) (monoBold ?: mono) else (regular ?: bold ?: displayRegular)
        }

        /**
         * تیترهای بولدِ واقعی داریم؟ اگر نه، fakeBold لازم است:
         *  • اگر خانوادهٔ نمایشی جدا نصب است، ملاک **خودِ همان خانواده** است
         *    (نباید بولدِ فونت متن، جلوبُودیِ تیترهای لاترِ مختلف را بسازد)؛
         *  • اگر نصب نیست، بولدِ خانوادهٔ بدنه کافی است.
         */
        fun needsFakeBold(role: Role): Boolean = when (role) {
            Role.Title -> if (hasDisplayFace) displayBold == null else bold == null
            Role.Emphasized -> bold == null
            else -> false
        }

        fun typefaceFor(role: Role, text: String? = null): Typeface =
            faceFor(role, text) ?: when (role) {
                Role.Number -> Typeface.MONOSPACE
                else -> Typeface.DEFAULT
            }

        /**
         * فقط کاراکترهایی حذف می‌شوند که **هیچ‌کدام** از چهره‌های نصب‌شده گلیف
         * ندارند (ایموجی و نمادهای خاص). بدون این کار، نشانک‌هایی مثل
         * «🚨 💳 ⚠️» داخل متن کارت‌ها به «توفو» تبدیل می‌شدند.
         *
         * نکتهٔ مهم: فاصله، خط‌شکن، کنترل‌های دوجهتی و کل بازهٔ عربی/فارسی
         * همیشه نگه داشته می‌شوند — حذف‌کردن آن‌ها چیدمان فارسی را خراب می‌کند.
         */
        fun supports(codePoint: Int): Boolean {
            if (codePoint <= 0x20 || Character.isWhitespace(codePoint)) return true
            // ZWJ / ZWNJ / RLM / LRM / ALM — حیاتی برای متن فارسی
            if (codePoint in 0x200B..0x200F || codePoint == 0x061C) return true
            // عربی/فارسی و مکمل‌ها و فرم‌های اتصال
            if (codePoint in 0x0600..0x06FF || codePoint in 0x0750..0x077F ||
                codePoint in 0xFB50..0xFDFF || codePoint in 0xFE70..0xFEFF ||
                codePoint in 0xFB00..0xFB4F
            ) return true
            val faces = listOfNotNull(regular, bold, displayRegular, displayBold, mono, monoBold)
            if (faces.isEmpty()) return true
            val sample = try {
                String(Character.toChars(codePoint))
            } catch (t: Throwable) {
                return true
            }
            val paint = android.graphics.Paint()
            return try {
                faces.any { face ->
                    paint.typeface = face
                    paint.hasGlyph(sample)
                }
            } catch (t: Throwable) {
                true
            }
        }
    }

    @Volatile
    private var cached: Pair<String, FontSet>? = null

    /**
     * فونت‌ها یک‌بار برای هر پروسه اسکن و کش می‌شوند (ارزان، thread-safe).
     */
    fun fontSet(context: Context): FontSet {
        val app = context.applicationContext
        val key = app.packageName + "|" + ASSET_DIR
        cached?.let { hit -> if (hit.first == key) return hit.second }
        val result = synchronized(this) {
            cached?.let { hit ->
                if (hit.first == key) return@synchronized hit.second
            }
            val loaded = load(app)
            cached = key to loaded
            loaded
        }
        return result
    }

    /** در صورت افزودن/تعویض فایل فونت، کش را بی‌اعتبار کنید. */
    fun invalidateCache() {
        cached = null
    }

    // ------------------------------------------------------------------------
    //  بارگذاری و خوشه‌بندی فایل‌ها
    // ------------------------------------------------------------------------

    private fun load(app: Context): FontSet {
        val available: List<String> = try {
            app.assets.list(ASSET_DIR)?.toList()?.sorted() ?: emptyList()
        } catch (t: Throwable) {
            emptyList()
        }
        val fontFiles = available.filter { name ->
            val ext = name.substringAfterLast('.', "").lowercase()
            ALLOWED_EXTENSIONS.contains(ext) && looksLikeFont(app, name)
        }
        if (fontFiles.isEmpty()) return FontSet(null, null, null, null, null, null, emptyList())

        val groups = fontFiles
            .map { it to familyKeyOf(it) }
            .groupBy({ it.second }, { it.first })

        fun groupFor(keys: List<String>): Pair<String?, List<String>?> {
            for (pref in keys) {
                val wanted = pref.lowercase()
                val hit = groups.entries.firstOrNull { it.key.contains(wanted) }
                if (hit != null) return hit.key to hit.value
            }
            return null to null
        }

        val (numberKey, numberFiles0) = groupFor(NUMBER_FAMILY_KEYS)
        val (bodyKey0, bodyFiles0) = groupFor(PREFERRED_FAMILY_KEYS)
        val (displayKey, displayFiles0) = groupFor(DISPLAY_FAMILY_KEYS)

        // اگر خانوادهٔ بدنه مشخص نبود: بزرگ‌ترین گروه (یا همان نمایشی/ارقام) برداشته می‌شود
        val bodyKey: String? = bodyKey0 ?: displayKey ?: numberKey
        val bodyFilesRaw: List<String>? = bodyFiles0
            ?: bodyKey?.let { groups[it] }
            ?: groups.maxByOrNull { it.value.size }?.value
        // فونت ارقام نباید به‌عنوان قلم متن بدنه انتخاب شود؛ اگر کل خانواده مونو بود،
        // همان‌ها می‌ماند (تا رندر بی‌قلم نماند).
        val bodyNonMono = bodyFilesRaw?.filterNot { isMonoLike(it) }
        val bodyFiles: List<String>? =
            if (bodyNonMono != null && bodyNonMono.isNotEmpty()) bodyNonMono else bodyFilesRaw

        val loadedNames = mutableListOf<String>()
        val faceCache = HashMap<String, Typeface?>()
        fun face(name: String?): Typeface? {
            if (name == null) return null
            if (faceCache.containsKey(name)) return faceCache[name]
            val tf = resolveTypeface(app, name)
            faceCache[name] = tf
            if (tf != null) loadedNames += name
            return tf
        }
        /**
         * دو چهرهٔ یک خانواده: (معمولی, بولد). اگر خانواده فقط یک فایل داشته باشد،
         * همان فایل هر دو نقش را می‌گیرد (یک نمونه، دو ارجاع) تا انتخاب چهره هیچ‌وقت
         * بی‌دلیل خالی نماند.
         */
        fun facesOf(files: List<String>?): Pair<Typeface?, Typeface?> =
            face(pickRegular(files)) to face(pickBold(files))

        val (bodyRegular, bodyBold) = facesOf(bodyFiles)
        val displayIsSeparate = displayKey != null && displayKey != bodyKey
        val (displayRegular, displayBold) = if (displayIsSeparate) facesOf(displayFiles0) else null to null
        val (monoRegular, monoBold) = facesOf(numberFiles0)

        return FontSet(
            regular = bodyRegular,
            bold = bodyBold ?: bodyRegular?.let { Typeface.create(it, Typeface.BOLD) },
            displayRegular = displayRegular,
            displayBold = displayBold,
            mono = monoRegular,
            monoBold = monoBold,
            loadedFileNames = loadedNames,
            bodyFamily = bodyKey,
            displayFamily = if (displayIsSeparate) displayKey else null,
            numberFamily = numberKey
        )
    }

    private fun isMonoLike(fileName: String): Boolean =
        MONO_KEYWORDS.any { fileName.lowercase().contains(it) }

    /** چهرهٔ معمولی یک خانواده: نه بولد، نه نازک، نه ایتالیک، نه مونو. */
    private fun pickRegular(files: List<String>?): String? =
        files?.firstOrNull { f ->
            val n = f.lowercase()
            !isItalic(n) && BOLD_KEYWORDS.none { n.contains(it) } && LIGHT_KEYWORDS.none { n.contains(it) }
        } ?: files?.firstOrNull { f -> !isItalic(f.lowercase()) && LIGHT_KEYWORDS.none { f.lowercase().contains(it) } }

    /** چهرهٔ بولد یک خانواده (اگر نباشد null تا رجیستری fakeBold یا فونت متن را انتخاب کند). */
    private fun pickBold(files: List<String>?): String? =
        files?.firstOrNull { f ->
            val n = f.lowercase()
            !isItalic(n) && BOLD_KEYWORDS.any { n.contains(it) } && LIGHT_KEYWORDS.none { n.contains(it) }
        }

    private fun isItalic(lowerName: String): Boolean = ITALIC_KEYWORDS.any { lowerName.contains(it) }

    /** «vazirmatn» از «Vazirmatn-Bold.ttf» — اولین جزء نام، بدون پسوند و وزن. */
    private fun familyKeyOf(fileName: String): String {
        val stem = fileName.substringBeforeLast('.').lowercase()
        val trimmed = stem.split('-', '_', ' ').firstOrNull { it.isNotBlank() } ?: stem
        return trimmed.ifBlank { stem }
    }

    /**
     * آیا واقعاً یک فایل فونت است؟ برای پسوندهای `.txt` (فونتِ تغییرنام‌یافته)
     * این بررسی حیاتی است؛ در غیر این صورت هر فایل متنی کنار فونت‌ها، رجیستری
     * را خراب می‌کرد. فایل‌های `.ttf/.otf` هم بی‌ضرر از همین مسیر عبور می‌کنند.
     */
    private fun looksLikeFont(app: Context, assetFileName: String): Boolean = try {
        val head = ByteArray(4)
        var read = 0
        app.assets.open("$ASSET_DIR/$assetFileName").use { input ->
            while (read < 4) {
                val n = input.read(head, read, 4 - read)
                if (n <= 0) break
                read += n
            }
        }
        read >= 4 && FONT_MAGIC_PREFIXES.any { it.contentEquals(head) }
    } catch (t: Throwable) {
        false
    }

    /**
     * استخراج فایل فونت از assets به کش و ساخت Typeface.
     * فایل‌های `.txt` دقیقاً مثل `.ttf` رفتار می‌شوند (کپی با پسوند ttf).
     */
    private fun resolveTypeface(app: Context, assetFileName: String): Typeface? = try {
        val fontDir = File(app.cacheDir, "appfonts").apply { mkdirs() }
        val target = File(fontDir, assetFileName.substringBeforeLast('.') + ".ttf")
        if (!target.exists() || target.length() <= 0L) {
            app.assets.open("$ASSET_DIR/$assetFileName").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        Typeface.createFromFile(target)
    } catch (t: Throwable) {
        null
    }

    // ------------------------------------------------------------------------
    //  قاعدهٔ «مونواسپیس فقط برای لاتین» — تنها پیاده‌سازی این تشخیص
    // ------------------------------------------------------------------------

    /**
     * `true` یعنی این رشته گلیفِ عربی/فارسی ندارد و می‌تواند با فونت
     * مونواسپیس (که فقط لاتین دارد) رندر شود. رشتهٔ `null` هم `true` است، چون
     * یعنی فراخوان می‌داند چه می‌فرستد و مسئول انتخاب قلم است.
     *
     * مثال: `"IR82 5100 0123"` ⇒ true ، `"1,200,000 تومان"` ⇒ false
     */
    fun isMonoSafeText(text: String?): Boolean {
        if (text == null) return true
        if (text.isEmpty()) return false
        text.forEach { ch ->
            val cp = ch.code
            if (cp in 0x0590..0x08FF || cp in 0xFB50..0xFEFF || cp in 0x0750..0x077F) return false
            if (cp > 0xFFFF) return false // ایموجی و مکمل‌ها با مونواسپیس معنایی ندارند
        }
        return true
    }

    // ------------------------------------------------------------------------
    //  کارخانهٔ ساخت Paint — تنها راه ساخت قلم برای همهٔ رندرها
    // ------------------------------------------------------------------------

    /**
     * یک [TextPaint] استاندارد برای نقش خواسته‌شده می‌سازد.
     * @param sizePx بر حسب پیکسلِ بومِ خروجی (نه اسپِ دستگاه) تا همهٔ کارت‌ها با
     *   عرض‌های مختلف، چگالی بصری یکسان داشته باشند.
     * @param text اگر داده می‌شود، انتخاب قلم مونواسپیس بر اساس محتوای همان
     *   رشته انجام می‌شود ([isMonoSafeText]) تا متن فارسی هرگز با قلم لاتین
     *   رندر نشود.
     */
    fun textPaint(
        context: Context,
        role: Role,
        sizePx: Float,
        colorInt: Int,
        align: Paint.Align = Paint.Align.LEFT,
        letterSpacingEm: Float = 0f,
        text: String? = null
    ): TextPaint {
        val set = fontSet(context)
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            isAntiAlias = true
            isDither = true
            color = colorInt
            textSize = sizePx.coerceAtLeast(1f)
            textAlign = align
            typeface = set.typefaceFor(role, text)
            isFakeBoldText = set.needsFakeBold(role)
            this.letterSpacing = letterSpacingEm
        }
    }

    /** اعمال قلم استاندارد روی یک TextPaint که از قبل ساخته شده. */
    fun applyTypeface(context: Context, paint: TextPaint, role: Role, text: String? = null) {
        val set = fontSet(context)
        paint.typeface = set.typefaceFor(role, text)
        paint.isFakeBoldText = set.needsFakeBold(role)
    }

    /** حذف کاراکترهایی که هیچ‌یک از فونت‌های نصب‌شده گلیف ندارند (ایموجی و نمادها). */
    fun sanitize(context: Context, text: String?): String {
        if (text.isNullOrEmpty()) return text.orEmpty()
        val set = fontSet(context)
        if (!set.hasCustomFont) return text
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val count = Character.charCount(cp)
            if (set.supports(cp)) sb.appendCodePoint(cp)
            i += count
        }
        // پاک‌سازی فاصله‌های دوبل و خط‌های خالیِ به‌جا‌مانده از ایموجی‌ها
        return sb.toString()
            .replace(Regex("[ \\t]{2,}"), " ")
            .replace(Regex("[ \\t]+(\n)"), "$1")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    // ------------------------------------------------------------------------
    //  پل به Compose — تا UI و خروجی‌ها یک فونت داشته باشند
    // ------------------------------------------------------------------------

    /** [FontFamily] متن بدنه (Vazirmatn Regular)؛ `null` یعنی فونت سیستم. */
    fun composeFontFamily(context: Context): FontFamily? {
        val face = fontSet(context).regular ?: return null
        return FontFamily(face)
    }

    /** [FontFamily] تیترها — خانوادهٔ نمایشی، و در نبودِ آن بولدِ بدنه. */
    fun composeBoldFontFamily(context: Context): FontFamily? {
        val set = fontSet(context)
        val face = set.displayBold ?: set.displayRegular ?: set.bold ?: set.regular ?: return null
        return FontFamily(face)
    }

    /** [FontFamily] ارقام و لاتین — مونواسپیس، و در نبودِ آن فونت بدنه. */
    fun composeNumberFontFamily(context: Context): FontFamily? {
        val set = fontSet(context)
        val face = set.monoBold ?: set.mono ?: set.regular ?: return null
        return FontFamily(face)
    }

    /** برای تست/دیباگ: آیا فونت سفارشی واقعاً لود شده است؟ */
    fun hasCustomFont(context: Context): Boolean = fontSet(context).hasCustomFont

    /** دیباگ: چه چیزی لود شده (برای لاگ/اسکرین‌شات پشتیبانی). */
    fun describe(context: Context): String = fontSet(context).describe()

    /**
     * فونت متن در Compose. اگر فونت پروژه نصب نباشد، `FontFamily.Default`
     * (فونت سیستم) برگردانده می‌شود — پس هیچ‌وقت UI بی‌قلم نمی‌ماند.
     */
    @Composable
    fun textFamily(): FontFamily {
        val ctx = LocalContext.current
        return remember(ctx) { composeFontFamily(ctx) } ?: FontFamily.Default
    }

    /** فونت تیترها در Compose (خانوادهٔ نمایشی؛ همان چیزی که در خروجی‌ها تیتر است). */
    @Composable
    fun titleFamily(): FontFamily {
        val ctx = LocalContext.current
        return remember(ctx) { composeBoldFontFamily(ctx) } ?: FontFamily.Default
    }

    /**
     * فونت ارقام (مونواسپیس پروژه) — برای شماره کارت، IBAN، مبالغ.
     * اگر چهرهٔ مونواسپیس نصب نباشد، همان فونت متنی پروژه برگردانده می‌شود تا
     * UI و خروجیِ تصویر/PDF یک قلم داشته باشند؛ Monospace سیستم فقط آخرین
     * fallback است.
     */
    @Composable
    fun numberFamily(): FontFamily {
        val ctx = LocalContext.current
        val number = remember(ctx) { composeNumberFontFamily(ctx) }
        return number ?: FontFamily.Monospace
    }
}

/** مقدار استاندارد فاصلهٔ خطی برای فارسی (دم‌ها و کشیده‌ها بریده نشوند). */
const val PERSIAN_LINE_SPACING_MULT = 1.18f
const val PERSIAN_LINE_SPACING_ADD_PX = 4f
