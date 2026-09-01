package com.example.util

/**
 * اعتبارسنجی کد ملی ایران.
 *
 * ورود کد ملی **اختیاری** است: اگر کاربر چیزی وارد نکند، فرم معتبر شمرده می‌شود؛
 * اما اگر چیزی وارد کرد، باید یک کد ملی واقعاً معتبر باشد.
 *
 * الگوریتم رسمی (چک‌سام وزنی، مبنای ۱۱):
 *  ۱. باید دقیقاً ۱۰ رقم باشد (کدهای کوتاه‌تر با صفر از چپ پر می‌شوند).
 *  ۲. کدهای تکراری مثل ۰۰۰۰۰۰۰۰۰۰ یا ۱۱۱۱۱۱۱۱۱۱ نامعتبرند — با اینکه در
 *     فرمول چک‌سام صدق می‌کنند، سازمان ثبت احوال آن‌ها را صادر نمی‌کند.
 *  ۳. نه رقم اول در ضرایب ۱۰ تا ۲ ضرب و جمع می‌شوند.
 *  ۴. باقی‌ماندهٔ این مجموع بر ۱۱ حساب می‌شود:
 *       اگر r < 2  →  رقم کنترل باید برابر r باشد.
 *       اگر r ≥ 2  →  رقم کنترل باید برابر 11 − r باشد.
 */
object NationalIdValidator {

    /** تبدیل ارقام فارسی/عربی به لاتین و حذف هر چیز غیر رقم. */
    fun normalize(raw: String): String {
        val map = mapOf(
            '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
            '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9',
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
        )
        return raw.map { map[it] ?: it }.filter { it.isDigit() }.joinToString("")
    }

    /**
     * کد را به ۱۰ رقم استاندارد می‌رساند.
     *
     * کدهای ۸ و ۹ رقمی (متعلق به شهرهای کوچک) با صفرِ ابتدایی کامل می‌شوند؛
     * این همان کاری است که خود سازمان ثبت احوال انجام می‌دهد.
     */
    fun padded(raw: String): String {
        val digits = normalize(raw)
        return if (digits.length in 8..9) digits.padStart(10, '0') else digits
    }

    /** آیا کد ملی معتبر است؟ (رشتهٔ خالی → false) */
    fun isValid(raw: String): Boolean {
        val code = padded(raw)
        if (code.length != 10) return false
        if (code.all { it == code[0] }) return false

        var sum = 0
        for (i in 0..8) {
            sum += (code[i] - '0') * (10 - i)
        }
        val remainder = sum % 11
        val check = code[9] - '0'
        return if (remainder < 2) check == remainder else check == 11 - remainder
    }

    /**
     * اعتبارسنجی برای فرم‌هایی که ورود کد ملی در آن‌ها **اختیاری** است.
     * خالی = معتبر؛ هر مقدار واردشده باید کاملاً معتبر باشد.
     */
    fun isValidOrBlank(raw: String): Boolean =
        raw.isBlank() || isValid(raw)

    /** پیام خطای مناسب برای نمایش زیر فیلد — یا null اگر ایرادی نیست. */
    fun errorMessage(raw: String, isPersian: Boolean = true): String? {
        if (raw.isBlank()) return null
        val code = padded(raw)
        return when {
            code.length < 10 ->
                if (isPersian) "کد ملی باید ۱۰ رقم باشد" else "National ID must be 10 digits"
            code.length > 10 ->
                if (isPersian) "کد ملی بیش از ۱۰ رقم است" else "National ID is longer than 10 digits"
            code.all { it == code[0] } ->
                if (isPersian) "کد ملی نامعتبر است (ارقام تکراری)" else "Invalid national ID (repeated digits)"
            !isValid(code) ->
                if (isPersian) "کد ملی نامعتبر است (رقم کنترل نادرست)" else "Invalid national ID (checksum failed)"
            else -> null
        }
    }
}
