# Agri Lanka — AI-Powered Farming Companion

<p align="center">
  <img src="output%20images%20of%20app/front%20page%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%20%202%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/sigingin%20%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/signing%20up%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/navigation%20panel%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/light%20mode%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/weather%20forcasting%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/ecommerce%20%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/product%20description%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/check%20out%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/Aricales%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/details%20of%20product%20and%20market%20prices.png" width="200" />
  <img src="output%20images%20of%20app/socila%20media%20post%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/survare%20for%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/saved%20survay%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/front%20statite%20view.png" width="200" />
  <img src="output%20images%20of%20app/satalite%20view%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/Agri%20Chatbot.png" width="200" />
  <img src="output%20images%20of%20app/login%20page%202.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%203%20.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%204.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%205.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%206.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%207.png" width="200" />
</p>

**Agri Lanka** is an Android app for Sri Lankan farmers and field workers. It combines everyday farming tools (weather, surveys, map, market info) with a growing **AI decision-support** layer.

Built for **AI Challenge Sri Lanka 2026 – AI Buildathon** direction:

1. What is wrong with my crop? *(disease AI — planned next)*  
2. Can I talk to the system in Sinhala? *(voice AI — started)*  
3. Where is the best place to grow this crop? *(suitability AI — planned)*

The app stays usable offline for core fieldwork. AI features call a **Python FastAPI** server (laptop during development, cloud later). The APK does **not** run ML models inside the phone.

---

## Table of Contents

