-- ============================================================================
-- DentalPro — Migration 0010: ردِ اسنادِ انتقال چک + تأییدهای بانکی
-- ============================================================================
-- تا پیش از این، وقتی یک چک به هزینه یا حساب شخصی «انتقال» داده می‌شد، اسنادِ
-- مقصد (expense_payments / personal_transactions / children_expenses) ساخته
-- می‌شدند اما هیچ ارجاعی از چک به آن‌ها نگهداری نمی‌شد.
--
-- نتیجه‌اش سه باگِ حسابداری بود:
--   ۱. حذف چک   → سندِ مقصد باقی می‌ماند و هزینه تا ابد «پرداخت‌شده» می‌ماند.
--   ۲. ویرایش مبلغ چک → مبلغِ سندِ مقصد قدیمی می‌ماند و دو طرف نامساوی می‌شدند.
--   ۳. برگشت‌خوردنِ چک → تسویهٔ هزینهٔ مقصد برنمی‌گشت.
--
-- ستون‌های transfer_*_id این ارجاع را نگه می‌دارند تا هر سه عملیات بتوانند
-- اثرِ انتقال را دقیقاً برگردانند.
--
-- دو ستون تأیید هم اضافه می‌شود؛ این‌ها «یک‌بارمصرف»‌اند: پس از true شدن دیگر
-- در رابط کاربری قابل برداشتن نیستند.
-- ============================================================================

alter table public.payments
    add column if not exists transfer_expense_payment_id  bigint,
    add column if not exists transfer_expense_id          bigint,
    add column if not exists transfer_transaction_id      bigint,
    add column if not exists transfer_children_expense_id bigint,
    add column if not exists bank_receipt_confirmed  boolean not null default false,
    add column if not exists bank_transfer_confirmed boolean not null default false;

comment on column public.payments.transfer_expense_payment_id is
    'ExpensePayment ساخته‌شده هنگام انتقال چک — برای برگرداندن اثر در حذف/ویرایش/برگشت';
comment on column public.payments.transfer_expense_id is
    'ClinicExpense ساخته‌شده هنگام انتقال چک (فقط وقتی هزینهٔ تازه ساخته شده باشد)';
comment on column public.payments.transfer_transaction_id is
    'PersonalTransaction ساخته‌شده هنگام انتقال چک به حساب شخصی/اشخاص';
comment on column public.payments.transfer_children_expense_id is
    'ChildrenExpense ساخته‌شده هنگام انتقال چک به «حساب اشخاص»';
comment on column public.payments.bank_receipt_confirmed is
    'تیک یک‌بارمصرف: دریافت بانکیِ چکِ پاس‌شده تأیید شد';
comment on column public.payments.bank_transfer_confirmed is
    'تیک یک‌بارمصرف: انتقال بانکیِ چکِ انتقال‌یافته تأیید شد';
