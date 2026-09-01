package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LabOrder
import com.example.ui.DentalViewModel

/**
 * ویرایش یک سفارش لابراتوار.
 *
 * تا پیش از این فقط «حذف» و «تغییر وضعیت» ممکن بود و هیچ راهی برای اصلاح مبلغ،
 * شمارهٔ دندان یا نام لابراتوار وجود نداشت.
 *
 * ذخیره از مسیر [DentalViewModel.updateLabOrder] انجام می‌شود که تغییرات را روی
 * هزینهٔ متصل در «هزینه‌های مطب» هم اعمال می‌کند — پس مبلغ سفارش و مبلغ هزینه
 * هرگز از هم جدا نمی‌افتند.
 */
@Composable
fun LabOrderEditDialog(
    order: LabOrder,
    viewModel: DentalViewModel,
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
    fun t(fa: String, en: String) = if (isPersian) fa else en
    val currency = viewModel.label("currency")

    var labName by remember(order.id) { mutableStateOf(order.labName) }
    var patientName by remember(order.id) { mutableStateOf(order.patientName) }
    var toothNumber by remember(order.id) { mutableStateOf(order.toothNumber) }
    var prosthesisType by remember(order.id) { mutableStateOf(order.prosthesisType) }
    var costStr by remember(order.id) { mutableStateOf(order.cost.toString()) }
    var dueDate by remember(order.id) { mutableLongStateOf(order.dueDate) }
    var shadeBrand by remember(order.id) { mutableStateOf(order.shadeBrand.orEmpty()) }
    var shadeColor by remember(order.id) { mutableStateOf(order.shadeColor.orEmpty()) }
    var showDuePicker by remember { mutableStateOf(false) }

    val costValid = (costStr.trim().toLongOrNull() ?: 0L) > 0L
    val canSave = costValid && labName.isNotBlank()

    if (showDuePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = dueDate,
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
                Text(t("ویرایش سفارش لابراتوار", "Edit Lab Order"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = t(
                            "تغییرات روی هزینهٔ ثبت‌شده در «هزینه‌های مطب» هم اعمال می‌شود.",
                            "Changes also update the linked clinic expense."
                        ),
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                OutlinedTextField(
                    value = labName,
                    onValueChange = { labName = it },
                    label = { Text(t("نام لابراتوار", "Lab name"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text(t("نام بیمار", "Patient name"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = prosthesisType,
                    onValueChange = { prosthesisType = it },
                    label = { Text(t("نوع پروتز", "Prosthesis type"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = toothNumber,
                    onValueChange = { toothNumber = it },
                    label = { Text(t("شماره دندان‌ها (با , جدا کنید)", "Tooth numbers (comma separated)"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it.filter(Char::isDigit) },
                    label = { Text("${t("مبلغ کل سفارش", "Total cost")} ($currency)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !costValid,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // مبلغ هر واحد از روی تعداد دندان‌های همین سفارش حساب می‌شود.
                val unitCount = parseTeethCount(toothNumber)
                val total = costStr.trim().toLongOrNull() ?: 0L
                if (unitCount > 0 && total > 0) {
                    Text(
                        text = t(
                            "$unitCount واحد — مبلغ هر واحد: ${formatAmount(total / unitCount)} $currency",
                            "$unitCount units — unit price: ${formatAmount(total / unitCount)} $currency"
                        ),
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = shadeBrand,
                        onValueChange = { shadeBrand = it },
                        label = { Text(t("برند شید", "Shade brand"), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = shadeColor,
                        onValueChange = { shadeColor = it },
                        label = { Text(t("رنگ", "Shade"), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedButton(
                    onClick = { showDuePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${t("سررسید تحویل:", "Due:")} ${viewModel.formatDate(dueDate)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!costValid) {
                    Text(
                        text = t("مبلغ باید بزرگ‌تر از صفر باشد.", "Cost must be greater than zero."),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    viewModel.updateLabOrder(
                        order = order,
                        labName = labName,
                        patientName = patientName,
                        toothNumber = toothNumber,
                        prosthesisType = prosthesisType,
                        cost = costStr.trim().toLongOrNull() ?: order.cost,
                        dueDate = dueDate,
                        shadeBrand = shadeBrand.trim().ifBlank { null },
                        shadeColor = shadeColor.trim().ifBlank { null }
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
