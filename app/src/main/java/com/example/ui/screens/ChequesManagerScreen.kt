package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Payment
import com.example.ui.DentalViewModel

/** رنگ ثابتِ وضعیت‌ها — یک‌جا تعریف می‌شود تا کارت، KPI و برچسب هماهنگ بمانند. */
private val ClearedGreen = Color(0xFF2E7D32)
private val TransferredPurple = Color(0xFF6A1B9A)
/** هالهٔ زردِ هشدار برای چکی که هنوز «تأیید دریافت بانکی» نخورده است. */
private val ChequeWarnYellow = Color(0xFFFFC107)

@Composable
fun ChequesManagerScreen(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    onBack: () -> Unit
) {
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val currency = viewModel.label("currency")

    fun t(fa: String, en: String) = if (isPersian) fa else en

    // دکمه‌های «ثبت هوشمند دسته‌ای» و «ثبت دستی/بارکد» طبق درخواست کاربر از این صفحه حذف شدند.
    // ثبت چک همچنان از فرم پرداخت بیمار (روش «چک صیادی») انجام می‌شود.
    var filterStatus by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var chequeToEdit by remember { mutableStateOf<Payment?>(null) }
    var chequeToDelete by remember { mutableStateOf<Payment?>(null) }
    var chequeToTransfer by remember { mutableStateOf<Payment?>(null) }
    var chequeToClear by remember { mutableStateOf<Payment?>(null) }
    var chequeToBounce by remember { mutableStateOf<Payment?>(null) }
    var chequeToUndoTransfer by remember { mutableStateOf<Payment?>(null) }
    var photoToPreview by remember { mutableStateOf<String?>(null) }

    val allCheques = remember(payments) {
        payments.filter { it.type == "CHEQUE" && !it.isDeleted }
            .sortedByDescending { it.chequeDueDate ?: it.date }
    }

    val filteredCheques = remember(allCheques, filterStatus, searchQuery) {
        allCheques.filter { c ->
            // چکِ «انتقال‌یافته» دیگر در جریان وصول نیست و نباید در آن فیلتر بیاید.
            val matchStatus = when (filterStatus) {
                "UNCLEARED" -> c.chequeStatus == "PENDING"
                "CLEARED" -> c.chequeStatus == "CLEARED"
                "TRANSFERRED" -> c.chequeStatus == "TRANSFERRED"
                "BOUNCED" -> c.chequeStatus == "BOUNCED"
                else -> true
            }
            val matchQuery = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                c.chequeNumber.orEmpty().contains(q) ||
                c.sayadiId.orEmpty().contains(q) ||
                c.chequeDrawer.orEmpty().lowercase().contains(q) ||
                c.chequeBank.orEmpty().lowercase().contains(q) ||
                c.transferredTo.orEmpty().lowercase().contains(q)
            }
            matchStatus && matchQuery
        }
    }

    val totalUncleared = allCheques.filter { it.chequeStatus == "PENDING" }.sumOf { it.amount }
    val totalCleared = allCheques.filter { it.chequeStatus == "CLEARED" }.sumOf { it.amount }
    val totalTransferred = allCheques.filter { it.chequeStatus == "TRANSFERRED" }.sumOf { it.amount }
    val totalBounced = allCheques.filter { it.chequeStatus == "BOUNCED" }.sumOf { it.amount }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = t("دفتر و مدیریت چک‌های صیادی", "Sayad Cheques Ledger"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // KPI Cards — «انتقال‌یافته» کارت مستقل دارد و دیگر داخل «در جریان» شمرده نمی‌شود
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChequeKpiTile(
                    label = t("در جریان", "In Progress"),
                    amount = totalUncleared,
                    currency = currency,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ChequeKpiTile(
                    label = t("پاس‌شده", "Cleared"),
                    amount = totalCleared,
                    currency = currency,
                    tint = ClearedGreen,
                    modifier = Modifier.weight(1f)
                )
                ChequeKpiTile(
                    label = t("انتقال‌یافته", "Transferred"),
                    amount = totalTransferred,
                    currency = currency,
                    tint = TransferredPurple,
                    modifier = Modifier.weight(1f)
                )
                ChequeKpiTile(
                    label = t("برگشتی", "Bounced"),
                    amount = totalBounced,
                    currency = currency,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search and Status Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(t("جستجو بر اساس شماره چک، شناسه صیادی یا صادرکننده...", "Search cheque #, sayad ID, drawer...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotBlank()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) } }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "ALL" to t("همه (${allCheques.size})", "All (${allCheques.size})"),
                    "UNCLEARED" to t("در جریان", "In Progress"),
                    "CLEARED" to t("پاس‌شده", "Cleared"),
                    "TRANSFERRED" to t("انتقالی", "Transferred"),
                    "BOUNCED" to t("برگشتی", "Bounced")
                ).forEach { (key, labelTxt) ->
                    FilterChip(
                        selected = filterStatus == key,
                        onClick = { filterStatus = key },
                        label = { Text(labelTxt, fontSize = 9.sp, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Cheque List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (filteredCheques.isEmpty()) {
                    item {
                        Text(
                            text = t("هیچ چکی یافت نشد.", "No cheques found."),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
                items(filteredCheques, key = { it.id }) { cheque ->
                    val patientName = patients.find { it.id == cheque.patientId }?.name ?: ""
                    val isChequeCleared = cheque.chequeStatus == "CLEARED"
                    val isChequeBounced = cheque.chequeStatus == "BOUNCED"
                    val isChequeTransferred = cheque.chequeStatus == "TRANSFERRED"
                    val statusColor = when {
                        isChequeCleared -> ClearedGreen
                        isChequeBounced -> MaterialTheme.colorScheme.error
                        isChequeTransferred -> TransferredPurple
                        else -> MaterialTheme.colorScheme.primary
                    }
                    // چکِ منتقل‌نشده‌ای که هنوز «تأیید دریافت بانکی» نخورده، با هالهٔ
                    // زردِ ۱۰٪ متمایز می‌شود تا یادآوری شود این تیک باید زده شود.
                    val needsReceiptTick = !isChequeTransferred && !cheque.bankReceiptConfirmed
                    // چکی که «تأیید انتقال بانکی» خورده و هنوز وصول/برگشت نشده،
                    // قفلِ ویرایش/حذف است.
                    val isTransferLocked = viewModel.isChequeLockedByTransferConfirm(cheque)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = if (needsReceiptTick) BorderStroke(1.5.dp, ChequeWarnYellow)
                                 else BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                        colors = if (needsReceiptTick)
                                     CardDefaults.cardColors(containerColor = ChequeWarnYellow.copy(alpha = 0.10f))
                                 else CardDefaults.cardColors()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // سطر اول: بانک و شماره چک
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${cheque.chequeBank.orEmpty().ifBlank { t("بانک نامشخص", "Bank") }} — ${cheque.chequeNumber.orEmpty().ifBlank { "—" }}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }

                            // مبلغ و سررسید — قرمز، بولد، هرکدام در سطر خودش (درخواست کاربر)
                            Text(
                                text = "${t("مبلغ:", "Amount:")} ${formatAmount(cheque.amount)} $currency",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            cheque.chequeDueDate?.let { due ->
                                Text(
                                    text = "${t("سررسید:", "Due:")} ${viewModel.formatDate(due)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Text(
                                text = listOfNotNull(
                                    if (patientName.isNotBlank()) "${t("بیمار:", "Patient:")} $patientName" else null,
                                    if (!cheque.chequeDrawer.isNullOrBlank()) "${t("صاحب حساب:", "Drawer:")} ${cheque.chequeDrawer}" else null
                                ).joinToString(" | "),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )

                            if (!cheque.sayadiId.isNullOrBlank()) {
                                Text(
                                    text = "${t("شناسه صیادی:", "Sayad ID:")} ${cheque.sayadiId}",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // وضعیت — شاملِ «انتقال یافته به: …»
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = viewModel.chequeStatusLabel(cheque, isPersian),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (isChequeTransferred && cheque.transferDate != null) {
                                Text(
                                    text = "${t("تاریخ انتقال:", "Transfer date:")} ${viewModel.formatDate(cheque.transferDate!!)}",
                                    fontSize = 9.5.sp,
                                    color = Color.Gray
                                )
                            }

                            // تصویر چک — در مدیریت چک‌ها هم نمایش داده می‌شود (با لمس، تمام‌صفحه)
                            if (!cheque.chequePhotoFileId.isNullOrBlank()) {
                                SmartImage(
                                    fileId = cheque.chequePhotoFileId,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { photoToPreview = cheque.chequePhotoFileId }
                                )
                            } else {
                                Text(
                                    text = t("تصویری برای این چک ثبت نشده است", "No cheque image on file"),
                                    fontSize = 9.5.sp,
                                    color = Color.Gray
                                )
                            }

                            // تیک‌های یک‌بارمصرفِ تأیید بانکی
                            // تیکِ اول «تأیید دریافت بانکی» — برای هر چکِ منتقل‌نشده.
                            if (!isChequeTransferred) {
                                OneWayConfirmRow(
                                    checked = cheque.bankReceiptConfirmed,
                                    label = t("تایید دریافت بانکی", "Bank receipt confirmed"),
                                    tint = if (cheque.bankReceiptConfirmed) ClearedGreen else ChequeWarnYellow,
                                    onConfirm = { viewModel.confirmChequeBankReceipt(cheque) }
                                )
                            }
                            // تیکِ دوم «تأیید انتقال بانکی» — فقط برای چکِ انتقال‌یافته
                            // و مشروط به وجودِ تیکِ اول.
                            if (isChequeTransferred && cheque.bankReceiptConfirmed) {
                                OneWayConfirmRow(
                                    checked = cheque.bankTransferConfirmed,
                                    label = t("تایید انتقال بانکی", "Bank transfer confirmed"),
                                    tint = TransferredPurple,
                                    onConfirm = { viewModel.confirmChequeBankTransfer(cheque) }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // پاس/برگشت فقط برای چکِ در جریان — و هر دو با دیالوگ تأیید
                                    if (cheque.chequeStatus == "PENDING") {
                                        TextButton(
                                            onClick = { chequeToClear = cheque },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ClearedGreen, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(t("پاس شد", "Clear"), fontSize = 10.sp, color = ClearedGreen)
                                        }
                                    }
                                    if (!isChequeBounced) {
                                        TextButton(
                                            onClick = { chequeToBounce = cheque },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(t("برگشت", "Bounce"), fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    if (isChequeTransferred) {
                                        TextButton(
                                            onClick = { chequeToUndoTransfer = cheque },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Undo, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(t("لغو انتقال", "Undo transfer"), fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // انتقال فقط از «در جریان وصول» ممکن است:
                                    // چکِ پاس‌شده، برگشتی یا قبلاً منتقل‌شده قابل انتقال نیست.
                                    if (viewModel.canTransferCheque(cheque)) {
                                        IconButton(onClick = { chequeToTransfer = cheque }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = t("انتقال چک", "Transfer cheque"), tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    // چکی که «تأیید انتقال بانکی» خورده و هنوز وصول/برگشت
                                    // نشده، قفلِ ویرایش/حذف است؛ دکمه‌ها پنهان می‌شوند و به‌جای
                                    // آن نشانِ قفل نمایش داده می‌شود.
                                    if (isTransferLocked) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = t("قفل تا وصول/برگشت", "Locked until cleared/bounced"),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = { chequeToEdit = cheque },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = { chequeToDelete = cheque },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // انتقال چک — با اثر واقعی و تأثیرگذار بر حسابداری (دیالوگ مشترک)
    if (chequeToTransfer != null) {
        ChequeTransferDialog(
            viewModel = viewModel,
            isPersian = isPersian,
            cheque = chequeToTransfer!!,
            onDismiss = { chequeToTransfer = null }
        )
    }

    // ویرایش چک — پیش از این این دیالوگ اصلاً رندر نمی‌شد و دکمهٔ ادیت بی‌اثر بود.
    if (chequeToEdit != null) {
        ChequeEditDialog(
            viewModel = viewModel,
            isPersian = isPersian,
            cheque = chequeToEdit!!,
            onDismiss = { chequeToEdit = null }
        )
    }

    // تصویر تمام‌صفحهٔ چک
    if (photoToPreview != null) {
        Dialog(onDismissRequest = { photoToPreview = null }) {
            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 6.dp) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartImage(
                        fileId = photoToPreview,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 420.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    TextButton(onClick = { photoToPreview = null }, modifier = Modifier.align(Alignment.End)) {
                        Text(t("بستن", "Close"))
                    }
                }
            }
        }
    }

    // «پاس شد» — دیالوگ تأیید
    if (chequeToClear != null) {
        val c = chequeToClear!!
        ChequeConfirmDialog(
            title = t("تأیید پاس شدن چک", "Confirm cheque cleared"),
            message = t(
                "چک ${c.chequeNumber.orEmpty().ifBlank { "—" }} به مبلغ ${formatAmount(c.amount)} $currency پاس‌شده ثبت شود؟ مبلغ به موجودی بانک اضافه می‌شود.",
                "Mark cheque ${c.chequeNumber.orEmpty()} for ${formatAmount(c.amount)} $currency as cleared? The amount will be added to the bank balance."
            ),
            confirmLabel = t("بله، پاس شد", "Yes, cleared"),
            confirmColor = ClearedGreen,
            isPersian = isPersian,
            onConfirm = { viewModel.markChequeCleared(c); chequeToClear = null },
            onDismiss = { chequeToClear = null }
        )
    }

    // «برگشت شد» — دیالوگ تأیید
    if (chequeToBounce != null) {
        val c = chequeToBounce!!
        val warnTransfer = if (c.chequeStatus == "TRANSFERRED") {
            t(" این چک به «${c.transferredTo.orEmpty()}» منتقل شده بود؛ تسویهٔ آن سند هم برگشت می‌خورد.", " This cheque was transferred to \"${c.transferredTo.orEmpty()}\"; that settlement will also be reversed.")
        } else ""
        ChequeConfirmDialog(
            title = t("تأیید برگشت چک", "Confirm cheque bounced"),
            message = t(
                "چک ${c.chequeNumber.orEmpty().ifBlank { "—" }} به مبلغ ${formatAmount(c.amount)} $currency برگشتی ثبت شود؟ این مبلغ از درآمد و بدهی بیمار کسر می‌شود.$warnTransfer",
                "Mark cheque ${c.chequeNumber.orEmpty()} for ${formatAmount(c.amount)} $currency as bounced?$warnTransfer"
            ),
            confirmLabel = t("بله، برگشت خورد", "Yes, bounced"),
            confirmColor = MaterialTheme.colorScheme.error,
            isPersian = isPersian,
            onConfirm = { viewModel.markChequeBounced(c); chequeToBounce = null },
            onDismiss = { chequeToBounce = null }
        )
    }

    // لغو انتقال — اسناد مقصد حذف و چک به «در جریان» برمی‌گردد
    if (chequeToUndoTransfer != null) {
        val c = chequeToUndoTransfer!!
        ChequeConfirmDialog(
            title = t("لغو انتقال چک", "Undo cheque transfer"),
            message = t(
                "انتقال این چک به «${c.transferredTo.orEmpty()}» لغو شود؟ سند هزینه/پرداختی که با این انتقال ساخته شده بود حذف می‌شود و چک به «در جریان وصول» برمی‌گردد.",
                "Undo the transfer to \"${c.transferredTo.orEmpty()}\"? The expense/payment document created by the transfer will be deleted and the cheque returns to In Progress."
            ),
            confirmLabel = t("بله، لغو کن", "Yes, undo"),
            confirmColor = MaterialTheme.colorScheme.tertiary,
            isPersian = isPersian,
            onConfirm = { viewModel.undoChequeTransfer(c); chequeToUndoTransfer = null },
            onDismiss = { chequeToUndoTransfer = null }
        )
    }

    if (chequeToDelete != null) {
        val c = chequeToDelete!!
        val transferWarn = if (c.chequeStatus == "TRANSFERRED") {
            t(
                "\n\nتوجه: این چک به «${c.transferredTo.orEmpty()}» منتقل شده است. با حذف چک، سند هزینه/پرداختی که در بخش هزینه‌ها ساخته شده بود هم حذف و آن بدهی دوباره باز می‌شود.",
                "\n\nNote: this cheque was transferred to \"${c.transferredTo.orEmpty()}\". Deleting it also removes the expense document created by the transfer."
            )
        } else ""
        AlertDialog(
            onDismissRequest = { chequeToDelete = null },
            title = { Text(t("حذف چک", "Delete Cheque"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    t(
                        "آیا از حذف این چک به مبلغ ${formatAmount(c.amount)} $currency اطمینان دارید؟$transferWarn",
                        "Delete cheque for ${formatAmount(c.amount)} $currency?$transferWarn"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(c)
                        chequeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(t("حذف", "Delete"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { chequeToDelete = null }) { Text(t("انصراف", "Cancel")) }
            }
        )
    }
}

/** کارت کوچک KPI بالای صفحهٔ چک‌ها. */
@Composable
private fun ChequeKpiTile(
    label: String,
    amount: Long,
    currency: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tint, maxLines = 1)
            Text(formatAmount(amount), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = tint, maxLines = 1)
            Text(currency, fontSize = 8.sp, color = tint.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}

/**
 * تیکِ «یک‌بار فعال» — پس از زدن، غیرفعال و قفل می‌شود و دیگر برداشته نمی‌شود.
 */
@Composable
private fun OneWayConfirmRow(
    checked: Boolean,
    label: String,
    tint: Color,
    onConfirm: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { if (!checked) onConfirm() },
            enabled = !checked,
            colors = CheckboxDefaults.colors(
                checkedColor = tint,
                disabledCheckedColor = tint
            ),
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
            color = if (checked) tint else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (checked) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Lock, contentDescription = null, tint = tint.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
        }
    }
}

/** دیالوگ تأیید ساده و مشترک برای عملیات وضعیتِ چک. */
@Composable
private fun ChequeConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    isPersian: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = confirmColor, fontSize = 14.sp) },
        text = { Text(message, fontSize = 12.sp) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
            ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") }
        }
    )
}
