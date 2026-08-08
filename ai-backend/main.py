"""
Agri Lanka AI Backend — Sinhala Voice + Crop Disease (Roboflow)

Run (PowerShell):
  cd ai-backend
  python -m venv .venv
  .\\.venv\\Scripts\\Activate.ps1
  pip install -r requirements.txt
  # optional: copy .env.example → .env and set Roboflow keys
  uvicorn main:app --reload --host 0.0.0.0 --port 8000

Docs: http://127.0.0.1:8000/docs
"""

from pathlib import Path

from dotenv import load_dotenv
from fastapi import FastAPI, File, Form, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from disease.roboflow_client import (
    demo_mode_enabled,
    is_configured,
    predict_disease_image,
)
from voice.intent import understand_agriculture
from voice.response_builder import build_response
from voice.speech_to_text import transcribe_audio
from voice.text_to_speech import synthesize_sinhala_base64

load_dotenv(Path(__file__).resolve().parent / ".env")

app = FastAPI(
    title="Agri Lanka AI Backend",
    description="Sinhala Voice Agricultural AI + Crop Disease (Roboflow) + (later) Suitability",
    version="0.2.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": "agri-lanka-ai",
        "modules": ["voice", "disease"],
        "disease": {
            "roboflow_configured": is_configured(),
            "demo_mode": demo_mode_enabled(),
        },
        "next": ["suitability"],
    }


@app.post("/speech-to-text")
async def speech_to_text(audio: UploadFile = File(...)):
    """Upload Sinhala speech audio → transcription."""
    data = await audio.read()
    if not data:
        raise HTTPException(400, "Empty audio file")
    try:
        result = transcribe_audio(data, filename=audio.filename or "audio.m4a")
    except Exception as e:
        raise HTTPException(500, f"Speech-to-text failed: {e}") from e
    return result


@app.post("/voice-assistant")
async def voice_assistant(
    audio: UploadFile | None = File(None),
    text: str | None = Form(None),
    include_tts: bool = Form(True),
):
    """
    Full Sinhala agricultural voice workflow.

    Provide either:
      - audio (multipart file), or
      - text (Sinhala/English form field) for testing without Whisper.

    Returns transcription, intent, crop/symptom slots, Sinhala+English reply,
    and optional TTS audio (base64 mp3) when include_tts=true.
    """
    transcription_si = (text or "").strip()
    transcription_en = ""
    stt_engine = "client_text"

    if audio is not None and audio.filename:
        raw = await audio.read()
        if raw:
            try:
                stt = transcribe_audio(raw, filename=audio.filename)
                transcription_si = stt.get("sinhala_text") or stt.get("text") or transcription_si
                transcription_en = stt.get("english_text") or ""
                stt_engine = stt.get("engine", "whisper")
            except Exception as e:
                if not transcription_si:
                    raise HTTPException(
                        500,
                        f"Speech-to-text failed and no text fallback provided: {e}",
                    ) from e

    if not transcription_si:
        raise HTTPException(
            400,
            "Provide audio file and/or form field 'text' with Sinhala question",
        )

    understanding = understand_agriculture(transcription_si)
    reply = build_response(understanding)

    payload = {
        "transcription_sinhala": transcription_si,
        "transcription_english": transcription_en or understanding.get("english_gloss", ""),
        "intent": understanding["intent"],
        "crop": understanding.get("crop"),
        "crop_sinhala": understanding.get("crop_sinhala"),
        "symptom": understanding.get("symptom"),
        "symptom_sinhala": understanding.get("symptom_sinhala"),
        "needs_photo": understanding.get("needs_photo", False),
        "response_sinhala": reply["sinhala"],
        "response_english": reply["english"],
        "stt_engine": stt_engine,
        "next_action": understanding.get("next_action"),
    }

    if include_tts:
        try:
            payload["tts_audio_base64"] = synthesize_sinhala_base64(reply["sinhala"])
            payload["tts_mime"] = "audio/mpeg"
        except Exception as e:
            payload["tts_audio_base64"] = None
            payload["tts_error"] = str(e)

    return JSONResponse(payload)


@app.post("/voice-assistant/text")
async def voice_assistant_text(body: dict):
    """JSON-only test endpoint: { \"text\": \"මගේ මිරිස් කොළ කහ වෙලා\" }."""
    text = (body.get("text") or "").strip()
    if not text:
        raise HTTPException(400, "JSON body must include non-empty 'text'")
    understanding = understand_agriculture(text)
    reply = build_response(understanding)
    return {
        "transcription_sinhala": text,
        "transcription_english": understanding.get("english_gloss", ""),
        "intent": understanding["intent"],
        "crop": understanding.get("crop"),
        "crop_sinhala": understanding.get("crop_sinhala"),
        "symptom": understanding.get("symptom"),
        "symptom_sinhala": understanding.get("symptom_sinhala"),
        "needs_photo": understanding.get("needs_photo", False),
        "response_sinhala": reply["sinhala"],
        "response_english": reply["english"],
        "next_action": understanding.get("next_action"),
        "stt_engine": "text_only",
    }


@app.post("/predict-disease")
async def predict_disease(
    image: UploadFile = File(...),
    crop: str | None = Form(None),
):
    """
    Crop Doctor: leaf/crop photo → Roboflow disease model → advice.

    Multipart fields:
      - image: JPEG/PNG photo (required)
      - crop: optional crop hint from Voice AI (e.g. tomato, chilli)

    Set ROBOFLOW_API_KEY + ROBOFLOW_MODEL_ID on the server.
    For local UI testing without a key: ROBOFLOW_DEMO_MODE=true
    """
    data = await image.read()
    if not data:
        raise HTTPException(400, "Empty image file")

    # Soft limit ~8 MB to protect free hosts
    if len(data) > 8 * 1024 * 1024:
        raise HTTPException(413, "Image too large (max 8 MB). Compress and retry.")

    try:
        result = await predict_disease_image(
            data,
            filename=image.filename or "crop.jpg",
            crop_hint=(crop or "").strip() or None,
        )
    except ValueError as e:
        raise HTTPException(400, str(e)) from e
    except RuntimeError as e:
        raise HTTPException(503, str(e)) from e
    except Exception as e:
        raise HTTPException(500, f"Disease prediction failed: {e}") from e

    return result
