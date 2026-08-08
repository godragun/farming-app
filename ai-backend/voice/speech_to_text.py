"""Sinhala / multilingual speech-to-text.

Uses OpenAI Whisper when installed. Otherwise raises a clear error so clients
can fall back to the text form field on /voice-assistant.
"""

from __future__ import annotations

import os
import tempfile
from pathlib import Path

_whisper_model = None


def _load_whisper():
    global _whisper_model
    if _whisper_model is not None:
        return _whisper_model
    try:
        import whisper  # type: ignore
    except ImportError as e:
        raise RuntimeError(
            "openai-whisper is not installed. "
            "pip install openai-whisper torch  OR send form field 'text' instead of audio."
        ) from e

    model_name = os.environ.get("WHISPER_MODEL", "base")
    _whisper_model = whisper.load_model(model_name)
    return _whisper_model


def transcribe_audio(data: bytes, filename: str = "audio.m4a") -> dict:
    """
    Returns:
      {
        "sinhala_text": "...",
        "english_text": "",
        "text": "...",
        "engine": "whisper:base",
        "language": "si"
      }
    """
    suffix = Path(filename).suffix or ".m4a"
    model = _load_whisper()

    with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
        tmp.write(data)
        tmp_path = tmp.name

    try:
        # language="si" biases toward Sinhala; Whisper still auto-detects if unsure
        result = model.transcribe(tmp_path, language="si", task="transcribe")
        text = (result.get("text") or "").strip()
        lang = result.get("language") or "si"
        model_name = os.environ.get("WHISPER_MODEL", "base")
        return {
            "sinhala_text": text,
            "english_text": "",
            "text": text,
            "engine": f"whisper:{model_name}",
            "language": lang,
        }
    finally:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass
