package com.example.reports

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

private const val MAX_SHARE_FILE_BYTES = 100L * 1024L * 1024L

/**
 * Stages only the requested file in the narrow FileProvider share directory. This avoids
 * exposing the whole cache directory while retaining short-lived URI grants for recipients.
 */
fun fileProviderUri(context: Context, file: File): Uri {
    require(file.isFile && file.canRead()) { "Share source does not exist or is unreadable" }
    require(file.length() in 1..MAX_SHARE_FILE_BYTES) { "Share source has an invalid size" }

    val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
    val shareRoot = shareDir.canonicalFile
    val source = file.canonicalFile
    val staged = if (source.parentFile == shareRoot) {
        source
    } else {
        shareDir.listFiles()
            ?.filter { System.currentTimeMillis() - it.lastModified() > 24 * 60 * 60 * 1000L }
            ?.forEach(File::delete)
        val safeExtension = source.extension
            .lowercase()
            .filter(Char::isLetterOrDigit)
            .take(10)
        val targetName = buildString {
            append("share-")
            append(UUID.randomUUID())
            if (safeExtension.isNotEmpty()) append('.').append(safeExtension)
        }
        File(shareRoot, targetName).also { target ->
            source.inputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    check(staged.canonicalPath.startsWith("${shareRoot.path}${File.separator}")) {
        "Refusing to share a file outside the staging directory"
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", staged)
}

/** Opens the system print dialog for an already-generated PDF file. */
fun printPdfFile(context: Context, file: File, jobName: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager == null) {
        Toast.makeText(context, "سرویس چاپ در این دستگاه در دسترس نیست", Toast.LENGTH_SHORT).show()
        return
    }
    printManager.print(jobName, PdfPrintDocumentAdapter(file), PrintAttributes.Builder().build())
}

private class PdfPrintDocumentAdapter(private val file: File) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination?.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        }
    }
}

