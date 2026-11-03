# Secrets scan report (pre-public push)

Date: 2026-11-03  
Branch: `hackathon/fieldguard`  
Repo: https://github.com/godragun/farming-app (public)

**This report does not print secret values.**

## Controls for FieldGuard

- `local.properties` is gitignored and **not** tracked.
- FieldGuard keys (`LLM_API_KEY`, `LLM_BASE_URL`, `LLM_MODEL`) come from `local.properties` → BuildConfig only.
- `local.properties.example` ships with `LLM_API_KEY=` empty.
- `app/google-services.json` remains gitignored.
- `app/build/` generated BuildConfig copies are not committed.

## Tools

1. Confirm `local.properties` ignored / untracked.
2. Working-tree checks for FieldGuard key literals.
3. `git log -S` heuristics for new LLM key material (none found).
4. **gitleaks** `detect --source . --no-git` (portable binary).

## Gitleaks summary (paths only)

| Severity for this push | Path | Rule | Notes |
|------------------------|------|------|-------|
| Blocked from commit | `local.properties` | generic-api-key | Gitignored; contains local Roboflow key — **do not commit** |
| Not committed | `app/build/**` | generic-api-key / jwt | Build outputs; ignored |
| Not committed | `app/google-services.json` | gcp-api-key | Already gitignored |
| Pre-existing (prior Agri Lanka) | `app/src/main/java/.../WeatherApi.kt` | generic-api-key | Hardcoded weather key predates FieldGuard |
| Pre-existing | `app/src/main/java/.../APMCApi.kt` | generic-api-key | Predates FieldGuard |
| Pre-existing | `app/src/main/java/.../supabase/SupabaseConfig.kt` | jwt | Supabase anon JWT in source (client-style); predates FieldGuard |

## Git history scan (`gitleaks detect` on branch)

Scanned 26 commits. Findings are **pre-existing** (not introduced by FieldGuard commits):

| Rule | File | Introduced in |
|------|------|----------------|
| jwt | `supabase/SupabaseConfig.kt` | older commit `8fb7619` |
| gcp-api-key | `app/google-services.json` (later gitignored) | older commit `ab4a7bd` |
| jwt | `SupabaseUploadHelper.java` | older commit `ab4a7bd` |
| generic-api-key | `APMCApi.kt` | older commit `ab4a7bd` |
| generic-api-key | `WeatherApi.kt` | older commit `ab4a7bd` |

## FieldGuard-specific result

- **No `LLM_API_KEY` (or Meta key) in tracked source or FieldGuard commits.**
- Hackathon commits do not add new secrets.

## Recommendations before / after public push

1. Keep real `LLM_API_KEY` only in local `local.properties`.
2. Rotate the local Roboflow key if it was ever pasted into chat/logs.
3. Consider moving pre-existing Weather/APMC keys to BuildConfig + `local.properties` in a follow-up (out of FieldGuard scope).
4. Supabase anon keys are often client-public; still enforce RLS on the project.
