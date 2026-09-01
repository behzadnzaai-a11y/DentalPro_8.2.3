package com.example

import com.example.ui.screens.birthDateDayFirstError
import com.example.ui.screens.calculateJalaliAge
import com.example.ui.screens.formatBirthDateYearFirst
import com.example.ui.screens.parseYearFirstBirthDate
import com.example.util.PersianCalendarHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthDateDayFirstTest {

    @Test
    fun testContinuousDigitsFormattedYearFirst() {
        assertEquals("1375", formatBirthDateYearFirst("1375"))
        assertEquals("1375/0", formatBirthDateYearFirst("13750"))
        assertEquals("1375/04", formatBirthDateYearFirst("137504"))
        assertEquals("1375/04/1", formatBirthDateYearFirst("1375041"))
        assertEquals("1375/04/10", formatBirthDateYearFirst("13750410"))
    }

    @Test
    fun testLegacyDayFirstAutoReversed() {
        assertEquals("1375/04/10", formatBirthDateYearFirst("10/04/1375"))
        assertEquals("1375/04/01", formatBirthDateYearFirst("1/4/1375"))
    }

    @Test
    fun testYearOnlyStaysAlone() {
        assertEquals("1375", formatBirthDateYearFirst("1375"))
    }

    @Test
    fun testPersianDigitsSupported() {
        assertEquals("1375/04/10", formatBirthDateYearFirst("۱۳۷۵۰۴۱۰"))
    }

    @Test
    fun testParseValidYearFirst() {
        assertEquals(Triple(1375, 4, 10), parseYearFirstBirthDate("1375/04/10"))
    }

    @Test
    fun testParseInvalidOrPartial() {
        assertNull(parseYearFirstBirthDate("1375/04"))
        assertNull(parseYearFirstBirthDate("1375/13/10"))
        assertNull(parseYearFirstBirthDate("1375/04/32"))
        assertNull(parseYearFirstBirthDate(""))
    }

    @Test
    fun testErrorMessages() {
        assertNull(birthDateDayFirstError("1375/04/10", true))
        assertNotNull(birthDateDayFirstError("1375/13/10", true))
        assertNotNull(birthDateDayFirstError("1375/04/32", true))
        assertNull(birthDateDayFirstError("", true))
    }

    @Test
    fun testJalaliAgeCalculation() {
        val today = PersianCalendarHelper.timestampToJalali(System.currentTimeMillis())
        val base = today.year - 1375
        val aSpring = calculateJalaliAge("1375/01/10")
        val aAutumn = calculateJalaliAge("1375/09/10")
        assertNotNull(aSpring)
        assertNotNull(aAutumn)
        assertTrue(aSpring!! in (base - 1)..base)
        assertTrue(aAutumn!! in (base - 1)..base)
        assertTrue(aSpring >= aAutumn)
        assertTrue(aSpring - aAutumn in 0..1)
    }

    @Test
    fun testJalaliAgeInvalidReturnsNull() {
        assertNull(calculateJalaliAge("1375/04"))
        assertNull(calculateJalaliAge("1375/13/10"))
        assertNull(calculateJalaliAge(""))
    }
}
