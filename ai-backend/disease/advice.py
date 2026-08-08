"""Map Roboflow class labels → farmer advice (EN + SI)."""

from __future__ import annotations

import re

# Normalized key → (english_name, severity, recommendation_en, prevention_en, recommendation_si, prevention_si)
_ADVICE: dict[str, tuple[str, str, str, str, str, str]] = {
    "healthy": (
        "Healthy",
        "None",
        "No disease detected. Keep monitoring the crop weekly.",
        "Maintain good airflow, balanced fertilizer, and avoid overwatering leaves.",
        "රෝගයක් පෙනෙන්නේ නැහැ. සතියකට වරක් පරීක්ෂා කරන්න.",
        "හොඳ වාතාශ්‍රය, සමබර පොහොර සහ කොළ අධිකව තෙත් නොකිරීම පවත්වන්න.",
    ),
    "early_blight": (
        "Early Blight",
        "Moderate",
        "Remove badly spotted leaves. Apply a copper or recommended fungicide. Avoid wetting foliage when watering.",
        "Crop rotation, mulch soil, and space plants for airflow.",
        "බරපතල ලප සහිත කොළ ඉවත් කරන්න. නිර්දේශිත දිලීරනාශකයක් යොදන්න. වතුර දමන විට කොළ තෙත් නොකරන්න.",
        "භෝග මාරුව, පස ආවරණය සහ හොඳ ඉඩක් තබන්න.",
    ),
    "late_blight": (
        "Late Blight",
        "High",
        "Act quickly: remove infected parts, improve airflow, and use a late-blight fungicide if available. Isolate badly infected plants.",
        "Avoid overhead irrigation in humid weather; destroy crop debris after harvest.",
        "ඉක්මනින් ක්‍රියා කරන්න: ආසාදිත කොටස් ඉවත් කර, වාතාශ්‍රය වැඩි කර, නිර්දේශිත ඖෂධ යොදන්න.",
        "තෙත් කාලගුණයේ කොළ මත වතුර ඉසීම අඩු කරන්න; අස්වනුවෙන් පසු අපද්‍රව්‍ය විනාශ කරන්න.",
    ),
    "leaf_spot": (
        "Leaf Spot",
        "Moderate",
        "Remove spotted leaves and apply a suitable fungicide. Water at the base of the plant.",
        "Do not reuse infected tools without cleaning; avoid dense planting.",
        "ලප සහිත කොළ ඉවත් කර නිර්දේශිත ඖෂධ යොදන්න. මුල අසලට වතුර දමන්න.",
        "ආසාදිත මෙවලම් පිරිසිදු කරන්න; අධික ඝනත්වයෙන් නොසිටුවන්න.",
    ),
    "leaf_curl": (
        "Leaf Curl",
        "Moderate",
        "Control whiteflies/aphids that spread viruses. Remove severely curled shoots. Avoid excess nitrogen.",
        "Use sticky traps and reflective mulch; buy clean seedlings.",
        "වෛරස් ව්‍යාප්ත කරන මකුණන් පාලනය කරන්න. බරපතලව නැමුණු අතු ඉවත් කරන්න.",
        "ඇලෙන උගුල් භාවිතා කර පිරිසිදු පැළ මිලදී ගන්න.",
    ),
    "bacterial_spot": (
        "Bacterial Spot",
        "Moderate",
        "Remove infected leaves. Use copper sprays where recommended. Avoid working plants when wet.",
        "Use disease-free seed; rotate crops; sanitize tools.",
        "ආසාදිත කොළ ඉවත් කර නිර්දේශිත තඹ ඖෂධ යොදන්න. තෙත් විට පැළ මත වැඩ නොකරන්න.",
        "නිරෝගී බීජ භාවිතා කර භෝග මාරු කරන්න.",
    ),
    "powdery_mildew": (
        "Powdery Mildew",
        "Low",
        "Improve airflow and apply sulfur or recommended mildew spray. Avoid excess shade.",
        "Do not overcrowd plants; water early in the day.",
        "වාතාශ්‍රය වැඩි කර නිර්දේශිත දිලීරනාශකයක් යොදන්න.",
        "අධික ඝනත්වයෙන් නොසිටුවන්න; උදේ වතුර දමන්න.",
    ),
    "anthracnose": (
        "Anthracnose",
        "Moderate",
        "Remove infected fruits/leaves. Apply recommended fungicide. Harvest carefully to avoid wounds.",
        "Rotate crops and destroy infected debris.",
        "ආසාදිත ඵල/කොළ ඉවත් කර නිර්දේශිත ඖෂධ යොදන්න.",
        "භෝග මාරු කර ආසාදිත අපද්‍රව්‍ය විනාශ කරන්න.",
    ),
    "mosaic_virus": (
        "Mosaic Virus",
        "High",
        "There is no chemical cure. Remove infected plants to protect neighbors. Control aphids.",
        "Use virus-free seedlings; control insect vectors.",
        "රසායනික සුවයක් නැත. ආසාදිත පැළ ඉවත් කර අසල්වැසියන් ආරක්ෂා කරන්න. මකුණන් පාලනය කරන්න.",
        "නිරෝගී පැළ භාවිතා කර කෘමි වාහකයන් පාලනය කරන්න.",
    ),
    "blight": (
        "Blight",
        "Moderate",
        "Remove infected tissue, improve airflow, and apply a suitable fungicide promptly.",
        "Avoid overhead watering in humid weather; rotate crops.",
        "ආසාදිත කොටස් ඉවත් කර වාතාශ්‍රය වැඩි කර නිර්දේශිත ඖෂධ යොදන්න.",
        "තෙත් කාලයේ කොළ මත වතුර අඩු කර භෝග මාරු කරන්න.",
    ),
    "rust": (
        "Rust",
        "Moderate",
        "Remove rusted leaves and apply a recommended fungicide. Reduce leaf wetness.",
        "Space plants; avoid dense canopy.",
        "මලකඩ වැනි කොළ ඉවත් කර නිර්දේශිත ඖෂධ යොදන්න.",
        "පැළ අතර ඉඩ තබන්න.",
    ),
    "nutrient_deficiency": (
        "Nutrient Deficiency",
        "Low",
        "This may be nutrition (e.g. nitrogen/yellowing) rather than infection. Check soil and apply balanced fertilizer.",
        "Soil test when possible; use compost regularly.",
        "මෙය රෝගයකට වඩා පෝෂණ ඌනතාවයක් විය හැක. පස පරීක්ෂා කර සමබර පොහොර යොදන්න.",
        "හැකි නම් පස පරීක්ෂා කර කොම්පෝස්ට් යොදන්න.",
    ),
}

