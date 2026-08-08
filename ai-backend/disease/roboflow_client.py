"""Call Roboflow Hosted Inference (detect or classify).

Env vars (server-side only — never ship to Android):
  ROBOFLOW_API_KEY      required for live inference
  ROBOFLOW_MODEL_ID     e.g. plant-disease-xxxx/1  OR just the model slug
  ROBOFLOW_MODEL_VERSION  default 1 (ignored if MODEL_ID already contains /version)
  ROBOFLOW_TASK         detect | classify  (default: detect)
  ROBOFLOW_CONFIDENCE   0-100 (default 40)
  ROBOFLOW_DEMO_MODE    true → return stub result when key missing (local UI testing)
"""

from __future__ import annotations

import os
from typing import Any

import httpx

from disease.advice import advice_for, extract_crop_hint


def _env(name: str, default: str = "") -> str:
    return (os.environ.get(name) or default).strip()


def _model_path() -> tuple[str, str]:
    """Return (model_id, version) for URL path."""
    model = _env("ROBOFLOW_MODEL_ID", "YOUR_MODEL")
    version = _env("ROBOFLOW_MODEL_VERSION", "1")
    if "/" in model:
        parts = model.rsplit("/", 1)
        if parts[-1].isdigit():
            return parts[0], parts[1]
    return model, version


def is_configured() -> bool:
    key = _env("ROBOFLOW_API_KEY")
    model = _env("ROBOFLOW_MODEL_ID")
    return bool(key) and bool(model) and model != "YOUR_MODEL"


def demo_mode_enabled() -> bool:
    return _env("ROBOFLOW_DEMO_MODE", "false").lower() in ("1", "true", "yes")


def _base_url(task: str, model_id: str, version: str) -> str:
    host = "classify.roboflow.com" if task == "classify" else "detect.roboflow.com"
    return f"https://{host}/{model_id}/{version}"


def _pick_top_prediction(payload: dict[str, Any]) -> tuple[str, float, list[dict]]:
    """Normalize Roboflow detect/classify JSON → (class, confidence, all)."""
    preds = payload.get("predictions") or []
    if not isinstance(preds, list):
        preds = []

    # Classification sometimes uses top-level fields
    top_class = payload.get("top") or payload.get("predicted_classes")
    top_conf = payload.get("confidence")

    normalized: list[dict] = []
    for p in preds:
        if not isinstance(p, dict):
            continue
        cls = p.get("class") or p.get("class_name") or p.get("prediction") or ""
        conf = p.get("confidence")
        if conf is None:
            conf = p.get("confidence_score", 0)
        try:
            conf_f = float(conf)
        except (TypeError, ValueError):
            conf_f = 0.0
        # Roboflow may return 0-100 or 0-1
        if conf_f > 1.0:
            conf_f = conf_f / 100.0
        normalized.append({"class": str(cls), "confidence": round(conf_f, 4)})

    if normalized:
        best = max(normalized, key=lambda x: x["confidence"])
        return best["class"], best["confidence"], normalized

    if isinstance(top_class, list) and top_class:
        conf_f = float(top_conf or 0)
        if conf_f > 1.0:
            conf_f = conf_f / 100.0
        return str(top_class[0]), round(conf_f, 4), [{"class": str(top_class[0]), "confidence": round(conf_f, 4)}]

    if isinstance(top_class, str) and top_class:
        conf_f = float(top_conf or 0)
        if conf_f > 1.0:
            conf_f = conf_f / 100.0
        return top_class, round(conf_f, 4), [{"class": top_class, "confidence": round(conf_f, 4)}]

    return "Unknown", 0.0, []


def _build_result(
    *,
    label: str,
    confidence: float,
    crop_hint: str | None,
    engine: str,
    raw_predictions: list[dict],
) -> dict[str, Any]:
    tip = advice_for(label)
    crop = extract_crop_hint(label, crop_hint) or crop_hint or "Unknown"
    return {
        "crop": crop,
        "disease": tip["disease"] if tip["advice_key"] != "unknown" or label == "Unknown" else (
            label.replace("_", " ").title()
        ),
        "raw_label": label,
        "confidence": confidence,
        "severity": tip["severity"],
        "recommendation": tip["recommendation"],
        "prevention": tip["prevention"],
        "recommendation_sinhala": tip["recommendation_sinhala"],
        "prevention_sinhala": tip["prevention_sinhala"],
        "engine": engine,
        "predictions": raw_predictions,
    }


def demo_prediction(crop_hint: str | None = None) -> dict[str, Any]:
    """Local UI testing without a Roboflow key."""
    return _build_result(
        label="Early Blight",
        confidence=0.91,
        crop_hint=crop_hint or "Tomato",
        engine="demo",
        raw_predictions=[{"class": "Early Blight", "confidence": 0.91}],
    )


async def predict_disease_image(
    image_bytes: bytes,
    filename: str = "crop.jpg",
    crop_hint: str | None = None,
) -> dict[str, Any]:
    if not image_bytes:
        raise ValueError("Empty image")

    if not is_configured():
        if demo_mode_enabled():
            return demo_prediction(crop_hint)
        raise RuntimeError(
            "Roboflow is not configured. Set ROBOFLOW_API_KEY and ROBOFLOW_MODEL_ID "
            "in the server environment, or set ROBOFLOW_DEMO_MODE=true for local UI tests."
        )

    model_id, version = _model_path()
    task = _env("ROBOFLOW_TASK", "detect").lower()
    if task not in ("detect", "classify"):
        task = "detect"
    confidence_thresh = _env("ROBOFLOW_CONFIDENCE", "40")
    api_key = _env("ROBOFLOW_API_KEY")

    url = _base_url(task, model_id, version)
    params = {"api_key": api_key, "confidence": confidence_thresh}

    # Roboflow accepts multipart file upload
    mime = "image/jpeg"
    lower = filename.lower()
    if lower.endswith(".png"):
        mime = "image/png"
    elif lower.endswith(".webp"):
        mime = "image/webp"

    async with httpx.AsyncClient(timeout=60.0) as client:
        response = await client.post(
            url,
            params=params,
            files={"file": (filename, image_bytes, mime)},
        )

    if response.status_code >= 400:
        raise RuntimeError(
            f"Roboflow API error {response.status_code}: {response.text[:400]}"
        )

    payload = response.json()
    label, conf, all_preds = _pick_top_prediction(payload)
    return _build_result(
        label=label,
        confidence=conf,
        crop_hint=crop_hint,
        engine=f"roboflow:{task}:{model_id}/{version}",
        raw_predictions=all_preds,
    )
