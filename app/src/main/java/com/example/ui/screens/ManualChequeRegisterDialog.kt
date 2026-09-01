package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.DentalViewModel
import java.io.File

/**
 * ثبت دستی چک صیادی:
 * گردش کار: عکسبرداری یا انتخاب عکس از گالری -> کادر برش چک -> فرم دستی مشخصات با پیش‌نمایش عکس بریده‌شده -> ثبت و ذخیره.
 */
@Composable
fun ManualChequeRegisterDialog(
    patientId: Long,
    viewModel: DentalViewModel,
    isPersian: Boolean,
    initialPhotoPath: String? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    fun t(fa: String, en: String) = if (isPersian) fa else en
    val context = LocalContext.current

    // مسیر تصویر بریده‌شده‌ی چک
    var currentPhotoPath by remember { mutableStateOf<String?>(initialPhotoPath) }
    var showPhotoCaptureOptions by remember { mutableStateOf(initialPhotoPath.isNullOrBlank()) }

    // فیلدها — مجموعه فیلدهای فرم چک صیادی
    var amountStr by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }
    var branchCode by remember { mutableStateOf("") }
    var drawerName by remember { mutableStateOf("") }
    var drawerNationalId by remember { mutableStateOf("") }
    var chequeNumber by remember { mutableStateOf("") }
    var sayadiId by remember { mutableStateOf("") }
    var dueDateStr by remember { mutableStateOf("") } // شمسی YYYY-MM-DD
    var attemptSaved by remember { mutableStateOf(false) }

    val currency = viewModel.label("currency")

    // لانچرهای ویرایش و برش هوشمند تصویر چک
    val photoEditLauncher = rememberPhotoEditLauncher(
        kind = PhotoAssetKind.SAYAD_CHEQUE,
        onEdited = { savedPath ->
            currentPhotoPath = savedPath
            showPhotoCaptureOptions = false
        }
    )

    val editableCameraLauncher = rememberEditableCameraLauncher(
        context = context,
        onImageCaptured = { uri ->
            photoEditLauncher(uri)
        }
    )

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoEditLauncher(uri)
        }
    }

    // مبلغ به حروف و تاریخ کامل — خودکار از ورودی‌ها
    val amountTextStr = remember(amountStr, currency) {
        val amount = amountStr.trim().toLongOrNull() ?: return@remember ""
        val text = com.example.util.PersianNumberFormatter.numberToPersianWords(amount)
        if (text.isNotBlank()) "$text $currency" else ""
    }
    val dueDateDisplayStr = remember(dueDateStr) {
        com.example.util.PersianNumberFormatter.jalaliDigitsToFullDate(dueDateStr)
    }

    val amountError = if (attemptSaved && amountStr.trim().toLongOrNull()?.let { it > 0 } != true)
        t("مبلغ چک را وارد کنید", "Enter cheque amount") else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (showPhotoCaptureOptions && currentPhotoPath.isNullOrBlank())
                        t("تصویر چک صیادی (مرحله ۱ از ۲)", "Cheque Photo (Step 1 of 2)")
                    else
                        t("ثبت دستی مشخصات چک (مرحله ۲ از ۲)", "Manual Cheque Details (Step 2 of 2)"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        text = {
            if (showPhotoCaptureOptions && currentPhotoPath.isNullOrBlank()) {
                // مرحله ۱: انتخاب عکس چک با دوربین یا گالری و اعمال کادر برش
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = t(
                            "برای ثبت چک، ابتدا عکس چک را با دوربین بگیرید یا از گالری انتخاب کنید تا کادر برش تنظیم شود:",
                            "To register the cheque, take a photo or select from gallery to crop:"
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { editableCameraLauncher() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t("عکسبرداری با دوربین و کادر برش", "Camera & Crop"), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { galleryPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t("انتخاب از گالری و کادر برش", "Gallery & Crop"), fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { showPhotoCaptureOptions = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = t("رد شدن از عکس و ورود مستقیم مشخصات دستی", "Skip photo & enter details manually"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // مرحله ۲: فرم ورود دستی مشخصات به همراه تصویر چک بریده‌شده در بالا
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    // نمایش پیش‌نمایش تصویر بریده‌شده‌ی چک در بالای فرم
                    if (!currentPhotoPath.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Text(t("تصویر برش‌خورده چک", "Cropped Cheque Image"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { editableCameraLauncher() },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(t("دوربین", "Camera"), fontSize = 10.sp)
                                        }
                                        TextButton(
                                            onClick = { galleryPickerLauncher.launch("image/*") },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(t("گالری", "Gallery"), fontSize = 10.sp)
                                        }
                                    }
                                }
                                val imageFile = File(currentPhotoPath!!)
                                if (imageFile.exists()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(imageFile),
                                        contentDescription = "Cropped Cheque",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                photoEditLauncher(Uri.fromFile(imageFile))
                                            },
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showPhotoCaptureOptions = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t("افزودن و برش تصویر چک", "Add & Crop Cheque Photo"), fontSize = 11.sp)
                        }
                    }

                    Text(
                        text = t(
                            "مشخصات چک را دستی وارد نمایید:",
                            "Enter cheque details manually:"
                        ),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = amountStr, onValueChange = { amountStr = it.filter(Char::isDigit) },
                        label = { Text(t("مبلغ رقمی ($currency)", "Amount ($currency)")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        isError = amountError != null,
                        supportingText = { if (amountError != null) Text(amountError, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = amountTextStr.ifBlank { t("— با نوشتن مبلغ رقمی، خودکار محاسبه می‌شود —", "— Calculated automatically from digits —") },
                        onValueChange = {}, readOnly = true, enabled = false,
                        label = { Text(t("مبلغ به حروف ($currency)", "Amount in words ($currency)")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    OutlinedTextField(
                        value = dueDateStr, onValueChange = { dueDateStr = it },
                        label = { Text(t("تاریخ سررسید (شمسی، مثال 1404-03-15)", "Due date (Jalali, e.g. 1404-03-15)")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (dueDateDisplayStr.isNotBlank()) {
                        OutlinedTextField(
                            value = dueDateDisplayStr, onValueChange = {}, readOnly = true, enabled = false,
                            label = { Text(t("تاریخ سررسید (خودکار)", "Due date (auto)")) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    OutlinedTextField(
                        value = bankName, onValueChange = { bankName = it },
                        label = { Text(t("بانک", "Bank")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = branchName, onValueChange = { branchName = it },
                            label = { Text(t("شعبه", "Branch")) },
                            modifier = Modifier.weight(1f), singleLine = true
                        )
                        OutlinedTextField(
                            value = branchCode, onValueChange = { branchCode = it.filter(Char::isDigit) },
                            label = { Text(t("کد شعبه", "Branch code")) },
                            modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    OutlinedTextField(
                        value = drawerName, onValueChange = { drawerName = it },
                        label = { Text(t("صاحب حساب / صادرکننده", "Account holder / Drawer")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = drawerNationalId, onValueChange = { drawerNationalId = it.filter(Char::isDigit) },
                        label = { Text(t("کد ملی صاحب حساب", "Account holder National ID")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = chequeNumber, onValueChange = { chequeNumber = it.filter(Char::isDigit) },
                        label = { Text(t("شماره سریال چک", "Cheque Serial Number")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = sayadiId, onValueChange = { sayadiId = it.filter(Char::isDigit) },
                        label = { Text(t("شناسه صیادی (۱۶ رقم)", "Sayad ID (16 digits)")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            if (!showPhotoCaptureOptions || !currentPhotoPath.isNullOrBlank()) {
                Button(onClick = {
                    attemptSaved = true
                    if (commitChequePaymentFromForm(
                            viewModel = viewModel,
                            patientId = patientId,
                            amountStr = amountStr,
                            bankName = bankName,
                            branchName = branchName,
                            branchCode = branchCode,
                            drawerName = drawerName,
                            drawerNationalId = drawerNationalId,
                            chequeNumber = chequeNumber,
                            sayadiId = sayadiId,
                            dueDateStr = dueDateStr,
                            amountTextStr = amountTextStr,
                            photoPath = currentPhotoPath
                        )
                    ) {
                        onSaved()
                        onDismiss()
                    }
                }) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(t("ثبت چک", "Register Cheque"), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (!showPhotoCaptureOptions && currentPhotoPath != null) {
                    showPhotoCaptureOptions = true
                } else {
                    onDismiss()
                }
            }) {
                Text(t("برگشت", "Back"))
            }
        }
    )
}

