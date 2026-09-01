package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.DentalViewModel
import com.example.ui.components.AssistantMultiSelectField
import com.example.ui.components.parseAssistantSyncIds
import com.example.ui.components.joinAssistantSyncIds
import com.example.util.IranianProvinces
import kotlinx.coroutines.launch

/**
 * فهرست هشدارهای پزشکی و بیماری‌های شایع دندانپزشکی
 */
val DENTAL_MEDICAL_ALERT_ITEMS = listOf(
    "حساسیت به پنی‌سیلین",
    "حساسیت به لیدوکائین",
    "حساسیت به لاتکس",
    "حساسیت به آسپرین / NSAIDs",
    "حساسیت دارویی (سایر)",
    "مصرف آسپرین / وارفارین",
    "مصرف رقیق‌کننده خون",
    "اختلال انعقادی / هموفیلی",
    "دیابت (نوع ۱)",
    "دیابت (نوع ۲)",
    "دیابت وابسته به انسولین",
    "فشار خون بالا",
    "بیماری قلبی / Pace Maker",
    "سابقه سکته مغزی / قلبی",
    "نارسایی قلبی",
    "پوکی استخوان (بیس‌فسفونات)",
    "استئونکروز فک",
    "شیمی‌درمانی / پرتودرمانی",
    "بارداری / شیردهی",
    "بارداری (سه ماهه اول)",
    "آسم / مشکلات تنفسی",
    "تشنج / صرع",
    "نارسایی کلیوی / دیالیز",
    "بیماری کبدی / هپاتیت (B/C)",
    "هپاتیت B",
    "هپاتیت C",
    "ایدز (HIV)",
    "مشکلات تیروئید (کم‌کار/پرکار)",
    "سیگار / دخانیات",
    "کم‌خونی (آنمی)",
    "آرتریت روماتوئید"
)

