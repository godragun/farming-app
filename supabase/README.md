# Supabase setup for the `media` bucket

This folder configures the Storage policies required by the Android upload feature
(`MainActivity` + `SupabaseUploadHelper`).

## What it does

- Enables RLS on `storage.objects` (already default).
- Adds an **INSERT** policy so the `anon` key can upload into the `media` bucket.
- Adds a **SELECT** policy so reads/listing in the `media` bucket work.

## Option A: SQL Editor (no terminal, fastest)

1. Open: Supabase Dashboard → **SQL** → **New query**.
2. Paste the contents of `policies.sql`.
3. Click **Run**.
4. Verify in Storage → Policies → `media` bucket: two policies should now appear.

## Option B: Terminal via `psql`

1. Install `psql` once:
   ```powershell
   winget install -e --id PostgreSQL.PostgreSQL.17
   ```
   (or use Chocolatey: `choco install postgresql`)

2. In Supabase Dashboard → **Project Settings → Database → Connection string (URI)**.
   Copy the URI (it contains your DB password). Do NOT paste it into chat or commit it.

3. In PowerShell (same shell where you'll run the script):
   ```powershell
   $env:SUPABASE_DB_URL = "postgresql://postgres.[ref]:[PASSWORD]@aws-0-....pooler.supabase.com:6543/postgres"
   ```

4. Run:
   ```powershell
   ./apply-policies.ps1
   ```

5. On success, the script prints the two created policies.

## Option C: Supabase CLI (repeatable migrations)

If you already use `supabase` CLI:
```powershell
supabase login
supabase link --project-ref nkjzjbjkcpcbefkypvdl
supabase db execute --file supabase/policies.sql
```

## After applying

- The Android app should be able to upload to `https://nkjzjbjkcpcbefkypvdl.supabase.co/storage/v1/object/media/...`
  using only the anon key.
- If you later want to restrict uploads to logged-in users, change `to anon` to
  `to authenticated` in `policies.sql` and re-run it.
