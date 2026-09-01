package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Payment
import com.example.ui.DentalViewModel

/**
 * سیستم کارتخوان (پوز) — تسویه‌ی روزانه
 *
 * درخواست کاربر:
 *  - تراکنش‌های یک روز کارتخوان به‌صورت یک ردیف (جمع روز) در همان صفحه‌ی ثبت نمایش داده شود.
 *  - روز بعد، در شروع اپ، پاپ‌آپی بپرسد تراکنش‌های روز قبل به کدام حساب واریز شوند.
 *  - پس از تایید، تراکنش‌ها از لیست فعال مخفی و به‌صورت بایگانی روزانه (با جمع روزانه در
 *    حساب انتخابی و حفظ طرف‌حساب هر تراکنش) نمایش داده شوند.
 */
@Composable
fun PosSettlementDialog(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    autoOpenedForPreviousDays: Boolean = false,
    onDismiss: () -> Unit
) {
    val unsettled by viewModel.unsettledPosByDay.collectAsStateWithLifecycle()
    val archive by viewModel.posArchiveByDay.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val currency = viewModel.label("currency")

    fun t(fa: String, en: String) = if (isPersian) fa else en

    var selectedTab by remember { mutableStateOf(if (unsettled.isNotEmpty()) 0 else 1) }
    var dayToSettle by remember { mutableStateOf<String?>(null) }
    var selectedAccountId by remember(dayToSettle) { mutableStateOf<Long?>(null) }
    var expandedArchiveDay by remember { mutableStateOf<String?>(null) }

    val activeAccounts = bankAccounts.filter { !it.isDeleted }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(t("کارتخوان — تسویه روزانه", "Card Reader — Daily Settlement"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(t("در انتظار تسویه (${unsettled.size})", "Unsettled (${unsettled.size})"), fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(t("بایگانی (${archive.size})", "Archive (${archive.size})"), fontSize = 10.sp) }
                    )
                }

                if (selectedTab == 0) {
                    if (autoOpenedForPreviousDays) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFC2410C).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = t(
                                    "تراکنش‌های کارتخوان روزهای قبل هنوز تسویه نشده‌اند. حساب مقصد هر روز را انتخاب و تایید کنید تا جمع آن روز به حساب واریز شود.",
                                    "Previous days' card reader transactions are unsettled. Choose the destination account for each day."
                                ),
                                fontSize = 10.5.sp,
                                color = Color(0xFFC2410C),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    if (unsettled.isEmpty()) {
                        Text(
                            text = t("همه‌ی تراکنش‌های کارتخوان تسویه شده‌اند.", "All card reader transactions are settled."),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 330.dp)
                        ) {
                            items(unsettled, key = { it.first }) { (dayKey, rows) ->
                                val daySum = rows.sumOf { it.amount }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(t("تاریخ: $dayKey", "Date: $dayKey"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(
                                                    text = t("${rows.size} تراکنش", "${rows.size} transactions"),
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text(
                                                text = "${formatAmount(daySum)} $currency",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                dayToSettle = dayKey
                                                selectedAccountId = null
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(t("واریز جمع این روز به حساب...", "Deposit day total to account..."), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (archive.isEmpty()) {
                        Text(
                            text = t("هنوز تسویه‌ای انجام نشده است.", "No settlements yet."),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 330.dp)
                        ) {
                            items(archive, key = { it.first }) { (dayKey, rows) ->
                                val daySum = rows.sumOf { it.amount }
                                val accountName = rows.firstOrNull()?.bankAccountId?.let { accId ->
                                    bankAccounts.find { it.id == accId }?.bankName
                                } ?: t("نامشخص", "Unknown")
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.06f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(t("تاریخ: $dayKey", "Date: $dayKey"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(
                                                    text = t("واریز به: $accountName", "Deposited to: $accountName"),
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "${formatAmount(daySum)} $currency",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                        TextButton(
                                            onClick = { expandedArchiveDay = if (expandedArchiveDay == dayKey) null else dayKey },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = if (expandedArchiveDay == dayKey) t("بستن جزئیات", "Hide details") else t("جزئیات تراکنش‌ها (${rows.size})", "Details (${rows.size})"),
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (expandedArchiveDay == dayKey) {
                                            // طرف‌حساب هر تراکنش حفظ می‌شود — نمایش بیمار و مبلغ هر تراکنش
                                            rows.forEach { p ->
                                                val pname = patients.find { it.id == p.patientId }?.name ?: t("نامشخص", "Unknown")
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("• $pname", fontSize = 10.sp)
                                                    Text("${formatAmount(p.amount)} $currency", fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t("بستن", "Close")) }
        }
    )

    // دیالوگ انتخاب حساب برای تسویه‌ی یک روز
    if (dayToSettle != null) {
        val dayRows = unsettled.firstOrNull { it.first == dayToSettle }?.second.orEmpty()
        val daySum = dayRows.sumOf { it.amount }
        AlertDialog(
            onDismissRequest = { dayToSettle = null },
            title = { Text(t("واریز جمع روز $dayToSettle", "Deposit day $dayToSettle total"), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = t(
                            "جمع ${dayRows.size} تراکنش کارتخوان این روز: ${formatAmount(daySum)} $currency\nحساب مقصد را انتخاب کنید؛ پس از تایید، تراکنش‌ها از لیست فعال خارج و در بایگانی ثبت می‌شوند (طرف‌حساب هر تراکنش حفظ می‌شود).",
                            "Total of ${dayRows.size} transactions: ${formatAmount(daySum)} $currency\nPick the destination account."
                        ),
                        fontSize = 11.sp
                    )
                    activeAccounts.forEach { acc ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedAccountId == acc.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (selectedAccountId == acc.id) MaterialTheme.colorScheme.primary else Color.Transparent),
                            onClick = { selectedAccountId = acc.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(acc.bankName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(listOfNotNull(acc.cardNumber.takeIf { it.isNotBlank() }, acc.cardHolder.takeIf { it.isNotBlank() }).joinToString(" — "), fontSize = 9.5.sp, color = Color.Gray)
                                }
                                Text("${formatAmount(acc.balance)} $currency", fontSize = 10.5.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val accId = selectedAccountId ?: return@Button
                        viewModel.settlePosDay(dayToSettle!!, accId)
                        dayToSettle = null
                    },
                    enabled = selectedAccountId != null
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(t("تایید واریز", "Confirm Deposit"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { dayToSettle = null }) { Text(t("انصراف", "Cancel")) }
            }
        )
    }
}
