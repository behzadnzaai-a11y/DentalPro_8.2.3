package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UnregisteredBankSms
import com.example.ui.DentalViewModel

/**
 * دیالوگ‌های مربوط به پیامک‌های بانکی:
 *  - پوشش‌دهنده (wrapper) برای BankSmsParserDialogImpl
 *  - دیالوگ شفاف‌سازی دسترسی پیامک (قبل از فعال‌سازی مجوز)
 *  - مدیر چهارگانه‌ی صف پیامک‌ها: ثبت‌نشده / ثبت‌شده / نادیده‌گرفته / سطل آشغال
 */

// ==========================================
// پوشش‌دهنده: دیالوگ استخراج پیامک بانکی
// ==========================================
@Composable
fun BankSmsParserDialog(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    initialSmsText: String? = null,
    onDismiss: () -> Unit
) {
    BankSmsParserDialogImpl(
        viewModel = viewModel,
        isPersian = isPersian,
        initialSmsText = initialSmsText,
        onDismiss = onDismiss
    )
}

// ==========================================
// شفاف‌سازی دسترسی به پیامک‌ها — قبل از درخواست مجوز نمایش داده می‌شود
// ==========================================
@Composable
fun ProminentSmsDisclosureDialog(
    isPersian: Boolean,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPersian) "دسترسی به پیامک‌های بانکی" else "Bank SMS Access",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isPersian)
                        "برای ثبت خودکار تراکنش‌های بانکی، نرم‌افزار فقط پیامک‌های حاوی کلیدواژه‌های بانکی (مانند «واریز» و «برداشت») را می‌خواند."
                    else "To auto-register bank transactions, the app only reads SMS messages containing banking keywords (e.g. deposit/withdrawal).",
                    fontSize = 12.sp
                )
                Text(
                    text = if (isPersian)
                        "• هیچ پیامک شخصی دیگری خوانده نمی‌شود.\n• متن پیامک‌ها فقط داخل همین اپ ذخیره می‌شود.\n• شما می‌توانید هر پیامک را نادیده بگیرید یا حذف کنید."
                    else "• No other personal SMS is read.\n• SMS content is stored only inside this app.\n• You can ignore or delete any SMS.",
                    fontSize = 11.5.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isPersian) "تایید و فعال‌سازی" else "Accept & Enable", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") }
        }
    )
}

