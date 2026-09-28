-- Nie Wtop cloud history schema
-- Run this only in the separate Supabase project created for Nie Wtop.
create extension if not exists pgcrypto;

create table if not exists public.analyses (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    input_type text not null check (input_type in ('url', 'screenshot')),
    normalized_url text,
    platform text,
    risk_level text not null check (risk_level in ('LOW', 'CAUTION', 'HIGH', 'UNKNOWN')),
    confidence text not null check (confidence in ('LOW', 'MEDIUM', 'HIGH')),
    summary text not null,
    created_at timestamptz not null default now()
);

create index if not exists analyses_user_created_idx
    on public.analyses(user_id, created_at desc);

alter table public.analyses enable row level security;

drop policy if exists "users can read own analyses" on public.analyses;
create policy "users can read own analyses"
on public.analyses
for select
to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists "users can insert own analyses" on public.analyses;
create policy "users can insert own analyses"
on public.analyses
for insert
to authenticated
with check ((select auth.uid()) = user_id);
