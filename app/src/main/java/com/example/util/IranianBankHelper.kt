package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import java.io.File
import java.io.FileOutputStream

data class IranianBankInfo(
    val bin: String,
    val nameFa: String,
    val nameEn: String,
    val ibanCode: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color = Color(0xFFFFD700),
    val textColor: Color = Color.White,
    val shortCode: String = "",
    val logoResName: String = ""
)

object IranianBankHelper {

    private val BANK_DATABASE: List<IranianBankInfo> = listOf(
        IranianBankInfo("603799", "بانک ملی ایران", "Bank Melli Iran", "017", Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFFFFD700), Color.White, "BMI", "ic_bank_melli"),
        IranianBankInfo("589210", "بانک سپه", "Bank Sepah", "015", Color(0xFFB78103), Color(0xFF795548), Color(0xFFFFE082), Color.White, "SEP", "ic_bank_sepah"),
        IranianBankInfo("627648", "بانک توسعه صادرات ایران", "Export Development Bank", "020", Color(0xFF006064), Color(0xFF00838F), Color(0xFF80DEEA), Color.White, "EDBI", "ic_bank_tosee_taavon"),
        IranianBankInfo("207177", "بانک توسعه صادرات ایران", "Export Development Bank", "020", Color(0xFF006064), Color(0xFF00838F), Color(0xFF80DEEA), Color.White, "EDBI", "ic_bank_tosee_taavon"),
        IranianBankInfo("627961", "بانک صنعت و معدن", "Sanat va Madan Bank", "011", Color(0xFF37474F), Color(0xFF546E7A), Color(0xFFFFD54F), Color.White, "BIM", "ic_bank_sanat_madan"),
        IranianBankInfo("603770", "بانک کشاورزی", "Keshavarzi Bank", "016", Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFFFFD700), Color.White, "BKI", "ic_bank_keshavarzi"),
        IranianBankInfo("639217", "بانک کشاورزی", "Keshavarzi Bank", "016", Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFFFFD700), Color.White, "BKI", "ic_bank_keshavarzi"),
        IranianBankInfo("628023", "بانک مسکن", "Maskan Bank", "014", Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFFE082), Color.White, "MASKAN", "ic_bank_maskan"),
        IranianBankInfo("627760", "پست بانک ایران", "Post Bank Iran", "021", Color(0xFF004D40), Color(0xFF00695C), Color(0xFF80CBC4), Color.White, "POST", "ic_bank_postbank"),
        IranianBankInfo("502908", "بانک توسعه تعاون", "Tose'e Ta'avon Bank", "022", Color(0xFF0D47A1), Color(0xFF00838F), Color(0xFF81D4FA), Color.White, "TT", "ic_bank_tosee_taavon"),
        IranianBankInfo("627412", "بانک اقتصاد نوین", "EN Bank", "055", Color(0xFF4A148C), Color(0xFF7B1FA2), Color(0xFFE1BEE7), Color.White, "EN", "ic_bank_eghtesad_novin"),
        IranianBankInfo("622106", "بانک پارسیان", "Parsian Bank", "054", Color(0xFF880E4F), Color(0xFFAD1457), Color(0xFFFFD700), Color.White, "PARSIAN", "ic_bank_parsian"),
        IranianBankInfo("639194", "بانک پارسیان", "Parsian Bank", "054", Color(0xFF880E4F), Color(0xFFAD1457), Color(0xFFFFD700), Color.White, "PARSIAN", "ic_bank_parsian"),
        IranianBankInfo("627884", "بانک پارسیان", "Parsian Bank", "054", Color(0xFF880E4F), Color(0xFFAD1457), Color(0xFFFFD700), Color.White, "PARSIAN", "ic_bank_parsian"),
        IranianBankInfo("502229", "بانک پاسارگاد", "Pasargad Bank", "057", Color(0xFF212121), Color(0xFF424242), Color(0xFFFFD700), Color(0xFFFFF8E1), "BPI", "ic_bank_pasargad"),
        IranianBankInfo("639347", "بانک پاسارگاد", "Pasargad Bank", "057", Color(0xFF212121), Color(0xFF424242), Color(0xFFFFD700), Color(0xFFFFF8E1), "BPI", "ic_bank_pasargad"),
        IranianBankInfo("627488", "بانک کارآفرین", "Karafarin Bank", "053", Color(0xFF00695C), Color(0xFF00897B), Color(0xFFFFD54F), Color.White, "KARAFARIN", "ic_bank_karafarin"),
        // تم بلو بانک سامان بنفش است (طبق درخواست، جایگزین آبی قبلی شد)
        IranianBankInfo("621986", "بلو بانک سامان", "Blue Bank Saman", "056", Color(0xFF4A148C), Color(0xFF7E24B8), Color(0xFFCE93D8), Color.White, "BLUE", "ic_bank_saman"),
        IranianBankInfo("639346", "بانک سینا", "Sina Bank", "059", Color(0xFF1A237E), Color(0xFF283593), Color(0xFFFFCA28), Color.White, "SINA", "ic_bank_sina"),
        IranianBankInfo("639607", "بانک سرمایه", "Sarmayeh Bank", "058", Color(0xFF3E2723), Color(0xFF5D4037), Color(0xFFFFD700), Color.White, "SARMAYEH", "ic_bank_sarmayeh"),
        IranianBankInfo("502806", "بانک شهر", "Shahr Bank", "061", Color(0xFFC62828), Color(0xFFD32F2F), Color(0xFFFFCDD2), Color.White, "SHAHR", "ic_bank_shahr"),
        IranianBankInfo("504706", "بانک شهر", "Shahr Bank", "061", Color(0xFFC62828), Color(0xFFD32F2F), Color(0xFFFFCDD2), Color.White, "SHAHR", "ic_bank_shahr"),
        IranianBankInfo("502938", "بانک دی", "Dey Bank", "066", Color(0xFF006064), Color(0xFF00838F), Color(0xFFFFD54F), Color.White, "DEY", "ic_bank_dey"),
        IranianBankInfo("603769", "بانک صادرات ایران", "Bank Saderat Iran", "019", Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFFFFD700), Color.White, "BSI", "ic_bank_saderat"),
        IranianBankInfo("610433", "بانک ملت", "Bank Mellat", "012", Color(0xFFC2185B), Color(0xFF880E4F), Color(0xFFFF80AB), Color.White, "MELLAT", "ic_bank_mellat"),
        IranianBankInfo("991975", "بانک ملت", "Bank Mellat", "012", Color(0xFFC2185B), Color(0xFF880E4F), Color(0xFFFF80AB), Color.White, "MELLAT", "ic_bank_mellat"),
        IranianBankInfo("627353", "بانک تجارت", "Tejarat Bank", "018", Color(0xFF1565C0), Color(0xFF0D47A1), Color(0xFFFF6F00), Color.White, "TEJARAT", "ic_bank_tejarat"),
        IranianBankInfo("585983", "بانک تجارت", "Tejarat Bank", "018", Color(0xFF1565C0), Color(0xFF0D47A1), Color(0xFFFF6F00), Color.White, "TEJARAT", "ic_bank_tejarat"),
        IranianBankInfo("589463", "بانک رفاه کارگران", "Refah Bank", "013", Color(0xFF1A237E), Color(0xFF303F9F), Color(0xFFFFD700), Color.White, "REFAH", "ic_bank_refah"),
        IranianBankInfo("627381", "بانک انصار (سپه)", "Ansar Bank", "063", Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFFFFD700), Color.White, "ANSAR", "ic_bank_ansar"),
        IranianBankInfo("505785", "بانک ایران زمین", "Iran Zamin Bank", "069", Color(0xFF4A148C), Color(0xFF6A1B9A), Color(0xFFFFD54F), Color.White, "IZ", "ic_bank_iran_zamin"),
        IranianBankInfo("636214", "بانک آینده", "Ayandeh Bank", "062", Color(0xFF4E342E), Color(0xFF6D4C41), Color(0xFFFFD700), Color.White, "AYANDEH", "ic_bank_ayandeh"),
        IranianBankInfo("505416", "بانک گردشگری", "Gardeshgari Bank", "064", Color(0xFFB71C1C), Color(0xFFD32F2F), Color(0xFFFFD54F), Color.White, "TOURISM", "ic_bank_gardeshgari"),
        IranianBankInfo("606373", "بانک قرض‌الحسنه مهر ایران", "Qarz Al-Hasaneh Mehr Iran", "060", Color(0xFF004D40), Color(0xFF00695C), Color(0xFF80CBC4), Color.White, "MEHR", "ic_bank_mehr_iran"),
        IranianBankInfo("504172", "بانک قرض‌الحسنه رسالت", "Resalat Bank", "070", Color(0xFF004D40), Color(0xFF00796B), Color(0xFFFFD700), Color.White, "RESALAT", "ic_bank_resalat"),
        IranianBankInfo("505809", "بانک خاورمیانه", "Middle East Bank", "078", Color(0xFF37474F), Color(0xFF455A64), Color(0xFFFFD54F), Color.White, "MEB", "ic_bank_khavar_mianeh"),
        IranianBankInfo("606256", "موسسه اعتباری ملل (عسکریه)", "Melal Credit Institution", "075", Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFFFFE082), Color.White, "MELAL", "ic_bank_tosee_taavon"),
        IranianBankInfo("628157", "موسسه اعتباری توسعه", "Tose'e Credit Institution", "051", Color(0xFF006064), Color(0xFF00838F), Color(0xFF80DEEA), Color.White, "TOWSEH", "ic_bank_tosee_taavon"),
        IranianBankInfo("505801", "موسسه اعتباری کوثر", "Kowsar Credit Institution", "073", Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFFFFD700), Color.White, "KOWSAR", "ic_bank_sepah"),
        IranianBankInfo("639599", "بانک قوامین", "Ghavamin Bank", "052", Color(0xFF1A237E), Color(0xFF283593), Color(0xFFFFD700), Color.White, "GHAVAMIN", "ic_bank_ghavamin"),
        IranianBankInfo("636949", "بانک حکمت ایرانیان", "Hekmat Bank", "065", Color(0xFF4527A0), Color(0xFF512DA8), Color(0xFFFFD54F), Color.White, "HEKMAT", "ic_bank_hekmat"),
        IranianBankInfo("636795", "بانک مرکزی جمهوری اسلامی", "Central Bank of Iran", "010", Color(0xFF263238), Color(0xFF37474F), Color(0xFFFFD700), Color.White, "CBI", "ic_bank_bank_markazi")
    )

    /**
     * هر درایوبلِ ریسورس (چه VectorDrawable/XML و چه بیت‌مپ) را به یک [Bitmap]
     * مربعیِ [sizePx] تبدیل می‌کند. برخلاف [android.graphics.BitmapFactory.decodeResource]
     * که برای وکتورها null می‌دهد، این تابع درایوبل را روی یک بوم می‌کشد، پس
     * لوگوهای بانکیِ وکتوری هم در تصویر کارت ظاهر می‌شوند.
     */
    fun drawableToBitmap(context: Context, resId: Int, sizePx: Int): Bitmap? {
        return try {
            val drawable = androidx.core.content.res.ResourcesCompat.getDrawable(
                context.resources, resId, context.theme
            ) ?: return null
            val size = sizePx.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bmp
        } catch (e: Exception) {
            null
        }
    }

    fun cleanDigits(input: String): String {
        return input.map { ch ->
            when (ch) {
                '۰', '٠' -> '0'
                '۱', '١' -> '1'
                '۲', '٢' -> '2'
                '۳', '٣' -> '3'
                '۴', '٤' -> '4'
                '۵', '٥' -> '5'
                '۶', '٦' -> '6'
                '۷', '٧' -> '7'
                '۸', '٨' -> '8'
                '۹', '٩' -> '9'
                else -> ch
            }
        }.filter { it.isDigit() }.joinToString("")
    }

    /** Detects Iranian Bank details from 16-digit card number or initial 6 digits. */
    fun detectBank(cardNumber: String): IranianBankInfo? {
        val digits = cleanDigits(cardNumber)
        if (digits.length < 6) return null
        val prefix = digits.take(6)
        return BANK_DATABASE.find { it.bin == prefix }
    }

    /** Detects Iranian Bank details from IBAN (Shaba) code like IRxx017... */
    fun detectBankFromIban(iban: String): IranianBankInfo? {
        val clean = iban.trim().uppercase()
        if (!clean.startsWith("IR") || clean.length < 5) return null
        val bankCode = clean.substring(4, minOf(7, clean.length))
        return BANK_DATABASE.find { it.ibanCode == bankCode }
    }

    /** Formats a card number into groups of 4 digits: 6037 - 9918 - 1234 - 5678 */
    fun formatCardNumber(cardNumber: String): String {
        val digits = cleanDigits(cardNumber)
        return digits.chunked(4).joinToString(" - ")
    }

    /** Validates Luhn checksum algorithm for 16-digit Iranian cards. */
    fun isValidCardNumber(cardNumber: String): Boolean {
        val digits = cleanDigits(cardNumber)
        if (digits.length != 16) return false
        var sum = 0
        for (i in 0 until 16) {
            var digit = digits[i] - '0'
            if (i % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return sum % 10 == 0
    }

    /**
     * Generates a high-quality Bitmap image of the bank card for sharing or saving.
     *
     * این تنها خروجی تصویری برنامه است که عمداً از رندرر یکپارچهٔ اسناد
     * ([com.example.reports.UnifiedImageGenerator]) استفاده **نمی‌کند** — چون
     * یک کارت بانکی است، نه سند. اما برای باقی موارد یکپارچگی رعایت شده:
     *  • ابعاد از [com.example.reports.ExportDocKind.BankCard] می‌آید
     *    (۱۰۰۰×۶۳۰ ⇒ نسبت استاندارد ISO/IEC 7810 ID-1) و همهٔ مختصات/قلم‌ها با
     *    همان ضریب مقیاس می‌شوند، پس اگر عرض مرجع عوض شود کارت به‌هم نمی‌ریزد.
     *  • قلم: فقط و فقط [com.example.reports.AppFonts] — یعنی همان TTF های
     *    استاندارد پروژه (شامل چهرهٔ مونواسپیس برای شماره کارت/شبا). دیگر
     *    `Typeface.DEFAULT` و `Typeface.MONOSPACE` سیستمی استفاده نمی‌شود تا
     *    کارت بانکی از بقیهٔ خروجی‌ها جدا نیفتد.
     *  • CVV2 و تاریخ انقضا عمداً ماسک می‌شوند (نشت اطلاعات در اشتراک‌گذاری).
     */
    fun generateCardImageBitmap(
        context: Context,
        bankInfo: IranianBankInfo?,
        cardNumber: String,
        cardHolder: String,
        cvv2: String,
        expireMonth: String,
        expireYear: String,
        accountNumber: String,
        iban: String,
        clinicTitle: String = ""
    ): Bitmap {
        val kind = com.example.reports.ExportDocKind.BankCard
        val width = kind.widthPx
        val height = kind.heightPx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // همهٔ مختصات زیر بر اساس بوم مرجع ۱۰۰۰×۶۳۰ نوشته شده‌اند و با همین
        // ضریب‌ها به ابعاد واقعی کارت نگاشت می‌شوند.
        val kx = width / 1000f
        val ky = height / 630f
        val sc = minOf(kx, ky)
        fun x(v: Float) = v * kx
        fun y(v: Float) = v * ky
        fun sz(v: Float) = v * sc

        val fonts = com.example.reports.AppFonts
        fun textPaint(
            role: com.example.reports.AppFonts.Role,
            size: Float,
            color: Int,
            align: Paint.Align = Paint.Align.LEFT,
            text: String? = null
        ): android.text.TextPaint = fonts.textPaint(context, role, sz(size), color, align, text = text)

        val primaryInt = bankInfo?.let {
            AndroidColor.rgb(
                (it.primaryColor.red * 255).toInt(),
                (it.primaryColor.green * 255).toInt(),
                (it.primaryColor.blue * 255).toInt()
            )
        } ?: AndroidColor.rgb(26, 35, 126)

        val secondaryInt = bankInfo?.let {
            AndroidColor.rgb(
                (it.secondaryColor.red * 255).toInt(),
                (it.secondaryColor.green * 255).toInt(),
                (it.secondaryColor.blue * 255).toInt()
            )
        } ?: AndroidColor.rgb(40, 53, 147)

        val accentInt = bankInfo?.let {
            AndroidColor.rgb(
                (it.accentColor.red * 255).toInt(),
                (it.accentColor.green * 255).toInt(),
                (it.accentColor.blue * 255).toInt()
            )
        } ?: AndroidColor.rgb(255, 215, 0)

        // Card background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(primaryInt, secondaryInt, primaryInt),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        val cardRect = RectF(x(20f), y(20f), x((1000 - 20).toFloat()), y((630 - 20).toFloat()))
        canvas.drawRoundRect(cardRect, 36f * sc, 36f * sc, bgPaint)

        // Decorative subtle pattern circles
        val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            alpha = 18
            style = Paint.Style.STROKE
            strokeWidth = 3f * sc
        }
        canvas.drawCircle(width * 0.85f, height * 0.25f, 180f * sc, patternPaint)
        canvas.drawCircle(width * 0.85f, height * 0.25f, 230f * sc, patternPaint)
        canvas.drawCircle(width * 0.15f, height * 0.85f, 160f * sc, patternPaint)

        // Bank Name & Emblem Header
        val bankNamePaint = textPaint(com.example.reports.AppFonts.Role.Title, 30f, AndroidColor.WHITE, Paint.Align.RIGHT)
        val bankName = fonts.sanitize(context, bankInfo?.nameFa ?: "کارت بانکی")

        val bankEn = fonts.sanitize(context, bankInfo?.nameEn ?: "Debit / Credit Card")
        val bankEnPaint = textPaint(com.example.reports.AppFonts.Role.Number, 20f, accentInt, Paint.Align.RIGHT, text = bankEn)

        var logoDrawn = false
        val resId = if (!bankInfo?.logoResName.isNullOrEmpty()) {
            context.resources.getIdentifier(bankInfo?.logoResName, "drawable", context.packageName)
        } else 0
        if (resId != 0) {
            // لوگوهای بانک VectorDrawable (XML) هستند؛ BitmapFactory.decodeResource
            // برای وکتور همیشه null برمی‌گرداند — دلیلِ «لوگوی بانک در عکس نیست».
            // به‌جای آن درایوبل را می‌گیریم و مستقیم روی بوم رسم می‌کنیم که هم
            // وکتور و هم بیت‌مپ را پوشش می‌دهد.
            val logo = drawableToBitmap(context, resId, (110f * sc).toInt().coerceAtLeast(1))
            if (logo != null) {
                // پس‌زمینهٔ سفیدِ گرد پشت لوگو تا روی هر رنگِ کارت واضح دیده شود
                val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE }
                val badgeRect = RectF(x(850f), y(45f), x(945f), y(140f))
                canvas.drawRoundRect(badgeRect, 18f * sc, 18f * sc, badgeBg)
                val destRect = RectF(x(862f), y(57f), x(933f), y(128f))
                canvas.drawBitmap(logo, null, destRect, Paint(Paint.ANTI_ALIAS_FLAG))
                canvas.drawText(bankName, x(830f), y(95f), bankNamePaint)
                canvas.drawText(bankEn, x(830f), y(135f), bankEnPaint)
                logoDrawn = true
            }
        }
        if (!logoDrawn) {
            // Draw a prominent Bank Emblem Badge with gold border and Bank Short Code
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentInt
                style = Paint.Style.STROKE
                strokeWidth = 4f * sc
            }
            val badgeRect = RectF(x(860f), y(45f), x(940f), y(125f))
            canvas.drawRoundRect(badgeRect, 20f * sc, 20f * sc, badgePaint)
            canvas.drawRoundRect(badgeRect, 20f * sc, 20f * sc, borderPaint)

            val badgeTextPaint = textPaint(com.example.reports.AppFonts.Role.Title, 22f, primaryInt, Paint.Align.CENTER)
            val shortCode = fonts.sanitize(context, bankInfo?.shortCode ?: if (bankName.length <= 6) bankName else "بانک")
            canvas.drawText(shortCode, x(900f), y(93f), badgeTextPaint)

            canvas.drawText(bankName, x(840f), y(95f), bankNamePaint)
            canvas.drawText(bankEn, x(840f), y(135f), bankEnPaint)
        }

        // Clinic watermark / header on the left
        if (clinicTitle.isNotBlank()) {
            val clinicPaint = textPaint(com.example.reports.AppFonts.Role.Title, 22f, AndroidColor.argb(200, 255, 255, 255))
            canvas.drawText(fonts.sanitize(context, clinicTitle), x(70f), y(95f), clinicPaint)
        }

        // Gold EMV Smart Chip
        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(230, 190, 80)
        }
        val chipRect = RectF(x(70f), y(160f), x(180f), y(245f))
        canvas.drawRoundRect(chipRect, 14f * sc, 14f * sc, chipPaint)

        val chipLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(180, 140, 40)
            strokeWidth = 2.5f * sc
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(chipRect, 14f * sc, 14f * sc, chipLinePaint)
        canvas.drawLine(x(125f), y(160f), x(125f), y(245f), chipLinePaint)
        canvas.drawLine(x(70f), y(202f), x(180f), y(202f), chipLinePaint)

        // Contactless payment wave symbol
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(220, 255, 255, 255)
            strokeWidth = 4f * sc
            style = Paint.Style.STROKE
        }
        val waveCenter = x(215f)
        for (r in listOf(18f, 28f, 38f)) {
            val arcRect = RectF(waveCenter - r * sc, y(182f) - r * sc, waveCenter + r * sc, y(182f) + r * sc)
            canvas.drawArc(arcRect, -45f, 90f, false, wavePaint)
        }

        // 16-Digit Card Number — با قلم عددی استاندارد پروژه (مونواسپیس TTF)
        val formattedCard = fonts.sanitize(context, formatCardNumber(cardNumber))
            .ifBlank { "---- - ---- - ---- - ----" }
        val cardNumPaint = textPaint(com.example.reports.AppFonts.Role.Number, 46f, AndroidColor.WHITE, Paint.Align.CENTER, text = formattedCard)
        cardNumPaint.setShadowLayer(8f * sc, 3f * sc, 3f * sc, AndroidColor.argb(140, 0, 0, 0))
        canvas.drawText(formattedCard, width / 2f, y(335f), cardNumPaint)

        // Labels Paint
        val labelPaint = textPaint(com.example.reports.AppFonts.Role.Body, 18f, AndroidColor.argb(200, 255, 255, 255), Paint.Align.RIGHT)
        // مقدارهای این دو برچسب عمداً ماسک‌اند («***» و «** / **») ⇒ نمونهٔ لاتین برای
        // گرفتن قلم ارقام کافی است.
        val valuePaint = textPaint(com.example.reports.AppFonts.Role.Number, 24f, AndroidColor.WHITE, Paint.Align.RIGHT, text = "00 / 00")

        // CVV2 and Expiry — masked on purpose for privacy while sharing
        canvas.drawText(fonts.sanitize(context, "CVV2"), x(930f), y(400f), labelPaint)
        canvas.drawText("***", x(930f), y(435f), valuePaint)

        canvas.drawText(fonts.sanitize(context, "انقضا (EXP)"), x(740f), y(400f), labelPaint)
        canvas.drawText("** / **", x(740f), y(435f), valuePaint)

        // Cardholder Name
        val holderLabelPaint = textPaint(com.example.reports.AppFonts.Role.Body, 18f, AndroidColor.argb(200, 255, 255, 255))
        val holderValuePaint = textPaint(com.example.reports.AppFonts.Role.Title, 28f, accentInt)
        holderValuePaint.setShadowLayer(4f * sc, 2f * sc, 2f * sc, AndroidColor.argb(120, 0, 0, 0))
        val holderName = fonts.sanitize(context, cardHolder.ifBlank { "دارنده کارت" })
        canvas.drawText(fonts.sanitize(context, "صاحب کارت / Cardholder"), x(70f), y(475f), holderLabelPaint)
        canvas.drawText(holderName, x(70f), y(518f), holderValuePaint)

        // IBAN (Shaba) & Account number bar at bottom
        if (iban.isNotBlank() || accountNumber.isNotBlank()) {
            val detailsText = buildString {
                if (accountNumber.isNotBlank()) append("حساب: $accountNumber   ")
                if (iban.isNotBlank()) append("شبا: ${iban.uppercase()}")
            }
            // این رشته معمولاً «حساب:/شبا:» فارسی دارد ⇒ قلم بدنه (با ارقام tabular)؛
            // اگر روزی فقط شمارهٔ خالی فرستاده شود، خودش مونواسپیس می‌شود.
            val ibanPaint = textPaint(
                com.example.reports.AppFonts.Role.Number,
                19f,
                AndroidColor.argb(230, 255, 255, 255),
                text = detailsText
            )
            canvas.drawText(fonts.sanitize(context, detailsText), x(70f), y(575f), ibanPaint)
        }

        return bitmap
    }

    /**
     * Saves card image bitmap for instant sharing. از همان [com.example.reports.ExportFiles]
     * استفاده می‌کند تا مسیر و نام‌گذاری همهٔ خروجی‌های تصویری یکسان باشد.
     */
    fun saveCardImageToFile(context: Context, bitmap: Bitmap, fileName: String = "bank_card.png"): File =
        com.example.reports.ExportFiles.newFile(context, "card_" + fileName.substringBeforeLast('.'), "png").also { file ->
            FileOutputStream(file).use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG encode failed" }
            }
        }
}
