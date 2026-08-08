"""Understand Sinhala/English agricultural utterances → intent + slots."""

from __future__ import annotations

from voice.agri_knowledge import CROPS, INTENT_KEYWORDS, SYMPTOMS


def _normalize(text: str) -> str:
    return " ".join(text.strip().lower().split())


def _find_crop(text: str) -> tuple[str | None, str | None]:
    for key, (english, aliases) in CROPS.items():
        for alias in aliases:
            if alias.lower() in text:
                # Prefer Sinhala alias display when matched on Sinhala
                si = next((a for a in aliases if not a.isascii()), english)
                return key, si if any(ord(c) > 127 for c in text) else english
    return None, None


def _find_symptom(text: str) -> tuple[str | None, str | None]:
    for key, (english, aliases) in SYMPTOMS.items():
        for alias in aliases:
            if alias.lower() in text:
                si = next((a for a in aliases if not a.isascii()), english)
                return key, si
    return None, None


def _find_intent(text: str, crop: str | None, symptom: str | None) -> str:
    scores: dict[str, int] = {}
    for intent, words in INTENT_KEYWORDS.items():
        score = sum(1 for w in words if w.lower() in text)
        if score:
            scores[intent] = score

    if symptom and "disease_help" not in scores:
        scores["disease_help"] = scores.get("disease_help", 0) + 2
    if crop and symptom:
        scores["disease_help"] = scores.get("disease_help", 0) + 2

    if not scores:
        return "general_farming"
    return max(scores.items(), key=lambda kv: kv[1])[0]


def _english_gloss(text: str, crop: str | None, symptom: str | None, intent: str) -> str:
    parts = []
    if crop:
        parts.append(f"crop={CROPS[crop][0]}")
    if symptom:
        parts.append(f"symptom={SYMPTOMS[symptom][0]}")
    parts.append(f"intent={intent}")
    return "; ".join(parts) if parts else text[:120]


def understand_agriculture(raw_text: str) -> dict:
    text = _normalize(raw_text)
    crop, crop_si = _find_crop(text)
    symptom, symptom_si = _find_symptom(text)
    intent = _find_intent(text, crop, symptom)

    needs_photo = intent == "disease_help" and (symptom is not None or "කොළ" in text or "leaf" in text)
    next_action = None
    if needs_photo:
        next_action = "request_leaf_photo"
    elif intent == "suitability":
        next_action = "open_crop_suitability"
    elif intent == "weather":
        next_action = "open_weather"

    return {
        "intent": intent,
        "crop": crop,
        "crop_sinhala": crop_si,
        "crop_english": CROPS[crop][0] if crop else None,
        "symptom": symptom,
        "symptom_sinhala": symptom_si,
        "symptom_english": SYMPTOMS[symptom][0] if symptom else None,
        "needs_photo": needs_photo,
        "next_action": next_action,
        "english_gloss": _english_gloss(text, crop, symptom, intent),
        "raw_text": raw_text,
    }
