-- ============================================================================
-- DentalPro — Migration 0014: ظرف‌های حساب (Party Accounts) — طرف‌حساب‌های
-- چندظرفیِ هزینه‌های مطب
-- ============================================================================
-- مدل جدید:
--   • جدول party_accounts — هر «ظرف حساب» (مثل «لابراتوار آ»، «فنی مهندسی ب»،
--     «فروشگاه آ») یک ردیف مستقل با بخش (section)، عنوان (title)، نام (name) و
--     کد (code) است.
--   • clinic_expenses.party_account_id — هر فاکتور (بدهی ما) به ظرف وصل می‌شود.
--   • clinic_expenses.invoice_number — شمارهٔ فاکتورِ خودکارِ per-party.
--   • expense_payments.party_account_id — هر پرداختی به ظرف (یا فاکتورِ همان ظرف).
--   • payments.transfer_party_payment_id — ردِ پرداختیِ چکیِ ساخته‌شده هنگام
--     انتقال چک به ظرف (یک اشاره‌گر واحد به‌جای مسیرهای قبلی).
-- هم‌نگاشتِ camelCase↔snake_case سمتِ کلاینت:
--   partyAccountId↔party_account_id، partyAccountSyncId↔party_account_sync_id،
--   invoiceNumber↔invoice_number، transferPartyPaymentId↔transfer_party_payment_id،
--   invoiceSeq↔invoice_seq.
-- ============================================================================

create table if not exists public.party_accounts (
    section         text not null default '',
    code            text not null default '',
    title           text not null default '',
    name            text not null default '',
    phone           text not null default '',
    notes           text not null default '',
    invoice_seq     bigint not null default 0,
    owner_id        text not null default '',
    sync_id         text primary key,
    updated_at      bigint not null default 0,
    is_deleted      boolean not null default false
);

create index if not exists idx_party_accounts_owner_id on public.party_accounts (owner_id);
create index if not exists idx_party_accounts_section on public.party_accounts (owner_id, section);
create index if not exists idx_party_accounts_name on public.party_accounts (owner_id, name);

-- فاکتورها → ظرف
alter table public.clinic_expenses
    add column if not exists party_account_id bigint;
alter table public.clinic_expenses
    add column if not exists party_account_sync_id text not null default '';
alter table public.clinic_expenses
    add column if not exists invoice_number text not null default '';

create index if not exists idx_clinic_expenses_party_account
    on public.clinic_expenses (owner_id, party_account_id);

-- پرداختی‌ها → ظرف
alter table public.expense_payments
    add column if not exists party_account_id bigint;
alter table public.expense_payments
    add column if not exists party_account_sync_id text not null default '';

create index if not exists idx_expense_payments_party_account
    on public.expense_payments (owner_id, party_account_id);

-- ردِ پرداختیِ چکیِ انتقال‌یافته به ظرف
alter table public.payments
    add column if not exists transfer_party_payment_id bigint;

-- ---------------------------------------------------------------------------
-- RLS — همان Policy واحدِ «فقط مالک»
-- ---------------------------------------------------------------------------
alter table public.party_accounts enable row level security;

drop policy if exists "party_accounts_owner_rows" on public.party_accounts;
create policy "party_accounts_owner_rows" on public.party_accounts
    for all to authenticated
    using (owner_id = auth.uid()::text)
    with check (owner_id = auth.uid()::text);
