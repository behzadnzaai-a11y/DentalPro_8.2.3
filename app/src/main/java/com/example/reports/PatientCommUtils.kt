package com.example.reports

import android.content.Context
import com.example.ui.screens.honorificPatientName
import java.io.File

/** One line item shown on a statement or treatment-plan card. */
data class SummaryLineItem(val title: String, val subtitle: String, val amountText: String)

// کارت‌های اشتراک‌گذاریِ بیمار/تأمین‌کننده. مثلِ بقیهٔ خروجی‌ها بدونِ منطقِ رندر:
// فقط UnifiedDocument می‌سازند و generateUnifiedImage همه‌چیز را رندر می‌کند.
// themeSpec پیش‌فرض ندارد تا پاس‌ندادنش خطای کامپایل بدهد.

/**
 * Renders a branded, shareable PNG card — used when sending a patient
 * statement or treatment plan over WhatsApp (image beats a wall of text
 * inside a chat). Colors come from the currently active neon theme so the
 * image matches the app's look.
 */
fun buildSummaryImage(
    context: Context,
    cardTitle: String,
    clinicName: String,
    doctorName: String,
    patientName: String,
    items: List<SummaryLineItem>,
    footerLines: List<Pair<String, String>>, // label to value, last one is emphasized total
    themeSpec: ExportThemeSpec,
    fileNamePrefix: String,
    gender: String = "مرد"
): File {
    val formattedPatient = honorificPatientName(patientName, gender)
    val columns = listOf(
        UnifiedImageGenerator.TableColumn("شرح", 3.1f),
        UnifiedImageGenerator.TableColumn("جزئیات", 1.9f, center = true),
        UnifiedImageGenerator.TableColumn("مبلغ", 2.0f)
    )
    val rows = items.map { listOf(it.title, it.subtitle, it.amountText) }
    val footRows = footerLines.mapIndexed { index, pair ->
        UnifiedImageGenerator.KeyRow(
            label = pair.first,
            value = pair.second,
            emphasize = index == footerLines.lastIndex,
            monospace = index == footerLines.lastIndex || pair.second.any { c -> c.isDigit() }
        )
    }
    return UnifiedImageGenerator.generateUnifiedImage(
        context = context,
        document = UnifiedImageGenerator.UnifiedDocument(
            kind = ExportDocKind.StatementCard,
            title = cardTitle,
            fileNamePrefix = fileNamePrefix,
            tone = UnifiedImageGenerator.DocTone.NeonDark,
            clinicName = clinicName,
            doctorName = doctorName,
            metaRight = "مراجع عزیز: $formattedPatient",
            blocks = listOfNotNull(
                if (rows.isEmpty()) null else UnifiedImageGenerator.DocBlock.Table(columns, rows),
                footRows.takeIf { it.isNotEmpty() }?.let { UnifiedImageGenerator.DocBlock.KeyValues(it) }
            )
        ),
        theme = themeSpec
    )
}

/** Short SMS-safe Persian summary of a patient's account balance. */
fun buildStatementSmsText(
    clinicName: String,
    patientName: String,
    currency: String,
    totalCostText: String,
    totalPaidText: String,
    balanceText: String,
    isDebtor: Boolean,
    gender: String = "مرد"
): String {
    val formattedPatient = honorificPatientName(patientName, gender)
    val balanceLine = if (isDebtor) "مانده بدهی: $balanceText $currency" else "مانده بستانکاری: $balanceText $currency"
    return "$clinicName\nصورت‌حساب مراجع عزیز: $formattedPatient\nجمع درمان: $totalCostText $currency\nجمع پرداختی: $totalPaidText $currency\n$balanceLine"
}

/** Short SMS-safe Persian summary of a proposed treatment plan and price. */
fun buildTreatmentPlanSmsText(
    clinicName: String,
    patientName: String,
    currency: String,
    treatmentTitles: List<String>,
    totalPriceText: String,
    gender: String = "مرد"
): String {
    val formattedPatient = honorificPatientName(patientName, gender)
    val itemsText = if (treatmentTitles.size <= 4) {
        treatmentTitles.joinToString("، ")
    } else {
        treatmentTitles.take(4).joinToString("، ") + " و ${treatmentTitles.size - 4} مورد دیگر"
    }
    return "$clinicName\nطرح درمان پیشنهادی برای مراجع عزیز: $formattedPatient\n$itemsText\nهزینه کل تخمینی: $totalPriceText $currency"
}

