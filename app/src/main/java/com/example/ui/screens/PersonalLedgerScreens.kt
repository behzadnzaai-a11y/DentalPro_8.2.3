@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChildrenExpense
import com.example.data.PersonalSubAccount
import com.example.data.PersonalTransaction
import com.example.data.PersonProfile
import com.example.reports.ReportGenerators
import com.example.reports.sharePdf
import com.example.reports.shareImagesNativeChooser
import com.example.reports.PdfReportBuilder
import com.example.ui.DentalViewModel
import com.example.ui.components.AmountInputField
import com.example.ui.components.JalaliDateField
import com.example.ui.components.parseGroupedAmount
import com.example.ui.theme.LocalNeonPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ============================================================================
 *  دفاتر تمام‌صفحهٔ «مالی و حسابداری ← شخصی» (درخواست کاربر):
 *  - لیستِ تراکنش‌های قبلی از صفحهٔ اصلی حذف شد و به کادرهای بالای صفحه لینک
 *    می‌شود؛ این دفتر تمام‌صفحه است و فیلتر تاریخ/حساب اشخاص/حساب شخصی دارد و
 *    با همان «موتور تولید گزارش» (PDF) بقیهٔ گزارش‌ها خروجی می‌گیرد.
 *  - mode = "EXPENSE" | "INCOME" | "PERSONS"
 * ============================================================================
 */
