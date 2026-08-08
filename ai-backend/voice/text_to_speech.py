"""Sinhala text-to-speech → base64 MP3 (gTTS prototype)."""

from __future__ import annotations

import base64
import io


def synthesize_sinhala_base64(text: str) -> str:
    """
    Convert Sinhala text to MP3 bytes (base64).
    Requires internet for gTTS. For offline demos, Android can skip TTS playback.
    """
    if not text or not text.strip():
        raise ValueError("Empty text for TTS")

    try:
        from gtts import gTTS
    except ImportError as e:
        raise RuntimeError("gTTS not installed. pip install gTTS") from e

    buf = io.BytesIO()
    # gTTS uses language code 'si' for Sinhala
    tts = gTTS(text=text.strip(), lang="si")
    tts.write_to_fp(buf)
    return base64.b64encode(buf.getvalue()).decode("ascii")
