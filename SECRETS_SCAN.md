# Secrets scan report

Branch: `hackathon/fieldguard`  
Repo: public (`godragun/farming-app`)  
**This report does not print secret values.**

## Inventory (gitleaks `--redact`)

### Working tree (`gitleaks detect --no-git --redact`)

| Type | File | Location |
|------|------|----------|
| OpenWeatherMap key | `app/src/.../model/WeatherApi.kt` | **was current code** → moved to `OPENWEATHER_API_KEY` / BuildConfig |
| APMC / data.gov.in key | `app/src/.../model/APMCApi.kt` | **was current code** → moved to `APMC_API_KEY` / BuildConfig |
| Supabase JWT (role=`anon`) | `app/src/.../supabase/SupabaseConfig.kt` | **was current code** → moved to `SUPABASE_ANON_KEY` / BuildConfig |
| Firebase Android API key | `app/google-services.json` | **local untracked** (removed from index); still in **git history** |
| Roboflow key | `local.properties` | local only (gitignored) |
| Build outputs | `app/build/**` | generated from BuildConfig; not committed |

### Git history (`gitleaks detect --redact`)

| Type | File | Notes |
|------|------|-------|
| Supabase JWT | `SupabaseConfig.kt`, historical `SupabaseUploadHelper.java`, historical `SupabaseUploader.kt` | history |
| OpenWeatherMap | `WeatherApi.kt` | history |
| APMC | `APMCApi.kt` | history |
| Firebase / GCP key | `app/google-services.json` | history (must filter-repo) |

### Supabase JWT role

Decoded payload claim `role` = **`anon`** (not `service_role`). No `service_role` usage found in current source.

## Controls after remediation

- All runtime secrets via `local.properties` → BuildConfig.
- `app/google-services.json` untracked + gitignored; example file provided.
- Pre-commit + GitHub Actions gitleaks.
- History purge: **prepared only** in `HISTORY_CLEANUP.md` — await owner confirmation before force-push.
