@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FixedIncome
import com.example.data.PersonalSubAccount
import com.example.ui.DentalViewModel

/**
 * دیالوگ‌های مدیریتی بخش تنظیمات: درآمد‌های ثابت و سرفصل‌های شخصی.
 */

// ==========================================
// مدیریت درآمدهای ثابت
// ==========================================
@Composable
fun ManageFixedIncomesDialog(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
    val fixedIncomes by viewModel.fixedIncomes.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val currency = viewModel.label("currency")

    fun t(fa: String, en: String) = if (isPersian) fa else en

    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var dayStr by remember { mutableStateOf("1") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var incomeToDelete by remember { mutableStateOf<FixedIncome?>(null) }

    val active = fixedIncomes.filter { !it.isDeleted }.sortedByDescending { it.amount }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(t("مدیریت درآمدهای ثابت", "Manage Fixed Incomes"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showForm = !showForm },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showForm) t("بستن فرم", "Close Form") else t("افزودن درآمد ثابت", "Add Fixed Income"), fontWeight = FontWeight.Bold)
                }

                if (showForm) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            OutlinedTextField(
                                value = title, onValueChange = { title = it },
                                label = { Text(t("عنوان (مثلاً اجاره مطب)", "Title (e.g. Clinic Rent)")) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = amountStr, onValueChange = { amountStr = it.filter(Char::isDigit) },
                                    label = { Text("${t("مبلغ", "Amount")} ($currency)") },
                                    modifier = Modifier.weight(1f), singleLine = true
                                )
                                OutlinedTextField(
                                    value = dayStr, onValueChange = { dayStr = it.filter(Char::isDigit).take(2) },
                                    label = { Text(t("روز ماه", "Day")) },
                                    modifier = Modifier.weight(1f), singleLine = true
                                )
                            }
                            Text(t("حساب بانکی واریز (اختیاری):", "Bank account (optional):"), fontSize = 10.sp, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScrollCompat2(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedAccountId == null,
                                    onClick = { selectedAccountId = null },
                                    label = { Text(t("بدون حساب", "None"), fontSize = 10.sp) }
                                )
                                bankAccounts.filter { !it.isDeleted }.forEach { acc ->
                                    FilterChip(
                                        selected = selectedAccountId == acc.id,
                                        onClick = { selectedAccountId = acc.id },
                                        label = { Text(acc.bankName, fontSize = 10.sp) }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    val amount = amountStr.trim().toLongOrNull() ?: 0L
                                    val day = dayStr.trim().toIntOrNull() ?: 1
                                    if (title.isNotBlank() && amount > 0) {
                                        val accName = bankAccounts.find { it.id == selectedAccountId }?.bankName ?: ""
                                        viewModel.addFixedIncome(
                                            title = title.trim(),
                                            amount = amount,
                                            dayOfMonth = day.coerceIn(1, 31),
                                            bankAccountId = selectedAccountId,
                                            bankAccountName = accName,
                                            subAccountName = if (isPersian) "درآمد ثابت" else "Fixed Income"
                                        )
                                        title = ""; amountStr = ""; dayStr = "1"; selectedAccountId = null
                                        showForm = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = title.isNotBlank() && (amountStr.trim().toLongOrNull() ?: 0L) > 0
                            ) { Text(t("ثبت درآمد ثابت", "Save"), fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f, fill = false)) {
                    if (active.isEmpty()) {
                        item {
                            Text(
                                text = t("هنوز درآمد ثابتی تعریف نشده است.", "No fixed incomes yet."),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    items(active, key = { it.id }) { income ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(income.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        text = t(
                                            "روز ${income.dayOfMonth} هر ماه${if (income.bankAccountName.isNotBlank()) " — ${income.bankAccountName}" else ""}",
                                            "Day ${income.dayOfMonth} of each month"
                                        ),
                                        fontSize = 9.5.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = "${formatAmount(income.amount)} $currency",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                IconButton(onClick = { incomeToDelete = income }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
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

    if (incomeToDelete != null) {
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = { Text(t("حذف درآمد ثابت", "Delete Fixed Income"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(t("«${incomeToDelete!!.title}» حذف شود؟", "Delete ${incomeToDelete!!.title}?")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFixedIncome(incomeToDelete!!)
                        incomeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(t("حذف", "Delete"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) { Text(t("انصراف", "Cancel")) }
            }
        )
    }
}

// ==========================================
// مدیریت سرفصل‌های شخصی (زیرحساب‌ها)
// ==========================================
@Composable
fun ManageSubAccountsDialog(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
    val subAccounts by viewModel.personalSubAccounts.collectAsStateWithLifecycle()
    val childrenExpenses by viewModel.childrenExpenses.collectAsStateWithLifecycle()
    val personalTransactions by viewModel.personalTransactions.collectAsStateWithLifecycle()

    fun t(fa: String, en: String) = if (isPersian) fa else en

    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("EXPENSE") }
    var parentName by remember { mutableStateOf("") }
    var grandParentName by remember { mutableStateOf("") }
    // برای «اشخاص»: مبلغ اولیه + اولویت + ستاره (همان دادهٔ واحدِ بخش اشخاص)
    var openingStr by remember { mutableStateOf("") }
    var priorityStr by remember { mutableStateOf("") }
    var starred by remember { mutableStateOf(false) }
    var subToEdit by remember { mutableStateOf<PersonalSubAccount?>(null) }
    var subToDelete by remember { mutableStateOf<PersonalSubAccount?>(null) }
    // حافظهٔ وضعیتِ باز/بستهٔ هر گره در درخت (کلید = نام سرشاخه/شاخه). با
    // rememberSaveable بین چرخش صفحه و باز/بست دیالوگ حفظ می‌شود.
    val expandedNodes = rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver(
        save = { it.toList() }, restore = { it.toMutableStateList() }
    )) { mutableStateListOf<String>() }
    fun toggleNode(key: String) { if (key in expandedNodes) expandedNodes.remove(key) else expandedNodes.add(key) }

    val active = remember(subAccounts) {
        // Deduplicate by (category|parent|name) so repeated headings never appear in the list,
        // even if leftover duplicates from older builds are still in the local database.
        subAccounts
            .filter { !it.isDeleted }
            .distinctBy { "${it.category.trim()}|${it.parentName.trim()}|${it.name.trim()}" }
            .sortedBy { it.name }
    }
    val availableParents = remember(subAccounts) {
        subAccounts.map { it.parentName.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableGrandParents = remember(subAccounts) {
        subAccounts.map { it.grandParentName.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val persons = remember(subAccounts) {
        subAccounts.filter { !it.isDeleted && it.category == com.example.data.PERSONS_CATEGORY }
    }
    val personDocs = remember(childrenExpenses, personalTransactions) {
        val map = mutableMapOf<String, Long>()
        childrenExpenses.filter { !it.isDeleted && it.childName.isNotBlank() }
            .forEach { map[it.childName.trim()] = (map[it.childName.trim()] ?: 0L) + it.amount }
        personalTransactions.filter { !it.isDeleted && it.subAccountName.startsWith("حساب اشخاص: ") }
            .forEach {
                val n = it.subAccountName.removePrefix("حساب اشخاص: ").trim()
                if (n.isNotBlank()) map[n] = (map[n] ?: 0L) + it.amount
            }
        map
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(t("مدیریت سرفصل‌های شخصی (سرفصل حساب ▸ حساب ▸ زیرحساب)", "Manage personal chart of accounts"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        subToEdit = null
                        name = ""
                        parentName = ""
                        grandParentName = ""
                        openingStr = ""; priorityStr = ""; starred = false
                        showForm = !showForm
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showForm) t("بستن فرم", "Close Form") else t("افزودن زیرحساب", "Add sub-account"), fontWeight = FontWeight.Bold)
                }

                if (showForm) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                text = if (subToEdit != null) t("ویرایش زیرحساب و لینک به بالا", "Edit sub-account & link to parent") else t("افزودن زیرحساب جدید", "New sub-account"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                t("ساختار استاندارد حسابداری سه‌سطحی: سرفصل حساب ▸ حساب ▸ زیرحساب. فقط «زیرحساب»‌ها در تراکنش‌ها شرکت می‌کنند.",
                                    "Standard 3-level chart of accounts: Heading ▸ Account ▸ Sub-account. Only sub-accounts appear in transactions."),
                                fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = name, onValueChange = { name = it },
                                label = { Text(t("نام زیرحساب (سطح آخر، مثلاً هزینه خانه)", "Sub-account name (leaf, e.g. Home)")) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true
                            )
                            OutlinedTextField(
                                value = parentName, onValueChange = { parentName = it },
                                label = { Text(t("حساب (والد، اختیاری)", "Account (parent, optional)")) },
                                placeholder = { Text(t("مثلاً: هزینه‌های عمومی", "e.g. General Expenses")) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true
                            )
                            if (availableParents.isNotEmpty()) {
                                Text(t("انتخاب از حساب‌های موجود:", "Pick existing account:"), fontSize = 10.sp, color = Color.Gray)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScrollCompat2(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    availableParents.take(6).forEach { p ->
                                        SuggestionChip(
                                            onClick = { parentName = p },
                                            label = { Text(p, fontSize = 9.5.sp) }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = grandParentName, onValueChange = { grandParentName = it },
                                label = { Text(t("سرفصل حساب (سطح ریشه، اختیاری)", "Account heading (root, optional)")) },
                                placeholder = { Text(t("مثلاً: هزینه‌ها", "e.g. Expenses")) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true
                            )
                            if (availableGrandParents.isNotEmpty()) {
                                Text(t("انتخاب از سرفصل‌های موجود:", "Pick existing heading:"), fontSize = 10.sp, color = Color.Gray)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScrollCompat2(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    availableGrandParents.take(6).forEach { gp ->
                                        SuggestionChip(
                                            onClick = { grandParentName = gp },
                                            label = { Text(gp, fontSize = 9.5.sp) }
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScrollCompat2(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "EXPENSE" to t("هزینه", "Expense"),
                                    "INCOME" to t("درآمد", "Income"),
                                    "DRAW" to t("برداشت", "Draw"),
                                    "ASSET" to t("دارایی", "Asset"),
                                    "PERSONS" to t("اشخاص", "Persons")
                                ).forEach { (key, labelTxt) ->
                                    FilterChip(
                                        selected = category == key,
                                        onClick = { category = key },
                                        label = { Text(labelTxt, fontSize = 10.sp) }
                                    )
                                }
                            }
                            // برای «اشخاص»: مبلغ اولیه + اولویت + ستاره (مدیریت واحد با بخش حساب اشخاص)
                            if (category == "PERSONS") {
                                com.example.ui.components.AmountInputField(
                                    value = openingStr,
                                    onValueChange = { openingStr = it },
                                    label = t("مبلغ اولیه (افتتاحیه)", "Opening balance"),
                                    currency = viewModel.label("currency"),
                                    isPersian = isPersian
                                )
                                OutlinedTextField(
                                    value = priorityStr,
                                    onValueChange = { priorityStr = it.filter(Char::isDigit).take(3) },
                                    label = { Text(t("شمارهٔ اولویت (کوچک‌تر = اول لیست‌ها)", "Priority (lower = first)"), fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { starred = !starred }) {
                                        Icon(
                                            if (starred) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = t("ستاره‌دار", "Starred"),
                                            tint = if (starred) Color(0xFFF9A825) else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(t("ستاره‌دار — در تراکنش‌ها اول لیست ظاهر می‌شود", "Starred — appears first in transaction pickers"), fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Button(
                                onClick = {
                                    if (name.isNotBlank()) {
                                        val opening = com.example.ui.components.parseGroupedAmount(openingStr)
                                        val priority = priorityStr.toIntOrNull() ?: 0
                                        if (subToEdit == null) {
                                            if (category == "PERSONS") {
                                                // اشخاص: همان رکورد واحد PERSONS (دیده‌شده در «حساب اشخاص»)
                                                viewModel.updatePersonalSubAccount(
                                                    PersonalSubAccount(
                                                        name = name.trim(), category = "PERSONS",
                                                        parentName = parentName.trim(), grandParentName = grandParentName.trim(),
                                                        ledgerSide = "NONE", openingBalance = opening, priority = priority,
                                                        isQuickSelect = starred
                                                    )
                                                )
                                            } else {
                                                viewModel.addPersonalSubAccount(name.trim(), category, parentName.trim(), grandParentName = grandParentName.trim())
                                            }
                                        } else {
                                            viewModel.updatePersonalSubAccount(
                                                subToEdit!!.copy(
                                                    name = name.trim(), category = category,
                                                    parentName = parentName.trim(), grandParentName = grandParentName.trim(),
                                                    openingBalance = if (category == "PERSONS") opening else subToEdit!!.openingBalance,
                                                    priority = if (category == "PERSONS") priority else subToEdit!!.priority,
                                                    isQuickSelect = if (category == "PERSONS") starred else subToEdit!!.isQuickSelect
                                                )
                                            )
                                        }
                                        name = ""
                                        parentName = ""
                                        grandParentName = ""
                                        openingStr = ""; priorityStr = ""; starred = false
                                        subToEdit = null
                                        showForm = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = name.isNotBlank()
                            ) {
                                Text(
                                    if (subToEdit != null) t("ذخیره تغییرات و لینک", "Save Changes & Link") else t("ثبت زیرحساب", "Save"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = t(
                        "درختِ کرکره‌ای با اصطلاحات استاندارد حسابداری: «سرفصل حساب ▸ حساب ▸ زیرحساب»؛ فقط زیرحساب‌ها در تراکنش‌ها شرکت می‌کنند. هر حساب در انتها سطرِ «سایر / متفرقه» و «افزودن» دارد و حساب‌های اشخاص (بستانکار/بدهکار) در انتهای لیست، هم‌بسته با بخش «حساب اشخاص» مدیریت می‌شوند.",
                        "Collapsible tree with standard accounting terms: Heading ▸ Account ▸ Sub-account; only sub-accounts join transactions. Each account ends with an \"Other/Misc\" row and an add row; person accounts (creditor/debtor) are managed at the end, unified with the Persons section."
                    ),
                    fontSize = 9.5.sp,
                    color = Color.Gray
                )

                fun categoryLabel(c: String) = when (c) {
                    "EXPENSE" -> t("هزینه", "Expense"); "INCOME" -> t("درآمد", "Income")
                    "DRAW" -> t("برداشت", "Draw"); "ASSET" -> t("دارایی", "Asset")
                    "DEBT" -> t("بدهی", "Debt"); "CAPITAL" -> t("سرمایه", "Capital")
                    "PERSONS" -> t("اشخاص", "Persons"); else -> c
                }

                // ساختار استاندارد حسابداری: سرفصل حساب (root) ▸ حساب (branch) ▸ زیرحساب (leaf).
                // به‌درخواست کاربر: سطرِ «(بدون سرشاخه)» حذف شد — حساب‌های بدونِ سرفصل
                // خودشان در سطح اول (به‌عنوان حسابِ مستقل) باز می‌شوند و زیرحساب‌های بدونِ
                // حساب هم مستقیم به‌صورت برگِ سطح اول دیده می‌شوند؛ هیچ سطرِ شبه‌گروهی
                // نمایش داده نمی‌شود. اشخاص هم از درخت جدا و در بخش اختصاصیِ انتهای
                // لیست می‌آیند.
                val nonPersons = remember(active) { active.filter { it.category != com.example.data.PERSONS_CATEGORY } }
                val tree = remember(nonPersons) {
                    nonPersons.groupBy { it.grandParentName.trim() }
                        .toSortedMap(compareBy { it })
                        .mapValues { (_, branchList) ->
                            branchList.groupBy { it.parentName.trim() }
                                .toSortedMap(compareBy { it })
                        }
                }
                val rootedTree = remember(tree) { tree.filterKeys { it.isNotBlank() } }
                val rootlessBranches = remember(tree) { tree[""].orEmpty().filterKeys { it.isNotBlank() } }
                val rootlessLeaves = remember(tree) { tree[""].orEmpty()[""].orEmpty() }

                val miscName = t("سایر / متفرقه", "Other / Misc")
                fun dominantCategory(leaves: List<PersonalSubAccount>): String =
                    leaves.groupBy { it.category }.maxByOrNull { (_, v) -> v.size }?.key ?: "EXPENSE"

                /** سطر پایانیِ «سایر / متفرقه» — با لمس، زیرحسابِ واقعیِ هم‌نام در همان حساب ساخته می‌شود. */
                @Composable
                fun MiscRow(branch: String, grand: String, leaves: List<PersonalSubAccount>, indent: Int) {
                    if (leaves.any { it.name.trim() == miscName }) return
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (indent * 14).dp)
                            .clickable {
                                viewModel.addPersonalSubAccount(miscName, dominantCategory(leaves), branch, grandParentName = grand)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Text(t("سایر / متفرقه", "Other / Misc"), fontSize = 11.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }

                /** سطر پایانیِ «＋ اضافه کردن» — فرمِ افزودن را برای همان حساب پیش‌پر می‌کند. */
                @Composable
                fun AddToBranchRow(branch: String, grand: String, leaves: List<PersonalSubAccount>, indent: Int) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (indent * 14).dp)
                            .clickable {
                                subToEdit = null
                                parentName = branch
                                grandParentName = grand
                                category = dominantCategory(leaves)
                                name = ""
                                openingStr = ""; priorityStr = ""; starred = false
                                showForm = true
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Text(t("افزودن زیرحساب به همین حساب", "Add sub-account to this account"), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                @Composable
                fun LeafRow(sub: PersonalSubAccount, indent: Int) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(start = (indent * 14).dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(t("دسته: ", "Category: ") + categoryLabel(sub.category), fontSize = 9.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { viewModel.updatePersonalSubAccount(sub.copy(isQuickSelect = !sub.isQuickSelect)) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        if (sub.isQuickSelect) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = t("اولویت پیشنهادی", "Priority"),
                                        tint = if (sub.isQuickSelect) Color(0xFFF9A825) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        subToEdit = sub
                                        name = sub.name
                                        category = sub.category
                                        parentName = sub.parentName
                                        grandParentName = sub.grandParentName
                                        openingStr = if (sub.openingBalance > 0L) formatAmount(sub.openingBalance) else ""
                                        priorityStr = if (sub.priority > 0) sub.priority.toString() else ""
                                        starred = sub.isQuickSelect
                                        showForm = true
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp)) }
                                IconButton(onClick = { subToDelete = sub }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }

                @Composable
                fun NodeHeader(label: String, count: Int, expanded: Boolean, indent: Int, onToggle: () -> Unit) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(start = (indent * 14).dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (indent == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                        onClick = onToggle
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowLeft,
                                contentDescription = null, modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("(${if (isPersian) com.example.util.PersianNumberFormatter.toPersianDigits(count.toString()) else count})", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f, fill = false)) {
                    if (active.isEmpty()) {
                        item {
                            Text(
                                text = t("هنوز زیرحسابی تعریف نشده است.", "No sub-accounts yet."),
                                fontSize = 11.sp, color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    // ۱) سرفصل‌های واقعی (دارای عنوان) ▸ حساب‌ها ▸ زیرحساب‌ها
                    rootedTree.forEach { (grand, branches) ->
                        val grandKey = "G:$grand"
                        val grandCount = branches.values.sumOf { it.size }
                        val grandExpanded = grandKey in expandedNodes
                        item(key = grandKey) {
                            NodeHeader(grand, grandCount, grandExpanded, 0) { toggleNode(grandKey) }
                        }
                        if (grandExpanded) {
                            branches.forEach { (branch, leaves) ->
                                if (branch.isBlank()) {
                                    // زیرحساب‌هایی که مستقیم زیرِ سرفصلند (بدون حساب میانی)
                                    items(leaves.sortedBy { it.name }, key = { "L:${it.id}" }) { sub -> LeafRow(sub, 1) }
                                    item(key = "$grandKey|misc") { MiscRow("", grand, leaves, 1) }
                                    item(key = "$grandKey|add") { AddToBranchRow("", grand, leaves, 1) }
                                } else {
                                    val branchKey = "$grandKey|B:$branch"
                                    val branchExpanded = branchKey in expandedNodes
                                    item(key = branchKey) {
                                        NodeHeader(branch, leaves.size, branchExpanded, 1) { toggleNode(branchKey) }
                                    }
                                    if (branchExpanded) {
                                        items(leaves.sortedBy { it.name }, key = { "L:${it.id}" }) { sub -> LeafRow(sub, 2) }
                                        item(key = "$branchKey|misc") { MiscRow(branch, grand, leaves, 2) }
                                        item(key = "$branchKey|add") { AddToBranchRow(branch, grand, leaves, 2) }
                                    }
                                }
                            }
                        }
                    }
                    // ۲) حساب‌های بدون سرفصل — به‌جای سطر «(بدون سرشاخه)» خودشان سطح اول‌اند
                    rootlessBranches.forEach { (branch, leaves) ->
                        val branchKey = "G:$branch"
                        val branchExpanded = branchKey in expandedNodes
                        item(key = branchKey) {
                            NodeHeader(branch, leaves.size, branchExpanded, 0) { toggleNode(branchKey) }
                        }
                        if (branchExpanded) {
                            items(leaves.sortedBy { it.name }, key = { "L:${it.id}" }) { sub -> LeafRow(sub, 1) }
                            item(key = "$branchKey|misc") { MiscRow(branch, "", leaves, 1) }
                            item(key = "$branchKey|add") { AddToBranchRow(branch, "", leaves, 1) }
                        }
                    }
                    // ۳) زیرحساب‌های بدون حساب و بدون سرفصل — برگِ سطح اول (بدون سطر شبه‌گروه)
                    items(rootlessLeaves.sortedBy { it.name }, key = { "L:${it.id}" }) { sub -> LeafRow(sub, 0) }

                    // ——— حساب‌های اشخاص به تفکیک بستانکار / بدهکار (مدیریت واحد با «حساب اشخاص») ———
                    if (persons.isNotEmpty()) {
                        item(key = "personsHeader") {
                            Text(
                                t("حساب‌های اشخاص (بستانکار / بدهکار) — همان دادهٔ واحد بخش «حساب اشخاص»", "Person accounts (creditor / debtor) — same unified data as the Persons section"),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        listOf(
                            "CREDITOR" to t("بستانکاران", "Creditors"),
                            "DEBTOR" to t("بدهکاران", "Debtors"),
                            "NONE" to t("بدون ماهیت", "Unspecified")
                        ).forEach { (side, sideLabel) ->
                            val group = persons.filter { it.ledgerSide == side }
                            if (group.isNotEmpty()) {
                                item(key = "personsSide$side") {
                                    Text(sideLabel, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                                }
                                items(group.sortedWith(compareByDescending<PersonalSubAccount> { it.isQuickSelect }.thenBy { it.priority }.thenBy { it.name }), key = { "P:${it.id}" }) { person ->
                                    val total = (personDocs[person.name.trim()] ?: 0L) + person.openingBalance
                                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    if (person.isQuickSelect) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = t("ستاره", "Star"),
                                                    tint = if (person.isQuickSelect) Color(0xFFF9A825) else Color.Gray,
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clickable { viewModel.updatePersonalSubAccount(person.copy(isQuickSelect = !person.isQuickSelect, updatedAt = System.currentTimeMillis())) }
                                                )
                                                Column {
                                                    Text(person.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    if (person.parentName.isNotBlank()) Text(person.parentName, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (person.priority > 0) {
                                                    Text(t("اولویت ${person.priority}", "P${person.priority}"), fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(end = 4.dp))
                                                }
                                                Text(
                                                    "${formatAmount(total)} ${viewModel.label("currency")}",
                                                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                                    color = if (side == "DEBTOR") MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                                )
                                                IconButton(onClick = {
                                                    subToEdit = person
                                                    name = person.name
                                                    category = "PERSONS"
                                                    parentName = person.parentName
                                                    grandParentName = person.grandParentName
                                                    openingStr = if (person.openingBalance > 0L) formatAmount(person.openingBalance) else ""
                                                    priorityStr = if (person.priority > 0) person.priority.toString() else ""
                                                    starred = person.isQuickSelect
                                                    showForm = true
                                                }, modifier = Modifier.size(28.dp)) {
                                                    Icon(Icons.Default.Edit, contentDescription = t("ویرایش", "Edit"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                }
                                                IconButton(onClick = { subToDelete = person }, modifier = Modifier.size(28.dp)) {
                                                    Icon(Icons.Default.Delete, contentDescription = t("حذف", "Delete"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
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

    if (subToDelete != null) {
        AlertDialog(
            onDismissRequest = { subToDelete = null },
            title = { Text(t("حذف زیرحساب", "Delete sub-account"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(t("زیرحساب «${subToDelete!!.name}» حذف شود؟", "Delete ${subToDelete!!.name}?")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePersonalSubAccount(subToDelete!!)
                        subToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(t("حذف", "Delete"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { subToDelete = null }) { Text(t("انصراف", "Cancel")) }
            }
        )
    }
}

/** اسکرول افقی برای ردیف چیپ‌ها (کمک‌تابع). */
@Composable
private fun Modifier.horizontalScrollCompat2(): Modifier = this.horizontalScroll(rememberScrollState())
