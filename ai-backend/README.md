# Agri Lanka AI Backend — Language first (Sinhala Voice)

This folder is the **Python FastAPI** server. Android does **not** run Whisper here.

## What works now

| Endpoint | Purpose |
|----------|---------|
| `GET /health` | Server alive check |
| `POST /voice-assistant/text` | Test Sinhala agricultural AI with JSON text (no Whisper needed) |
| `POST /voice-assistant` | Audio and/or `text` form field → intent + Sinhala reply + optional TTS |
| `POST /speech-to-text` | Audio → Whisper transcription (needs `openai-whisper`) |

Agricultural understanding covers crops (මිරිස්, තක්කාලි, වී…), symptoms (කොළ කහ…), and intents (disease, fertilizer, weather, suitability…).

If intent is crop disease + leaf symptom → `needs_photo: true` and `next_action: request_leaf_photo` (ready to connect Crop Doctor later).

## Setup (Windows PowerShell)

```powershell
cd "C:\Users\VICTUS\Desktop\farming app\Farming-App\ai-backend"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

Open: http://127.0.0.1:8000/docs

## Test without microphone (recommended first)

```powershell
curl -X POST "http://127.0.0.1:8000/voice-assistant/text" `
  -H "Content-Type: application/json" `
  -d "{\"text\": \"මගේ මිරිස් ගස් වල කොළ කහ වෙලා. මොකක්ද කරන්න ඕන?\"}"
```

Expected JSON fields: `intent=disease_help`, `crop=chilli`, `needs_photo=true`, Sinhala `response_sinhala`.

## Optional: real speech (Whisper)

```powershell
pip install openai-whisper torch
$env:WHISPER_MODEL = "base"
```

Then upload audio to `/speech-to-text` or `/voice-assistant`.

## Android → this server

| Device | Base URL |
|--------|----------|
| Emulator | `http://10.0.2.2:8000/` |
| Phone on same Wi‑Fi | `http://YOUR_PC_LAN_IP:8000/` |

Laptop Python is **not** on the public internet until you deploy later.

## Crop Doctor — Roboflow disease API

| Endpoint | Purpose |
|----------|---------|
| `POST /predict-disease` | Multipart `image` (+ optional `crop` hint) → disease JSON |

### Configure Roboflow (server only)

1. Train/deploy your model in Roboflow.
2. Copy `ai-backend/.env.example` → `ai-backend/.env`
3. Set:

```
ROBOFLOW_API_KEY=rf_...
ROBOFLOW_MODEL_ID=your-workspace/your-model
ROBOFLOW_MODEL_VERSION=1
ROBOFLOW_TASK=detect
# or: classify
ROBOFLOW_DEMO_MODE=false
```

4. Restart uvicorn. Check `GET /health` → `disease.roboflow_configured: true`.

**Never put the Roboflow API key in the Android app.** Android only calls this FastAPI server.

### Local UI testing without a trained model

```
ROBOFLOW_DEMO_MODE=true
```

Returns a stub Early Blight result so Crop Doctor + Voice handoff can be demoed.

### Example curl

```powershell
curl -X POST "http://127.0.0.1:8000/predict-disease" `
  -F "image=@leaf.jpg" `
  -F "crop=tomato"
```

## Later modules

- `suitability/` — crop suitability ML (Supabase survey data)
