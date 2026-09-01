package com.example.data

/**
 * ============================================================================
 *  پروندهٔ شخص در «مالی و حسابداری ← اشخاص»
 * ============================================================================
 * برای اینکه هیچ جدول تازه‌ای به دیتابیس اضافه نشود (و مهاجرت Room لازم نیاید)،
 * هر شخص روی همان جدول موجود [PersonalSubAccount] با `category = "PERSONS"`
 * ذخیره می‌شود:
 *
 *   • name        → «نام و نام خانوادگی» شخص (کلید ریزحساب‌ها هم همین است)
 *   • parentName  → «عنوان شخص» (مثلاً تأمین‌کننده، همکار، بستانکار شخصی، ...)
 *   • ledgerSide  → «ماهیت شخص»: DEBTOR (بدهکار) یا CREDITOR (بستانکار)
 *
 * [PersonProfile] نمای راحتِ همان رکورد است و تبدیل دوطرفه را انجام می‌دهد.
 */

/** ماهیت شخص — بدهکار یا بستانکار. */
enum class PersonNature(val storageKey: String) {
    DEBTOR("DEBTOR"),
    CREDITOR("CREDITOR"),
    NONE("NONE");

    /** برچسب فارسی/انگلیسی برای نمایش در رابط کاربری. */
    fun label(isPersian: Boolean): String = when (this) {
        DEBTOR -> if (isPersian) "بدهکار" else "Debtor"
        CREDITOR -> if (isPersian) "بستانکار" else "Creditor"
        NONE -> if (isPersian) "نامشخص" else "Unspecified"
    }

    companion object {
        fun fromStorage(value: String?): PersonNature =
            entries.firstOrNull { it.storageKey.equals(value?.trim(), ignoreCase = true) } ?: NONE
    }
}

/** مقدار [PersonalSubAccount.category] که یک رکورد را «شخص» می‌کند. */
const val PERSONS_CATEGORY = "PERSONS"

/**
 * پروندهٔ یک شخص. [firstName]/[lastName] از [fullName] مشتق می‌شوند تا نیازی به
 * ستون جدید در دیتابیس نباشد: اولین واژه نام، بقیه نام خانوادگی.
 */
data class PersonProfile(
    val id: Long = 0,
    val firstName: String = "",
    val lastName: String = "",
    /** عنوان شخص، مثلاً «تأمین‌کننده مواد مصرفی» یا «همکار». */
    val title: String = "",
    val nature: PersonNature = PersonNature.NONE,
    /** مبلغ اولیه (افتتاحیهٔ حساب شخص) — در جمعِ جریات شخص لحاظ می‌شود. */
    val openingBalance: Long = 0,
    /** شمارهٔ اولویت نمایش (کوچک‌تر = بالاتر). اشخاص اولویت‌دار اول لیست می‌آیند. */
    val priority: Int = 0,
    /** ستاره‌دار — با [PersonalSubAccount.isQuickSelect] یکسان است. */
    val isStarred: Boolean = false,
    val syncId: String = ""
) {
    /** نام کامل — همان کلیدی که ریزحساب‌ها و تراکنش‌ها با آن به شخص وصل می‌شوند. */
    val fullName: String
        get() = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ")

    fun toSubAccount(existing: PersonalSubAccount? = null): PersonalSubAccount =
        (existing ?: PersonalSubAccount(name = fullName, category = PERSONS_CATEGORY)).copy(
            id = id,
            name = fullName,
            category = PERSONS_CATEGORY,
            parentName = title.trim(),
            ledgerSide = nature.storageKey,
            openingBalance = openingBalance,
            priority = priority,
            isQuickSelect = isStarred,
            updatedAt = System.currentTimeMillis()
        ).let { if (syncId.isNotBlank()) it.copy(syncId = syncId) else it }

    companion object {
        fun fromSubAccount(sub: PersonalSubAccount): PersonProfile {
            val parts = sub.name.trim().split(Regex("\\s+"), limit = 2)
            return PersonProfile(
                id = sub.id,
                firstName = parts.getOrNull(0).orEmpty(),
                lastName = parts.getOrNull(1).orEmpty(),
                title = sub.parentName.trim(),
                nature = PersonNature.fromStorage(sub.ledgerSide),
                openingBalance = sub.openingBalance,
                priority = sub.priority,
                isStarred = sub.isQuickSelect,
                syncId = sub.syncId
            )
        }

        /** ساخت پرونده از یک نام خام (برای اشخاص قدیمیِ ذخیره‌شده در childrenNames). */
        fun fromLegacyName(name: String): PersonProfile {
            val parts = name.trim().split(Regex("\\s+"), limit = 2)
            return PersonProfile(
                firstName = parts.getOrNull(0).orEmpty(),
                lastName = parts.getOrNull(1).orEmpty()
            )
        }
    }
}
