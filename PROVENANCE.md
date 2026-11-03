# Provenance — FieldGuard AI (Meta Global AI Developer Hackathon)

## (a) Prior work before Oct 26, 2026 — Agri Lanka app

The existing **Agri Lanka** Android application (`com.project.farmingapp`) predates this hackathon entry. It already included:

- Accounts / auth (Supabase + guest mode)
- Leaf scanner (Roboflow)
- Sinhala voice chatbot / voice AI screens
- Field surveys (Room + offline sync)
- Weather + satellite/map (Leaflet)
- Crop articles
- Marketplace / e-commerce demo
- Community social posts
- AI Crop Doctor
- Shared theme, localization (`values` / `values-si`), dashboard shell

None of that prior product surface was rewritten for this submission.

## (b) Built during the hackathon — FieldGuard AI

Everything under:

- `app/src/main/java/com/project/farmingapp/fieldguard/`
- `app/src/main/res/layout/fragment_fieldguard_*.xml`
- `app/src/main/res/layout/item_fieldguard_*.xml`
- `app/src/main/res/drawable/fieldguard_*`
- `app/src/main/res/xml/fieldguard_network_security_config.xml`
- `app/src/main/assets/fieldguard/`
- `app/src/test/java/com/project/farmingapp/fieldguard/`
- `ASSETS.md`, `LIMITATIONS.md`, this `PROVENANCE.md`
- `local.properties.example` (LLM_* placeholders)

### Existing files touched (integration only)

| File | Change |
|------|--------|
| `app/build.gradle` | `LLM_*` BuildConfig fields + WorkManager dependency |
| `app/src/main/AndroidManifest.xml` | `POST_NOTIFICATIONS`, vibrate, networkSecurityConfig |
| `app/src/main/java/.../DashboardActivity.kt` | Drawer route → FieldGuard home |
| `app/src/main/java/.../DashboardFragment.kt` | Home card click → FieldGuard |
| `app/src/main/res/layout/fragment_dashboard.xml` | One FieldGuard entry card |
| `app/src/main/res/menu/nav_drawer_menu.xml` | One FieldGuard nav item |
| `app/src/main/res/values/strings.xml` | FieldGuard strings (EN) |
| `app/src/main/res/values-si/strings.xml` | FieldGuard strings (SI) |
| `app/src/main/res/values-ta/strings.xml` | New Tamil resource file for FieldGuard strings |
| `README.md` | FieldGuard AI (Meta Hackathon) section |

Branch: `hackathon/fieldguard`
