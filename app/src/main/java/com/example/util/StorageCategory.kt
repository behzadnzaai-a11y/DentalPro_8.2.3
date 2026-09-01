package com.example.util

import java.io.File
import java.security.MessageDigest

/**
 * دسته‌بندی فایل‌های Storage.
 *
 * ## چرا این فایل وجود دارد
 *
 * پیش از این هر آپلود مسیرِ `{uid}/{UUID}.jpg` می‌گرفت. دو مشکل داشت:
 *
 *  ۱. **همه‌چیز در یک پوشه ریخته می‌شد** — عکس چک، امضای بیمار، آواتار پرسنل و
 *     لوگوی مطب همه کنار هم و بدون هیچ نظمی.
 *  ۲. **هر بار UUID تازه** — ذخیرهٔ دوبارهٔ *همان* فایل یک شیء جدید می‌ساخت.
 *     ویرایش یک بیمار بدون تغییر عکس، یا سینکِ دوبارهٔ یک رکورد، فضای انبار را
 *     با کپی‌های یکسان پر می‌کرد و هیچ راهی برای فهمیدنِ تکراری‌بودنشان نبود.
 *
 * راه‌حل: مسیر شیء از **هشِ محتوای خود فایل** ساخته می‌شود، داخل پوشهٔ دستهٔ
 * خودش و زیر شناسهٔ کاربر:
 *
 * ```
 * {uid}/{category}/{sha256}.{ext}
 * ```
 *
 * چون نام از محتوا می‌آید، آپلودِ دوبارهٔ همان فایل دقیقاً همان مسیر را می‌دهد و
 * با `x-upsert` روی همان شیء می‌نویسد — هرگز نسخهٔ دوم ساخته نمی‌شود.
 * پیشوندِ `{uid}` هم باعث می‌شود دسته‌بندی هر کاربر کاملاً از بقیه جدا بماند و
 * قواعد RLS روی Storage (که مسیر را با `auth.uid()` می‌سنجند) دست‌نخورده کار کنند.
 */
enum class StorageCategory(val folder: String) {
    /** چک‌ها — تصویر چک‌های صیادی. */
    CHEQUES("cheques"),

    /** امضاها و پروفایل بیماران — عکس پروفایل، امضای پذیرش و مدارک بیمار. */
    PATIENTS("patients"),

    /** پروفایل پرسنل و پزشک، آواتار و لوگوی مطب. */
    CLINIC("clinic"),

    /** هر چیزی که در سه دستهٔ بالا نمی‌گنجد (فاکتور تأمین‌کننده و …). */
    MISC("misc");

    companion object {
        /** همهٔ نام پوشه‌ها — برای اعتبارسنجی مسیرها. */
        val ALL_FOLDERS: Set<String> = entries.map { it.folder }.toSet()

        /**
         * دستهٔ درست را از روی نام فیلدِ رکورد تشخیص می‌دهد.
         *
         * این تابع تنها جای تصمیم‌گیری دربارهٔ دسته است؛ هر جای دیگری که لازم شد
         * فایل آپلود شود باید از همین‌جا بپرسد تا دسته‌بندی همه‌جا یکسان بماند.
         */
        fun forField(fieldName: String): StorageCategory {
            val f = fieldName.lowercase()
            return when {
                f.contains("cheque") || f.contains("check") -> CHEQUES
                f.contains("signature") -> PATIENTS
                f.contains("logo") || f.contains("avatar") -> CLINIC
                // عکس پرسنل/پزشک از عکس بیمار جدا می‌شود: هر دو photoFileId نام
                // دارند، پس تشخیص فقط از روی نام فیلد ممکن نیست و صداکننده باید
                // دستهٔ درست را صریح بدهد. این‌جا حالتِ پیش‌فرضِ بیمار است.
                f.contains("staff") || f.contains("doctor") || f.contains("personnel") -> CLINIC
                f.contains("photo") || f.contains("document") || f.contains("patient") -> PATIENTS
                f.contains("invoice") -> MISC
                else -> MISC
            }
        }

        /**
         * آیا این مسیر از قبل دسته‌بندی‌شده است؟
         *
         * مسیرهای قدیمی (`{uid}/{uuid}.jpg`) دو بخشی‌اند و بخش میانی ندارند؛
         * مسیرهای جدید سه بخشی‌اند و بخش میانی یکی از نام پوشه‌هاست.
         */
        fun isCategorized(objectPath: String): Boolean {
            val parts = objectPath.split("/")
            return parts.size >= 3 && parts[parts.size - 2] in ALL_FOLDERS
        }

        /**
         * هشِ SHA-256 محتوای فایل — مبنای نام‌گذاریِ محتوامحور.
         *
         * فایل تکه‌تکه خوانده می‌شود تا عکس بزرگ حافظه را نترکاند.
         */
        fun contentHash(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        /**
         * مسیر شیء را می‌سازد: `{uid}/{category}/{sha256}.{ext}`.
         *
         * چون نام از محتوا می‌آید، دو فایل با محتوای یکسان همیشه یک مسیر
         * می‌گیرند و آپلود دوم روی همان شیء می‌نویسد (بدون ساختن نسخهٔ تکراری).
         */
        fun objectPathFor(uid: String, category: StorageCategory, file: File): String {
            val ext = file.extension.ifBlank {
                if (file.name.endsWith(".png", ignoreCase = true)) "png" else "jpg"
            }.lowercase()
            return "$uid/${category.folder}/${contentHash(file)}.$ext"
        }
    }
}
