package com.example

import com.example.reports.ExportDocKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حفاظت از «قواعد ابعاد خروجی‌ها»: هر بار که کسی عدد عرض/ارتفاع تازه‌ای را
 * هارد‌کد کند یا یک خروجی را بی‌نسبت رها کند، این تست‌ها می‌شکنند.
 * (ریشهٔ باگ اصلی همین بود: هر ژنراتور عرض دلخواه خودش را داشت.)
 */
class ExportLayoutTest {

    @Test
    fun everyOutputKindDeclaresADeliberateWidth() {
        for (kind in ExportDocKind.entries) {
            assertTrue("${kind.name} width out of the agreed band", kind.widthPx in 1000..1500)
            assertTrue("${kind.name} has no margin", kind.margin > 0)
            assertTrue("${kind.name} needs a Persian label", kind.label.isNotBlank())
        }
    }

    @Test
    fun geometryScaleIsDerivedFromTheReferenceWidthOnly() {
        for (kind in ExportDocKind.entries) {
            assertEquals(
                "${kind.name} must scale from ${ExportDocKind.REFERENCE_WIDTH_PX}px",
                kind.widthPx.toFloat() / ExportDocKind.REFERENCE_WIDTH_PX,
                kind.scale,
                1e-6f
            )
        }
    }

    @Test
    fun everyDocumentHeightComesFromItsContent() {
        // تنها خروجی مجاز با ارتفاع ثابت، کارت بانکی است (نسبت از پیش‌محدد ISO).
        for (kind in ExportDocKind.entries) {
            if (kind == ExportDocKind.BankCard) continue
            assertEquals("${kind.name} must not fix its height", 0, kind.heightPx)
        }
    }

    @Test
    fun bankCardKeepsTheId1AspectRatio() {
        val ratio = ExportDocKind.BankCard.widthPx.toFloat() / ExportDocKind.BankCard.heightPx
        // ISO/IEC 7810 ID-1: 85.6 × 53.98 ⇒ 1.5858
        assertEquals(1.586f, ratio, 0.01f)
    }

    @Test
    fun chatCardsShareOneWidthAndPaperDocsAnother() {
        assertEquals(ExportDocKind.InfoCard.widthPx, ExportDocKind.StatementCard.widthPx)
        assertEquals(ExportDocKind.AdmissionForm.widthPx, ExportDocKind.LegalConsent.widthPx)
        assertEquals(ExportDocKind.LegalConsent.widthPx, ExportDocKind.PostOpCare.widthPx)
        assertEquals(ExportDocKind.InstallmentNotice.widthPx, ExportDocKind.AdmissionForm.widthPx)
        // خروجی‌های جدولی پهن‌ترند تا ستون‌ها نشکنند
        assertTrue(ExportDocKind.Prescription.widthPx > ExportDocKind.AdmissionForm.widthPx)
        assertEquals(ExportDocKind.LabInvoice.widthPx, ExportDocKind.Prescription.widthPx)
        assertEquals(ExportDocKind.VendorLedger.widthPx, ExportDocKind.Prescription.widthPx)
    }
}