// مدیرِ صفِ پیامک‌های بانکی — چهار تب. وضعیتِ هر پیامک (ثبت‌شده/ثبت‌نشده/نادیده/سطل)
// بعد از خروج و ورود از سرور ریستور می‌شود: سینکِ این جدول در pullAll فعال است و
// عملیات‌ها از مسیرِ repository (صفِ سینک) انجام می‌شوند.
@Composable
fun UnregisteredBankSmsManagerDialog(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
    val pending by viewModel.unregisteredBankSmsList.collectAsStateWithLifecycle()
    val registered by viewModel.registeredBankSmsList.collectAsStateWithLifecycle()
    val ignored by viewModel.ignoredBankSmsList.collectAsStateWithLifecycle()
    val trashed by viewModel.trashedBankSmsList.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var smsToRegister by remember { mutableStateOf<UnregisteredBankSms?>(null) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    // ——— فیلترها: نوع تراکنش و بانک ———
    var typeFilter by remember { mutableStateOf("ALL") }   // ALL | DEPOSIT | WITHDRAWAL
    var bankFilter by remember { mutableStateOf("ALL") }
    // ——— انتخاب گروهی ———
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    val allSms = remember(pending, registered, ignored, trashed) {
        pending + registered + ignored + trashed
    }
    val bankNames = remember(allSms) {
        allSms.mapNotNull { it.bankNameGuess.trim().takeIf { n -> n.isNotBlank() } }
            .distinct().sorted()
    }

    fun applyFilters(list: List<UnregisteredBankSms>): List<UnregisteredBankSms> = list.filter { sms ->
        val typeOk = when (typeFilter) {
            "DEPOSIT" -> sms.type == "DEPOSIT"
            "WITHDRAWAL" -> sms.type != "DEPOSIT"
            else -> true
        }
        val bankOk = bankFilter == "ALL" || sms.bankNameGuess.trim() == bankFilter
        typeOk && bankOk
    }

    val pendingF = applyFilters(pending)
    val registeredF = applyFilters(registered)
    val ignoredF = applyFilters(ignored)
    val trashedF = applyFilters(trashed)

    // فهرست قابل‌مشاهدهٔ تب فعلی — مبنای «انتخاب همه» و عملیات گروهی
    val visibleList = when (selectedTab) {
        0 -> pendingF
        1 -> registeredF
        2 -> ignoredF
        else -> trashedF
    }

    // با عوض‌شدن تب یا فیلتر، انتخاب‌های نامرتبط پاک می‌شوند تا عملیات گروهی
    // هرگز روی ردیفی که کاربر نمی‌بیند اجرا نشود.
    LaunchedEffect(selectedTab, typeFilter, bankFilter) {
        selectedIds.clear()
        selectionMode = false
    }

    val tabs = listOf(
        if (isPersian) "ثبت‌نشده (${pendingF.size})" else "Unregistered (${pendingF.size})",
        if (isPersian) "ثبت‌شده (${registeredF.size})" else "Registered (${registeredF.size})",
        if (isPersian) "نادیده‌گرفته (${ignoredF.size})" else "Ignored (${ignoredF.size})",
        if (isPersian) "سطل آشغال (${trashedF.size})" else "Trash (${trashedF.size})"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPersian) "پیامک‌های بانکی" else "Bank SMS",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(title, fontSize = 9.5.sp, maxLines = 1) }
                        )
                    }
                }

                // ——— نوار فیلتر: نوع تراکنش ———
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "ALL" to (if (isPersian) "همه" else "All"),
                        "DEPOSIT" to (if (isPersian) "واریز" else "Deposits"),
                        "WITHDRAWAL" to (if (isPersian) "برداشت" else "Withdrawals")
                    ).forEach { (key, lbl) ->
                        FilterChip(
                            selected = typeFilter == key,
                            onClick = { typeFilter = key },
                            label = { Text(lbl, fontSize = 9.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ——— نوار فیلتر: بانک ———
                if (bankNames.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = bankFilter == "ALL",
                            onClick = { bankFilter = "ALL" },
                            label = { Text(if (isPersian) "همهٔ بانک‌ها" else "All banks", fontSize = 9.sp, maxLines = 1) }
                        )
                        bankNames.forEach { bank ->
                            FilterChip(
                                selected = bankFilter == bank,
                                onClick = { bankFilter = if (bankFilter == bank) "ALL" else bank },
                                label = { Text(bank, fontSize = 9.sp, maxLines = 1) }
                            )
                        }
                    }
                }

                // ——— نوار عملیات گروهی ———
                if (visibleList.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            selectionMode = !selectionMode
                            if (!selectionMode) selectedIds.clear()
                        }) {
                            Icon(
                                if (selectionMode) Icons.Default.Close else Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = if (selectionMode) (if (isPersian) "لغو انتخاب" else "Cancel")
                                       else (if (isPersian) "انتخاب گروهی" else "Select"),
                                fontSize = 9.5.sp
                            )
                        }
                        if (selectionMode) {
                            TextButton(onClick = {
                                if (selectedIds.size == visibleList.size) {
                                    selectedIds.clear()
                                } else {
                                    selectedIds.clear()
                                    selectedIds.addAll(visibleList.map { it.id })
                                }
                            }) {
                                Text(
                                    text = if (selectedIds.size == visibleList.size)
                                        (if (isPersian) "هیچ‌کدام" else "None")
                                    else (if (isPersian) "همه (${visibleList.size})" else "All (${visibleList.size})"),
                                    fontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }

                if (selectionMode && selectedIds.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isPersian) "${selectedIds.size} پیامک انتخاب شده" else "${selectedIds.size} selected",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (selectedTab == 0) {
                                    TextButton(onClick = {
                                        viewModel.ignorePendingSmsBulk(selectedIds.toList())
                                        selectedIds.clear(); selectionMode = false
                                    }) {
                                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(if (isPersian) "نادیده" else "Ignore", fontSize = 9.5.sp)
                                    }
                                }
                                if (selectedTab == 1 || selectedTab == 2 || selectedTab == 3) {
                                    TextButton(onClick = {
                                        viewModel.restoreBankSmsBulk(selectedIds.toList())
                                        selectedIds.clear(); selectionMode = false
                                    }) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(if (isPersian) "بازگردانی" else "Restore", fontSize = 9.5.sp)
                                    }
                                }
                                if (selectedTab != 3) {
                                    TextButton(onClick = {
                                        viewModel.trashBankSmsBulk(selectedIds.toList())
                                        selectedIds.clear(); selectionMode = false
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(if (isPersian) "حذف" else "Trash", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                when (selectedTab) {
                    0 -> SmsListPane(
                        items = pendingF,
                        isPersian = isPersian,
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        onToggleSelect = { id -> if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id) },
                        emptyText = if (isPersian) "همه‌ی پیامک‌های بانکی تعیین‌تکلیف شده‌اند." else "All bank SMS are processed.",
                        // لمس کل کارت → پاپ‌آپ کامل ثبت (همان صفحهٔ ماشین‌حساب نوتیفیکیشن)
                        onRowClick = { sms -> smsToRegister = sms },
                        rowContent = { sms ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { smsToRegister = sms }) {
                                    Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(if (isPersian) "ثبت" else "Register", fontSize = 9.5.sp)
                                }
                                TextButton(onClick = { viewModel.ignorePendingSms(sms.id) }) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(if (isPersian) "نادیده" else "Ignore", fontSize = 9.5.sp)
                                }
                                IconButton(onClick = { viewModel.trashBankSms(sms.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Trash", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    )

                    1 -> SmsListPane(
                        items = registeredF,
                        isPersian = isPersian,
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        onToggleSelect = { id -> if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id) },
                        emptyText = if (isPersian) "هنوز پیامکی ثبت نشده است." else "No registered SMS yet.",
                        rowContent = { sms ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { viewModel.revertRegisteredBankSms(sms.id, "PENDING") }) {
                                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(if (isPersian) "بازگردانی" else "Revert", fontSize = 9.5.sp)
                                }
                                IconButton(onClick = { viewModel.trashBankSms(sms.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Trash", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    )

                    2 -> SmsListPane(
                        items = ignoredF,
                        isPersian = isPersian,
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        onToggleSelect = { id -> if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id) },
                        emptyText = if (isPersian) "پیامک نادیده‌گرفته‌ای وجود ندارد." else "No ignored SMS.",
                        rowContent = { sms ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { viewModel.restoreIgnoredBankSms(sms.id) }) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(if (isPersian) "بازگردانی" else "Restore", fontSize = 9.5.sp)
                                }
                                IconButton(onClick = { viewModel.trashBankSms(sms.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Trash", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    )

                    3 -> Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f, fill = false)) {
                        if (trashedF.isEmpty()) {
                            Text(
                                text = if (isPersian) "سطل آشغال خالی است." else "Trash is empty.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            OutlinedButton(
                                onClick = { showEmptyTrashConfirm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isPersian) "خالی‌کردن کامل سطل آشغال" else "Empty Trash", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(trashedF, key = { it.id }) { sms ->
                                    SmsRowCard(
                                        sms = sms,
                                        isPersian = isPersian,
                                        onClick = if (selectionMode) {
                                            { if (sms.id in selectedIds) selectedIds.remove(sms.id) else selectedIds.add(sms.id) }
                                        } else null,
                                        selectionMode = selectionMode,
                                        isSelected = sms.id in selectedIds,
                                        onToggleSelect = {
                                            if (sms.id in selectedIds) selectedIds.remove(sms.id) else selectedIds.add(sms.id)
                                        },
                                        trailing = {
                                            TextButton(onClick = { viewModel.restoreBankSmsFromTrash(sms.id) }) {
                                                Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text(if (isPersian) "بازگردانی" else "Restore", fontSize = 9.5.sp)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "بستن" else "Close") }
        }
    )

    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text(if (isPersian) "خالی‌کردن سطل آشغال" else "Empty Trash", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(if (isPersian) "پیامک‌های داخل سطل آشغال برای همیشه حذف شوند؟" else "Permanently delete all trashed SMS?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyBankSmsTrash()
                        showEmptyTrashConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (isPersian) "حذف قطعی" else "Delete Forever", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) { Text(if (isPersian) "انصراف" else "Cancel") }
            }
        )
    }

    // همان پاپ‌آپ کامل ثبت تراکنش (با ماشین‌حساب و تخصیص چندردیفی) که از لمس
    // نوتیفیکیشن پیامک بانکی باز می‌شود — برای آیتم‌های صف «پیامک‌های اسکن‌شده/ثبت‌نشده».
    if (smsToRegister != null) {
        val sms = smsToRegister!!
        BankSmsTransactionDialog(
            viewModel = viewModel,
            isPersian = isPersian,
            info = BankSmsTxnInfo(
                bankName = sms.bankNameGuess.ifBlank { "بانک" },
                type = sms.type.ifBlank { "DEPOSIT" },
                amountTomans = sms.amountTomans,
                dateStr = sms.dateStr,
                timeStr = sms.timeStr,
                accountNumberRaw = sms.accountNumberRaw,
                rawText = sms.content,
                score = sms.score,
                queueId = sms.id,
                smsNativeId = sms.smsNativeId
            ),
            onDismiss = { smsToRegister = null }
        )
    }
}

@Composable
private fun SmsListPane(
    items: List<UnregisteredBankSms>,
    isPersian: Boolean,
    emptyText: String,
    onRowClick: ((UnregisteredBankSms) -> Unit)? = null,
    selectionMode: Boolean = false,
    selectedIds: List<Long> = emptyList(),
    onToggleSelect: (Long) -> Unit = {},
    rowContent: @Composable (UnregisteredBankSms) -> Unit
) {
    if (items.isEmpty()) {
        Text(emptyText, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.heightIn(max = 360.dp)
    ) {
        items(items, key = { it.id }) { sms ->
            SmsRowCard(
                sms = sms,
                isPersian = isPersian,
                // در حالت انتخاب، لمس کارت به‌جای بازکردن فرم ثبت، تیک می‌زند.
                onClick = if (selectionMode) {
                    { onToggleSelect(sms.id) }
                } else {
                    onRowClick?.let { handler -> { handler(sms) } }
                },
                selectionMode = selectionMode,
                isSelected = sms.id in selectedIds,
                onToggleSelect = { onToggleSelect(sms.id) },
                trailing = rowContent
            )
        }
    }
}

@Composable
private fun SmsRowCard(
    sms: UnregisteredBankSms,
    isPersian: Boolean,
    onClick: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    trailing: @Composable (UnregisteredBankSms) -> Unit
) {
    val isDeposit = sms.type == "DEPOSIT"
    val currency = if (isPersian) "تومان" else "Toman"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sms.isDeleted) Color.Gray.copy(alpha = 0.10f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = "${formatAmount(sms.amountTomans)} $currency",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (isDeposit) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (isDeposit) Color(0xFF2E7D32) else Color(0xFFC62828)).copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isDeposit) (if (isPersian) "واریز" else "Deposit") else (if (isPersian) "برداشت" else "Withdrawal"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDeposit) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = listOfNotNull(
                    sms.bankNameGuess.takeIf { it.isNotBlank() },
                    sms.dateStr.takeIf { it.isNotBlank() },
                    sms.timeStr.takeIf { it.isNotBlank() }
                ).joinToString(" | "),
                fontSize = 9.5.sp,
                color = Color.Gray,
                maxLines = 1
            )
            if (sms.registeredForSummary.isNotBlank()) {
                Text(
                    text = if (isPersian) "بابت: ${sms.registeredForSummary}" else "For: ${sms.registeredForSummary}",
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                trailing(sms)
            }
        }
    }
}
