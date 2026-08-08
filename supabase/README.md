# Supabase setup for Agri Lanka

This folder configures **Supabase Database + Storage** used by the Android app.

## Project

- Project URL: `https://nkjzjbjkcpcbefkypvdl.supabase.co`
- Client config (anon key only): `app/.../supabase/SupabaseConfig.kt`
- Do **not** put the service-role key in the Android app.

## 1) Apply database schema

1. Open Supabase Dashboard → **SQL** → **New query**
2. Paste `schema.sql`
3. Click **Run**

Creates tables:

| Table | Purpose |
|---|---|
| `users` | Profiles (linked to Supabase Auth) |
| `posts` | Community / field observation posts |
| `products` | E-commerce catalog |
| `article_fruits` | Agriculture articles |
| `surveys` | Field survey sync targets |
| `cart_items` | Shopping cart |
| `orders` | Order history |
| `uploaded_images` | Demo upload records |
| `ai_disease_results` | Future AI Crop Doctor results |

Prototype RLS policies are permissive (`using (true)`). Tighten before production.

## 2) Storage bucket `media`

1. Create a public (or authenticated) Storage bucket named **`media`** if it does not exist.
2. Apply `policies.sql` (anon insert/select for prototype uploads).

## Option A: SQL Editor (fastest)

1. SQL → New query → paste `policies.sql` → Run.
2. Verify Storage → Policies → `media` bucket.

## Option B: Terminal via `psql`

```powershell
$env:SUPABASE_DB_URL = "postgresql://postgres.[ref]:[PASSWORD]@aws-0-....pooler.supabase.com:6543/postgres"
./apply-policies.ps1
```

## Option C: Supabase CLI

```powershell
supabase login
supabase link --project-ref nkjzjbjkcpcbefkypvdl
supabase db execute --file supabase/schema.sql
supabase db execute --file supabase/policies.sql
```

## Android client modules

| File | Role |
|---|---|
| `SupabaseConfig.kt` | URL + anon key |
| `SupabaseSession.kt` | Persisted auth tokens |
| `SupabaseAuth.kt` | Signup / login / Google / reset |
| `SupabaseDb.kt` | PostgREST CRUD |
| `SupabaseHttp.kt` | OkHttp helpers |
| `RemoteDocument.kt` | UI-friendly row wrapper |
| `SupabaseUploader.kt` | Storage uploads |

## Auth notes

- Email/password uses Supabase Auth (`/auth/v1`).
- Google Sign-In: enable **Google** provider in Supabase Auth and exchange the Google ID token.
- Offline demo: login password `guest` enters guest mode without a cloud session.

## After applying

The app can:

- Sign users in via Supabase Auth
- Read/write catalog, posts, surveys, cart, orders
- Upload media to `https://nkjzjbjkcpcbefkypvdl.supabase.co/storage/v1/object/media/...`
