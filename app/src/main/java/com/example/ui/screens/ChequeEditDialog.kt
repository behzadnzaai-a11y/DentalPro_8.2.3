package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Payment
import com.example.ui.DentalViewModel

/**
 * ویرایش یک چک از «دفتر مدیریت چک‌ها».
 *
 * پیش از این، دکمهٔ ادیت در آن صفحه فقط یک متغیر state را پر می‌کرد و هیچ
 * دیالوگی برایش رندر نمی‌شد — یعنی ویرایش عملاً کار نمی‌کرد. این دیالوگ همان
 * حفرهٔ خالی را پر می‌کند.
 *
 * نکتهٔ حسابداری: ذخیره از مسیر [DentalViewModel.updatePayment] انجام می‌شود؛
 * بنابراین اگر چک قبلاً منتقل شده باشد، مبلغ و مشخصاتِ سندِ مقصد در بخش
 * هزینه‌ها هم خودکار به‌روز می‌شود و دو طرفِ دفتر نامساوی نمی‌مانند.
 */
@Composable
fun ChequeEditDialog(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    cheque: Payment,
    onDismiss: () -> Unit
) {
    fun t(fa: String, en: String) = if (isPersian) fa else en

    val currency = viewModel.label("currency")

    var amountStr by remember(cheque.id) { mutableStateOf(cheque.amount.takeIf { it > 0 }?.toString().orEmpty()) }
    var chequeNum by remember(cheque.id) { mutableStateOf(cheque.chequeNumber.orEmpty()) }
    var sayadiId by remember(cheque.id) { mutableStateOf(cheque.sayadiId.orEmpty()) }
    var chequeBank by remember(cheque.id) { mutableStateOf(cheque.chequeBank.orEmpty()) }
    var chequeDrawer by remember(cheque.id) { mutableStateOf(cheque.chequeDrawer.orEmpty()) }
    var dueDate by remember(cheque.id) { mutableStateOf(cheque.chequeDueDate) }
    var showDuePicker by remember { mutableStateOf(false) }

    val amountValid = com.example.util.isValidPositiveAmount(amountStr)
    val isTransferred = cheque.chequeStatus == "TRANSFERRED"

    if (showDuePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = dueDate ?: System.currentTimeMillis(),
            isPersian = isPersian,
            onDateSelected = { jy, jm, jd ->
                dueDate = com.example.util.PersianCalendarHelper.jalaliToTimestamp(jy, jm, jd)
                showDuePicker = false
            },
            onDismiss = { showDuePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(t("ویرایش چک", "Edit Cheque"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isTransferred) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = t(
                                "این چک به «${cheque.transferredTo.orEmpty()}» منتقل شده است. با ذخیره، مبلغ و مشخصات سندِ هزینهٔ مقصد هم به‌روز می‌شود.",
                                "This cheque was transferred to \"${cheque.transferredTo.orEmpty()}\". Saving also updates the linked expense document."
                            ),
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter(Char::isDigit) },
                    label = { Text("${t("مبلغ", "Amount")} ($currency)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !amountValid,
                    supportingText = if (!amountValid) {
                        { Text(t("مبلغ باید بزرگ‌تر از صفر باشد", "Amount must be greater than zero"), color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = chequeNum,
                    onValueChange = { chequeNum = it },
                    label = { Text(t("شماره چک", "Cheque number"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sayadiId,
                    onValueChange = { sayadiId = it.filter(Char::isDigit).take(16) },
                    label = { Text(t("شناسه صیادی (۱۶ رقم)", "Sayad ID (16 digits)"), fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = chequeBank,
                    onValueChange = { chequeBank = it },
                    label = { Text(t("بانک", "Bank"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = chequeDrawer,
                    onValueChange = { chequeDrawer = it },
                    label = { Text(t("صاحب حساب / صادرکننده", "Drawer"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { showDuePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = dueDate?.let { "${t("سررسید:", "Due:")} ${viewModel.formatDate(it)}" }
                            ?: t("انتخاب تاریخ سررسید", "Pick due date"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${t("وضعیت فعلی:", "Current status:")} ${viewModel.chequeStatusLabel(cheque, isPersian)}",
                    fontSize = 10.5.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                enabled = amountValid,
                onClick = {
                    viewModel.updatePayment(
                        cheque.copy(
                            amount = amountStr.toEnglishLong(),
                            chequeNumber = chequeNum.trim().ifBlank { null },
                            sayadiId = sayadiId.trim().ifBlank { null },
                            chequeBank = chequeBank.trim().ifBlank { null },
                            chequeDrawer = chequeDrawer.trim().ifBlank { null },
                            chequeDueDate = dueDate,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    onDismiss()
                }
            ) { Text(t("ذخیره تغییرات", "Save"), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t("انصراف", "Cancel")) }
        }
    )
}
