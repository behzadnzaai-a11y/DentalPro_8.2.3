package com.example

import com.example.data.Appointment
import com.example.data.BankAccount
import com.example.data.ClinicExpense
import com.example.data.ExpensePayment
import com.example.data.Payment
import com.example.data.Treatment
import com.example.data.Vendor
import com.example.data.isCashFund
import com.example.data.isEffectivelyPaid
import com.example.data.netCost
import com.example.data.postedTotal
import com.example.data.postedTotalFor
import com.example.data.splitAmountAcrossUnits
import com.example.data.unmatchedLabLedgers
import com.example.data.vendorLedger
import com.example.security.SecurityUtils
import com.example.ui.screens.parseTeethCount
import com.example.ui.screens.toEnglishLong
import com.example.util.AppointmentConflictDetector
import com.example.util.PersianCalendarHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalDomainLogicTest {

    @Test
    fun testTreatmentNetCostNeverNegative() {
        val normal = Treatment(patientId = 1, title = "RCT", cost = 1_000_000L, discount = 200_000L)
        assertEquals(800_000L, normal.netCost)

        val overDiscount = Treatment(patientId = 1, title = "Fill", cost = 100_000L, discount = 250_000L)
        assertEquals(0L, overDiscount.netCost)
    }

    @Test
    fun testBankAccountIsCashFund() {
        assertTrue(BankAccount(bankName = "صندوق نقدی مطب", accountNumber = "CASH-01").isCashFund)
        assertTrue(BankAccount(bankName = "تنخواه گردان مطب", accountNumber = "PETTY-01").isCashFund)
        assertTrue(BankAccount(bankName = "صندوق ویژه", accountNumber = "CASH-EXTRA-1").isCashFund)
        assertFalse(BankAccount(bankName = "بانک ملت", accountNumber = "123456").isCashFund)
    }

    @Test
    fun testPersianToEnglishLongConversion() {
        val persianNum = "۱۲۳۴۵۶۷۸۹۰"
        val converted = persianNum.toEnglishLong()
        assertEquals(1234567890L, converted)

        val formattedAmount = "۲,۵۰۰,۰۰۰ تومان"
        val cleanAmount = formattedAmount.toEnglishLong()
        assertEquals(2500000L, cleanAmount)
    }

    @Test
    fun testParseTeethCount() {
        assertEquals(1, parseTeethCount("14"))
        assertEquals(2, parseTeethCount("14, 15"))
        assertEquals(4, parseTeethCount("11-14"))
        assertEquals(3, parseTeethCount("21, 22, 23"))
    }

    @Test
    fun testPersianCalendarHelperConversions() {
        // Test 1 Farvardin 1405 conversion (21 March 2026)
        val jDate = PersianCalendarHelper.gregorianToJalali(2026, 3, 21)
        assertEquals(1405, jDate.year)
        assertEquals(1, jDate.month)
        assertEquals(1, jDate.day)

        val (gy, gm, gd) = PersianCalendarHelper.jalaliToGregorian(1405, 1, 1)
        assertEquals(2026, gy)
        assertEquals(3, gm)
        assertEquals(21, gd)

        // Leap year checks
        assertTrue(PersianCalendarHelper.isJalaliLeapYear(1403))
        assertFalse(PersianCalendarHelper.isJalaliLeapYear(1405))
    }

    @Test
    fun testAppointmentConflictDetection() {
        val baseTime = 1770000000000L
        val existingAppointments = listOf(
            Appointment(
                id = 1,
                patientId = 10,
                patientName = "علی رضایی",
                date = baseTime, // start: baseTime, end: baseTime + 30m
                treatmentType = "عصب‌کشی"
            ),
            Appointment(
                id = 2,
                patientId = 11,
                patientName = "مریم حسینی",
                date = baseTime + (60 * 60 * 1000L), // 1 hour later
                treatmentType = "ترمیم"
            )
        )

        // Test 1: Exact overlap
        val overlapResult = AppointmentConflictDetector.checkConflict(
            targetDate = baseTime + (10 * 60 * 1000L), // 10 mins into Ali's slot
            durationMinutes = 30,
            existingAppointments = existingAppointments
        )
        assertTrue(overlapResult.hasConflict)
        assertEquals("علی رضایی", overlapResult.conflictingAppointment?.patientName)

        // Test 2: Free slot (35 mins after first appointment)
        val freeResult = AppointmentConflictDetector.checkConflict(
            targetDate = baseTime + (35 * 60 * 1000L),
            durationMinutes = 20,
            existingAppointments = existingAppointments
        )
        assertFalse(freeResult.hasConflict)

        // Test 3: Same appointment edit (excluding own id)
        val selfEditResult = AppointmentConflictDetector.checkConflict(
            targetDate = baseTime,
            durationMinutes = 30,
            excludeAppointmentId = 1L,
            existingAppointments = existingAppointments
        )
        assertFalse(selfEditResult.hasConflict)

        val otherUnit = AppointmentConflictDetector.checkConflict(
            targetDate = baseTime + (10 * 60 * 1000L),
            durationMinutes = 30,
            existingAppointments = existingAppointments,
            unit = "UNIT_2"
        )
        assertFalse(otherUnit.hasConflict)

        val longExisting = listOf(
            Appointment(
                id = 3,
                patientId = 12,
                patientName = "نوبت طولانی",
                date = baseTime,
                treatmentType = "جراحی",
                durationMinutes = 90
            )
        )
        val overlapLong = AppointmentConflictDetector.checkConflict(
            targetDate = baseTime + (60 * 60 * 1000L),
            durationMinutes = 30,
            existingAppointments = longExisting
        )
        assertTrue(overlapLong.hasConflict)
    }

    @Test
    fun testPaymentIsEffectivelyPaidExcludesBouncedAndPendingCheques() {
        val cash = Payment(patientId = 1, amount = 100, type = "CASH")
        assertTrue(cash.isEffectivelyPaid)

        val pendingCheque = Payment(patientId = 1, amount = 200, type = "CHEQUE", isCleared = false, chequeStatus = "PENDING")
        assertFalse(pendingCheque.isEffectivelyPaid)

        val clearedCheque = Payment(patientId = 1, amount = 300, type = "CHEQUE", isCleared = true, chequeStatus = "CLEARED")
        assertTrue(clearedCheque.isEffectivelyPaid)

        val bounced = Payment(patientId = 1, amount = 400, type = "CHEQUE", isCleared = false, chequeStatus = "BOUNCED")
        assertFalse(bounced.isEffectivelyPaid)

        val transferred = Payment(patientId = 1, amount = 500, type = "CHEQUE", isCleared = false, chequeStatus = "TRANSFERRED")
        assertTrue(transferred.isEffectivelyPaid)

        val deletedCash = Payment(patientId = 1, amount = 600, type = "CASH", isDeleted = true)
        assertFalse(deletedCash.isEffectivelyPaid)

        val mixed = listOf(cash, pendingCheque, clearedCheque, bounced, transferred, deletedCash)
        assertEquals(100L + 300L + 500L, mixed.postedTotal())
        assertEquals(900L, mixed.postedTotalFor(1))
    }

    @Test
    fun testSplitAmountRemainderLandsOnLastUnit() {
        assertEquals(listOf(33L, 33L, 34L), splitAmountAcrossUnits(100L, 3))
        assertEquals(listOf(50L, 50L), splitAmountAcrossUnits(100L, 2))
        assertEquals(emptyList<Long>(), splitAmountAcrossUnits(100L, 0))
    }

    @Test
    fun testVendorLedgerFromExpensesAndPayments() {
        val vendor = Vendor(id = 7, name = "لاب البرز", company = "البرز", phone = "", itemType = "پروتز")
        val expenses = listOf(
            ClinicExpense(id = 1, category = "لابراتوار", amount = 1_000_000L, subAccountName = "لاب البرز", vendorId = 7),
            ClinicExpense(id = 2, category = "لابراتوار", amount = 400_000L, subAccountName = "لاب دیگر")
        )
        val payments = listOf(
            ExpensePayment(id = 1, clinicExpenseId = 1, amount = 250_000L)
        )
        val ledger = vendorLedger(vendor, expenses, payments)
        assertEquals(1_000_000L, ledger.orders)
        assertEquals(250_000L, ledger.paid)
        assertEquals(750_000L, ledger.remaining)

        val unmatched = unmatchedLabLedgers(listOf(vendor), expenses, payments)
        assertEquals(1, unmatched.size)
        assertEquals("لاب دیگر", unmatched[0].first)
        assertEquals(400_000L, unmatched[0].second.remaining)
    }

    @Test
    fun testSecurityPinHashingAndVerification() {
        val pin = "7890"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin(pin, salt)

        assertTrue(SecurityUtils.verifyPin("7890", hash, salt))
        assertFalse(SecurityUtils.verifyPin("1234", hash, salt))
        assertFalse(SecurityUtils.verifyPin("0000", hash, salt))

        val signaturePayload = "Consent: Ali Rezaei, Doctor: Dr. Dentist, Timestamp: 1770000000"
        val sha256Hash = SecurityUtils.sha256(signaturePayload)
        assertEquals(64, sha256Hash.length) // Standard SHA-256 hex length
    }
}