private const val PERSON_DOC_PREFIX = "حساب اشخاص: "

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalLedgerFullScreen(
    viewModel: DentalViewModel,
    mode: String,
    onDismiss: () -> Unit
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val currency = viewModel.label("currency")
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val accent = LocalNeonPalette.current.primary.toArgb()
    val transactions by viewModel.personalTransactions.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val subAccounts by viewModel.personalSubAccounts.collectAsStateWithLifecycle()
    val childrenExpenses by viewModel.childrenExpenses.collectAsStateWithLifecycle()
    val personProfiles by viewModel.personProfiles.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    fun t(fa: String, en: String) = if (isPersian) fa else en

    val title = when (mode) {
        "INCOME" -> t("دفتر درآمد شخصی", "Personal Income Ledger")
        "PERSONS" -> t("حساب اشخاص", "Person Accounts")
        else -> t("دفتر هزینه‌های شخصی", "Personal Expense Ledger")
    }

    // ——— فیلترها ———
    var dateFilterOn by remember { mutableStateOf(false) }
    var dateFrom by remember { mutableStateOf(System.currentTimeMillis()) }
    var dateTo by remember { mutableStateOf(System.currentTimeMillis()) }
    var personFilter by remember { mutableStateOf<String?>(null) }
    var accountFilter by remember { mutableStateOf<String?>(null) }
    var bankFilter by remember { mutableStateOf<Long?>(null) }

    // زیرحساب‌های برگ (همان منطق صفحهٔ اصلی) برای فیلتر «حساب شخصی / نوع درآمد»
    val leafAccounts = remember(subAccounts) {
        val alive = subAccounts.filter { !it.isDeleted && it.category != com.example.data.PERSONS_CATEGORY }
        val usedAsParent = (alive.map { it.parentName.trim() } + alive.map { it.grandParentName.trim() })
            .filter { it.isNotBlank() }.toSet()
        alive.filter { it.name.trim() !in usedAsParent }
            .distinctBy { "${it.category.trim()}|${it.grandParentName.trim()}|${it.parentName.trim()}|${it.name.trim()}" }
    }

    // جریات اشخاص (همان منبع یکتا: ChildrenExpense + تراکنش‌های پیشونددار)
    val personRecords = remember(childrenExpenses, transactions, isPersian) {
        buildPersonDocRows(childrenExpenses, transactions, isPersian)
    }
    val personNames = remember(personRecords, personProfiles, settings) {
        (personProfiles.map { it.fullName } +
            settings.childrenNames.split(",", "،").map { it.trim() }.filter { it.isNotBlank() } +
            personRecords.keys)
            .filter { it.isNotBlank() }
            .distinctBy { it.trim().lowercase() }
            .sorted()
    }

    // ——— اعمال فیلترها روی تراکنش‌ها ———
    val dayMs = 24L * 60 * 60 * 1000
    val filteredTx = remember(transactions, dateFilterOn, dateFrom, dateTo, personFilter, accountFilter, bankFilter, mode) {
        transactions
            .asSequence()
            .filter { !it.isDeleted }
            .filter { if (mode == "INCOME") it.type == "INCOME" else it.type == "EXPENSE" }
            .filter {
                !dateFilterOn || (it.date >= dateFrom && it.date < dateTo + dayMs)
            }
            .filter { tx ->
                val p = personFilter ?: return@filter true
                tx.subAccountName == "$PERSON_DOC_PREFIX$p"
            }
            .filter { tx ->
                val acc = accountFilter ?: return@filter true
                tx.subAccountName == acc
            }
            .filter { tx ->
                val bank = bankFilter ?: return@filter true
                tx.bankAccountId == bank
            }
            .sortedByDescending { it.date }
            .toList()
    }
    // اسنادِ شخصِ انتخاب‌شده (ChildrenExpense) هم در نمای هزینه/اشخاص می‌آیند تا
    // فیلترِ «حساب اشخاص» تصویرِ کاملِ همان شخص را نشان دهد.
    val personDocs = remember(childrenExpenses, personFilter, dateFilterOn, dateFrom, dateTo) {
        val p = personFilter ?: return@remember emptyList()
        childrenExpenses
            .filter { !it.isDeleted && it.childName.trim().equals(p.trim(), ignoreCase = true) }
            .filter { !dateFilterOn || (it.date >= dateFrom && it.date < dateTo + dayMs) }
            .sortedByDescending { it.date }
    }

    var txToEdit by remember { mutableStateOf<PersonalTransaction?>(null) }
    var txToDelete by remember { mutableStateOf<PersonalTransaction?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ——— سربرگ ———
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("بازگشت", "Back"))
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    if (mode == "PERSONS") {
                                        val rows = personNames.map { name ->
                                            val profile = personProfiles.firstOrNull { it.fullName.equals(name, ignoreCase = true) }
                                            val signed = personSignedBalance(
                                                name, personRecords, profile
                                            )
                                            ReportGenerators.PersonBalanceRow(name, signed)
                                        }
                                        ReportGenerators.personsBalancesReport(
                                            context, settings.clinicName, settings.doctorName, accent, currency,
                                            t("جاری", "Current"), rows, ::formatAmount
                                        )
                                    } else {
                                        ReportGenerators.personalTransactionsReport(
                                            context, settings.clinicName, settings.doctorName, accent, currency,
                                            title,
                                            if (dateFilterOn) "${viewModel.formatDate(dateFrom)} — ${viewModel.formatDate(dateTo)}" else t("کل دوره", "All time"),
                                            filteredTx,
                                            { viewModel.formatDate(it) }, ::formatAmount
                                        )
                                    }
                                }
                                sharePdf(context, file)
                            } catch (e: Exception) {
                                Toast.makeText(context, e.localizedMessage ?: t("خطا در تولید گزارش", "Report failed"), Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = t("گزارش PDF", "PDF report"), tint = MaterialTheme.colorScheme.error)
                    }
                    // «گزارش تصویری» — همان گزارش با جنریتور اصلی تصویرساز اپ (به‌درخواست کاربر)
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    PdfReportBuilder.renderNextAsImage = true
                                    try {
                                        if (mode == "PERSONS") {
                                            val rows = personNames.map { name ->
                                                val profile = personProfiles.firstOrNull { it.fullName.equals(name, ignoreCase = true) }
                                                val signed = personSignedBalance(name, personRecords, profile)
                                                ReportGenerators.PersonBalanceRow(name, signed)
                                            }
                                            ReportGenerators.personsBalancesReport(
                                                context, settings.clinicName, settings.doctorName, accent, currency,
                                                t("جاری", "Current"), rows, ::formatAmount
                                            )
                                        } else {
                                            ReportGenerators.personalTransactionsReport(
                                                context, settings.clinicName, settings.doctorName, accent, currency,
                                                title,
                                                if (dateFilterOn) "${viewModel.formatDate(dateFrom)} — ${viewModel.formatDate(dateTo)}" else t("کل دوره", "All time"),
                                                filteredTx,
                                                { viewModel.formatDate(it) }, ::formatAmount
                                            )
                                        }
                                    } finally {
                                        PdfReportBuilder.renderNextAsImage = false
                                    }
                                }
                                shareImagesNativeChooser(
                                    context, listOf(file),
                                    t("اشتراک‌گذاری گزارش تصویری", "Share image report")
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, e.localizedMessage ?: t("خطا در تولید گزارش", "Report failed"), Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Image, contentDescription = t("گزارش تصویری", "Image report"), tint = MaterialTheme.colorScheme.secondary)
                    }
                }

                // ——— فیلترها ———
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // بازهٔ تاریخ
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = dateFilterOn,
                                onClick = { dateFilterOn = !dateFilterOn },
                                label = { Text(t("فیلتر تاریخ", "Date filter"), fontSize = 10.sp) }
                            )
                            if (dateFilterOn) {
                                TextButton(onClick = { dateFilterOn = false }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                    Text(t("حذف فیلتر", "Clear"), fontSize = 10.sp)
                                }
                            }
                        }
                        if (dateFilterOn) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                JalaliDateField(
                                    timestamp = dateFrom,
                                    onDateSelected = { dateFrom = it },
                                    isPersian = isPersian,
                                    label = t("از تاریخ", "From"),
                                    formatDate = { viewModel.formatDate(it) },
                                    modifier = Modifier.weight(1f)
                                )
                                JalaliDateField(
                                    timestamp = dateTo,
                                    onDateSelected = { dateTo = it },
                                    isPersian = isPersian,
                                    label = t("تا تاریخ", "To"),
                                    formatDate = { viewModel.formatDate(it) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        when (mode) {
                            "PERSONS", "EXPENSE" -> {
                                // فیلتر حساب اشخاص
                                PersonFilterDropdown(
                                    selected = personFilter,
                                    options = personNames,
                                    isPersian = isPersian,
                                    onSelect = { personFilter = it }
                                )
                            }
                        }
                        when (mode) {
                            "EXPENSE" -> {
                                AccountFilterDropdown(
                                    selected = accountFilter,
                                    options = leafAccounts,
                                    isPersian = isPersian,
                                    allLabel = t("همهٔ حساب‌های شخصی", "All personal accounts"),
                                    label = t("حساب شخصی", "Personal account"),
                                    onSelect = { accountFilter = it }
                                )
                            }
                            "INCOME" -> {
                                AccountFilterDropdown(
                                    selected = accountFilter,
                                    options = leafAccounts.filter { it.category == "INCOME" },
                                    isPersian = isPersian,
                                    allLabel = t("همهٔ انواع درآمد", "All income types"),
                                    label = t("نوع درآمد", "Income type"),
                                    onSelect = { accountFilter = it }
                                )
                                // فیلتر حساب مقصد (بانکی)
                                var bankExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = bankExpanded,
                                    onExpandedChange = { bankExpanded = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val selectedBank = bankAccounts.filter { !it.isDeleted }.firstOrNull { it.id == bankFilter }
                                    OutlinedTextField(
                                        value = selectedBank?.let { "${it.bankName} — ${it.cardNumber.takeLast(4).padStart(4, '•')}" }
                                            ?: t("همهٔ حساب‌های مقصد", "All destination accounts"),
                                        onValueChange = { },
                                        readOnly = true,
                                        label = { Text(t("حساب مقصد (بانکی)", "Destination bank account"), fontSize = 10.sp) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        singleLine = true
                                    )
                                    ExposedDropdownMenu(expanded = bankExpanded, onDismissRequest = { bankExpanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text(t("همهٔ حساب‌های مقصد", "All destination accounts"), fontSize = 11.sp) },
                                            onClick = { bankFilter = null; bankExpanded = false }
                                        )
                                        bankAccounts.filter { !it.isDeleted }.forEach { bank ->
                                            DropdownMenuItem(
                                                text = { Text("${bank.bankName} — ${bank.cardNumber.takeLast(4)}", fontSize = 11.sp) },
                                                onClick = { bankFilter = bank.id; bankExpanded = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (mode == "PERSONS") {
                    PersonsFullListSection(
                        viewModel = viewModel,
                        isPersian = isPersian,
                        currency = currency,
                        personProfiles = personProfiles,
                        personRecords = personRecords,
                        personNames = personNames,
                        dateFilterOn = dateFilterOn,
                        dateFrom = dateFrom,
                        dateTo = dateTo
                    )
                } else {
                    // ——— جمع‌ها ———
                    val total = filteredTx.sumOf { it.amount }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            t("تعداد: ${filteredTx.size + personDocs.size} سند", "${filteredTx.size + personDocs.size} documents"),
                            fontSize = 10.sp, color = Color.Gray
                        )
                        Text(
                            "${t("جمع کل:", "Total:")} ${formatAmount(total)} $currency",
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (mode == "INCOME") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        items(filteredTx, key = { "tx${it.id}" }) { tx ->
                            LedgerTxRow(
                                title = tx.subAccountName.ifBlank { t("سایر", "Other") },
                                desc = tx.description,
                                date = tx.date,
                                amount = tx.amount,
                                currency = currency,
                                isPersian = isPersian,
                                positive = mode == "INCOME",
                                formatDate = { viewModel.formatDate(it) },
                                onEdit = { txToEdit = tx },
                                onDelete = { txToDelete = tx }
                            )
                        }
                        items(personDocs, key = { "doc${it.id}" }) { doc ->
                            LedgerTxRow(
                                title = "${t("حساب اشخاص", "Person")}: ${doc.childName}",
                                desc = doc.title,
                                date = doc.date,
                                amount = doc.amount,
                                currency = currency,
                                isPersian = isPersian,
                                positive = false,
                                formatDate = { viewModel.formatDate(it) },
                                onEdit = null,
                                onDelete = null
                            )
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }

    if (txToEdit != null) {
        EditPersonalTxDialog(
            tx = txToEdit!!,
            bankAccounts = bankAccounts.filter { !it.isDeleted },
            currency = currency,
            isPersian = isPersian,
            viewModel = viewModel,
            onDismiss = { txToEdit = null }
        )
    }
    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text(t("حذف تراکنش شخصی", "Delete Transaction"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(t("حذف تراکنش به مبلغ ${formatAmount(txToDelete!!.amount)} $currency؟", "Delete transaction for ${formatAmount(txToDelete!!.amount)} $currency?")) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deletePersonalTx(txToDelete!!); txToDelete = null },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(t("حذف", "Delete"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { txToDelete = null }) { Text(t("انصراف", "Cancel")) } }
        )
    }
}

/** یک سند در دفتر: عنوان، توضیح، تاریخ، مبلغ + ویرایش/حذف. */
@Composable
private fun LedgerTxRow(
    title: String,
    desc: String,
    date: Long,
    amount: Long,
    currency: String,
    isPersian: Boolean,
    positive: Boolean,
    formatDate: (Long) -> String,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    fun t(fa: String, en: String) = if (isPersian) fa else en
    val color = if (positive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    text = listOfNotNull(desc.takeIf { it.isNotBlank() }, formatDate(date)).joinToString(" — "),
                    fontSize = 10.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${if (positive) "+" else "-"}${formatAmount(amount)} $currency",
                    fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = color
                )
                if (onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = t("ویرایش", "Edit"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = t("حذف", "Delete"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

/** کشوی فیلتر شخص. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonFilterDropdown(
    selected: String?,
    options: List<String>,
    isPersian: Boolean,
    onSelect: (String?) -> Unit
) {
    fun t(fa: String, en: String) = if (isPersian) fa else en
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected ?: t("همهٔ اشخاص", "All persons"),
            onValueChange = { },
            readOnly = true,
            label = { Text(t("حساب اشخاص", "Person account"), fontSize = 10.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(t("همهٔ اشخاص", "All persons"), fontSize = 11.sp) },
                onClick = { onSelect(null); expanded = false }
            )
            options.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name, fontSize = 11.sp) },
                    onClick = { onSelect(name); expanded = false }
                )
            }
        }
    }
}

/** کشوی فیلتر حساب شخصی/نوع درآمد. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFilterDropdown(
    selected: String?,
    options: List<PersonalSubAccount>,
    isPersian: Boolean,
    allLabel: String,
    label: String,
    onSelect: (String?) -> Unit
) {
    fun displayNameOf(sub: PersonalSubAccount): String =
        listOf(sub.grandParentName.trim(), sub.parentName.trim(), sub.name.trim())
            .filter { it.isNotBlank() }.joinToString(" - ")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected ?: allLabel,
            onValueChange = { },
            readOnly = true,
            label = { Text(label, fontSize = 10.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel, fontSize = 11.sp) }, onClick = { onSelect(null); expanded = false })
            options.map(::displayNameOf).sorted().forEach { dn ->
                DropdownMenuItem(text = { Text(dn, fontSize = 11.sp) }, onClick = { onSelect(dn); expanded = false })
            }
        }
    }
}

// ——— داده‌های جریات اشخاص (همان منطق واحدِ «حساب اشخاص») ———

private data class PersonDocRow(val date: Long, val label: String, val amount: Long)

private fun buildPersonDocRows(
    childrenExpenses: List<ChildrenExpense>,
    transactions: List<PersonalTransaction>,
    isPersian: Boolean
): Map<String, List<PersonDocRow>> {
    val map = LinkedHashMap<String, MutableList<PersonDocRow>>()
    val seen = mutableSetOf<String>()
    fun put(person: String, r: PersonDocRow) {
        val key = "${r.date}|${r.amount}|${r.label}"
        if (seen.add(key)) map.getOrPut(person) { mutableListOf() }.add(r)
    }
    childrenExpenses
        .filter { !it.isDeleted && it.childName.isNotBlank() }
        .forEach { e -> put(e.childName.trim(), PersonDocRow(e.date, e.title.trim().ifBlank { if (isPersian) "برداشت بانکی" else "Bank withdrawal" }, e.amount)) }
    transactions
        .filter { !it.isDeleted && it.subAccountName.startsWith(PERSON_DOC_PREFIX) }
        .forEach { tx ->
            val person = tx.subAccountName.removePrefix(PERSON_DOC_PREFIX).trim()
            if (person.isBlank()) return@forEach
            val desc = tx.description.trim()
                .substringBefore(" — $person").trim()
                .ifBlank { if (isPersian) "برداشت بانکی" else "Bank withdrawal" }
            put(person, PersonDocRow(tx.date, desc, tx.amount))
        }
    return map.mapValues { entry -> entry.value.sortedBy { it.date } }
}

/** ماندهٔ علامت‌دار شخص = مبلغ اولیه + جمع اسناد؛ منفی = بدهکار. */
private fun personSignedBalance(
    name: String,
    records: Map<String, List<PersonDocRow>>,
    profile: PersonProfile?
): Long {
    val total = (records[name].orEmpty().sumOf { it.amount }) + (profile?.openingBalance ?: 0L)
    return if (profile?.nature == com.example.data.PersonNature.DEBTOR) -total else total
}

/**
 * بخش «اشخاص» تمام‌صفحه: فرم ثبت/ویرایش با مبلغ اولیه + ستاره + اولویت، و لیستِ
 * فقط‌عنوانِ باز/بسته‌شونده (همان دادهٔ واحد PERSONS — تغییر اینجا همان‌جا در
 * دیالوگ «حساب اشخاص» صفحهٔ اصلی هم دیده می‌شود و برعکس).
 */
@Composable
private fun ColumnScope.PersonsFullListSection(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    currency: String,
    personProfiles: List<PersonProfile>,
    personRecords: Map<String, List<PersonDocRow>>,
    personNames: List<String>,
    dateFilterOn: Boolean,
    dateFrom: Long,
    dateTo: Long
) {
    fun t(fa: String, en: String) = if (isPersian) fa else en

    var showForm by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf(0L) }
    var fFirst by remember { mutableStateOf("") }
    var fLast by remember { mutableStateOf("") }
    var fTitle by remember { mutableStateOf("") }
    var fNature by remember { mutableStateOf(com.example.data.PersonNature.DEBTOR) }
    var fOpeningStr by remember { mutableStateOf("") }
    var fPriorityStr by remember { mutableStateOf("") }
    var fStarred by remember { mutableStateOf(false) }
    var expandedPerson by remember { mutableStateOf<String?>(null) }

    val dayMs = 24L * 60 * 60 * 1000

    // ترتیب نمایش: ستاره‌دارها اول، بعد شمارهٔ اولویت (کوچک‌تر = بالاتر)، بعد نام.
    val orderedPersons = remember(personNames, personProfiles) {
        personNames.sortedWith(
            compareByDescending<String> { name ->
                personProfiles.firstOrNull { it.fullName.equals(name, ignoreCase = true) }?.isStarred == true
            }.thenBy { name ->
                personProfiles.firstOrNull { it.fullName.equals(name, ignoreCase = true) }?.priority ?: 0
            }.thenBy { it }
        )
    }

    // به‌درخواست کاربر: لیست اشخاص در سه بخش — بدهکاران، بستانکاران، اعضای خانواده.
    fun natureOf(name: String): com.example.data.PersonNature {
        val p = personProfiles.firstOrNull { it.fullName.equals(name, ignoreCase = true) }
        return when (p?.nature) {
            com.example.data.PersonNature.NONE -> com.example.data.PersonNature.NONE
            com.example.data.PersonNature.CREDITOR -> com.example.data.PersonNature.CREDITOR
            com.example.data.PersonNature.DEBTOR -> com.example.data.PersonNature.DEBTOR
            else -> if (personSignedBalance(name, personRecords, p) > 0L) com.example.data.PersonNature.CREDITOR
            else com.example.data.PersonNature.DEBTOR
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    t("لیست اشخاص — فقط عنوان؛ برای جریات، لمس کنید:", "Persons — title only; tap to expand:"),
                    fontSize = 10.5.sp, color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                // به‌درخواست کاربر: دکمهٔ مستقل «افزودن عضو خانواده» — عضو خانواده نه بدهکار
                // است نه بستانکار؛ مبالغش به‌عنوان هزینه ثبت می‌شود (ماهیت NONE).
                TextButton(onClick = {
                    editingId = 0L; fFirst = ""; fLast = ""; fTitle = ""
                    fNature = com.example.data.PersonNature.NONE; fOpeningStr = ""; fPriorityStr = ""; fStarred = false
                    showForm = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text(t("عضو خانواده", "Family member"), fontSize = 10.sp)
                }
                TextButton(onClick = {
                    editingId = 0L; fFirst = ""; fLast = ""; fTitle = ""
                    fNature = com.example.data.PersonNature.DEBTOR; fOpeningStr = ""; fPriorityStr = ""; fStarred = false
                    showForm = !showForm
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text(if (showForm) t("بستن فرم", "Close") else t("ثبت شخص", "Add person"), fontSize = 10.sp)
                }
            }
        }
        if (showForm) {
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(
                        // نکتهٔ مهم (رفع کرش): این Column داخل item یک LazyColumn است؛
                        // verticalScroll تو در تو با LazyColumn هم‌جهت، در Compose باعث
                        // IllegalStateException و بسته‌شدن برنامه هنگام باز شدن فرم
                        // (دکمهٔ «ثبت شخص» و قلم ویرایش) می‌شد. اسکرول را خود LazyColumn می‌دهد.
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            if (editingId == 0L) t("ثبت شخص جدید", "New person") else t("ویرایش شخص", "Edit person"),
                            fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = fFirst, onValueChange = { fFirst = it },
                                label = { Text(t("نام", "First name"), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            OutlinedTextField(
                                value = fLast, onValueChange = { fLast = it },
                                label = { Text(t("نام خانوادگی", "Last name"), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = fTitle, onValueChange = { fTitle = it },
                            label = { Text(t("عنوان شخص (تأمین‌کننده، همکار…)", "Person title"), fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // سه ماهیت: بدهکار، بستانکار، عضو خانواده (نه بدهکار نه بستانکار — هزینه)
                            listOf(
                                com.example.data.PersonNature.DEBTOR,
                                com.example.data.PersonNature.CREDITOR,
                                com.example.data.PersonNature.NONE
                            ).forEach { nature ->
                                FilterChip(
                                    selected = fNature == nature,
                                    onClick = { fNature = nature },
                                    label = {
                                        Text(
                                            if (nature == com.example.data.PersonNature.NONE) t("عضو خانواده", "Family") else nature.label(isPersian),
                                            fontSize = 10.sp
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (fNature == com.example.data.PersonNature.NONE) {
                            Text(
                                t("عضو خانواده نه بدهکار است نه بستانکار — مبالغ ثبت‌شده برای او به‌عنوان هزینه منظور می‌شود.", "Family member is neither debtor nor creditor — amounts are recorded as expenses."),
                                fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary
                            )
                        }
                        AmountInputField(
                            value = fOpeningStr,
                            onValueChange = { fOpeningStr = it },
                            label = t("مبلغ اولیه (افتتاحیه)", "Opening balance"),
                            currency = currency,
                            isPersian = isPersian
                        )
                        OutlinedTextField(
                            value = fPriorityStr,
                            onValueChange = { fPriorityStr = it.filter(Char::isDigit).take(3) },
                            label = { Text(t("شمارهٔ اولویت (کوچک‌تر = بالاتر در لیست)", "Priority (lower = higher)"), fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { fStarred = !fStarred }) {
                                Icon(
                                    if (fStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = t("ستاره‌دار", "Starred"),
                                    tint = if (fStarred) Color(0xFFF9A825) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(t("ستاره‌دار (اول همهٔ لیست‌ها)", "Starred (top of all lists)"), fontSize = 10.sp, color = Color.Gray)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    val profile = PersonProfile(
                                        id = editingId,
                                        firstName = fFirst.trim(),
                                        lastName = fLast.trim(),
                                        title = fTitle.trim(),
                                        nature = fNature,
                                        openingBalance = parseGroupedAmount(fOpeningStr),
                                        priority = fPriorityStr.toIntOrNull() ?: 0,
                                        isStarred = fStarred
                                    )
                                    if (profile.fullName.isNotBlank()) {
                                        viewModel.savePersonProfile(profile)
                                        showForm = false
                                    }
                                },
                                enabled = fFirst.isNotBlank() || fLast.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (editingId == 0L) t("ثبت", "Save") else t("به‌روزرسانی", "Update"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            if (editingId != 0L) {
                                TextButton(onClick = { showForm = false }) { Text(t("انصراف", "Cancel"), fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
        item {
            val debtorCount = orderedPersons.count { natureOf(it) == com.example.data.PersonNature.DEBTOR }
            val creditorCount = orderedPersons.count { natureOf(it) == com.example.data.PersonNature.CREDITOR }
            val familyCount = orderedPersons.size - debtorCount - creditorCount
            Text(
                t(
                    "${orderedPersons.size} شخص  |  بدهکاران: $debtorCount  |  بستانکاران: $creditorCount  |  اعضای خانواده: $familyCount",
                    "${orderedPersons.size} persons | debtors: $debtorCount | creditors: $creditorCount | family: $familyCount"
                ),
                fontSize = 10.5.sp, color = Color.Gray, fontWeight = FontWeight.Bold
            )
        }
        listOf(
            Triple(t("بدهکاران", "Debtors"), com.example.data.PersonNature.DEBTOR, Color(0xFFC62828)),
            Triple(t("بستانکاران", "Creditors"), com.example.data.PersonNature.CREDITOR, Color(0xFF2E7D32)),
            Triple(t("اعضای خانواده", "Family members"), com.example.data.PersonNature.NONE, Color(0xFF1565C0))
        ).forEach { (secTitle, secNature, secColor) ->
            val secNames = orderedPersons.filter { natureOf(it) == secNature }
            item(key = "sechdr-$secTitle") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Text(secTitle, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = secColor)
                    Text(
                        if (isPersian) "«${secNames.size} نفر»" else "(${secNames.size})",
                        fontSize = 10.sp, color = Color.Gray
                    )
                }
            }
            if (secNames.isEmpty()) {
                item(key = "secempty-$secTitle") {
                    Text(t("موردی ثبت نشده است.", "Nothing recorded."), fontSize = 10.sp, color = Color.Gray)
                }
            }
            items(secNames, key = { "person-$secTitle-$it" }) { name ->
            val profile = personProfiles.firstOrNull { it.fullName.equals(name, ignoreCase = true) }
            val records = personRecords[name].orEmpty()
                .filter { !dateFilterOn || (it.date >= dateFrom && it.date < dateTo + dayMs) }
            val signed = personSignedBalance(name, personRecords, profile)
            val expanded = expandedPerson == name
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expandedPerson = if (expanded) null else name },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            if (profile?.isStarred == true) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF9A825), modifier = Modifier.size(15.dp))
                            }
                            // طبق درخواست: در حالت بسته فقط عنوان نشان داده می‌شود
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (expanded) {
                                IconButton(onClick = {
                                    editingId = profile?.id ?: 0L
                                    fFirst = profile?.firstName.orEmpty()
                                    fLast = profile?.lastName.orEmpty()
                                    fTitle = profile?.title.orEmpty()
                                    fNature = profile?.nature ?: com.example.data.PersonNature.DEBTOR
                                    fOpeningStr = if ((profile?.openingBalance ?: 0L) > 0L) formatAmount(profile!!.openingBalance) else ""
                                    fPriorityStr = (profile?.priority ?: 0).takeIf { it > 0 }?.toString() ?: ""
                                    fStarred = profile?.isStarred == true
                                    showForm = true
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = t("ویرایش", "Edit"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                }
                            }
                            Text(
                                "${formatAmount(kotlin.math.abs(signed))} $currency",
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (signed >= 0L) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        val subtitle = listOfNotNull(
                            profile?.title?.takeIf { it.isNotBlank() },
                            profile?.nature?.let { n ->
                                if (n == com.example.data.PersonNature.NONE) t("عضو خانواده — هزینه", "Family member — expense")
                                else n.label(isPersian)
                            },
                            (profile?.openingBalance ?: 0L).takeIf { it != 0L }?.let { t("مبلغ اولیه: ${formatAmount(it)} $currency", "Opening: ${formatAmount(it)} $currency") }
                        ).joinToString(" • ")
                        if (subtitle.isNotBlank()) Text(subtitle, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary)
                        if (records.isEmpty()) {
                            Text(t("سندی ثبت نشده است.", "No documents."), fontSize = 10.sp, color = Color.Gray)
                        } else {
                            var running = 0L
                            records.sortedBy { it.date }.forEach { r ->
                                running += r.amount
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(r.label, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(viewModel.formatDate(r.date), fontSize = 9.sp, color = Color.Gray)
                                    }
                                    Text("${formatAmount(r.amount)} $currency", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(t("جمع اسناد + مبلغ اولیه", "Docs + opening"), fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    "${formatAmount(running + (profile?.openingBalance ?: 0L))} $currency",
                                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (signed >= 0L) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