val DENTAL_MEDICATIONS_ITEMS = listOf(
    "وارفارین",
    "آسپرین",
    "کلوپیدوگرل (پلاویکس)",
    "ریواروکسابان",
    "آپیکسابان",
    "هپارین / انوکساپارین",
    "آلندرونات (فوسامکس)",
    "زولدرونیک اسید",
    "دنوسوماب (پرولیا)",
    "پردنیزولون",
    "دگزامتازون",
    "انسولین",
    "متفورمین",
    "نیتروگلیسیرین",
    "پنی‌سیلین",
    "آموکسی‌سیلین",
    "کلیندامایسین",
    "مترونیدازول",
    "NSAIDs / ایبوپروفن",
    "استامینوفن کدئین"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvinceDropdown(
    selectedProvince: String,
    onProvinceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPersian: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val provinces = remember { IranianProvinces.getProvinces() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedProvince,
            onValueChange = {},
            readOnly = true,
            label = { Text(if (isPersian) "استان" else "Province", fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            provinces.forEach { prov ->
                DropdownMenuItem(
                    text = { Text(prov, fontSize = 13.sp) },
                    onClick = {
                        onProvinceSelected(prov)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityDropdown(
    province: String,
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPersian: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val cities = remember(province) {
        if (province.isBlank()) emptyList() else IranianProvinces.getCities(province)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (province.isNotBlank()) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCity,
            onValueChange = { onCitySelected(it) },
            readOnly = false,
            label = { Text(if (isPersian) "شهرستان / شهر" else "City", fontSize = 12.sp) },
            placeholder = { Text(if (province.isBlank()) (if (isPersian) "ابتدا استان" else "Pick province") else "", fontSize = 11.sp) },
            trailingIcon = { if (province.isNotBlank()) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true,
            enabled = province.isNotBlank()
        )
        if (cities.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                cities.forEach { city ->
                    DropdownMenuItem(
                        text = { Text(city, fontSize = 13.sp) },
                        onClick = {
                            onCitySelected(city)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Composable
fun PatientTouchSignatureDialog(
    patientName: String,
    isPersian: Boolean = true,
    onDismiss: () -> Unit,
    onSignatureSaved: (savedFilePath: String) -> Unit
) {
    val context = LocalContext.current
    val pathList = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var drawTrigger by remember { mutableIntStateOf(0) }
    var boxSizePx by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPersian) "ثبت امضای دیجیتال بیمار: $patientName" else "Digital Signature: $patientName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = if (isPersian) "لطفاً امضای خود را با انگشت یا قلم در کادر زیر رسم نمایید:" else "Please sign with finger or stylus inside the box:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .onSizeChanged { boxSizePx = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newP = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath = newP
                                    drawTrigger++
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPath?.let { p ->
                                        p.lineTo(change.position.x, change.position.y)
                                    }
                                    drawTrigger++
                                },
                                onDragEnd = {
                                    currentPath?.let { p ->
                                        pathList.add(p)
                                        currentPath = null
                                    }
                                    drawTrigger++
                                }
                            )
                        }
                ) {
                    val dummy = drawTrigger
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (dummy >= 0) {
                            pathList.forEach { path ->
                                drawPath(path = path, color = Color.Blue, style = Stroke(width = 5f))
                            }
                            currentPath?.let { path ->
                                drawPath(path = path, color = Color.Blue, style = Stroke(width = 5f))
                            }
                        }
                    }
                    if (pathList.isEmpty() && currentPath == null) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Draw, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(32.dp))
                            Text(
                                text = if (isPersian) "محل رسم امضا" else "Sign here",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (pathList.isNotEmpty()) {
                                pathList.removeAt(pathList.size - 1)
                                drawTrigger++
                            }
                        },
                        enabled = pathList.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPersian) "بازگشت" else "Undo", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = {
                            pathList.clear()
                            drawTrigger++
                        },
                        enabled = pathList.isNotEmpty()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPersian) "پاک کردن" else "Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isPersian) "انصراف" else "Cancel")
                    }
                    Button(
                        onClick = {
                            val bmpWidth = if (boxSizePx.width > 0) boxSizePx.width else 800
                            val bmpHeight = if (boxSizePx.height > 0) boxSizePx.height else 400
                            val bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(android.graphics.Color.TRANSPARENT)
                            val paint = Paint().apply {
                                color = android.graphics.Color.BLUE
                                style = Paint.Style.STROKE
                                strokeWidth = 8f
                                isAntiAlias = true
                            }
                            pathList.forEach { p ->
                                canvas.drawPath(p.asAndroidPath(), paint)
                            }
                            val savedPath = saveBitmapToLocalFile(
                                context = context,
                                bitmap = bmp,
                                maxDim = 800,
                                format = Bitmap.CompressFormat.PNG,
                                quality = 100
                            )
                            onSignatureSaved(savedPath)
                            onDismiss()
                        },
                        enabled = pathList.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPersian) "تایید امضا" else "Confirm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// تاریخ تولد (فرمت «روز/ماه/سال» با دو جداکننده «/») و رابطه سن ↔ تاریخ تولد
// ==========================================

/** تبدیل ارقام فارسی/عربی به لاتین و حذف کاراکترهای غیرمجاز (رقم و اسلش).
 *  تبدیلِ ارقام از مرجعِ واحدِ [com.example.util.PersianNumberFormatter.toAsciiDigits]
 *  می‌آید و اینجا فقط فیلترِ «فقط رقم و /» اعمال می‌شود. */
internal fun persianDigitsToAscii(raw: String): String =
    com.example.util.PersianNumberFormatter.toAsciiDigits(raw)
        .filter { it.isDigit() || it == '/' }

/**
 * قالب‌بندی خودکار ورودی تاریخ تولد به ترتیب «روز/ماه/سال» با دو جداکننده «/»:
 *  - تایپ پیوسته ارقام: ۱۰۰۱۳۷ -> ۱۰/۰۴/۱۳۷
 *  - ورودی فرمت قدیمی «سال/ماه/روز» (مثلاً ۱۳۷۵/۰۴/۱۰) خودکار به «روز/ماه/سال» معکوس می‌شود
 *  - سالِ چهاررقمیِ تنها (مثلاً ۱۳۷۵) دست‌نخورده باقی می‌ماند
 */
internal fun formatBirthDateDayFirst(raw: String): String = formatBirthDateYearFirst(raw)

internal fun formatBirthDateYearFirst(raw: String): String {
    val normalized = persianDigitsToAscii(raw)
    if (normalized.filter { it.isDigit() }.isEmpty()) return ""
    val parts = normalized.split('/').map { it.trim() }.filter { it.isNotEmpty() }
    var ordered = normalized
    if (parts.size == 3) {
        val a = parts[0].toIntOrNull()
        val b = parts[1].toIntOrNull()
        val c = parts[2].toIntOrNull()
        when {
            // already year/month/day
            parts[0].length == 4 && a != null && a in 1200..1500 && b != null && b in 1..12 && c != null && c in 1..31 ->
                ordered = a.toString() + "/" + b.toString().padStart(2, '0') + "/" + c.toString().padStart(2, '0')
            // legacy day/month/year
            parts[0].length <= 2 && a != null && a in 1..31 && b != null && b in 1..12 && parts[2].length == 4 && c != null && c in 1200..1500 ->
                ordered = c.toString() + "/" + b.toString().padStart(2, '0') + "/" + a.toString().padStart(2, '0')
        }
    }
    if (!normalized.contains('/') && normalized.length == 4 && normalized.toIntOrNull() in 1200..1500) {
        return normalized
    }
    val digits = ordered.filter { it.isDigit() }.take(8)
    return when {
        digits.length <= 4 -> digits
        digits.length <= 6 -> "${digits.take(4)}/${digits.drop(4)}"
        else -> "${digits.take(4)}/${digits.substring(4, 6)}/${digits.drop(6)}"
    }
}

internal fun parseDayFirstBirthDate(text: String): Triple<Int, Int, Int>? = parseYearFirstBirthDate(text)

/** Returns Triple(year, month, day). */
internal fun parseYearFirstBirthDate(text: String): Triple<Int, Int, Int>? {
    val parts = text.split('/').map { it.trim() }
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    if (d !in 1..31 || m !in 1..12 || y !in 1200..1500) return null
    return Triple(y, m, d)
}

/** محاسبه سن شمسی از تاریخ تولد (روز/ماه/سال) با احتساب اینکه تولدِ امسال هنوز نرسیده باشد. */
internal fun calculateJalaliAge(birthDate: String): Int? {
    val (y, m, d) = parseYearFirstBirthDate(birthDate) ?: return null
    val today = com.example.util.PersianCalendarHelper.timestampToJalali(System.currentTimeMillis())
    var age = today.year - y
    if (today.month < m || (today.month == m && today.day < d)) age -= 1
    return age.takeIf { it in 0..120 }
}

/** اعتبارسنجی نمایشی تاریخ تولد روز/ماه/سال — در صورت نامعتبری پیام خطا برمی‌گرداند. */
internal fun birthDateDayFirstError(text: String, isPersian: Boolean): String? {
    if (text.isBlank()) return null
    val parts = text.split('/')
    var err: String? = null
    if (parts.size >= 2 && parts[1].length == 2) {
        val m = parts[1].toIntOrNull() ?: 0
        if (m !in 1..12) err = if (isPersian) "ماه باید بین ۱ تا ۱۲ باشد" else "Month must be 1-12"
    }
    if (err == null && parts.size >= 3 && parts[2].length == 2) {
        val d = parts[2].toIntOrNull() ?: 0
        if (d !in 1..31) err = if (isPersian) "روز باید بین ۱ تا ۳۱ باشد" else "Day must be 1-31"
    }
    return err
}

// ==========================================
// ثبت بیمار جدید (Add Patient Dialog)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchableChipSelector(
    items: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    queryHint: String
) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val filtered = remember(query, items) {
        val q = query.trim()
        if (q.isBlank()) items else items.filter { it.contains(q, ignoreCase = true) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; expanded = true },
                label = { Text(queryHint, fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                singleLine = true
            )
            ExposedDropdownMenu(expanded = expanded && filtered.isNotEmpty(), onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 240.dp)) {
                filtered.take(40).forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item, fontSize = 12.sp) },
                        onClick = {
                            onToggle(item)
                            query = ""
                            expanded = false
                        }
                    )
                }
            }
        }
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { item ->
                val isSelected = selected.contains(item)
                FilterChip(selected = isSelected, onClick = { onToggle(item) }, label = { Text(item, fontSize = 11.sp) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPatientDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        phone: String,
        address: String,
        note: String,
        gender: String,
        medicalAlerts: String,
        selfDeclarationJson: String,
        age: Int,
        birthDate: String,
        preferredMessenger: String,
        referrer: String,
        assistantIds: String,
        fatherName: String,
        nationalId: String,
        signatureFilePath: String?,
        medicalHistory: String
    ) -> Unit,
    allStaff: List<StaffMember> = emptyList(),
    isPersian: Boolean = true,
    initialPatient: Patient? = null
) {
    val initialAddrParts = initialPatient?.address?.split(" - ") ?: emptyList()
    var name by remember { mutableStateOf(initialPatient?.name ?: "") }
    // فقط ۹ رقمِ محلی نگهداری می‌شود؛ پیشوند «+989» ثابت است و کاربر آن را نمی‌بیند/نمی‌زند
    var phoneDigits by remember { mutableStateOf(com.example.util.IranMobile.toLocalDigits(initialPatient?.phone ?: "")) }
    var fatherName by remember { mutableStateOf(initialPatient?.fatherName ?: "") }
    var nationalId by remember { mutableStateOf(initialPatient?.nationalId ?: "") }
    var gender by remember { mutableStateOf(if (initialPatient?.gender?.isNotBlank() == true) initialPatient.gender else "مرد") }
    var ageStr by remember { mutableStateOf(if (initialPatient != null && initialPatient.age > 0) initialPatient.age.toString() else "") }
    // تاریخ تولد همیشه به فرمت «روز/ماه/سال» نمایش می‌شود (فرمت قدیمی «سال/ماه/روز» خودکار معکوس می‌شود)
    var birthDate by remember { mutableStateOf(formatBirthDateDayFirst(initialPatient?.birthDate ?: "")) }
    var selectedProvince by remember { mutableStateOf(if (initialPatient == null) "آذربایجان شرقی" else initialAddrParts.getOrNull(0) ?: "") }
    var selectedCity by remember { mutableStateOf(if (initialPatient == null) "تبریز" else initialAddrParts.getOrNull(1) ?: "") }
    var streetAddress by remember { mutableStateOf(initialAddrParts.getOrNull(2) ?: initialAddrParts.drop(2).joinToString(" - ")) }
    var preferredMessenger by remember { mutableStateOf(if (initialPatient?.preferredMessenger?.isNotBlank() == true) initialPatient.preferredMessenger else "واتساپ") }
    var referrer by remember { mutableStateOf(initialPatient?.referrer ?: "") }
    var selectedAssistantSyncIds by remember { mutableStateOf(initialPatient?.assistantIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()) }
    var selectedAlerts by remember { mutableStateOf(initialPatient?.medicalAlerts?.split("، ")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }
    var selectedMedications by remember { mutableStateOf(initialPatient?.medicalHistory?.split("، ")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }
    var customAlert by remember { mutableStateOf("") }
    var customMedication by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(initialPatient?.note ?: "") }
    var signatureFilePath by remember { mutableStateOf<String?>(initialPatient?.signatureFileId ?: initialPatient?.photoLocalPath) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var attemptSaved by remember { mutableStateOf(false) }

    fun t(fa: String, en: String) = if (isPersian) fa else en

    val isPhoneValid = remember(phoneDigits) {
        phoneDigits.isBlank() || com.example.util.IranMobile.isCompleteLocal(phoneDigits)
    }

    // ورود کد ملی اختیاری است؛ ولی اگر چیزی وارد شد باید رقم کنترلش درست باشد.
    val isNationalIdValid = remember(nationalId) {
        com.example.util.NationalIdValidator.isValidOrBlank(nationalId)
    }
    val nationalIdError = remember(nationalId, isPersian) {
        com.example.util.NationalIdValidator.errorMessage(nationalId, isPersian)
    }
    // «صادره از» فقط خواندنی است و از روی سه رقم اول کد ملی خودکار پر می‌شود.
    val issuancePlace = remember(nationalId) {
        com.example.util.NationalIdCityLookup.cityOfValid(nationalId).orEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = t("پذیرش و تشکیل پرونده بیمار جدید", "New Patient Registration"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            // نکته‌ی مهم (رفع باگ «فرم بیمار جدید خالی است»):
            // یک LazyColumn که مستقیماً بدنهٔ AlertDialog باشد و فقط heightIn(max=...)
            // داشته باشد، در اندازه‌گیری AlertDialog می‌تواند ارتفاع صفر بگیرد؛ نتیجه
            // این می‌شد که فقط تیتر دیالوگ دیده شود و کل فیلدهای فرم خالی/نامرئی بمانند.
            // با تعیین حداقل ارتفاع، لیست همیشه فضای واقعی می‌گیرد و کامل رندر می‌شود
            // (همان الگویی که بقیهٔ دیالوگ‌های برنامه استفاده می‌کنند).
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp, max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // نام و نام خانوادگی بیمار (اجباری)
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(t("نام و نام خانوادگی بیمار *", "Full Name *"), fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_patient_name_input"),
                        singleLine = true,
                        isError = attemptSaved && name.isBlank(),
                        supportingText = if (attemptSaved && name.isBlank()) {
                            { Text(t("نام بیمار الزامی است", "Name is required"), color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
                        } else null
                    )
                }

                // شماره تلفن همراه — پیشوند «+989» ثابت و غیرقابل‌ویرایش است؛
                // کاربر فقط ۹ رقم بعدی را وارد می‌کند و مقدار نهایی «+989xxxxxxxxx» ذخیره می‌شود.
                item {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = phoneDigits,
                            onValueChange = { phoneDigits = com.example.util.IranMobile.sanitizeInput(it) },
                            label = { Text(t("شماره همراه (۹ رقم) *", "Mobile Phone (9 digits) *"), fontSize = 12.sp) },
                            prefix = {
                                Text(
                                    com.example.util.IranMobile.PREFIX,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            placeholder = { Text("123456789", fontSize = 12.sp) },
                            isError = !isPhoneValid || (attemptSaved && phoneDigits.isBlank()),
                            supportingText = {
                                if (!isPhoneValid) {
                                    Text(t("شماره همراه باید دقیقاً ۹ رقم باشد", "Mobile number must be exactly 9 digits"), color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                                } else if (attemptSaved && phoneDigits.isBlank()) {
                                    Text(t("شماره تماس الزامی است", "Phone is required"), color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                                } else if (phoneDigits.isNotBlank()) {
                                    Text(com.example.util.IranMobile.formatForDisplay(com.example.util.IranMobile.toStored(phoneDigits)), fontSize = 10.sp)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_patient_phone_input"),
                            singleLine = true
                        )
                    }
                }

                // جنسیت
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(t("جنسیت:", "Gender:"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        listOf("مرد" to t("آقا / مرد", "Male"), "زن" to t("خانم / زن", "Female")).forEach { (valKey, labelStr) ->
                            FilterChip(
                                selected = gender == valKey,
                                onClick = { gender = valKey },
                                label = { Text(labelStr, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // کد ملی و نام پدر
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nationalId,
                            onValueChange = { nationalId = it.filter(Char::isDigit).take(10) },
                            label = { Text(t("کد ملی (اختیاری)", "National ID (optional)"), fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = !isNationalIdValid,
                            supportingText = nationalIdError?.let { msg ->
                                { Text(msg, fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                            }
                        )
                        OutlinedTextField(
                            value = fatherName,
                            onValueChange = { fatherName = it },
                            label = { Text(t("نام پدر", "Father's Name"), fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                // صادره از — فقط خواندنی، از روی سه رقم اول کد ملی پر می‌شود
                item {
                    OutlinedTextField(
                        value = issuancePlace,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text(t("صادره از", "Place of Issuance"), fontSize = 12.sp) },
                        placeholder = {
                            Text(
                                t("با وارد کردن کد ملی خودکار پر می‌شود", "Filled automatically from the national ID"),
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }

                // سن و تاریخ تولد — با رابطه دوطرفه:
                //  - تاریخ تولد (روز/ماه/سال) → سن به‌صورت خودکار محاسبه می‌شود
                //  - تغییر سن → سالِ تاریخ تولد به‌روزرسانی می‌شود (روز و ماه حفظ می‌شود)
                item {
                    val birthError = remember(birthDate) { birthDateDayFirstError(birthDate, isPersian) }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { input ->
                                ageStr = input.filter(Char::isDigit).take(3)
                                val age = ageStr.toIntOrNull()
                                if (age != null && age in 0..120) {
                                    val currentYear = com.example.util.PersianCalendarHelper
                                        .timestampToJalali(System.currentTimeMillis()).year
                                    val newYear = (currentYear - age).takeIf { it in 1200..1500 }
                                    if (newYear != null) {
                                        val parsed = parseYearFirstBirthDate(birthDate)
                                        birthDate = if (parsed != null) {
                                            "%04d/%02d/%02d".format(newYear, parsed.second, parsed.third)
                                        } else {
                                            "%04d/01/01".format(newYear)
                                        }
                                    }
                                }
                            },
                            label = { Text(t("سن", "Age"), fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = birthDate,
                            onValueChange = { input ->
                                birthDate = formatBirthDateYearFirst(input)
                                calculateJalaliAge(birthDate)?.let { ageStr = it.toString() }
                            },
                            label = { Text(t("تاریخ تولد (سال/ماه/روز)", "Birth Date (YYYY/MM/DD)"), fontSize = 12.sp) },
                            placeholder = { Text("1375/04/10", fontSize = 11.sp) },
                            supportingText = if (birthError != null) {
                                { Text(birthError!!, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
                            } else null,
                            isError = birthError != null,
                            modifier = Modifier.weight(2f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        }
                    }
                }

                // استان و شهرستان
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProvinceDropdown(
                            selectedProvince = selectedProvince,
                            onProvinceSelected = {
                                selectedProvince = it
                                selectedCity = IranianProvinces.getCities(it).firstOrNull() ?: ""
                            },
                            modifier = Modifier.weight(1f),
                            isPersian = isPersian
                        )
                        CityDropdown(
                            province = selectedProvince,
                            selectedCity = selectedCity,
                            onCitySelected = { selectedCity = it },
                            modifier = Modifier.weight(1f),
                            isPersian = isPersian
                        )
                    }
                }

                // آدرس پستی
                item {
                    OutlinedTextField(
                        value = streetAddress,
                        onValueChange = { streetAddress = it },
                        label = { Text(t("آدرس دقیق / خیابان / کوچه / پلاک", "Street Address / Unit"), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                // پیام‌رسان ترجیحی و معرف
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var messengerExpanded by remember { mutableStateOf(false) }
                        val messengers = listOf("واتساپ", "تلگرام", "ایتا", "روبیکا", "بله", "پیامک")
                        Box(modifier = Modifier.weight(1.1f)) {
                            OutlinedTextField(
                                value = preferredMessenger,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(t("پیام‌رسان ترجیحی", "Messenger"), fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().clickable { messengerExpanded = true },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { messengerExpanded = true }) },
                                singleLine = true
                            )
                            DropdownMenu(
                                expanded = messengerExpanded,
                                onDismissRequest = { messengerExpanded = false }
                            ) {
                                messengers.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m, fontSize = 12.sp) },
                                        onClick = {
                                            preferredMessenger = m
                                            messengerExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = referrer,
                            onValueChange = { referrer = it },
                            label = { Text(t("معرف / نحوه آشنایی", "Referrer"), fontSize = 11.sp) },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                    }
                }

                // دستیار(های) پذیرش
                if (allStaff.isNotEmpty()) {
                    item {
                        AssistantMultiSelectField(
                            selectedSyncIds = selectedAssistantSyncIds,
                            onSelectionChange = { selectedAssistantSyncIds = it },
                            allStaff = allStaff,
                            isPersian = isPersian
                        )
                    }
                }

                // انتخابگر خوداظهاری پزشکی و سوابق بیماری
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(t("هشدارها و سوابق پزشکی بیمار:", "Medical Alerts & History:"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        SearchableChipSelector(
                            items = DENTAL_MEDICAL_ALERT_ITEMS,
                            selected = selectedAlerts,
                            onToggle = { alert ->
                                selectedAlerts = if (selectedAlerts.contains(alert)) selectedAlerts - alert else selectedAlerts + alert
                            },
                            queryHint = t("جستجوی بیماری / هشدار...", "Search alert...")
                        )

                        // افزودن هشدار اختصاصی دیگر
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = customAlert,
                                onValueChange = { customAlert = it },
                                label = { Text(t("افزودن هشدار/بیماری دیگر...", "Add other alert..."), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    if (customAlert.isNotBlank()) {
                                        selectedAlerts = selectedAlerts + customAlert.trim()
                                        customAlert = ""
                                    }
                                },
                                enabled = customAlert.isNotBlank()
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add alert", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // سوابق دارویی (Medications)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(t("داروهای مصرفی با نام دارو (سوابق دارویی):", "Consumed Medications by Name (Drug History):"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        SearchableChipSelector(
                            items = DENTAL_MEDICATIONS_ITEMS,
                            selected = selectedMedications,
                            onToggle = { med ->
                                selectedMedications = if (selectedMedications.contains(med)) selectedMedications - med else selectedMedications + med
                            },
                            queryHint = t("جستجوی دارو...", "Search medication...")
                        )

                        // افزودن داروی دیگر
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = customMedication,
                                onValueChange = { customMedication = it },
                                label = { Text(t("افزودن داروی دیگر...", "Add other medication..."), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    if (customMedication.isNotBlank()) {
                                        selectedMedications = selectedMedications + customMedication.trim()
                                        customMedication = ""
                                    }
                                },
                                enabled = customMedication.isNotBlank()
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add medication", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // یادداشت و توضیحات تکمیلی
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(t("یادداشت و ملاحظات بالینی", "Clinical Notes / Remarks"), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                // امضای دیجیتال
                item {
                    OutlinedButton(
                        onClick = { showSignatureDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = if (signatureFilePath != null) ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Icon(
                            if (signatureFilePath != null) Icons.Default.CheckCircle else Icons.Default.Draw,
                            contentDescription = null,
                            tint = if (signatureFilePath != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (signatureFilePath != null) t("امضای دیجیتال ثبت شد (لمس برای تغییر)", "Signature Captured (Tap to re-sign)") else t("ثبت امضای دیجیتال بیمار (لمسی)", "Capture Digital Signature"),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    attemptSaved = true
                    if (name.isNotBlank() && com.example.util.IranMobile.isCompleteLocal(phoneDigits) && isNationalIdValid) {
                        val fullAddress = listOfNotNull(
                            selectedProvince.takeIf { it.isNotBlank() },
                            selectedCity.takeIf { it.isNotBlank() },
                            streetAddress.takeIf { it.isNotBlank() }
                        ).joinToString(" - ")

                        val alertsString = selectedAlerts.joinToString("، ")
                        val medicationsString = selectedMedications.joinToString("، ")
                        val age = ageStr.toIntOrNull() ?: 0
                        val assistIds = joinAssistantSyncIds(selectedAssistantSyncIds)

                        onConfirm(
                            name.trim(),
                            // همیشه به شکل «+989xxxxxxxxx» ذخیره می‌شود
                            com.example.util.IranMobile.toStored(phoneDigits),
                            fullAddress,
                            note.trim(),
                            gender,
                            alertsString,
                            "",
                            age,
                            birthDate.trim(),
                            preferredMessenger,
                            referrer.trim(),
                            assistIds,
                            fatherName.trim(),
                            nationalId.trim(),
                            signatureFilePath,
                            medicationsString
                        )
                    }
                },
                enabled = name.isNotBlank() && com.example.util.IranMobile.isCompleteLocal(phoneDigits) && isNationalIdValid
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(t("ثبت و تشکیل پرونده", "Register Patient"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("انصراف", "Cancel"))
            }
        }
    )

    if (showSignatureDialog) {
        PatientTouchSignatureDialog(
            patientName = name.ifBlank { t("بیمار جدید", "New Patient") },
            isPersian = isPersian,
            onDismiss = { showSignatureDialog = false },
            onSignatureSaved = { path ->
                signatureFilePath = path
                showSignatureDialog = false
            }
        )
    }
}

/**
 * نسخه سازگار AddPatientDialog با ViewModel
 */
@Composable
fun AddPatientDialog(
    viewModel: DentalViewModel,
    onDismiss: () -> Unit
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val allStaff by viewModel.allStaffMembers.collectAsStateWithLifecycle()

    AddPatientDialog(
        onDismiss = onDismiss,
        allStaff = allStaff,
        isPersian = isPersian,
        onConfirm = { name, phone, address, note, gender, medicalAlerts, selfDeclarationJson, age, birthDate, preferredMessenger, referrer, assistantIds, fatherName, nationalId, signatureFilePath, medicalHistory ->
            viewModel.addPatientFull(
                name = name,
                phone = phone,
                address = address,
                medicalHistory = medicalHistory,
                note = note,
                gender = gender,
                medicalAlerts = medicalAlerts,
                selfDeclarationJson = selfDeclarationJson,
                age = age,
                birthDate = birthDate,
                preferredMessenger = preferredMessenger,
                referrer = referrer,
                assistantIds = assistantIds,
                fatherName = fatherName,
                nationalId = nationalId,
                signatureLocalFilePath = signatureFilePath
            )
            onDismiss()
        }
    )
}

// ==========================================
// ویرایش بیمار
// ==========================================
@Composable
fun EditPatientDialog(
    patient: Patient,
    viewModel: DentalViewModel,
    onDismiss: () -> Unit
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val allStaff by viewModel.allStaffMembers.collectAsStateWithLifecycle()

    AddPatientDialog(
        onDismiss = onDismiss,
        allStaff = allStaff,
        isPersian = isPersian,
        initialPatient = patient,
        onConfirm = { name, phone, address, note, gender, medicalAlerts, selfDeclarationJson, age, birthDate, preferredMessenger, referrer, assistantIds, fatherName, nationalId, signatureFilePath, medicalHistory ->
            viewModel.updatePatientFull(
                patient = patient,
                name = name,
                phone = phone,
                address = address,
                medicalHistory = medicalHistory,
                note = note,
                gender = gender,
                medicalAlerts = medicalAlerts,
                selfDeclarationJson = selfDeclarationJson,
                age = age,
                birthDate = birthDate,
                preferredMessenger = preferredMessenger,
                referrer = referrer,
                assistantIds = assistantIds,
                fatherName = fatherName,
                nationalId = nationalId,
                signatureLocalFilePath = signatureFilePath
            )
            onDismiss()
        }
    )
}

// ==========================================
// مدیریت جلسات درمان نوبت
// ==========================================
@Composable
fun AppointmentSessionDialog(
    appointment: Appointment,
    viewModel: DentalViewModel,
    onDismiss: () -> Unit
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val allTreatments by viewModel.allTreatments.collectAsStateWithLifecycle()

    val plannedIds = remember(appointment.plannedTreatmentIds) {
        appointment.plannedTreatmentIds.split(",").mapNotNull { it.trim().toLongOrNull() }
    }
    val plannedTreatments = allTreatments.filter { !it.isDeleted && it.id in plannedIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPersian) "جلسات درمان — ${appointment.patientName}" else "Treatment Sessions — ${appointment.patientName}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // وضعیت کلی نوبت
                Text(
                    text = if (isPersian) "وضعیت نوبت:" else "Appointment status:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "PENDING" to (if (isPersian) "در انتظار" else "Pending"),
                        "IN_OFFICE" to (if (isPersian) "حاضر" else "In Office"),
                        "COMPLETED" to (if (isPersian) "انجام شد" else "Done"),
                        "CANCELED" to (if (isPersian) "لغو" else "Canceled")
                    ).forEach { (status, labelTxt) ->
                        FilterChip(
                            selected = appointment.status == status,
                            onClick = { viewModel.updateAppointment(appointment.copy(status = status, isCompleted = status == "COMPLETED")) },
                            label = { Text(labelTxt, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text(
                    text = if (isPersian) "درمان‌های برنامه‌ریزی‌شده این جلسه:" else "Planned treatments for this session:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (plannedTreatments.isEmpty()) {
                    Text(
                        text = if (isPersian) "درمانی به این نوبت متصل نشده است." else "No treatments linked to this appointment.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                } else {
                    plannedTreatments.forEach { tx ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tx.status == "COMPLETED")
                                    Color(0xFF2E7D32).copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(
                                        text = when (tx.status) {
                                            "COMPLETED" -> if (isPersian) "تکمیل‌شده" else "Completed"
                                            "IN_PROGRESS" -> if (isPersian) "در حال انجام" else "In Progress"
                                            else -> if (isPersian) "برنامه‌ریزی‌شده" else "Planned"
                                        },
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        val next = when (tx.status) {
                                            "PLANNED" -> "IN_PROGRESS"
                                            "IN_PROGRESS" -> "COMPLETED"
                                            else -> "PLANNED"
                                        }
                                        viewModel.updateTreatmentStatus(tx, next)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(if (isPersian) "مرحله بعد" else "Next", fontSize = 10.sp)
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
}

// ==========================================
// پروتکل بالینی درمان
// ==========================================
@Composable
fun ClinicalProtocolDialog(
    therapyTitle: String,
    patientMedicalHistory: String = "",
    patientMedicalAlerts: String = "",
    isPersian: Boolean = true,
    initialFontSizePt: Int = 11,
    onDismiss: () -> Unit
) {
    var fontSize by remember { mutableStateOf(initialFontSizePt.sp) }

    // پروتکل‌های پیش‌فرض بر اساس کلیدواژه‌های رایج درمانی
    val protocol = remember(therapyTitle) {
        val t = therapyTitle.trim()
        when {
            listOf("عصب", "اندو", "روت کانال", "Endo", "Root").any { t.contains(it, ignoreCase = true) } ->
                if (isPersian)
                    "پروتکل درمان ریشه:\n۱) رادیوگرافی اولیه و تعیین طول کار ریشه (Working Length)\n۲) ایزولاسیون با رابردم الزامی است\n۳) آماده‌سازی کانال با سوزن‌های افزایشی و شست‌وشو با هیپوکلریت سدیم\n۴) خشک‌کردن کانال با پوینت کاغذی\n۵) obturation با گوتا‌پرکا و سیلر در جلسه نهایی\n۶) رادیوگرافی کنترل و ثبت طول کار"
                else "Endodontic protocol:\n1) Initial radiograph and working length\n2) Rubber dam isolation is mandatory\n3) Canal preparation with incremental files + NaOCl irrigation\n4) Dry with paper points\n5) Obturate with gutta-percha and sealer\n6) Control radiograph"
            listOf("کشیده", "استخراج", "Extraction", "جراحی").any { t.contains(it, ignoreCase = true) } ->
                if (isPersian)
                    "پروتکل کشیدن دندان:\n۱) بررسی رادیوگرافیک و شرح‌حال طبی (هشدارهای خون‌ریزی/دارویی)\n۲) بی‌حسی موضعی و کنترل بی‌حسی\n۳) لوکس کردن و استخراج با حداقل تروما به استخوان آلوئول\n۴) بررسی سوکت و دبریدمان در صورت نیاز\n۵) گاز استریل و فشار ۲۰ دقیقه‌ای برای هموستاز"
                else "Extraction protocol:\n1) Radiographic review and medical history check\n2) Local anesthesia and verification\n3) Luxation and atraumatic removal\n4) Socket inspection and debridement\n5) Gauze pressure for 20 minutes"
            listOf("ترمیم", "کامپوزیت", "آمالگام", "Filling", "Composite").any { t.contains(it, ignoreCase = true) } ->
                if (isPersian)
                    "پروتکل ترمیم:\n۱) انتخاب رنگ در نور طبیعی قبل از هر اقدامی (برای ترمیم‌های همرنگ)\n۲) بی‌حسی در صورت نیاز + ایزولاسیون (روبال/ایزولایت)\n۳) برداشت پوسیدگی با حداقل تراش\n۴) اچ، باند و چیدمان لایه‌ای کامپوزیت (حداکثر ۲ میلی‌متر در هر لایه)\n۵) تنظیم اکلوژن با آرتیکولیشن پیپر و پالیش نهایی"
                else "Restoration protocol:\n1) Shade selection in natural light\n2) Anesthesia if needed + isolation\n3) Minimal caries removal\n4) Etch, bond and incremental layering (max 2mm)\n5) Occlusal adjustment with articulating paper and final polish"
            listOf("پروتز", "روکش", "بریج", "Crown", "Bridge").any { t.contains(it, ignoreCase = true) } ->
                if (isPersian)
                    "پروتکل پروتز ثابت:\n۱) آماده‌سازی دندان با پله واضح و رادیوگرافی کنترل عمود بر محور دندان (پارا‌اکلوژن)\n۲) قالب‌گیری دقیق با مواد سیلیکونی و ریکورد اکلوزال ثبت شود؛ حتماً با کد/رنگ به لابراتوار ارسال شود.\n۳) پروتزیون لازم در جلسه اول گذاشته شود.\n۴) در جلسه تحویل: بررسی تماس‌های اکلوزال، تنظیم و سپس سمان نهایی."
                else "Fixed prosthesis protocol:\n1) Preparation with clear shoulder\n2) Accurate silicone impression and bite registration; send shade to lab\n3) Provisional restoration at first visit\n4) Delivery: check contacts, adjust, then final cementation"
            listOf("جرم", "اسکالینگ", "Scaling", "پریو").any { t.contains(it, ignoreCase = true) } ->
                if (isPersian)
                    "پروتکل جرم‌گیری:\n۱) بررسی پریودنتال (پاکت، خون‌ریزی، موبیلیتی)\n۲) اسکالینگ التراسونیک + پلیش نهایی با پیس و پاست پروفیلاکسی\n۳) آموزش بهداشت و تعیین نوبت بعدی"
                else "Scaling protocol:\n1) Periodontal charting\n2) Ultrasonic scaling + polish\n3) Hygiene education and recall"
            else ->
                if (isPersian)
                    "پروتکل عمومی:\n۱) بررسی شرح‌حال و هشدارهای پزشکی بیمار قبل از شروع کار الزامی است.\n۲) ثبت رادیوگرافی در صورت نیاز و در پرونده بیمار ثبت شود.\n۳) رضایت آگاهانه بیمار قبل از شروع هر اقدام تهاجمی ثبت شود.\n۴) کنترل درد و بی‌حسی کافی در طول درمان.\n۵) ثبت مراحل انجام‌شده در پرونده بیمار و تعیین نوبت پیگیری در صورت نیاز."
                else "General protocol:\n1) Review medical history before starting.\n2) Radiographs if needed.\n3) Informed consent for invasive procedures.\n4) Adequate anesthesia.\n5) Document steps and schedule follow-up."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPersian) "پروتکل بالینی: $therapyTitle" else "Clinical Protocol: $therapyTitle",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (patientMedicalAlerts.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = if (isPersian) "⚠ هشدارهای پزشکی بیمار" else "⚠ Patient Medical Alerts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(patientMedicalAlerts, fontSize = 11.sp)
                        }
                    }
                }
                if (patientMedicalHistory.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = if (isPersian) "سوابق پزشکی بیمار" else "Medical History",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(patientMedicalHistory, fontSize = 11.sp)
                        }
                    }
                }
                Text(protocol, fontSize = fontSize, lineHeight = fontSize * 1.6)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isPersian) "اندازه متن:" else "Text size:", fontSize = 10.sp, color = Color.Gray)
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { fontSize = (fontSize.value - 1f).coerceAtLeast(9f).sp }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Smaller", modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = { fontSize = (fontSize.value + 1f).coerceAtMost(20f).sp }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Larger", modifier = Modifier.size(14.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "بستن" else "Close") }
        }
    )
}

// ==========================================
// طول کار کانال (اندو) — ذخیره در notes درمان با نشانگر [ENDO_WORKING_LENGTH: کانال=عدد,...]
// ==========================================

/** کانال‌های پیش‌فرض بر اساس شماره‌های دندان انتخاب‌شده (شماره‌گذاری یونیورسال ۱ تا ۳۲). */
fun getDefaultCanalsForTeeth(selectedTeeth: String): List<String> {
    val teeth = selectedTeeth.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct()
    val molarUpper = setOf(1, 2, 3, 14, 15, 16)
    val molarLower = setOf(17, 18, 19, 30, 31, 32)
    val premolars = setOf(4, 5, 12, 13, 20, 21, 28, 29)
    val canals = linkedSetOf<String>()
    for (t in teeth) {
        when {
            t in molarUpper -> canals.addAll(listOf("MB", "MB2", "ML", "DB", "DL"))
            t in molarLower -> canals.addAll(listOf("MB", "ML", "DB", "DL", "Disto-Lingual"))
            t in premolars -> canals.addAll(listOf("B", "L"))
            else -> canals.add("P")
        }
    }
    if (canals.isEmpty()) canals.add("P")
    return canals.toList()
}

/** رمزگذاری کانال‌های پیش‌فرض طول کار داخل یادداشت درمان. */
fun encodeEndoCanalData(notes: String, canals: List<String>): String {
    val marker = "[ENDO_WORKING_LENGTH:"
    if (notes.contains(marker)) return notes
    val segment = canals.joinToString(", ") { "$it=?" }
    val block = "$marker $segment]"
    return if (notes.isBlank()) block else notes.trimEnd() + "\n" + block
}

/** خواندن مقادیر طول کار از یادداشت درمان. */
fun decodeEndoCanalData(notes: String): Map<String, String> {
    val marker = "[ENDO_WORKING_LENGTH:"
    val idx = notes.indexOf(marker)
    if (idx < 0) return emptyMap()
    val end = notes.indexOf(']', idx)
    if (end < 0) return emptyMap()
    val segment = notes.substring(idx + marker.length, end)
    return segment.split(",").mapNotNull {
        val kv = it.trim().split("=", limit = 2)
        if (kv.size == 2) kv[0].trim() to kv[1].trim() else null
    }.toMap()
}

@Composable
fun EndoWorkingLengthCard(
    treatment: Treatment,
    viewModel: DentalViewModel,
    readOnly: Boolean = false
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value

    val parsed = remember(treatment.notes) { decodeEndoCanalData(treatment.notes) }

    val primaryTooth = remember(treatment.selectedTeeth) {
        parseToothNumbers(treatment.selectedTeeth).firstOrNull()
    }

    if (primaryTooth == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Text(
                text = if (isPersian)
                    "برای نمایش شماتیک دندان، شماره دندان را در درمان ثبت کنید."
                else
                    "Add a tooth number to this treatment to see the schematic.",
                fontSize = 9.5.sp,
                color = Color.Gray,
                modifier = Modifier.padding(12.dp)
            )
        }
        return
    }

    val fdi = remember(primaryTooth) { toFdiToothNumber(primaryTooth) }
    // آناتومی کانال‌ها از همان جدولی می‌آید که شماتیک با آن رسم می‌شود، پس
    // داده و تصویر هرگز از هم جدا نمی‌افتند.
    val canals = remember(fdi) { com.example.ui.components.schematicCanalsFor(fdi) }

    // وضعیت روشن/خاموش: ثابت‌ها همیشه روشن‌اند و قابل حذف نیستند؛ احتمالی‌ها
    // اگر قبلاً برای این درمان ثبت شده باشند روشن می‌مانند.
    val enabled = remember(treatment.id, fdi) {
        mutableStateMapOf<String, Boolean>().apply {
            canals.forEach { c -> put(c.name, c.isFixed || parsed.containsKey(c.name)) }
        }
    }
    // طول کار هر کانال — پیش‌فرض میانگین آناتومیک همان کانال.
    val lengths = remember(treatment.id, fdi) {
        mutableStateMapOf<String, Double>().apply {
            canals.forEach { c ->
                put(c.name, parsed[c.name]?.trim()?.toDoubleOrNull() ?: c.avg)
            }
        }
    }

    fun persist() {
        val marker = "[ENDO_WORKING_LENGTH:"
        val segment = canals.filter { enabled[it.name] == true }.joinToString(", ") { c ->
            val v = lengths[c.name]
            "${c.name}=${if (v == null) "?" else com.example.ui.components.formatMm(v).removeSuffix(" mm")}"
        }
        val block = "$marker $segment]"
        val idx = treatment.notes.indexOf(marker)
        val newNotes = if (idx < 0) {
            if (treatment.notes.isBlank()) block else treatment.notes.trimEnd() + "\n" + block
        } else {
            val end = treatment.notes.indexOf(']', idx)
            if (end < 0) treatment.notes + block
            else treatment.notes.substring(0, idx) + block + treatment.notes.substring(end + 1)
        }
        viewModel.updateTreatment(treatment.copy(notes = newNotes))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    text = if (isPersian) "طول کار کانال‌ها (Working Length)" else "Working Lengths",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // ——— شماتیک: تعداد ریشه از تعداد کانالِ انتخاب‌شده پیروی می‌کند ———
            com.example.ui.components.EndoToothSchematic(
                toothNumber = primaryTooth,
                workingLengths = lengths.toMap(),
                enabled = enabled.toMap(),
                isPersian = isPersian
            )

            // ——— کنترل هر کانال: ثابت / احتمالی + گام ۰٫۵ میلی‌متر ———
            canals.forEachIndexed { index, canal ->
                com.example.ui.components.CanalControlRow(
                    canal = canal,
                    colorIndex = index,
                    isOn = enabled[canal.name] == true,
                    workingLength = lengths[canal.name] ?: canal.avg,
                    onToggle = { v ->
                        enabled[canal.name] = v || canal.isFixed
                        if (!readOnly) persist()
                    },
                    onWorkingLengthChange = { v ->
                        lengths[canal.name] = v
                        if (!readOnly) persist()
                    },
                    readOnly = readOnly
                )
            }

            Text(
                text = if (isPersian)
                    "کانال‌های «ثابت» همیشه نمایش داده می‌شوند. کانال‌های احتمالی با درصد فراوانی مشخص شده‌اند و با انتخاب هرکدام، شکل ریشه و فورکا مطابق همان تغییر می‌کند. تغییر طول با گام ۰٫۵ میلی‌متر."
                else
                    "Fixed canals are always shown. Optional canals list their prevalence; selecting one updates the root and furcation. Length steps are 0.5 mm.",
                fontSize = 9.sp,
                color = Color.Gray
            )
        }
    }
}

// ==========================================
// سطوح ترمیم (Mesial, Distal, Occlusal, ...) — ذخیره در notes درمان با پیشوند [REST_SURF]
// ==========================================
@Composable
fun ToothSurfaceRestorationCard(
    treatment: Treatment,
    viewModel: DentalViewModel,
    readOnly: Boolean = false
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val surfaces = listOf(
        "M" to (if (isPersian) "مزیا" else "Mesial"),
        "D" to (if (isPersian) "دیستال" else "Distal"),
        "O" to (if (isPersian) "اکلوزال" else "Occlusal"),
        "I" to (if (isPersian) "اینسیزال" else "Incisal"),
        "B" to (if (isPersian) "باکال" else "Buccal"),
        "L" to (if (isPersian) "لینگویال" else "Lingual")
    )
    val selected = remember(treatment.notes) {
        val marker = "[REST_SURF]"
        treatment.notes.substringAfter(marker, "")
            .lineSequence().firstOrNull()?.trim()?.split(",")
            ?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }
    var checked by remember(treatment.id) { mutableStateOf(selected) }

    fun persist(newSet: Set<String>) {
        checked = newSet
        if (readOnly) return
        val marker = "[REST_SURF]"
        val baseNotes = treatment.notes.substringBefore(marker).trimEnd()
        val segment = newSet.sorted().joinToString(",")
        val newNotes = if (segment.isBlank()) baseNotes else (baseNotes + "\n$marker\n$segment")
        viewModel.updateTreatment(treatment.copy(notes = newNotes))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    text = if (isPersian) "سطوح درگیر ترمیم" else "Restoration Surfaces",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // ——— فرم تصویری/شماتیک سطوح (همان چارت بالینی قبلی) ———
            val diagramTooth = remember(treatment.selectedTeeth) {
                parseToothNumbers(treatment.selectedTeeth).firstOrNull()?.let { toFdiToothNumber(it) }
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                com.example.ui.components.ToothSurfaceDiagram(
                    toothNumber = diagramTooth,
                    selectedCodes = checked,
                    onToggle = { code ->
                        if (!readOnly) persist(if (code in checked) checked - code else checked + code)
                    },
                    readOnly = readOnly,
                    isPersian = isPersian
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                surfaces.forEach { (code, labelTxt) ->
                    val isOn = code in checked
                    FilterChip(
                        selected = isOn,
                        onClick = {
                            if (readOnly) return@FilterChip
                            persist(if (isOn) checked - code else checked + code)
                        },
                        label = { Text("$code\n$labelTxt", fontSize = 8.5.sp, textAlign = TextAlign.Center) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ==========================================
// فاکتور رسمی سفارش لابراتوار
// ==========================================
@Composable
fun OfficialLabInvoiceDialog(
    order: LabOrder,
    viewModel: DentalViewModel,
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
    val currency = viewModel.label("currency")
    var showPayConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPersian) "فاکتور رسمی لابراتوار" else "Official Lab Invoice",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InvoiceRow(
                    label = if (isPersian) "لابراتوار" else "Lab",
                    value = order.labName,
                    isPersian = isPersian
                )
                InvoiceRow(
                    label = if (isPersian) "بیمار" else "Patient",
                    value = order.patientName,
                    isPersian = isPersian
                )
                InvoiceRow(
                    label = if (isPersian) "پروتز" else "Prosthesis",
                    value = listOfNotNull(
                        order.prosthesisType.takeIf { it.isNotBlank() },
                        order.toothNumber.takeIf { !it.isNullOrBlank() }?.let { if (isPersian) "دندان $it" else "Tooth $it" }
                    ).joinToString(" — "),
                    isPersian = isPersian
                )
                InvoiceRow(
                    label = if (isPersian) "تاریخ ثبت" else "Order Date",
                    value = viewModel.formatDate(order.orderDate),
                    isPersian = isPersian
                )
                InvoiceRow(
                    label = if (isPersian) "تاریخ تحویل" else "Due Date",
                    value = viewModel.formatDate(order.dueDate),
                    isPersian = isPersian
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPersian) "مبلغ قابل پرداخت:" else "Amount Due:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${formatAmount(order.cost)} $currency",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "پرداخت از بخش هزینه‌ها" else "Pay from Expenses")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "بستن" else "Close") }
        }
    )
}

@Composable
private fun InvoiceRow(label: String, value: String, isPersian: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value.ifBlank { "—" }, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

// ==========================================
// دیالوگ حساب مطب (لوگوی کوچک بالای صفحه)
// درخواست ۲-۱: دقیقاً سه بخش — «مدیریت حساب کاربری»، «وضعیت ابری» و
// «تنظیمات اختصاصی نرم‌افزار مطب». بخش «تنظیمات مطب» حذف شد.
// ==========================================
@Composable
fun ClinicAccountDialog(
    viewModel: DentalViewModel,
    onNavigateToAccount: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExclusiveSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    var showCloudStatus by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPersian) "حساب مطب" else "Clinic Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccountMenuItem(
                    icon = Icons.Default.ManageAccounts,
                    title = if (isPersian) "مدیریت حساب کاربری" else "Manage Account",
                    subtitle = if (isPersian) "پروفایل، پرسنل، آواتار و دسترسی‌ها" else "Profile, staff, avatar & permissions",
                    onClick = {
                        onDismiss()
                        onNavigateToAccount()
                    }
                )
                AccountMenuItem(
                    icon = Icons.Default.CloudSync,
                    title = if (isPersian) "وضعیت ابری" else "Cloud Status",
                    subtitle = if (isPersian) "اتصال، آخرین سینک و گزارش خلاصه" else "Connection, last sync & summary report",
                    onClick = { showCloudStatus = true }
                )
                AccountMenuItem(
                    icon = Icons.Default.Tune,
                    title = if (isPersian) "تنظیمات اختصاصی نرم‌افزار مطب" else "Exclusive Clinic Software Settings",
                    subtitle = if (isPersian) "پیکربندی پیشرفته‌ی مخصوص مطب" else "Advanced clinic-specific configuration",
                    onClick = {
                        onDismiss()
                        onNavigateToExclusiveSettings()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "بستن" else "Close") }
        }
    )

    if (showCloudStatus) {
        ConnectionStatusDialog(
            viewModel = viewModel,
            onDismiss = { showCloudStatus = false }
        )
    }
}

@Composable
private fun AccountMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Text(subtitle, fontSize = 10.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

// ==========================================
// وضعیت ابری / اتصال — گزارش بسیار خلاصه (مطابق درخواست ۲-۲-۲)
// ==========================================
@Composable
fun ConnectionStatusDialog(
    viewModel: DentalViewModel,
    onDismiss: () -> Unit
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val syncEngine = viewModel.syncEngine
    val isOnline by syncEngine.isOnline.collectAsStateWithLifecycle()
    val lastSyncedAt by syncEngine.lastSyncedAt.collectAsStateWithLifecycle()
    val lastError by syncEngine.lastError.collectAsStateWithLifecycle()
    val failedCollections by syncEngine.failedCollections.collectAsStateWithLifecycle()

    // شمارش خلاصه‌ی عملیات در صف سینک
    var pendingCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            viewModel.database.pendingSyncOpDao().getPendingOps(Int.MAX_VALUE).let { pendingCount = it.size }
        } catch (e: Exception) { /* دسترس‌پذیری دیتابیس */ }
    }

    val scope = rememberCoroutineScope()
    var isPulling by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPersian) "وضعیت ابری" else "Cloud Status",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusLine(
                    ok = isOnline,
                    text = if (isPersian) {
                        if (isOnline) "✅ اتصال اینترنت: برقرار" else "❌ اتصال اینترنت: قطع"
                    } else {
                        if (isOnline) "✅ Internet: Connected" else "❌ Internet: Offline"
                    }
                )
                StatusLine(
                    ok = lastError == null,
                    text = if (isPersian) {
                        lastSyncedAt?.let { "✅ آخرین سینک موفق: ${viewModel.formatDateFullVerbalWithTime(it)}" }
                            ?: "⚠ هنوز سینک موفقی ثبت نشده"
                    } else {
                        lastSyncedAt?.let { "✅ Last sync: ${viewModel.formatDateFullVerbalWithTime(it)}" }
                            ?: "⚠ No successful sync yet"
                    }
                )
                StatusLine(
                    ok = pendingCount == 0,
                    text = if (isPersian) {
                        if (pendingCount == 0) "✅ صف ارسال: خالی" else "⏳ عملیات در انتظار ارسال: ${com.example.util.PersianNumberFormatter.formatAmount(pendingCount.toLong())} مورد"
                    } else {
                        if (pendingCount == 0) "✅ Upload queue: empty" else "⏳ Pending uploads: $pendingCount"
                    }
                )
                if (failedCollections.isNotEmpty()) {
                    StatusLine(
                        ok = false,
                        text = if (isPersian) "❌ سینک ناموفق: ${failedCollections.size} جدول" else "❌ Failed collections: ${failedCollections.size}"
                    )
                }
                if (lastError != null) {
                    Text(
                        text = lastError ?: "",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2
                    )
                }
                Button(
                    onClick = {
                        if (!isPulling) {
                            isPulling = true
                            scope.launch {
                                try { syncEngine.pullAll() } catch (e: Exception) { /* خطا در lastError ثبت می‌شود */ }
                                try { syncEngine.pushPending() } catch (e: Exception) { /* بعداً تلاش می‌شود */ }
                                isPulling = false
                            }
                        }
                    },
                    enabled = isOnline && !isPulling,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isPulling) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isPersian) (if (isPulling) "در حال سینک..." else "سینک دستی") else (if (isPulling) "Syncing..." else "Sync Now"), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "بستن" else "Close") }
        }
    )
}

@Composable
private fun StatusLine(ok: Boolean, text: String) {
    Text(
        text = text,
        fontSize = 11.5.sp,
        fontWeight = if (ok) FontWeight.SemiBold else FontWeight.Bold,
        color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
    )
}

// ==========================================
// تکمیل پروفایل مطب (اولین ورود)
// ==========================================
@Composable
fun ProfileCompletionDialog(
    viewModel: DentalViewModel,
    onComplete: () -> Unit
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var clinicName by remember { mutableStateOf(settings.clinicName) }
    var doctorName by remember { mutableStateOf(settings.doctorName) }

    AlertDialog(
        onDismissRequest = { /* اجباری: تا تکمیل بسته نمی‌شود */ },
        title = {
            Text(
                text = if (isPersian) "به دندان‌پرو خوش آمدید 👋" else "Welcome to DentalPro 👋",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isPersian)
                        "برای شروع، مشخصات مطب را وارد کنید. این اطلاعات روی فاکتورها، پیامک‌های یادآوری و کارت هویت مطب استفاده می‌شود."
                    else "Enter your clinic details to get started. They are used on invoices, reminders and the clinic identity card.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = clinicName,
                    onValueChange = { clinicName = it },
                    label = { Text(if (isPersian) "نام مطب" else "Clinic Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text(if (isPersian) "نام پزشک" else "Doctor Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateSettings(
                        clinicName = clinicName.trim(),
                        doctorName = doctorName.trim().ifBlank { settings.doctorName },
                        staffNames = settings.staffNames,
                        showSummary = settings.showSummaryCard,
                        showQuick = settings.showQuickActions,
                        showCharts = settings.showCharts
                    )
                    onComplete()
                },
                enabled = clinicName.isNotBlank()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isPersian) "شروع کار" else "Get Started", fontWeight = FontWeight.Bold)
            }
        }
    )
}


// ==========================================
// پرداخت قسط با انتخاب روش دریافت (نقد/کارتخوان/کارت به کارت) و حساب بانکی —
// درخواست کاربر: اقساط باید در لیست تراکنش‌های بانکی/نقدی/پوز دیده شوند.
// ==========================================
@Composable
fun PayInstallmentDialog(
    installment: Installment,
    viewModel: DentalViewModel,
    isPersian: Boolean,
    currency: String,
    onDismiss: () -> Unit
) {
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    var method by remember(installment.id) { mutableStateOf("CASH") }
    var selectedAccountId by remember(installment.id) { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPersian) "ثبت پرداخت قسط" else "Pay Installment", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = "${installment.title} — ${formatAmount(installment.amount)} $currency",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isPersian) "روش دریافت را انتخاب کنید تا تراکنش در لیست بانکی/نقدی/پوز ثبت شود:" else "Pick the receive method so it appears in bank/cash/POS transaction lists:",
                    fontSize = 10.5.sp,
                    color = Color.Gray
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "CASH" to (if (isPersian) "نقدی" else "Cash"),
                        "POS" to (if (isPersian) "کارتخوان" else "POS"),
                        "CARD_TRANSFER" to (if (isPersian) "کارت به کارت" else "Card")
                    ).forEach { (key, labelTxt) ->
                        FilterChip(
                            selected = method == key,
                            onClick = { method = key },
                            label = { Text(labelTxt, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (method != "CASH") {
                    val accounts = bankAccounts.filter { !it.isDeleted }
                    if (accounts.isEmpty()) {
                        Text(
                            text = if (isPersian) "حساب بانکی تعریف نشده است." else "No bank accounts defined.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(if (isPersian) "حساب بانکی دریافت‌کننده:" else "Receiving bank account:", fontSize = 10.5.sp, color = Color.Gray)
                        accounts.forEach { acc ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedAccountId == acc.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, if (selectedAccountId == acc.id) MaterialTheme.colorScheme.primary else Color.Transparent),
                                onClick = { selectedAccountId = acc.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(acc.bankName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("${formatAmount(acc.balance)} $currency", fontSize = 9.5.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.payInstallmentWithTransaction(installment, method, if (method == "CASH") null else selectedAccountId)
                    onDismiss()
                },
                enabled = method == "CASH" || selectedAccountId != null
            ) { Text(if (isPersian) "ثبت پرداخت" else "Pay", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") }
        }
    )
}

// ==========================================
// پیش‌نمایش زنده‌ی جدول نسخه (پیش از تایید پزشک)
// ==========================================
@Composable
fun PrescriptionLiveTablePreview(
    rows: List<com.example.reports.RxColumnRowData>,
    notes: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("پیش‌نمایش نسخه:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("${index + 1}.", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.title, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = listOf(
                                row.quantity.takeIf { it.isNotBlank() && it != "-" }?.let { "تعداد: $it" },
                                row.dosage.takeIf { it.isNotBlank() && it != "-" }?.let { "دوز: $it" },
                                row.timing.takeIf { it.isNotBlank() && it != "-" }?.let { "زمان‌بندی: $it" },
                                row.instructions.takeIf { it.isNotBlank() && it != "-" }?.let { "دستور: $it" }
                            ).filterNotNull().joinToString(" | "),
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            if (notes.isNotBlank()) {
                Text("یادداشت بیمار: $notes", fontSize = 9.5.sp, color = Color(0xFF2E7D32))
            }
        }
    }
}

// ==========================================
// کمک‌توابع نسخه‌ی دارویی و مراحل درمان (بازسازی‌شده)
// ==========================================

/** تجزیه‌ی رشته‌ی خام نسخه به ستون‌های جدول (نام، تعداد، دوز، زمان‌بندی، دستور). */
fun parseRxToColumns(raw: String, durationDays: Int): com.example.reports.RxColumnRowData {
    val clean = raw.trim()
    val sep = when {
        clean.contains(" - ") -> " - "
        clean.contains(";;;") -> ";;;"
        else -> null
    }
    val parts = if (sep != null) clean.split(sep).map { it.trim() } else listOf(clean)
    val title = parts.getOrNull(0)?.trim().orEmpty().ifBlank { "دارو" }
    val dosage = parts.getOrNull(1)?.trim().orEmpty()
    val timing = parts.getOrNull(2)?.trim().orEmpty()
    val instructions = parts.getOrNull(3)?.trim().orEmpty().ifBlank { "-" }
    val dur = parts.getOrNull(4)?.trim()?.toIntOrNull() ?: durationDays

    // محاسبه‌ی «تعداد» تقریبی از روی زمان‌بندی و طول درمان
    val timesPerDay = when {
        timing.contains("هر ۴ ساعت") || timing.contains("هر 4 ساعت") -> 6
        timing.contains("هر ۶ ساعت") || timing.contains("هر 6 ساعت") -> 4
        timing.contains("هر ۸ ساعت") || timing.contains("هر 8 ساعت") -> 3
        timing.contains("هر ۱۲ ساعت") || timing.contains("هر 12 ساعت") -> 2
        timing.contains("روزانه") || timing.contains("روزی") -> 1
        else -> 1
    }
    val quantity = com.example.util.PersianNumberFormatter.numberToPersianWords(((timesPerDay * dur.coerceAtLeast(1)).toLong()))
        .let { if (it.isBlank()) "${timesPerDay * dur.coerceAtLeast(1)}" else it }

    return com.example.reports.RxColumnRowData(
        title = title,
        quantity = quantity,
        dosage = dosage.ifBlank { "طبق دستور" },
        timing = timing.ifBlank { "روزانه" },
        instructions = instructions
    )
}

/** ساخت متن استاندارد نسخه برای اشتراک‌گذاری (پیامک/واتساپ). */
fun buildStandardRxShareText(
    patientName: String,
    patientNationalIdOrPhone: String,
    dateStr: String,
    durationDays: Int,
    rows: List<com.example.reports.RxColumnRowData>,
    notes: String,
    clinicName: String,
    doctorName: String,
    gender: String
): String {
    val sb = StringBuilder()
    if (clinicName.isNotBlank()) sb.appendLine(clinicName)
    sb.appendLine("نسخه دارویی")
    sb.appendLine("بیمار: $patientName${if (patientNationalIdOrPhone.isNotBlank()) " ($patientNationalIdOrPhone)" else ""}")
    sb.appendLine("تاریخ: $dateStr")
    if (durationDays > 0) sb.appendLine("طول درمان: $durationDays روز")
    sb.appendLine("------------------------------")
    rows.forEachIndexed { idx, r ->
        sb.appendLine("${idx + 1}. ${r.title}")
        sb.appendLine("   تعداد: ${r.quantity} | دوز: ${r.dosage}")
        sb.appendLine("   زمان‌بندی: ${r.timing} | دستور: ${r.instructions}")
    }
    if (notes.isNotBlank()) {
        sb.appendLine("------------------------------")
        sb.appendLine("توضیحات: $notes")
    }
    sb.appendLine("------------------------------")
    sb.appendLine("پزشک معالج: $doctorName")
    return sb.toString().trimEnd()
}

/**
 * جداکنندهٔ رسمی مراحل درمان.
 *
 * از «|||» استفاده می‌شود چون در متن بالینی فارسی هرگز ظاهر نمی‌شود؛ کاما (چه
 * فارسی «،» و چه انگلیسی «,») داخل خودِ شرح مرحله فراوان است و نمی‌تواند
 * جداکننده باشد.
 */
const val STAGE_SEPARATOR = "|||"

/**
 * الگوی شروع یک جلسه در متن الگوهای آماده: «جلسه ۱:»، «جلسه ۲:»،
 * «مراجعات دوره‌ای:»، «جلسه پایانی:» و مانند آن‌ها.
 *
 * از lookahead استفاده می‌کنیم تا خودِ کلمهٔ «جلسه» در ابتدای هر بخش حفظ شود.
 */
private val STAGE_MARKER_REGEX =
    Regex("""(?=(?:جلسه|مراجعات|مراجعه)\s*(?:[۰-۹0-9]+|پایانی|دوره‌ای)?\s*[:：])""")

/**
 * تجزیهٔ رشتهٔ مراحلِ درمان به فهرستِ جلسات. جداکننده به‌ترتیبِ اطمینان:
 * (۱) [STAGE_SEPARATOR] رسمی، (۲) خطِ جدید، (۳) «جلسه N:»، (۴) « - » قدیمی،
 * (۵) کاما فقط برای دادهٔ خیلی قدیمی. کاما آخر است چون شرحِ هر جلسه خودش
 * پر از کاماست و گرنه یک جلسه به چند مرحلهٔ بی‌معنی تکه می‌شود.
 */
fun parseStagesList(stages: String): List<String> {
    val raw = stages.trim()
    if (raw.isBlank()) return emptyList()

    // قالب جدید: با جداکنندهٔ رسمی نوشته شده — هیچ حدسی لازم نیست.
    if (raw.contains(STAGE_SEPARATOR)) {
        return raw.split(STAGE_SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }
    }
    if (raw.contains('\n')) {
        return raw.split('\n').map { it.trim() }.filter { it.isNotBlank() }
    }

    val byMarker = STAGE_MARKER_REGEX.split(raw)
        .map { it.trim().trim('-', '–', '—').trim() }
        .filter { it.isNotBlank() }
    if (byMarker.size >= 2) return byMarker

    if (raw.contains(" - ")) {
        return raw.split(" - ").map { it.trim() }.filter { it.isNotBlank() }
    }

    return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
}

/**
 * ساخت رشتهٔ مراحل از روی فهرست.
 *
 * همیشه با [STAGE_SEPARATOR] شروع می‌شود — این پیشوند نشانهٔ «قالب جدید» است.
 * بدون آن، فهرستی با یک عضو (که خودش پر از کاما و خط تیره است) هنگام خواندن
 * دوباره تکه‌تکه می‌شد.
 */
fun joinStagesList(stages: List<String>): String {
    val clean = stages.map { it.trim() }.filter { it.isNotBlank() }
    if (clean.isEmpty()) return ""
    return STAGE_SEPARATOR + clean.joinToString(STAGE_SEPARATOR)
}

/**
 * تجزیهٔ فهرست مراحلِ «انجام‌شده» یا «ایگنورشده».
 *
 * این‌ها نامِ کاملِ مرحله‌اند و خودشان کاما دارند، پس دقیقاً مثل [parseStagesList]
 * باید با جداکنندهٔ رسمی خوانده شوند. برای داده‌های قدیمی که با کاما ذخیره
 * شده‌اند، عقب‌گرد به کاما انجام می‌شود.
 */
fun parseStageMarkers(raw: String): List<String> {
    val s = raw.trim()
    if (s.isBlank()) return emptyList()
    // قالب جدید (با پیشوند جداکننده) — حتی یک عضوِ دارای کاما هم سالم می‌ماند.
    return if (s.contains(STAGE_SEPARATOR)) {
        s.split(STAGE_SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }
    } else {
        // داده‌های قدیمیِ کاما-جدا
        s.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
}

/** ساخت رشتهٔ مراحلِ انجام‌شده/ایگنورشده — همیشه با پیشوند جداکنندهٔ رسمی. */
fun joinStageMarkers(markers: List<String>): String {
    val clean = markers.map { it.trim() }.filter { it.isNotBlank() }
    if (clean.isEmpty()) return ""
    return STAGE_SEPARATOR + clean.joinToString(STAGE_SEPARATOR)
}

// ---------------------------------------------------------------------------
// انتساب مرحله به جلسه (Treatment.stageSessions)
// ---------------------------------------------------------------------------

private const val STAGE_SESSION_DELIM = "::"

/** خواندن نگاشت «مرحله → شناسهٔ جلسه» از رشتهٔ ذخیره‌شده. */
fun parseStageSessions(raw: String): Map<String, Long> {
    if (raw.isBlank()) return emptyMap()
    val out = LinkedHashMap<String, Long>()
    parseStageMarkers(raw).forEach { entry ->
        val idx = entry.indexOf(STAGE_SESSION_DELIM)
        if (idx > 0) {
            val id = entry.substring(0, idx).trim().toLongOrNull()
            val stage = entry.substring(idx + STAGE_SESSION_DELIM.length).trim()
            if (id != null && stage.isNotBlank()) out[stage] = id
        }
    }
    return out
}

/** سریال‌سازی نگاشت «مرحله → شناسهٔ جلسه». */
fun joinStageSessions(map: Map<String, Long>): String =
    joinStageMarkers(map.map { (stage, id) -> "$id$STAGE_SESSION_DELIM$stage" })

// انتقالِ چک (اندورس) با اثرِ حسابداری: به هزینه ⇒ ExpensePayment چکی روی همان
// هزینه؛ به حسابِ اشخاص ⇒ PersonalTransaction با روشِ CHEQUE_TRANSFER. در هر دو
// حالت وضعیتِ چک به TRANSFERRED می‌رود.
@Composable
fun ChequeTransferDialog(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    cheque: Payment,
    onDismiss: () -> Unit
) {
    val currency = viewModel.label("currency")
    val activeExpenses = viewModel.clinicExpenses.collectAsStateWithLifecycle().value.filter { !it.isDeleted && it.amount > 0 }
    val personalTargets = viewModel.personalSubAccounts.collectAsStateWithLifecycle().value
        .filter { !it.isDeleted }
        .distinctBy { "${it.category.trim()}|${it.parentName.trim()}|${it.name.trim()}" }
        // رفعِ قطعیِ کرشِ «Key was already used»: اگر سینک ابری ردیفی را دوبار
        // کشیده باشد، syncId یکسان دو کلیدِ تکراری می‌سازد؛ dedupe نهایی بر اساس
        // خودِ syncId انجام می‌شود (اولین ردیف برمی‌ماند).
        .distinctBy { it.syncId }
    val parties = viewModel.partyAccounts.collectAsStateWithLifecycle().value
        .filter { !it.isDeleted }
        .distinctBy { it.syncId }
    val expensePayments = viewModel.allExpensePayments.collectAsStateWithLifecycle().value.filter { !it.isDeleted }

    fun t(fa: String, en: String) = if (isPersian) fa else en

    // سطرهای طرف‌حساب‌ها از قبل ساخته می‌شوند (بدون متغیرِ تغییرپذیرِ درونِ
    // ثبتِ آیتم‌ها) و کلیدها رشتهٔ یکتا با پیشوندِ متمایزِ هر بخش‌اند.
    // sectionTitle != null یعنی «سطرِ عنوانِ بخش»؛ در غیر این صورت سطرِ خودِ طرف‌حساب است.
    data class PartyRowUi(
        val key: String,
        val sectionTitle: String? = null,
        val party: com.example.data.PartyAccount? = null,
        val dueLabel: String = ""
    )
    val partyRows = buildList {
        var lastSectionTitle = ""
        parties.sortedWith(compareBy({ it.section }, { it.name })).forEach { party ->
            val secTitle = PartySection.fromKey(party.section).title(isPersian)
            if (secTitle != lastSectionTitle) {
                lastSectionTitle = secTitle
                add(PartyRowUi(key = "party-sec-${party.section}", sectionTitle = secTitle))
            }
            val ledger = partyLedgerOf(party, activeExpenses, expensePayments)
            val dueLabel = when {
                ledger.remaining > 0L -> "${t("مانده:", "Due:")} ${formatAmount(ledger.debtAmount)}"
                ledger.isPartyDebtor -> "${t("بستانکار:", "Credit:")} ${formatAmount(ledger.partyDebtAmount)}"
                else -> t("تسویه", "Settled")
            }
            add(PartyRowUi(key = "party-${party.syncId}", party = party, dueLabel = dueLabel))
        }
    }

    var personName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                Text(t("انتقال چک به بدهی", "Transfer Cheque to Payable Debt"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${t("چک", "Cheque")} ${cheque.chequeBank.orEmpty().ifBlank { "—" }} ${cheque.chequeNumber.orEmpty().ifBlank { "—" }} — ${formatAmount(cheque.amount)} $currency",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = t(
                        "با انتقال، مبلغ چک بلافاصله به‌عنوان پرداختِ طرف‌حساب انتخاب‌شده در حسابداری ثبت می‌شود و وضعیت چک «انتقال یافته به: …» می‌گیرد. پس از آن، انتقالِ دوباره ممکن نیست (برای تغییر، اول انتقال را لغو کنید).",
                        "On transfer the cheque amount is posted immediately to the selected payable party and the cheque becomes \"Transferred to: …\". A transferred cheque cannot be transferred again — undo the transfer first."
                    ),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                if (!viewModel.canTransferCheque(cheque)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = t(
                                "این چک قابل انتقال نیست: ${viewModel.chequeStatusLabel(cheque, true)}.",
                                "This cheque cannot be transferred: ${viewModel.chequeStatusLabel(cheque, false)}."
                            ),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                // انتقال به «حساب اشخاصِ» غیرمرتبط با هزینه‌های مطب: با نامِ آزاد،
                // چک فقط در «حساب اشخاص» می‌نشیند و در هزینه‌های مطب ثبت نمی‌شود.
                if (viewModel.canTransferCheque(cheque)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                t("انتقال به شخصِ غیرمرتبط با هزینه‌های مطب (فقط در «حساب اشخاص» ثبت می‌شود)",
                                    "Transfer to a person unrelated to clinic expenses (recorded only in Person Accounts)"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            OutlinedTextField(
                                value = personName,
                                onValueChange = { personName = it },
                                label = { Text(t("نام شخص", "Person name"), fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val name = personName.trim()
                                            if (name.isNotBlank()) {
                                                viewModel.transferChequeToPerson(cheque, name)
                                                onDismiss()
                                            }
                                        },
                                        enabled = personName.isNotBlank()
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = t("ثبت در حساب اشخاص", "Add to person account"), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
                if (viewModel.canTransferCheque(cheque))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // طرف‌حساب‌های هزینه‌های مطب — مسیر اصلیِ انتقال:
                    // چکِ منتقل‌شده یک سندِ پرداختیِ چکی در دفتر همان طرف‌حساب می‌شود (transferPartyPaymentId).
                    if (partyRows.isNotEmpty()) {
                        item(key = "hdr-party") { Text(t("طرف‌حساب‌های حسابداری (هزینه‌های مطب)", "Party accounts (clinic expenses)"), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        partyRows.forEach { row ->
                            item(key = row.key) {
                                if (row.sectionTitle != null) {
                                    Text(
                                        text = row.sectionTitle,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {
                                    val target = row.party ?: return@item
                                    TextButton(onClick = { viewModel.transferChequeToParty(cheque, target); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                                        Text("${target.displayLabel} — ${row.dueLabel} $currency", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    item { Text(t("فاکتورهای بدون طرف‌حساب (هزینه‌های قدیمی)", "Legacy invoices without party"), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    val legacyExpenses = activeExpenses.filter { it.partyAccountId == null }
                    if (legacyExpenses.isEmpty()) {
                        item { Text(t("همهٔ فاکتورها به طرف‌حساب‌ها پیوند خورده‌اند", "All invoices are linked to parties"), fontSize = 10.sp, color = Color.Gray) }
                    }
                    items(legacyExpenses.distinctBy { it.id }, key = { "legacy-${it.id}" }) { expense ->
                        TextButton(onClick = { viewModel.transferChequeToExpense(cheque, expense.subAccountName, expense.category, expense.id); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                            Text("${expense.subAccountName.ifBlank { expense.category }} — ${formatAmount(expense.amount)} $currency", maxLines = 1)
                        }
                    }
                    item { Text(t("حساب‌های شخصی طرف حساب (حساب اشخاص)", "Personal payable accounts (persons)"), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    if (personalTargets.isEmpty()) {
                        item { Text(t("حساب شخصی تعریف نشده است", "No personal accounts defined"), fontSize = 10.sp, color = Color.Gray) }
                    }
                    items(personalTargets, key = { "pers-${it.syncId}" }) { target ->
                        val label = if (target.parentName.isBlank()) target.name else "${target.parentName} - ${target.name}"
                        TextButton(
                            onClick = {
                                if (target.category == "PERSONS") {
                                    // حساب اشخاص: با دو سند (ChildrenExpense + تراکنش شخصی) مستند می‌شود
                                    viewModel.transferChequeToPerson(cheque, label)
                                } else {
                                    viewModel.transferChequeToPersonal(cheque, label, label)
                                }
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (target.category == "PERSONS") "👤 $label" else label, maxLines = 1)
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(t("انصراف", "Cancel")) } }
    )
}

