package com.example.data

import org.json.JSONObject

/**
 * لینک‌های لوکیشن مطب.
 *
 * علاوه بر گوگل‌مپ و ویز، دو سرویس پرکاربرد ایرانی «نشان» (neshan.org) و
 * «بلد» (balad.ir) هم پشتیبانی می‌شوند تا بیمار ایرانی بتواند مسیر مطب را در
 * همان اپلیکیشنی که روی گوشی دارد باز کند.
 *
 * قالب ذخیره‌سازی در ستون `clinic_settings.clinicMapLink` یک JSON ساده است:
 * `{"google":"...","waze":"...","neshan":"...","balad":"..."}`
 * برای سازگاری با نسخه‌های قدیمی، اگر مقدار ذخیره‌شده JSON نباشد همان رشته به
 * عنوان لینک گوگل‌مپ در نظر گرفته می‌شود.
 */
data class ClinicMapLinks(
    val google: String = "",
    val waze: String = "",
    val neshan: String = "",
    val balad: String = ""
)

fun parseClinicMapLinks(raw: String): ClinicMapLinks {
    val t = raw.trim()
    if (t.isBlank()) return ClinicMapLinks()
    if (t.startsWith("{")) {
        return try {
            val o = JSONObject(t)
            ClinicMapLinks(
                google = o.optString("google", "").trim(),
                waze = o.optString("waze", "").trim(),
                neshan = o.optString("neshan", "").trim(),
                balad = o.optString("balad", "").trim()
            )
        } catch (e: Exception) {
            ClinicMapLinks(google = t)
        }
    }
    return ClinicMapLinks(google = t)
}

fun encodeClinicMapLinks(
    google: String,
    waze: String,
    neshan: String = "",
    balad: String = ""
): String {
    val g = google.trim()
    val w = waze.trim()
    val n = neshan.trim()
    val b = balad.trim()
    if (g.isBlank() && w.isBlank() && n.isBlank() && b.isBlank()) return ""
    return JSONObject().apply {
        put("google", g)
        put("waze", w)
        put("neshan", n)
        put("balad", b)
    }.toString()
}
