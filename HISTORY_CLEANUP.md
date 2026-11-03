# History cleanup plan (DO NOT RUN until confirmed)

Secrets that remain in **git history** after the code fix must be removed with a history rewrite, then **rotated**. Old forks/clones keep the old blobs forever.

## Warning

- Force-pushing rewrites history for **all** branches and tags.
- Every teammate must **re-clone** (or hard-reset to the new history). Local work based on old SHAs will diverge.
- **Rotating keys is still required** even after filter-repo: anyone who cloned/forked the public repo may still have the old values.
- Do **not** run these steps until the repo owner explicitly confirms.

## 1. Backup (mirror clone)

```bash
# From a parent directory — full mirror including all refs
git clone --mirror https://github.com/godragun/farming-app.git farming-app-backup.git
# Keep this mirror offline until you verify the rewrite.
```

## 2. Prepare replacements file (never commit real secrets)

Create `replacements.txt` **locally** (gitignored). Each line:

```text
literal_old_secret==>REMOVED
```

Include at least (copy values from your private notes / password manager — not from chat):

- Former OpenWeatherMap key from `WeatherApi.kt`
- Former APMC / data.gov.in key from `APMCApi.kt`
- Former Supabase anon JWT from `SupabaseConfig.kt` (and any historical duplicates)
- Any Firebase Android API key strings that appeared inside historical `google-services.json`

Also prepare a path removal for `app/google-services.json`.

## 3. Install git-filter-repo

```bash
pip install git-filter-repo
```

## 4. Rewrite (run only after confirmation)

```bash
cd Farming-App   # or a fresh clone of the public repo

# Replace secret blobs with REMOVED
git filter-repo --replace-text replacements.txt --force

# Drop google-services.json from every commit
git filter-repo --path app/google-services.json --invert-paths --force
```

Re-add the `origin` remote if filter-repo removed it:

```bash
git remote add origin https://github.com/godragun/farming-app.git
```

## 5. Verify

```bash
gitleaks detect --source . --redact
# Expect: no findings for removed keys / google-services.json in history
```

## 6. Force-push (destructive)

```bash
git push --force --all origin
git push --force --tags origin
```

## 7. After rewrite — rotate / restrict

| Secret | Manual action |
|--------|----------------|
| OpenWeatherMap | Regenerate key in OpenWeatherMap account; put new value in `local.properties` as `OPENWEATHER_API_KEY` |
| APMC / data.gov.in | Regenerate API key; set `APMC_API_KEY` |
| Supabase anon JWT | Rotate anon key in Supabase dashboard if exposed; update `SUPABASE_ANON_KEY`. Confirm RLS is enabled. Never ship `service_role`. |
| Firebase / google-services | Restrict Android API key in Google Cloud Console; download a fresh `google-services.json` from Firebase Console → Project settings → Your apps. |
| Roboflow / HF / LLM | Rotate if ever committed or pasted into logs. |
