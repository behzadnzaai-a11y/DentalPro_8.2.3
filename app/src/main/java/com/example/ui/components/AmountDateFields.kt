package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.JalaliDatePickerDialog

/**
 * ============================================================================
 *  کامپوننت‌های استاندارد ورودی مبلغ و تاریخ (درخواست کاربر — یکسان در همهٔ فرم‌ها)
 * ============================================================================
 *  - [AmountInputField]: ورودی مبلغ با کیبورد عددی، جداسازیِ زندهٔ ۳رقمی و
 *    واحدِ انتخابی نرم‌افزار؛ «معادل حروفی» همان لحظه و به رنگ قرمز زیر فیلد.
 *  - [JalaliDateField]: فیلد تاریخ فقط‌خواندنی که تقویمِ انتخابیِ نرم‌افزار
 *    (جلالی — همان تقویم بقیهٔ فرم‌ها) را باز می‌کند.
 *
 *  مقدار مبلغ به‌صورت «رشتهٔ گروه‌بندی‌شده» (مثل 1,250,000) نگه داشته می‌شود و
 *  با [parseGroupedAmount] به عدد تبدیل می‌شود؛ ارقام فارسی/عربی هم پذیرفته
 *  می‌شوند.
 */

/** ارقام مجاز ورودی (تبدیل ارقام فارسی/عربی → لاتین) و سقف ۱۵ رقم برای Long. */
private const val AMOUNT_MAX_DIGITS = 15

/** رشتهٔ ورودی را به رقم‌های لاتینِ خالص تبدیل می‌کند (بدون جداکننده). */
fun amountDigitsOf(raw: String): String =
    com.example.util.PersianNumberFormatter.toAsciiDigits(raw).filter(Char::isDigit).take(AMOUNT_MAX_DIGITS)

/** رشتهٔ گروه‌بندی‌شدهٔ فیلد مبلغ را به Long تبدیل می‌کند (۰ در صورت نامعتبر). */
fun parseGroupedAmount(raw: String): Long = amountDigitsOf(raw).toLongOrNull() ?: 0L

/** گروه‌بندیِ زندهٔ ۳رقمی با همان جداکنندهٔ یکسانِ بقیهٔ اپ (کاما). */
fun groupAmountDigits(digits: String): String {
    val v = digits.toLongOrNull() ?: return ""
    return if (v == 0L && digits.isEmpty()) "" else com.example.util.PersianNumberFormatter.formatAmount(v)
}

/** معادل حروفی قرمزِ زندهٔ مبلغ + واحد پول. */
@Composable
fun AmountWordsInRed(raw: String, currency: String, isPersian: Boolean) {
    val value = parseGroupedAmount(raw)
    if (value <= 0L) return
    val words = try {
        com.example.util.PersianNumberFormatter.numberToPersianWords(value)
    } catch (_: Exception) { "" }
    Column(modifier = Modifier.padding(top = 1.dp)) {
        Text(
            text = if (isPersian) "معادل حروفی: $words $currency" else "In words: $words $currency",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC62828), // قرمز — طبق درخواست
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${groupAmountDigits(amountDigitsOf(raw))} $currency",
            fontSize = 9.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * فیلد مبلغ استاندارد: کیبورد عددی + جداکنندهٔ ۳رقمیِ زنده + واحد + معادل حروفی
 * قرمزِ زنده. مقدارِ value رشتهٔ گروه‌بندی‌شده است (با [parseGroupedAmount] عدد
 * بگیرید).
 */
@Composable
fun AmountInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    currency: String,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showWords: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { incoming ->
                val digits = amountDigitsOf(incoming)
                onValueChange(groupAmountDigits(digits))
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                Text(
                    text = currency,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        )
        if (showWords) {
            AmountWordsInRed(value, currency, isPersian)
        }
    }
}

/**
 * فیلد تاریخ استاندارد: فقط‌خواندنی، با آیکون تقویم؛ با لمس، تقویمِ انتخابیِ
 * نرم‌افزار (جلالی) باز می‌شود — همان تقویمِ بقیهٔ فرم‌های اپ.
 */
@Composable
fun JalaliDateField(
    timestamp: Long,
    onDateSelected: (Long) -> Unit,
    isPersian: Boolean,
    label: String,
    formatDate: (Long) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }
    if (showPicker) {
        JalaliDatePickerDialog(
            initialTimestamp = timestamp,
            isPersian = isPersian,
            onDateSelected = { jy, jm, jd ->
                onDateSelected(com.example.util.PersianCalendarHelper.jalaliToTimestamp(jy, jm, jd))
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
    // لایهٔ نامرئیِ دریافت لمس روی فیلدِ فقط‌خواندنی — الگوی رایج Compose برای
    // فیلدهای تاریخ (بدون آن، OutlinedTextField لمس را مصرف می‌کند و تقویم باز نمی‌شود).
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        OutlinedTextField(
            value = formatDate(timestamp),
            onValueChange = { },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            readOnly = true,
            trailingIcon = {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        if (enabled) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showPicker = true }
            )
        }
    }
}
