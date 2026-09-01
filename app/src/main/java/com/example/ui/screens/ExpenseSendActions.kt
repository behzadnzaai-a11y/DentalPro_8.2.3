package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ClinicExpense
import com.example.data.ExpensePayment
import com.example.reports.buildVendorStatementImage
import com.example.reports.buildOfficialLabInvoiceImage
import com.example.reports.shareImageToWhatsApp
import com.example.reports.shareImageNativeChooser
import com.example.reports.sendSmsPrefilled
import com.example.ui.DentalViewModel

/**
 * ============================================================================
 *  ارسال «صورت‌حساب» و «فاکتور» برای طرف‌حساب‌های هزینه‌ها
 * ============================================================================
 * دقیقاً همان الگوی [PatientSendActionsRow] در صفحهٔ بیماران، اما برای طرف‌حساب
 * هزینه‌ها. از همان مولدهای تصویر موجود استفاده می‌کند تا ابعاد و قالب خروجی با
 * بقیهٔ اسناد برنامه یکی باشد:
 *
 *   • صورت‌حساب  → buildVendorStatementImage  (ExportDocKind.VendorLedger، ۱۴۸۰px)
 *   • فاکتور     → buildOfficialLabInvoiceImage (ExportDocKind.LabInvoice)
 *
 * شمارهٔ تماس از رکورد تأمین‌کننده (Vendor) با همان نام طرف‌حساب پیدا می‌شود؛
 * اگر شماره‌ای نبود، سند با انتخابگر سیستمی (Native Chooser) به اشتراک گذاشته
 * می‌شود تا هیچ‌وقت مسیر ارسال بن‌بست نشود.
 */
