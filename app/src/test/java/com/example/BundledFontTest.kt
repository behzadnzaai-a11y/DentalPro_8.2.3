package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * نگهبان «استاندارد تایپوگرافی» برنامه — فونت‌های `app/src/main/assets/fonts/`.
 *
 * چرا لازم است؟ چون کل یکپارچگی خروجی‌ها روی همین یک پوشه سوار است: اگر فایل
 * فونت خراب/غیرواقعی باشد، یا فقط یک چهره (مثلاً Regular) گذاشته شود، یا یک
 * فایل متنی (مثل `OFL.txt`) بین فونت‌ها بیفتد، رندرر بی‌سروصدا به فونت سیستم
 * می‌افتد و دقیقاً همان «دو‌تاییِ ظاهر UI و خروجی» برمی‌گردد که باگ اصلی بود.
 *
 * نکته: پوشهٔ خالی **مجاز** است (fallback سیستم، مستندات: docs/FONTS.md)؛
 * قاعده فقط وقتی اعمال می‌شود که فایلی داخل پوشه باشد.
 */
class BundledFontTest {

    private val fontsDir: File?
        get() = listOf("src/main/assets/fonts", "app/src/main/assets/fonts")
            .map(::File)
            .firstOrNull { it.isDirectory }

    private val licensesDir: File?
        get() = listOf("../LICENSES", "LICENSES").map(::File).firstOrNull { it.isDirectory }

    /** امضای ابتدای فایل‌های فونت sfnt/TTF/OTF — همان چیزی که AppFonts می‌پذیرد. */
    private val fontMagics = listOf(
        byteArrayOf(0, 1, 0, 0),
        "true".toByteArray(Charsets.US_ASCII),
        "OTTO".toByteArray(Charsets.US_ASCII),
        "ttcf".toByteArray(Charsets.US_ASCII)
    )

    private val acceptedExtensions = listOf("ttf", "otf", "txt")
    private val weightKeys = listOf(
        "bold", "heavy", "black", "extrabold", "semibold", "medium", "light", "thin", "hairline"
    )

    private fun bundledFonts(): List<File> =
        fontsDir?.listFiles()?.filter { f ->
            f.isFile && acceptedExtensions.contains(f.extension.lowercase())
        }.orEmpty().sortedBy { it.name.lowercase() }

    /** «vazirmatn» از «Vazirmatn-Bold.ttf» — هم‌قاعدهٔ AppFonts.familyKeyOf. */
    private fun familyKey(fileName: String): String {
        val stem = fileName.substringBeforeLast('.').lowercase()
        return stem.split('-', '_', ' ').firstOrNull { it.isNotBlank() } ?: stem
    }

    private fun headOf(file: File): ByteArray {
        val head = ByteArray(4)
        file.inputStream().use { input ->
            var read = 0
            while (read < 4) {
                val n = input.read(head, read, 4 - read)
                if (n <= 0) break
                read += n
            }
        }
        return head
    }

    @Test
    fun everyBundledFontFileIsARealFont() {
        val files = bundledFonts()
        for (file in files) {
            val head = headOf(file)
            val isFont = head.size == 4 && fontMagics.any { it.contentEquals(head) }
            assertTrue(
                "${file.name} is inside assets/fonts but is not a TTF/OTF binary " +
                    "(فایل متنی مجوز نباید کنار فونت‌ها باشد؛ آن را به LICENSES/ منتقل کنید)",
                isFont
            )
            assertTrue("${file.name} is too small to be a usable font", file.length() > 20_000L)
        }
    }

    /**
     * سه لایه، نه بیشتر: بدنه (Vazirmatn) + نمایشی (Estedad) + ارقام (JetBrains Mono).
     * اگر کسی خانوادهٔ چهارمی اضافه کند، یا UI و خروجی دوباره از هم جدا می‌شود و
     * یا حجم بیهودهٔ APK؛ پس هر دو حالت در همین تست گرفته می‌شود.
     */
    @Test
    fun bundledFamiliesMatchTheThreeSlots() {
        val families = bundledFonts().map { familyKey(it.name) }.distinct().sorted()
        assertEquals(
            "assets/fonts must contain exactly the three slotted families " +
                "(vazirmatn=body, estedad=titles, jetbrainsmono=figures) — found: $families",
            listOf("estedad", "jetbrainsmono", "vazirmatn"),
            families
        )
    }

    @Test
    fun everyFamilyHasAtLeastOneValidFace() {
        val byFamily = bundledFonts().groupBy { familyKey(it.name) }
        for ((family, files) in byFamily) {
            assertTrue("$family has no font file", files.isNotEmpty())
            // چهره‌ای که AppFonts برای این لایه برمی‌دارد باید پیدا شود (ایتالیک حساب نمی‌شود)
            val usable = files.filterNot { f ->
                val n = f.name.lowercase()
                n.contains("italic") || n.contains("oblique")
            }
            assertTrue("$family has only italic faces", usable.isNotEmpty())
        }
    }

    @Test
    fun thereIsAtLeastOnePlainRegularFace() {
        val files = bundledFonts()
        if (files.isEmpty()) return // حالت مجاز: بدون فونت سفارشی، fallback سیستم
        val regular = files.firstOrNull { f ->
            val name = f.name.lowercase()
            name.contains("regular") || weightKeys.none { name.contains(it) }
        }
        assertTrue(
            "اگر فونت نصب می‌کنید، حداقل یک چهرهٔ Regular (بدون پسوند وزن) لازم است",
            regular != null
        )
    }

    @Test
    fun licenseForBundledFamilyIsStoredInRepo() {
        val files = bundledFonts()
        if (files.isEmpty()) return
        val families = files.map { familyKey(it.name) }.distinct()
        val licenses = licensesDir?.listFiles()?.filter { it.isFile }.orEmpty()
        for (family in families) {
            val forFamily = licenses.filter { it.name.lowercase().contains(family) }
            val match = forFamily.firstOrNull { f ->
                val n = f.name.lowercase()
                n.contains("licen") || n.contains("ofl")
            } ?: forFamily.firstOrNull()
            assertTrue(
                "متن مجوز «$family» باید در LICENSES/ باشد (OFL اجازهٔ بازتوزیع می‌دهد " +
                    "اما کپی مجوز الزامی است)",
                match != null
            )
            val text = match!!.readText()
            assertTrue(
                "${match.name} should contain the license text",
                text.isNotBlank() && text.length > 200
            )
        }
    }
}
