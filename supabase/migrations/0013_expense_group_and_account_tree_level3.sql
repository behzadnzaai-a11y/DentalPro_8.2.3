-- ============================================================================
-- DentalPro — Migration 0013: هابِ «هزینه‌های مطب» + سطحِ سومِ درختِ حساب‌ها
-- ============================================================================
-- ۱) clinic_expenses.expense_group — گروهِ اصلیِ هزینه در هابِ چهارکارته:
--    LAB / CONSUMABLES / EQUIPMENT / RUNNING / OTHER (خالی = دسته‌بندی‌نشده).
-- ۲) personal_sub_accounts.grand_parent_name — سطحِ سومِ درخت (سرشاخهٔ ریشه)،
--    برای ساختارِ سه‌سطحیِ «سرشاخه ▸ شاخه ▸ حساب».
-- هم‌نگاشتِ camelCase↔snake_case سمتِ کلاینت: expenseGroup↔expense_group،
-- grandParentName↔grand_parent_name.
-- ============================================================================

alter table public.clinic_expenses
    add column if not exists expense_group text not null default '';

create index if not exists idx_clinic_expenses_group
    on public.clinic_expenses (owner_id, expense_group);

alter table public.personal_sub_accounts
    add column if not exists grand_parent_name text not null default '';

create index if not exists idx_personal_sub_accounts_grandparent
    on public.personal_sub_accounts (owner_id, grand_parent_name);