@Composable
fun ExpensePartySendActionsRow(
    partyName: String,
    expenses: List<ClinicExpense>,
    payments: List<ExpensePayment>,
    viewModel: DentalViewModel,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val vendors by viewModel.vendors.collectAsStateWithLifecycle()
    val currency = viewModel.label("currency")

    var pendingAction by remember { mutableStateOf<String?>(null) } // "statement" | "invoice"

    // شماره تماس طرف‌حساب — از روی تأمین‌کننده‌ای که نام یا شرکتش با نام طرف‌حساب می‌خواند
    val partyPhone = remember(vendors, partyName) {
        vendors.firstOrNull {
            !it.isDeleted && (it.name.trim() == partyName.trim() || it.company.trim() == partyName.trim())
        }?.phone?.trim().orEmpty()
    }
    val hasPhone = partyPhone.isNotBlank() && partyPhone != "ثبت نشده"

    val totalDebt = remember(expenses) { expenses.sumOf { it.amount } }
    val totalPaid = remember(payments) { payments.sumOf { it.amount } }
    // مانده علامت‌دار — با coerceAtLeast(0) اضافه‌پرداخت پنهان می‌شد و طرف‌حسابِ
    // بستانکار «تسویه» نشان داده می‌شد.
    val remaining = totalDebt - totalPaid
    // منفی یعنی اضافه‌پرداخت کرده‌ایم و طرف‌حساب به ما بدهکار است.
    val isPartyDebtor = remaining < 0L

    fun sendStatement(messenger: String) {
        val file = buildVendorStatementImage(
            context = context,
            clinicName = settings.clinicName,
            partyName = partyName,
            expenses = expenses.sortedBy { it.date },
            payments = payments.sortedBy { it.date },
            totalDebt = totalDebt,
            totalPaid = totalPaid,
            remaining = remaining,
            themeSpec = viewModel.currentExportTheme()
        )
        if (messenger == "واتساپ" && hasPhone) {
            shareImageToWhatsApp(context, file, partyPhone)
        } else if (messenger == "پیامک" && hasPhone) {
            // پیامک تصویر نمی‌پذیرد؛ خلاصهٔ متنی می‌فرستیم و تصویر جداگانه به اشتراک می‌گذاریم
            val text = buildString {
                appendLine(settings.clinicName)
                appendLine("صورت‌حساب طرف‌حساب: $partyName")
                appendLine("جمع هزینه‌ها: ${formatAmount(totalDebt)} $currency")
                appendLine("جمع پرداختی‌ها: ${formatAmount(totalPaid)} $currency")
                append(
                    when {
                        isPartyDebtor -> "بدهی $partyName به ما: ${formatAmount(-remaining)} $currency"
                        remaining > 0L -> "مانده بدهی: ${formatAmount(remaining)} $currency"
                        else -> "حساب تسویه است."
                    }
                )
            }
            sendSmsPrefilled(context, partyPhone, text)
        } else {
            shareImageNativeChooser(context, file)
        }
    }

    fun sendInvoice(messenger: String) {
        // آخرین هزینه/فاکتور ثبت‌شدهٔ این طرف‌حساب مبنای فاکتور رسمی است
        val latest = expenses.maxByOrNull { it.date }
        val invoiceAmount = latest?.amount ?: totalDebt
        // اگر این هزینه به یک «سفارش لابراتوار» گره خورده، شماره دندان‌ها و مبلغِ
        // واحد باید از خودِ سفارش کپی شوند (درخواست ۴):
        //   • شماره دندان(ها) ← order.toothNumber
        //   • مبلغ هر واحد    ← order.cost / تعداد واحد دندان
        val linkedOrder = latest?.labOrderId?.let { loId ->
            viewModel.labOrders.value.firstOrNull { it.id == loId && !it.isDeleted }
        }
        val orderToothNumber = linkedOrder?.toothNumber?.trim().orEmpty()
        val orderTeethCount = if (orderToothNumber.isNotBlank())
            com.example.ui.screens.parseTeethCount(orderToothNumber).coerceAtLeast(1) else 1
        val orderUnitPrice = linkedOrder?.let { it.cost / orderTeethCount }
        val unitPriceText = if (orderUnitPrice != null && orderUnitPrice > 0L)
            "${formatAmount(orderUnitPrice)} $currency" else ""
        val file = buildOfficialLabInvoiceImage(
            context = context,
            clinicName = settings.clinicName,
            doctorName = settings.doctorName,
            labName = partyName,
            // نامِ طرف‌حساب، «مراجع» نیست — خالی می‌ماند تا «آقای لابراتوار …» چاپ نشود.
            patientName = "",
            invoiceNo = (latest?.id ?: 0L).toString().padStart(5, '0'),
            prosthesisType = (linkedOrder?.prosthesisType?.ifBlank { null })
                ?: latest?.description?.ifBlank { "هزینه / خرید" } ?: "هزینه / خرید",
            // شماره دندان‌ها از سفارش کپی می‌شود
            toothNumber = orderToothNumber,
            teethCount = orderTeethCount,
            // مبلغِ واحدِ ثبت‌شده در سفارش کپی می‌شود
            unitPriceText = unitPriceText,
            totalCostText = "${formatAmount(invoiceAmount)} $currency",
            orderDateText = viewModel.formatDate(latest?.date ?: System.currentTimeMillis()),
            frameDateText = "",
            finalDateText = viewModel.formatDate(latest?.date ?: System.currentTimeMillis()),
            paymentMethodText = when {
                isPartyDebtor -> "بدهکار به ما: ${formatAmount(-remaining)} $currency"
                remaining > 0L -> "مانده: ${formatAmount(remaining)} $currency"
                else -> "تسویه‌شده"
            },
            themeSpec = viewModel.currentExportTheme()
        )
        if (messenger == "واتساپ" && hasPhone) {
            shareImageToWhatsApp(context, file, partyPhone)
        } else {
            shareImageNativeChooser(context, file)
        }
    }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { pendingAction = "statement" },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isPersian) "ارسال صورت‌حساب" else "Send Statement", fontSize = 11.sp)
        }
        OutlinedButton(
            onClick = { pendingAction = "invoice" },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isPersian) "ارسال فاکتور" else "Send Invoice", fontSize = 11.sp)
        }
    }

    val action = pendingAction
    if (action != null) {
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = {
                Text(
                    text = if (action == "statement") {
                        if (isPersian) "ارسال صورت‌حساب — $partyName" else "Send Statement — $partyName"
                    } else {
                        if (isPersian) "ارسال فاکتور — $partyName" else "Send Invoice — $partyName"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (hasPhone) {
                            if (isPersian) "شماره طرف‌حساب: $partyPhone" else "Party phone: $partyPhone"
                        } else {
                            if (isPersian)
                                "شماره‌ای برای این طرف‌حساب ثبت نشده — سند با انتخابگر سیستم ارسال می‌شود."
                            else
                                "No phone recorded — the document will be shared via the system chooser."
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isPersian) "کانال ارسال را انتخاب کنید:" else "Select a channel:",
                        fontSize = 11.sp
                    )

                    val messengers = listOf("واتساپ", "تلگرام", "روبیکا", "بله", "پیامک")
                    messengers.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { m ->
                                Button(
                                    onClick = {
                                        if (action == "statement") sendStatement(m) else sendInvoice(m)
                                        pendingAction = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Text(m, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // مسیر همیشه‌دردسترس: اشتراک‌گذاری با هر برنامه‌ای روی گوشی
                    OutlinedButton(
                        onClick = {
                            if (action == "statement") sendStatement("سایر") else sendInvoice("سایر")
                            pendingAction = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isPersian) "اشتراک‌گذاری با سایر برنامه‌ها" else "Share via other apps",
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        )
    }
}
