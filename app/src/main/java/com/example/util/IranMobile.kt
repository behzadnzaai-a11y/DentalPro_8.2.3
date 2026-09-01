package com.example.util

/**
 * شماره موبایل ایران — همیشه به شکل `+989xxxxxxxxx` ذخیره می‌شود.
 *
 * پیشوند `+989` ثابت است و کاربر آن را تایپ یا حذف نمی‌کند؛ در رابط کاربری به
 * صورت یک برچسبِ غیرقابل‌ویرایش کنار فیلد نشان داده می‌شود و کاربر فقط ۹ رقمِ
 * باقی‌مانده را وارد می‌کند.
 *
 * این شیء نقطهٔ واحدِ تبدیل بین دو نمایش است:
 *  * «۹ رقم» — چیزی که در فیلد ورودی دیده و ویرایش می‌شود.
 *  * «‎+989xxxxxxxxx‎» — چیزی که در پایگاه داده و سرور ذخیره می‌شود.
 */
object IranMobile {

    /** پیشوند ثابتِ غیرقابل‌ویرایش. */
    const val PREFIX = "+989"

    /** تعداد رقم‌هایی که کاربر وارد می‌کند (بعد از پیشوند). */
    const val LOCAL_DIGITS = 9

    /** تبدیل ارقام فارسی/عربی به لاتین و حذف هر چیز غیر رقم. */
    private fun digitsOnly(raw: String): String {
        val map = mapOf(
            '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
            '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9',
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
        )
        return raw.map { map[it] ?: it }.filter { it.isDigit() }.joinToString("")
    }

    /**
     * از هر شکلِ ورودی، ۹ رقمِ محلی را بیرون می‌کشد.
     *
     * همهٔ این‌ها به `123456789` تبدیل می‌شوند:
     * `+989123456789`، `00989123456789`، `989123456789`، `09123456789`،
     * `9123456789`، `123456789` و نسخه‌های دارای فاصله یا خط تیره.
     */
    fun toLocalDigits(raw: String): String {
        var d = digitsOnly(raw)
        if (d.startsWith("0098")) d = d.removePrefix("0098")
        if (d.startsWith("98") && d.length > LOCAL_DIGITS + 1) d = d.removePrefix("98")
        if (d.startsWith("0")) d = d.trimStart('0')
        if (d.startsWith("9") && d.length > LOCAL_DIGITS) d = d.removePrefix("9")
        return d.take(LOCAL_DIGITS)
    }

    /** فیلترِ ورودیِ فیلد: فقط رقم، حداکثر ۹ رقم. */
    fun sanitizeInput(raw: String): String = digitsOnly(raw).take(LOCAL_DIGITS)

    /** آیا ۹ رقمِ واردشده کامل است؟ */
    fun isCompleteLocal(localDigits: String): Boolean =
        localDigits.length == LOCAL_DIGITS && localDigits.all(Char::isDigit)

    /**
     * شکل ذخیره‌سازی: `+989xxxxxxxxx`.
     * اگر ۹ رقم کامل نباشد رشتهٔ خالی برمی‌گردد تا شمارهٔ ناقص ذخیره نشود.
     */
    fun toStored(localDigits: String): String =
        if (isCompleteLocal(localDigits)) PREFIX + localDigits else ""

    /** نرمال‌سازی هر ورودی مستقیماً به شکل ذخیره‌سازی (یا خالی اگر ناقص بود). */
    fun normalize(raw: String): String = toStored(toLocalDigits(raw))

    /** آیا مقدارِ ذخیره‌شده یک موبایل ایرانیِ معتبر است؟ */
    fun isValidStored(stored: String): Boolean =
        Regex("^\\+989\\d{9}$").matches(stored.trim())

    /** نمایش خوانا برای کاربر: `+989123456789` → `+98 912 345 6789`. */
    fun formatForDisplay(stored: String): String {
        val local = toLocalDigits(stored)
        if (!isCompleteLocal(local)) return stored
        return "+98 9${local.substring(0, 2)} ${local.substring(2, 5)} ${local.substring(5)}"
    }
}