_DEFAULT = (
    "Unknown condition",
    "Unknown",
    "Could not match a known disease label. Take a clearer close-up of the affected leaf and try again, or consult an extension officer.",
    "Keep records of symptoms and weather; isolate badly affected plants.",
    "දන්නා රෝග ලේබලයකට ගැලපෙන්නේ නැත. රෝගී කොළයේ පැහැදිලි ඡායාරූපයක් ගෙන නැවත උත්සාහ කරන්න.",
    "ලක්ෂණ සහ කාලගුණය සටහන් කරන්න; බරපතල පැළ වෙන් කරන්න.",
)


def _normalize(label: str) -> str:
    s = label.strip().lower()
    s = s.replace("-", " ").replace("_", " ")
    s = re.sub(r"[^a-z0-9\s]", "", s)
    s = re.sub(r"\s+", "_", s.strip())
    # Common Roboflow naming patterns
    aliases = {
        "earlyblight": "early_blight",
        "lateblight": "late_blight",
        "leafspot": "leaf_spot",
        "bacterialspot": "bacterial_spot",
        "powderymildew": "powdery_mildew",
        "mosaic": "mosaic_virus",
        "tomato_early_blight": "early_blight",
        "tomato_late_blight": "late_blight",
        "tomato_healthy": "healthy",
        "potato_early_blight": "early_blight",
        "potato_late_blight": "late_blight",
        "potato_healthy": "healthy",
        "pepper_bell_bacterial_spot": "bacterial_spot",
        "pepper_bell_healthy": "healthy",
        "yellow_leaf": "nutrient_deficiency",
        "yellow_leaves": "nutrient_deficiency",
    }
    compact = s.replace("_", "")
    if compact in aliases:
        return aliases[compact]
    if s in aliases:
        return aliases[s]
    for key in _ADVICE:
        if key in s or s in key:
            return key
    return s


def advice_for(label: str) -> dict[str, str]:
    key = _normalize(label)
    row = _ADVICE.get(key, _DEFAULT)
    display = row[0] if row is not _DEFAULT else (label.replace("_", " ").title() or _DEFAULT[0])
    return {
        "disease": display if row is not _DEFAULT else display,
        "severity": row[1],
        "recommendation": row[2],
        "prevention": row[3],
        "recommendation_sinhala": row[4],
        "prevention_sinhala": row[5],
        "advice_key": key,
    }


def extract_crop_hint(label: str, fallback: str | None = None) -> str | None:
    """If Roboflow class embeds crop name (e.g. Tomato___Early_blight), pull it out."""
    if fallback:
        return fallback
    raw = label.replace("-", " ").replace("_", " ")
    parts = [p for p in re.split(r"[\s/]+", raw) if p]
    known = {
        "tomato",
        "potato",
        "pepper",
        "chilli",
        "chili",
        "rice",
        "corn",
        "maize",
        "grape",
        "apple",
        "bell",
    }
    for p in parts:
        if p.lower() in known:
            name = p.lower()
            if name == "bell":
                return "Pepper"
            if name in ("chili", "chilli"):
                return "Chilli"
            return name.title()
    return None
