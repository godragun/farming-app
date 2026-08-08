-- Agri Lanka — Supabase schema (replaces Firebase Auth / Firestore / Realtime DB)
-- Run in Supabase Dashboard → SQL → New query → Run

-- USERS (profile; auth users live in auth.users)
create table if not exists public.users (
  id uuid primary key,
  email text unique not null,
  name text,
  mob_no text,
  city text,
  about text,
  user_type text default 'normal',
  profile_image text default '',
  back_image text default '',
  posts text[] default '{}',
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- Keep camelCase aliases readable from Android via views or JSON columns if needed.
-- Android also sends profileImage/userType; the client maps these to snake_case.

create table if not exists public.posts (
  id uuid primary key default gen_random_uuid(),
  name text,
  title text,
  description text,
  user_id text not null,
  time_stamp bigint,
  upload_type text default '',
  image_url text,
  image_id text,
  created_at timestamptz default now()
);

create index if not exists posts_user_id_idx on public.posts (user_id);
create index if not exists posts_time_stamp_idx on public.posts (time_stamp desc);

create table if not exists public.products (
  id text primary key,
  title text,
  price text,
  retailer text,
  availability text,
  rating double precision default 0,
  image_url jsonb default '[]'::jsonb,
  type text,
  short_desc text,
  long_desc text,
  howtouse text,
  del_charge text,
  attributes jsonb default '{}'::jsonb
);

create table if not exists public.article_fruits (
  id text primary key,
  title text,
  description text,
  process text,
  soil text,
  state text,
  images jsonb default '[]'::jsonb,
  diseases jsonb default '[]'::jsonb,
  attributes jsonb default '{}'::jsonb,
  market_prices jsonb
);

create table if not exists public.surveys (
  remote_id text primary key,
  uid text not null,
  local_id integer,
  farmer_name text,
  location_text text,
  latitude double precision,
  longitude double precision,
  crop_type text,
  soil_condition text,
  irrigation_type text,
  pest_notes text,
  estimated_yield_kg double precision,
  image_url text,
  audio_url text,
  saved_at bigint,
  synced_at bigint,
  created_at timestamptz default now()
);

create index if not exists surveys_uid_idx on public.surveys (uid);

create table if not exists public.cart_items (
  uid text not null,
  product_id text not null,
  quantity integer not null default 1,
  time_ms bigint,
  primary key (uid, product_id)
);

create table if not exists public.orders (
  id uuid primary key default gen_random_uuid(),
  uid text not null,
  product_id text,
  product_name text,
  quantity integer,
  cost text,
  status text,
  address text,
  phone text,
  time_ms bigint,
  raw jsonb default '{}'::jsonb,
  created_at timestamptz default now()
);

create index if not exists orders_uid_idx on public.orders (uid);

create table if not exists public.uploaded_images (
  id uuid primary key default gen_random_uuid(),
  image_url text not null,
  timestamp bigint,
  created_at timestamptz default now()
);

-- AI results (Phase 1+ disease / suitability)
create table if not exists public.ai_disease_results (
  id uuid primary key default gen_random_uuid(),
  uid text,
  image_url text,
  crop text,
  disease text,
  confidence double precision,
  severity text,
  recommendation text,
  created_at timestamptz default now()
);

-- RLS: permissive prototype policies (tighten before production)
alter table public.users enable row level security;
alter table public.posts enable row level security;
alter table public.products enable row level security;
alter table public.article_fruits enable row level security;
alter table public.surveys enable row level security;
alter table public.cart_items enable row level security;
alter table public.orders enable row level security;
alter table public.uploaded_images enable row level security;
alter table public.ai_disease_results enable row level security;

-- Demo-friendly: allow anon/authenticated read/write for Phase 1 prototype.
-- Replace with user-scoped policies for production.
do $$
declare
  t text;
begin
  foreach t in array array[
    'users','posts','products','article_fruits','surveys',
    'cart_items','orders','uploaded_images','ai_disease_results'
  ]
  loop
    execute format('drop policy if exists %I on public.%I', t || '_select_all', t);
    execute format('drop policy if exists %I on public.%I', t || '_insert_all', t);
    execute format('drop policy if exists %I on public.%I', t || '_update_all', t);
    execute format('drop policy if exists %I on public.%I', t || '_delete_all', t);

    execute format(
      'create policy %I on public.%I for select using (true)',
      t || '_select_all', t
    );
    execute format(
      'create policy %I on public.%I for insert with check (true)',
      t || '_insert_all', t
    );
    execute format(
      'create policy %I on public.%I for update using (true)',
      t || '_update_all', t
    );
    execute format(
      'create policy %I on public.%I for delete using (true)',
      t || '_delete_all', t
    );
  end loop;
end $$;