/** Generic share sheet for a PDF file (any app that accepts documents). */
fun sharePdf(context: Context, file: File, chooserTitle: String = "اشتراک‌گذاری سند") {
    val uri = fileProviderUri(context, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

fun normalizePhoneForJid(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    return when {
        digits.startsWith("0098") -> digits.removePrefix("00")
        digits.startsWith("98") -> digits
        digits.startsWith("0") -> "98${digits.removePrefix("0")}"
        else -> digits
    }
}

/** Shares a PDF via Android native system chooser (universal sharing to any app). */
fun sharePdfToWhatsApp(context: Context, file: File, phone: String? = null) {
    sharePdf(context, file, "اشتراک‌گذاری گزارش PDF")
}

/** Shares a rendered summary image (PNG) via Android native system chooser. */
fun shareImageToWhatsApp(context: Context, imageFile: File, phone: String? = null) {
    shareImageNativeChooser(context, imageFile, null, "اشتراک‌گذاری تصویر")
}

/** Shares all rendered PDF pages. URI grants are limited to explicitly staged files. */
fun shareImagesNativeChooser(
    context: Context,
    imageFiles: List<File>,
    chooserTitle: String = "اشتراک‌گذاری تصاویر"
) {
    require(imageFiles.isNotEmpty()) { "No images were generated" }
    val uris = ArrayList(imageFiles.map { fileProviderUri(context, it) })
    val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/png"
        if (uris.size == 1) {
            putExtra(Intent.EXTRA_STREAM, uris.first())
        } else {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
        clipData = ClipData.newUri(context.contentResolver, "report-page-1", uris.first()).also { clip ->
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "برنامه‌ای برای اشتراک‌گذاری یافت نشد", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens standard sharing or dialer for the given phone number using Android native intents.
 */
fun openWhatsAppChat(context: Context, rawPhone: String) {
    val digits = rawPhone.filter { it.isDigit() }
    val normalized = when {
        digits.startsWith("0098") -> "+98${digits.removePrefix("0098")}"
        digits.startsWith("98") -> "+98${digits.removePrefix("98")}"
        digits.startsWith("0") -> "+98${digits.removePrefix("0")}"
        digits.isNotEmpty() -> "+98$digits"
        else -> ""
    }
    if (normalized.isBlank()) {
        Toast.makeText(context, "شماره تلفن معتبر نیست", Toast.LENGTH_SHORT).show()
        return
    }
    shareTextNativeChooser(context, normalized, "ارسال یا تماس با شماره")
}

/**
 * Generic "share as text" via the system chooser — lists every installed app
 * that accepts plain text (Telegram, Bale, Rubika, email, etc.) without
 * needing a dedicated integration per app.
 */
fun shareTextNativeChooser(context: Context, text: String, chooserTitle: String = "اشتراک‌گذاری") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "برنامه‌ای برای اشتراک‌گذاری یافت نشد", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Generic "share as image" via the system chooser — lists every installed
 * app that accepts images (Telegram, Bale, Rubika, WhatsApp, etc.).
 */
fun shareImageNativeChooser(context: Context, imageFile: File, caption: String? = null, chooserTitle: String = "اشتراک‌گذاری تصویر") {
    val uri = fileProviderUri(context, imageFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        if (!caption.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "برنامه‌ای برای اشتراک‌گذاری یافت نشد", Toast.LENGTH_SHORT).show()
    }
}

// کارت‌ها و اسنادِ تصویری — همه از یک رندرر. این توابع فقط محتوا را به‌صورت
// [UnifiedImageGenerator.UnifiedDocument] توصیف می‌کنند؛ رندر (عرض/حاشیه/فونت/
// رنگِ تم/ارتفاع/ذخیره) داخلِ generateUnifiedImage انجام می‌شود. عرض از
// ExportDocKind می‌آید و themeSpec پیش‌فرض ندارد تا نبودِ تم خطای کامپایل بدهد
// (باگِ قبلی: همه‌چیز طلایی رندر می‌شد). مقیاسِ فونتِ کاربر داخلِ ExportThemeSpec است.

/** سطرهای جدول استاندارد نسخهٔ دارویی (همان ۵ ستون رسمی). */
data class RxColumnRowData(
    val title: String,
    val quantity: String,
    val dosage: String,
    val timing: String,
    val instructions: String
)

/**
 * کارت سادهٔ اطلاع‌رسانی (یادآوری نوبت، پیامک تبدیل‌شده به تصویر).
 * مناسب پیش‌نمایش چت ⇒ [ExportDocKind.InfoCard].
 */
fun generateSimpleInfoCardImage(
    context: Context,
    title: String,
    lines: List<String>,
    signatureB64: String? = null,
    themeSpec: ExportThemeSpec
): File = UnifiedImageGenerator.generateUnifiedImage(
    context = context,
    document = UnifiedImageGenerator.UnifiedDocument(
        kind = ExportDocKind.InfoCard,
        title = title,
        fileNamePrefix = "info_card",
        tone = UnifiedImageGenerator.DocTone.NeonDark,
        blocks = listOfNotNull(
            lines.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let {
                UnifiedImageGenerator.DocBlock.Lines(it, UnifiedImageGenerator.Emphasis.Body)
            },
            signatureB64?.takeIf { it.isNotBlank() }?.let {
                UnifiedImageGenerator.DocBlock.SignatureBlock(signatureBase64 = it)
            }
        )
    ),
    theme = themeSpec
)

/**
 * کارت نسخهٔ دارویی رسمی با جدول ۵ ستونه (عنوان | تعداد | مقادیر مصرف |
 * زمان مصرف | دستورات متفرقه). برای این جدول عرض پهن‌تر لازم است تا خانه‌ها
 * نشکنند ⇒ [ExportDocKind.Prescription].
 */
fun generateStandardPrescriptionCardImage(
    context: Context,
    patientName: String,
    patientNationalIdOrPhone: String,
    dateStr: String,
    rows: List<RxColumnRowData>,
    notes: String,
    clinicName: String,
    doctorName: String,
    themeSpec: ExportThemeSpec
): File {
    val columns = listOf(
        UnifiedImageGenerator.TableColumn("عنوان", 3.8f),
        UnifiedImageGenerator.TableColumn("تعداد", 1.3f, center = true),
        UnifiedImageGenerator.TableColumn("مقادیر مصرف", 1.9f, center = true),
        UnifiedImageGenerator.TableColumn("زمان مصرف", 2.0f, center = true),
        UnifiedImageGenerator.TableColumn("دستورات متفرقه", 2.0f)
    )
    val tableRows = rows.map { listOf(it.title, it.quantity, it.dosage, it.timing, it.instructions) }
    val hasNotes = notes.isNotBlank()
    return UnifiedImageGenerator.generateUnifiedImage(
        context = context,
        document = UnifiedImageGenerator.UnifiedDocument(
            kind = ExportDocKind.Prescription,
            title = "نسخه دارویی الکترونیک",
            fileNamePrefix = "prescription_card",
            tone = UnifiedImageGenerator.DocTone.PaperLight,
            clinicName = clinicName,
            doctorName = doctorName,
            metaRight = "نام و نام خانوادگی مراجع: $patientName",
            metaLeft = "شماره پرونده: ${patientNationalIdOrPhone.ifBlank { "-" }}\nتاریخ نسخه: $dateStr",
            blocks = listOfNotNull(
                if (tableRows.isEmpty()) null else UnifiedImageGenerator.DocBlock.Table(columns, tableRows),
                if (hasNotes) UnifiedImageGenerator.DocBlock.Section("دستورات متفرقه و نکات مهم") else null,
                if (hasNotes) UnifiedImageGenerator.DocBlock.Paragraph(notes, UnifiedImageGenerator.Emphasis.Body, boxed = true) else null
            ),
            showBrand = false
        ),
        theme = themeSpec
    )
}

/**
 * فرم پذیرش و تاریخچه پزشکی (با سوابق مثبت و امضای بیمار). سند چاپی ⇒
 * [ExportDocKind.AdmissionForm] با tone کاغذی روشن.
 */
fun generateAdmissionFormCardImage(
    context: Context,
    patientName: String,
    patientPhone: String,
    patientAge: String,
    patientNationalId: String,
    admissionDateStr: String,
    positiveMedicalHistory: List<String>,
    chiefComplaint: String,
    signatureBase64: String?,
    clinicName: String,
    themeSpec: ExportThemeSpec
): File {
    val hasHistory = positiveMedicalHistory.isNotEmpty()
    val historyBlock: UnifiedImageGenerator.DocBlock = if (hasHistory) {
        UnifiedImageGenerator.DocBlock.Lines(
            positiveMedicalHistory.filter { it.isNotBlank() }.map { "• $it" },
            UnifiedImageGenerator.Emphasis.Warning
        )
    } else {
        UnifiedImageGenerator.DocBlock.Paragraph(
            "فاقد بیماری زمینه‌ای خاص، بر اساس اظهارات بیمار",
            UnifiedImageGenerator.Emphasis.Success
        )
    }
    return UnifiedImageGenerator.generateUnifiedImage(
        context = context,
        document = UnifiedImageGenerator.UnifiedDocument(
            kind = ExportDocKind.AdmissionForm,
            title = "فرم رسمی پذیرش و تاریخچه پزشکی بیمار",
            fileNamePrefix = "admission_form",
            tone = UnifiedImageGenerator.DocTone.PaperLight,
            subtitle = "$clinicName | تاریخ پذیرش: $admissionDateStr",
            blocks = listOf(
                UnifiedImageGenerator.DocBlock.KeyValues(
                    listOf(
                        UnifiedImageGenerator.KeyRow("نام بیمار", patientName),
                        UnifiedImageGenerator.KeyRow("شماره تماس", patientPhone.ifBlank { "ثبت‌نشده" }, monospace = true),
                        UnifiedImageGenerator.KeyRow("کد ملی", patientNationalId.ifBlank { "ثبت‌نشده" }, monospace = true),
                        UnifiedImageGenerator.KeyRow("سن", patientAge.ifBlank { "ثبت‌نشده" }, monospace = true)
                    )
                ),
                UnifiedImageGenerator.DocBlock.Section("سوابق پزشکی و هشدارهای سلامتی (موارد مثبت)", UnifiedImageGenerator.Emphasis.Warning),
                historyBlock,
                UnifiedImageGenerator.DocBlock.Section("شکایت اصلی و ملاحظات اولیه"),
                UnifiedImageGenerator.DocBlock.Paragraph(
                    chiefComplaint.ifBlank { "جهت معاینه و چکاپ دوره‌ای دندان‌پزشکی" },
                    UnifiedImageGenerator.Emphasis.Body
                ),
                UnifiedImageGenerator.DocBlock.SignatureBlock(
                    signatureBase64 = signatureBase64,
                    legalNote = "اینجانب $patientName صحت اطلاعات سوابق پزشکی فوق را تأیید نموده و موافقت کامل خود را با انجام معاینات و پروتکل‌های درمانی اعلام می‌دارم.",
                    stampLabel = "امضای دیجیتال تأییدشده بیمار"
                )
            ),
            showBrand = false
        ),
        theme = themeSpec
    )
}

/**
 * سند رسمی رضایت‌نامهٔ آگاهانه و برائت‌نامه (قابل ارائه به مراجع قانونی).
 * مطابق مواد ۴۹۵ و ۴۹۷ قانون مجازات اسلامی و مواد ۷ و ۱۰ قانون تجارت الکترونیک.
 */
fun generateLegalConsentFormDocument(
    context: Context,
    patientName: String,
    patientNationalId: String = "",
    patientFatherName: String = "",
    patientPhone: String = "",
    patientAge: String = "",
    signerRole: String = "خود بیمار",
    doctorName: String = "",
    doctorMedicalNo: String = "",
    clinicName: String = "",
    consentTitle: String = "رضایت‌نامه آگاهانه و برائت‌نامه درمان دندان‌پزشکی",
    legalText: String = "",
    signatureBase64: String? = null,
    timestamp: Long = System.currentTimeMillis(),
    themeSpec: ExportThemeSpec
): File {
    val legalCode = com.example.data.LegalConsentKnowledgeBase.generateLegalVerificationCode(
        patientNationalId.ifBlank { patientName },
        timestamp
    )
    val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd - HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    val fatherInfo = if (patientFatherName.isNotBlank()) " | نام پدر: $patientFatherName" else ""
    val docStr = doctorName.ifBlank { "پزشک معالج" }
    val medStr = doctorMedicalNo.ifBlank { "ثبت‌شده" }
    val clStr = clinicName.ifBlank { "کلینیک دندان‌پزشکی" }
    val bodyText = legalText.ifBlank {
        "اینجانب با آگاهی کامل از تمامی عوارض، خطرات و هزینه‌های درمان، موافقت و برائت خود را اعلام می‌دارم."
    }
    return UnifiedImageGenerator.generateUnifiedImage(
        context = context,
        document = UnifiedImageGenerator.UnifiedDocument(
            kind = ExportDocKind.LegalConsent,
            title = "فرم رسمی رضایت‌نامه آگاهانه و برائت‌نامه پزشکی",
            fileNamePrefix = "legal_consent_document",
            tone = UnifiedImageGenerator.DocTone.PaperLight,
            subtitle = "استناد به مواد ۴۹۵ و ۴۹۷ قانون مجازات اسلامی و مواد ۷ و ۱۰ قانون تجارت الکترونیک",
            metaRight = "کد تأییدیه حقوقی: $legalCode",
            metaLeft = "تاریخ و زمان ثبت: $dateStr",
            blocks = listOf(
                UnifiedImageGenerator.DocBlock.InfoColumns(
                    right = UnifiedImageGenerator.InfoColumn(
                        "مشخصات امضاکننده / بیمار",
                        listOf(
                            "نام: $patientName$fatherInfo",
                            "کد ملی: ${patientNationalId.ifBlank { "-" }}",
                            "تماس: ${patientPhone.ifBlank { "-" }} | سن: ${patientAge.ifBlank { "-" }}",
                            "سمت: $signerRole"
                        )
                    ),
                    left = UnifiedImageGenerator.InfoColumn(
                        "پزشک معالج و مرکز درمانی",
                        listOf(
                            "پزشک: $docStr",
                            "نظام پزشکی: $medStr",
                            "مرکز: $clStr"
                        )
                    )
                ),
                UnifiedImageGenerator.DocBlock.Section("موضوع رضایت‌نامه: $consentTitle"),
                UnifiedImageGenerator.DocBlock.Paragraph(bodyText, UnifiedImageGenerator.Emphasis.Body, boxed = true),
                UnifiedImageGenerator.DocBlock.Callout(
                    "اینجانب $patientName (کد ملی: ${patientNationalId.ifBlank { "ثبت‌شده" }}) بدین‌وسیله اقرار می‌نمایم که متن فوق را با دقت مطالعه نموده و برائت صریح پزشک معالج را صادر نموده‌ام.",
                    UnifiedImageGenerator.CalloutTone.Info
                ),
                UnifiedImageGenerator.DocBlock.VerificationTrio(
                    fingerprintNote = "اثر انگشت سبابه دست راست (بیمار / ولی قانونی)",
                    signatureNote = "امضای دیجیتال / لمسی — تأییدشده (ماده ۷ تجارت الکترونیک)",
                    stampTitle = "مهر و امضای پزشک معالج",
                    stampSubtitle = "$docStr\nنظام پزشکی: $medStr",
                    signatureBase64 = signatureBase64
                ),
                UnifiedImageGenerator.DocBlock.Banner(
                    "سند الکترونیکی غیرقابل‌انکار — رمزنگاری‌شده مطابق مواد ۷ و ۱۰ قانون تجارت الکترونیک ج.ا.ایران",
                    "کد پیگیری حقوقی سند: $legalCode | بایگانی الکترونیکی کلینیک"
                )
            ),
            showBrand = false
        ),
        theme = themeSpec
    )
}

/**
 * راهنمای مراقبت‌های بالینی پس از درمان (توضیحات ترخیص).
 * [ExportDocKind.PostOpCare] — کاغذ A4 تک‌ستونه.
 */
fun generatePostOpCareInstructionsCardImage(
    context: Context,
    clinicName: String,
    doctorName: String,
    patientName: String,
    patientFatherName: String = "",
    patientNationalId: String = "",
    treatmentTitle: String,
    careInstructions: List<String>,
    emergencyPhone: String = "",
    themeSpec: ExportThemeSpec
): File {
    val fatherStr = if (patientFatherName.isNotBlank()) " | نام پدر: $patientFatherName" else ""
    val emergencyText = buildString {
        append("در صورت بروز درد شدید، خونریزی غیرقابل‌کنترل یا تورم ناگهانی، سریعاً با مطب تماس بگیرید.")
        if (emergencyPhone.isNotBlank()) append("\nتلفن تماس اضطراری: ").append(emergencyPhone)
    }
    return UnifiedImageGenerator.generateUnifiedImage(
        context = context,
        document = UnifiedImageGenerator.UnifiedDocument(
            kind = ExportDocKind.PostOpCare,
            title = "راهنمای مراقبت‌های بالینی پس از درمان",
            fileNamePrefix = "post_op_instructions",
            tone = UnifiedImageGenerator.DocTone.PaperLight,
            subtitle = "$clinicName | اقدام درمانی: $treatmentTitle",
            doctorName = doctorName.takeIf { it.isNotBlank() },
            metaRight = "مراجع عزیز: $patientName$fatherStr",
            metaLeft = "کد ملی: ${patientNationalId.ifBlank { "-" }}",
            blocks = listOfNotNull(
                UnifiedImageGenerator.DocBlock.Section("دستورالعمل‌های مراقبتی"),
                careInstructions.filter { it.isNotBlank() }
                    .takeIf { it.isNotEmpty() }
                    ?.let { UnifiedImageGenerator.DocBlock.Lines(it.map { line -> "• $line" }, UnifiedImageGenerator.Emphasis.Body) },
                UnifiedImageGenerator.DocBlock.Callout(
                    emergencyText,
                    UnifiedImageGenerator.CalloutTone.Warning,
                    title = "هشدارهای فوری"
                )
            ),
            showBrand = false
        ),
        theme = themeSpec
    )
}

/** بلوک اطلاعات واریز (شماره کارت/بانک/صاحب‌حساب) به‌صورت ردیف‌های مونواسپیس. */
private fun bankTransferBlock(
    bankCardNumber: String,
    bankName: String,
    accountHolder: String,
    clinicName: String,
    fallbackText: String
): UnifiedImageGenerator.DocBlock {
    if (bankCardNumber.isBlank()) {
        return UnifiedImageGenerator.DocBlock.Callout(fallbackText, UnifiedImageGenerator.CalloutTone.Info)
    }
    return UnifiedImageGenerator.DocBlock.KeyValues(
        listOf(
            UnifiedImageGenerator.KeyRow("شماره کارت جهت واریز", bankCardNumber, emphasize = true, monospace = true),
            UnifiedImageGenerator.KeyRow("بانک", bankName.ifBlank { "—" }, monospace = true),
            UnifiedImageGenerator.KeyRow("صاحب حساب", accountHolder.ifBlank { clinicName })
        )
    )
}

/**
 * یادآوری سررسید قسط. [ExportDocKind.InstallmentNotice].
 */
fun generateInstallmentDueReminderCardImage(
    context: Context,
    clinicName: String,
    patientName: String,
    patientFatherName: String = "",
    patientNationalId: String = "",
    installmentTitle: String,
    dueDateStr: String,
    amountStr: String,
    bankCardNumber: String = "",
    bankName: String = "",
    accountHolder: String = "",
    themeSpec: ExportThemeSpec
): File {
    val fatherStr = if (patientFatherName.isNotBlank()) " (فرزند $patientFatherName)" else ""
    return UnifiedImageGenerator.generateUnifiedImage(
        context = context,
        document = UnifiedImageGenerator.UnifiedDocument(
            kind = ExportDocKind.InstallmentNotice,
            title = "یادآوری سررسید قسط دندان‌پزشکی",
            fileNamePrefix = "installment_due",
            tone = UnifiedImageGenerator.DocTone.PaperLight,
            subtitle = "$clinicName | اطلاعیه پرداخت سررسید",
            metaRight = "مراجع گرامی: $patientName$fatherStr",
            metaLeft = "کد ملی: ${patientNationalId.ifBlank { "-" }}",
            blocks = listOf(
                UnifiedImageGenerator.DocBlock.KeyValues(
                    listOf(
                        UnifiedImageGenerator.KeyRow("عنوان قسط", installmentTitle),
                        UnifiedImageGenerator.KeyRow("تاریخ سررسید", dueDateStr, monospace = true),
                        UnifiedImageGenerator.KeyRow("مبلغ قسط", amountStr, emphasize = true, monospace = true)
                    )
                ),
                bankTransferBlock(
                    bankCardNumber = bankCardNumber,
                    bankName = bankName,
                    accountHolder = accountHolder,
                    clinicName = clinicName,
                    fallbackText = "لطفاً جهت تسویه قسط به پذیرش کلینیک مراجعه فرمایید."
                )
            ),
            footNote = "با تشکر از همکاری شما — $clinicName"
        ),
        theme = themeSpec
    )
}

/**
 * اخطار تأخیر در وصول اقساط — همان رندرر، با tone هشداری ([ExportDocKind] همان،
 * فقط رنگ سربرگ معنایی خطر می‌شود).
 */
fun generateOverdueInstallmentNoticeCardImage(
    context: Context,
    clinicName: String,
    patientName: String,
    patientFatherName: String = "",
    patientNationalId: String = "",
    installmentTitle: String,
    dueDateStr: String,
    delayDays: Int,
    amountStr: String,
    bankCardNumber: String = "",
    bankName: String = "",
    accountHolder: String = "",
    themeSpec: ExportThemeSpec
): File {
    val fatherStr = if (patientFatherName.isNotBlank()) " (فرزند $patientFatherName)" else ""
    return UnifiedImageGenerator.generateUnifiedImage(
        context = context,
        document = UnifiedImageGenerator.UnifiedDocument(
            kind = ExportDocKind.InstallmentNotice,
            title = "اخطار و یادآوری تأخیر وصول اقساط",
            fileNamePrefix = "overdue_installment",
            tone = UnifiedImageGenerator.DocTone.PaperAlert,
            subtitle = "$clinicName | اطلاعیه سررسید گذشته",
            metaRight = "مراجع محترم: $patientName$fatherStr",
            metaLeft = "کد ملی: ${patientNationalId.ifBlank { "-" }}",
            blocks = listOf(
                UnifiedImageGenerator.DocBlock.Callout(
                    "عنوان قسط: $installmentTitle\nتاریخ سررسید: $dueDateStr | میزان تأخیر: $delayDays روز\nمبلغ قابل تسویه: $amountStr\nخواهشمند است جهت جلوگیری از معوق شدن حساب، نسبت به واریز اقدام فرمایید.",
                    UnifiedImageGenerator.CalloutTone.Danger,
                    title = "حساب این مراجع معوق است"
                ),
                bankTransferBlock(
                    bankCardNumber = bankCardNumber,
                    bankName = bankName,
                    accountHolder = accountHolder,
                    clinicName = clinicName,
                    fallbackText = "لطفاً سریعاً جهت تسویه حساب به بخش حسابداری کلینیک مراجعه فرمایید."
                )
            ),
            footNote = "این پیام آخرین اطلاعیهٔ کلینیک پیش از ارجاع حساب است."
        ),
        theme = themeSpec
    )
}

/**
 * Opens the default SMS app with the recipient and a pre-filled short text
 * summary — the user reviews and taps send themselves. Deliberately does
 * NOT send silently (no SEND_SMS permission requested), which keeps this
 * safe for a healthcare app and avoids restricted-permission Play policy.
 */
fun sendSmsPrefilled(context: Context, phone: String, message: String) {
    val digits = phone.filter { it.isDigit() }
    val normalizedPhone = when {
        digits.startsWith("0098") -> "+98${digits.removePrefix("0098")}"
        digits.startsWith("98") -> "+98${digits.removePrefix("98")}"
        digits.startsWith("0") -> "+98${digits.removePrefix("0")}"
        digits.isNotEmpty() -> "+98$digits"
        else -> phone
    }

    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$normalizedPhone")).apply {
        putExtra("sms_body", message)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "برنامه پیامک روی این دستگاه یافت نشد", Toast.LENGTH_SHORT).show()
    }
}