/**
 * کارت تصویر باکیفیت «فاکتور رسمی سفارش لابراتوار» برای ارسال مستقیم در چت.
 * جدول‌گونه و پهن ⇒ [ExportDocKind.LabInvoice].
 */
fun buildOfficialLabInvoiceImage(
    context: Context,
    clinicName: String,
    doctorName: String,
    labName: String,
    patientName: String,
    invoiceNo: String,
    prosthesisType: String,
    toothNumber: String,
    @Suppress("UNUSED_PARAMETER") teethCount: Int = 1,
    unitPriceText: String,
    totalCostText: String,
    orderDateText: String,
    frameDateText: String,
    finalDateText: String,
    paymentMethodText: String,
    themeSpec: ExportThemeSpec,
    gender: String = "مرد"
): File {
    // «مراجع» فقط وقتی پیشوند آقای/خانم می‌گیرد که واقعاً یک شخص باشد. در
    // فاکتورِ طرف‌حسابِ هزینه، این فیلد نام لابراتوار/فروشگاه است و «آقای
    // لابراتوار نیازی» غلط بود.
    val formattedPatient = if (patientName.isBlank() || patientName.trim() == labName.trim()) {
        ""
    } else {
        honorificPatientName(patientName, gender)
    }
    val rows = mutableListOf(
        UnifiedImageGenerator.KeyRow("لابراتوار سازنده", labName, emphasize = true)
    )
    if (formattedPatient.isNotBlank()) {
        rows += UnifiedImageGenerator.KeyRow("مراجع / بیمار", formattedPatient)
    }
    rows += UnifiedImageGenerator.KeyRow("نوع پروتز / سفارش", prosthesisType, emphasize = true)
    if (toothNumber.isNotBlank() && toothNumber != "—") {
        rows += UnifiedImageGenerator.KeyRow("شماره دندان(ها)", toothNumber, monospace = true)
    }
    // ردیف «تعداد واحدهای ساخت» طبق درخواست کاربر حذف شد.
    if (unitPriceText.isNotBlank() && unitPriceText != totalCostText) {
        rows += UnifiedImageGenerator.KeyRow("مبلغ هر واحد", unitPriceText, monospace = true)
    }
    rows += UnifiedImageGenerator.KeyRow("مبلغ کل سفارش", totalCostText, emphasize = true, monospace = true)
    rows += UnifiedImageGenerator.KeyRow("تاریخ ثبت سفارش", orderDateText, monospace = true)
    if (frameDateText.isNotBlank()) {
        rows += UnifiedImageGenerator.KeyRow("تاریخ امتحان فریم", frameDateText, monospace = true)
    }
    rows += UnifiedImageGenerator.KeyRow("تاریخ تحویل نهایی", finalDateText, monospace = true)
    rows += UnifiedImageGenerator.KeyRow("تسویه مالی", paymentMethodText)
    return UnifiedImageGenerator.generateUnifiedImage(
        context = context,
        document = UnifiedImageGenerator.UnifiedDocument(
            kind = ExportDocKind.LabInvoice,
            title = "فاکتور سفارش لابراتوار",
            fileNamePrefix = "lab_invoice",
            tone = UnifiedImageGenerator.DocTone.NeonDark,
            clinicName = clinicName,
            doctorName = doctorName,
            metaRight = "بند حساب: $labName",
            metaLeft = "شماره فاکتور: $invoiceNo",
            blocks = listOf(UnifiedImageGenerator.DocBlock.KeyValues(rows)),
            footNote = "این فاکتور در سامانهٔ دنتال‌پرو ثبت و بایگانی شده است."
        ),
        theme = themeSpec
    )
}

/**
 * صورت‌حساب رسمی طرف‌حساب/تأمین‌کننده (دفتر هزینه و پرداخت).
 * [ExportDocKind.VendorLedger] با عنوان وسط‌چین و ردیف‌های رنگی معنایی.
 */