- [What is built now](#what-is-built-now)
- [What the app can do](#what-the-app-can-do)
- [How AI connects to the APK](#how-ai-connects-to-the-apk)
- [Architecture & tech stack](#architecture--tech-stack)
- [Sinhala Voice AI (current)](#sinhala-voice-ai-current)
- [Supabase backend](#supabase-backend)
- [Running the Android app](#running-the-android-app)
- [Running the AI backend](#running-the-ai-backend)
- [Project structure](#project-structure)
- [Roadmap](#roadmap)
- [Known notes](#known-notes)

---

## What is built now

### Android app (Kotlin, MVVM)

| Area | Status |
|------|--------|
| Login / Signup / Guest mode | Done (Supabase Auth; guest password `guest`) |
| English + Sinhala UI strings | Done (`values` / `values-si`, Settings toggle) |
| Dashboard + weather (GPS / OpenWeatherMap) | Done |
| Satellite map (Leaflet WebView) | Done |
| Field surveys (photo + voice notes, offline Room → sync) | Done |
| Community posts, articles, e-commerce / cart / orders | Done |
| Rule-based Agri Chatbot | Done (not ML) |
| **Sinhala Voice AI screen** | Done (talks to FastAPI) |
| **AI Crop Doctor (disease photo)** | Done (camera → FastAPI → Roboflow; Voice handoff) |
| AI Crop Suitability + map zones | Not yet |

### Python AI backend (separate private repo)

**Canonical source (host / Render):**  
Local folder: `C:\Users\VICTUS\Desktop\farming app backend`  
GitHub (private): https://github.com/godragun/farming-app-backend  

AI Python code lives **only** in that repo — not inside this Android project.

| Module | Status |
|--------|--------|
| FastAPI server (`/health`, `/voice-assistant`, `/speech-to-text`) | Done |
| Agricultural Sinhala/English intent (crops, symptoms, fertilizer, weather…) | Done |
| Text path for testing without Whisper | Done |
| Optional Whisper STT + gTTS Sinhala speech | Scaffolded (Whisper optional install) |
| Disease detection API (`POST /predict-disease` via Roboflow) | Done (set keys in `.env`; demo mode available) |
| Crop suitability API | Not yet |

### Cloud data

| Service | Status |
|---------|--------|
| Supabase Auth + Database + Storage (`media`) | Done |
| SQL schema (`supabase/schema.sql`) including `surveys`, `ai_disease_results` | Done |
| Firebase Auth/Firestore | Removed from app code |

---

## What the app can do

After install / run, a user can:

1. **Sign in** with email/password (Supabase), Google (if configured), or **guest** (password `guest`).
2. Switch **English / Sinhala** UI and **dark / light** mode in Settings.
3. See **live weather** for their GPS location (Colombo fallback).
4. Open a **satellite map** (pan/zoom Leaflet).
5. Create **field surveys** with farmer details, crop, soil, irrigation, GPS, **camera photo**, and **voice note** — saved offline in Room and synced to Supabase when online.
6. Browse **crop articles**, **market/APMC** context, and an **input catalog** (e-commerce) with cart/orders (offline demo products if cloud empty).
7. Use the **community feed** to post field observations (images via Supabase Storage).
8. Use the floating / drawer **Agri Chatbot** for simple rule-based farming tips.
9. Open drawer → **Sinhala Voice AI**: type (or record) a farming question in Sinhala/English → get intent, crop/symptom slots, and a Sinhala reply from the Python server. Disease-style questions set `needs_photo` (ready to connect Crop Doctor later).

---

## How AI connects to the APK

```
Phone (Agri Lanka APK)
   camera / mic / typed Sinhala / GPS
           │  HTTP JSON
           ▼
Python FastAPI  (farming-app-backend — private repo)
   voice  │  disease (Roboflow)  │  suitability (soon)
           │
           ▼
      JSON result → Android UI

Optional: Supabase for login, surveys, media storage, saving AI results
```

- Building an APK does **not** put Whisper/YOLO inside the phone.
- During development: run FastAPI on your laptop; phone/emulator calls that URL.
- Later: deploy FastAPI to a cloud host (Render / Railway / Fly.io — better than Vercel for heavy ML) and set `AgriAiConfig.BASE_URL` to the HTTPS URL.

| Client | Typical AI base URL |
|--------|---------------------|
| Android emulator | `http://10.0.2.2:8000/` |
| Real phone (same Wi‑Fi as PC) | `http://YOUR_PC_LAN_IP:8000/` |
| Production | `https://your-deployed-api.../` |

Config file: `app/src/main/java/com/project/farmingapp/model/ai/AgriAiApi.kt` → `AgriAiConfig.BASE_URL`.

---

## Architecture & tech stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI | XML + ViewBinding / DataBinding |
| Architecture | MVVM (ViewModel + LiveData), single `DashboardActivity` + Fragments |
| Auth / DB / Storage | Supabase |
| Local DB | Room (field surveys) |
| Networking | Retrofit + OkHttp |
| Weather | OpenWeatherMap |
| Map | Leaflet in WebView (`leaflet_map.html` + `LeafletMapWebViewHelper`) |
| AI server | Python FastAPI ([farming-app-backend](https://github.com/godragun/farming-app-backend) private repo) |
| Min / Target SDK | 23 / 34 |

---

## Sinhala Voice AI (current)

**Android**

- Drawer item: **Sinhala Voice AI**
- `SinhalaVoiceFragment` + `VoiceAiViewModel`
- Type question or record audio → FastAPI → show Sinhala + English reply, intent/crop/symptom

**Backend**

```powershell
cd "C:\Users\VICTUS\Desktop\farming app backend"
.\.venv\Scripts\Activate.ps1
uvicorn main:app --host 0.0.0.0 --port 8000
```

Docs: http://127.0.0.1:8000/docs  

Quick test (no mic needed):

```powershell
curl -X POST "http://127.0.0.1:8000/voice-assistant/text" -H "Content-Type: application/json" -d "{\"text\": \"මගේ මිරිස් ගස් වල කොළ කහ වෙලා. මොකක්ද කරන්න ඕන?\"}"
```

Expected: `intent=disease_help`, `crop=chilli`, `needs_photo=true`.

More detail: private repo [farming-app-backend](https://github.com/godragun/farming-app-backend).

---

## Supabase backend

1. Run `supabase/schema.sql` in the Supabase SQL editor.  
2. Ensure Storage bucket `media` exists; apply `supabase/policies.sql`.  
3. Android client: `app/.../supabase/` (`SupabaseConfig`, Auth, Db, Session, Uploader).

Do **not** put the service-role key in the Android app (anon key only).

---

## Running the Android app

1. Open the project in Android Studio.  
2. Sync Gradle.  
3. Use an **API 33/34** emulator or a physical device.  
4. For Sinhala Voice AI / Crop Doctor: start the **standalone** backend first (`farming app backend` folder / private GitHub repo).  
5. Emulator uses `http://10.0.2.2:8000/` by default.  
6. Guest login: any email + password `guest`.

Cleartext HTTP is enabled for local AI testing (`usesCleartextTraffic` in the manifest). Use HTTPS in production.

---

## Running the AI backend

**Use the separate private repo** (recommended for Render):

Folder: `C:\Users\VICTUS\Desktop\farming app backend`  
GitHub: https://github.com/godragun/farming-app-backend  

```powershell
cd "C:\Users\VICTUS\Desktop\farming app backend"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

Optional real speech (Whisper):

```powershell
pip install openai-whisper torch
```

---

## Project structure (high level)

```
Farming-App/                    # Android Studio APK (this repo)
├── app/src/main/java/com/project/farmingapp/
│   ├── view/           # Fragments & Activities (dashboard, survey, ai, …)
│   ├── viewmodel/
│   ├── model/          # Repositories, WeatherApi, ai/AgriAiApi
│   ├── supabase/       # Supabase Auth/DB/HTTP helpers
│   └── utilities/      # AuthHelper, LeafletMapWebViewHelper, …
├── supabase/           # schema.sql, policies.sql
└── README.md

# Separate private repo (AI hosting):
# C:\Users\VICTUS\Desktop\farming app backend
# → https://github.com/godragun/farming-app-backend
```

---

## Roadmap

| Step | Goal |
|------|------|
| ✅ | App features + Supabase + Sinhala UI |
| ✅ | Sinhala Voice AI backend + Android screen |
| 🔜 | Crop disease model + `/predict-disease` + Crop Doctor screen |
| 🔜 | Connect voice `needs_photo` → camera → disease result |
| 🔜 | Crop suitability ML + map colour zones |
| 🔜 | Deploy FastAPI to cloud; point APK at HTTPS URL |

---

## Known notes

- Prices use **LKR**; default map/weather location is **Colombo** if GPS is unavailable.  
- Surveys stay on-device until sync; AI needs the FastAPI server reachable.  
- Prefer API 33/34 emulators over API 36 for this project.  
- Google Sign-In needs a valid OAuth web client id + Supabase Google provider.  
- Heavy ML is better hosted on Render/Railway/Fly.io than Vercel serverless.

---

## Licence / credit

Academic / competition project by [godragun](https://github.com/godragun). Earlier open-source farming apps were studied for MVVM/UI patterns; current Supabase + AI backend work is project-specific.