fun buildVendorStatementImage(
    context: Context,
    clinicName: String,
    partyName: String,
    expenses: List<com.example.data.ClinicExpense>,
    payments: List<com.example.data.ExpensePayment>,
    totalDebt: Long,
    totalPaid: Long,
    remaining: Long,
    themeSpec: ExportThemeSpec
): File {
    val allItems = (expenses.map { Pair(it.date, it as Any) } + payments.map { Pair(it.date, it as Any) })
        .sortedBy { it.first }
    val columns = listOf(
        UnifiedImageGenerator.TableColumn("تاریخ", 1.3f, center = true),
        UnifiedImageGenerator.TableColumn("شرح سفارش / پرداخت", 3.6f),
        UnifiedImageGenerator.TableColumn("مبلغ (تومان)", 1.6f)
    )
    val rows = allItems.map { (date, item) ->
        val jDate = com.example.util.PersianCalendarHelper.timestampToJalali(date)
        val dateText = String.format("%04d/%02d/%02d", jDate.year, jDate.month, jDate.day)
        val desc = when (item) {
            is com.example.data.ClinicExpense -> item.description.ifBlank { "هزینه / خرید" }.trim()
            is com.example.data.ExpensePayment ->
                if (item.note.isNotBlank()) "پرداختی: ${item.note.trim()}" else "پرداختی (${item.method})"
            else -> ""
        }
        val amountText = when (item) {
            is com.example.data.ClinicExpense -> com.example.ui.screens.formatAmount(item.amount)
            is com.example.data.ExpensePayment -> com.example.ui.screens.formatAmount(item.amount)
            else -> ""
        }
        listOf(dateText, desc, amountText)
    }
    val rowEmphasis = allItems.map { (_, item) ->
        when (item) {
            is com.example.data.ClinicExpense -> UnifiedImageGenerator.Emphasis.Danger
            is com.example.data.ExpensePayment -> UnifiedImageGenerator.Emphasis.Success
            else -> UnifiedImageGenerator.Emphasis.Body
        }
    }
    // مانده علامت‌دار: منفی یعنی بیش از فاکتورها پرداخت شده و در آن حالت
    // **طرف‌حساب به ما بدهکار است**، نه برعکس.
    val isPartyDebtor = remaining < 0L
    val balanceLabel = when {
        isPartyDebtor -> "بدهی $partyName به ما"
        remaining > 0L -> "مانده بدهی نهایی"
        else -> "مانده نهایی (تسویه)"
    }
    val summaryRows = listOf(
        UnifiedImageGenerator.KeyRow("جمع فاکتورها / هزینه‌ها", "${com.example.ui.screens.formatAmount(totalDebt)} تومان", monospace = true),
        UnifiedImageGenerator.KeyRow("جمع کل پرداختی‌ها", "${com.example.ui.screens.formatAmount(totalPaid)} تومان", monospace = true),
        UnifiedImageGenerator.KeyRow(
            balanceLabel,
            "${com.example.ui.screens.formatAmount(kotlin.math.abs(remaining))} تومان",
            emphasize = true,
            monospace = true
        )
    )
    return UnifiedImageGenerator.generateUnifiedImage(
        context = context,
        document = UnifiedImageGenerator.UnifiedDocument(
            kind = ExportDocKind.VendorLedger,
            title = "صورتحساب ($partyName)",
            fileNamePrefix = "vendor_statement",
            tone = UnifiedImageGenerator.DocTone.NeonDark,
            subtitle = "تاریخ صدور: " + com.example.ui.components.formatJalaliBannerDate(isPersian = true),
            titleAlign = UnifiedImageGenerator.TitleAlign.Center,
            blocks = listOf(
                UnifiedImageGenerator.DocBlock.Table(columns, rows, rowEmphasis),
                UnifiedImageGenerator.DocBlock.KeyValues(summaryRows)
            ),
            footNote = when {
                isPartyDebtor ->
                    "$partyName بابت این حساب ${com.example.ui.screens.formatAmount(-remaining)} تومان به ما بدهکار است " +
                        "(پرداختی بیش از مبلغ فاکتورها بوده است)."
                remaining > 0L -> "ماندهٔ قابل مطالبه: ${com.example.ui.screens.formatAmount(remaining)} تومان"
                else -> "حساب این طرف‌حساب تسویه است."
            }
        ),
        theme = themeSpec
    )
}
