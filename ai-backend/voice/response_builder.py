"""Build Sinhala + English farmer-facing replies from understanding."""

from __future__ import annotations

from voice.agri_knowledge import CROPS, SYMPTOMS


def build_response(understanding: dict) -> dict[str, str]:
    intent = understanding.get("intent") or "general_farming"
    crop = understanding.get("crop")
    symptom = understanding.get("symptom")
    crop_en = CROPS[crop][0] if crop else None
    crop_si = understanding.get("crop_sinhala") or (CROPS[crop][1][0] if crop else "බෝගය")
    symptom_en = SYMPTOMS[symptom][0] if symptom else None

    if intent == "greeting":
        return {
            "sinhala": "ආයුබෝවන්! මම Agri Lanka කෘෂිකාර්මික සහායකයා. ඔබේ බෝග, රෝග, පොහොර හෝ කාලගුණය ගැන අහන්න.",
            "english": "Ayubowan! I am the Agri Lanka farm assistant. Ask about crops, diseases, fertilizer, or weather.",
        }

    if intent == "disease_help":
        si_parts = []
        en_parts = []
        if crop and symptom:
            si_parts.append(f"ඔබේ {crop_si} වගාවේ {understanding.get('symptom_sinhala') or 'ගැටලුවක්'} පෙනෙනවා.")
            en_parts.append(f"Your {crop_en} shows signs of {symptom_en}.")
        elif crop:
            si_parts.append(f"ඔබේ {crop_si} ගැන රෝග උපදෙස් අවශ්‍ය වගේ.")
            en_parts.append(f"You need disease advice for {crop_en}.")
        else:
            si_parts.append("බෝග රෝගයක් ගැන උදව් අවශ්‍ය වගේ.")
            en_parts.append("You seem to need crop disease help.")

        if understanding.get("needs_photo"):
            si_parts.append("කරුණාකර රෝගී කොළයේ පැහැදිලි ඡායාරූපයක් ගන්න. AI Crop Doctor මගින් වඩාත් නිවැරදිව හඳුනාගත හැක.")
            en_parts.append("Please take a clear photo of the affected leaf. AI Crop Doctor can identify it more accurately.")
        else:
            si_parts.append("රෝගී කොටසේ ඡායාරූපයක් එවන්න, නැතහොත් රෝග ලක්ෂණ වැඩිදුර කියන්න.")
            en_parts.append("Send a photo of the affected part, or describe more symptoms.")

        si_parts.append("දැන්: ආසාදිත කොළ ඉවත් කරන්න, හොඳ වාතාශ්‍රය තබන්න, වැස්සට කලින් ඖෂධ නොඉසින්න.")
        en_parts.append("For now: remove badly infected leaves, improve airflow, and avoid spraying just before rain.")
        return {"sinhala": " ".join(si_parts), "english": " ".join(en_parts)}

    if intent == "fertilizer":
        crop_bit_si = f"{crop_si} " if crop else ""
        crop_bit_en = f"for {crop_en} " if crop_en else ""
        return {
            "sinhala": f"{crop_bit_si}වගාවට කොම්පෝස්ට් මූලිකව යොදා, ඉන්පසු බෙදා පොහොර යොදන්න. ජලය දැමීමෙන් පසු පොහොර යෙදීම හොඳයි.",
            "english": f"Use compost as a base {crop_bit_en}then apply fertilizer in split doses. Water after applying nutrients.",
        }

    if intent == "irrigation":
        return {
            "sinhala": "උදේ හෝ සවස වතුර දමන්න. කොළ තෙත් වනසේ ඉසීම අඩු කරන්න — රෝග වැඩිවීමට ඉඩ තියෙනවා. පස තෙතමනය අතින් පරීක්ෂා කරන්න.",
            "english": "Irrigate in morning or evening. Avoid wetting leaves too much (disease risk). Check soil moisture by hand.",
        }

    if intent == "weather":
        return {
            "sinhala": "කාලගුණය බලා ඖෂධ ඉසීම සහ වතුර කළමනාකරණය කරන්න. Agri Lanka Weather තිරයෙන් ඔබේ ස්ථානයේ පුරෝකථනය බලන්න.",
            "english": "Plan spraying and irrigation using weather. Open the Agri Lanka Weather screen for your location forecast.",
        }

    if intent == "planting":
        return {
            "sinhala": f"{crop_si} සිටුවීමට හොඳ ජලාපවහනයක් ඇති පසක් තෝරන්න. බීජ/පැළ සෞඛ්‍ය සම්පන්නදැයි පරීක්ෂා කරන්න.",
            "english": f"Choose well-drained soil for planting {crop_en or 'the crop'}. Use healthy seed or seedlings.",
        }

    if intent == "harvest":
        return {
            "sinhala": "අස්වැන්න නිවැරදි කාලයේ කපන්න. අධික තෙතමනයේදී අස්වනු නොකරන්න — ගබඩා කිරීමේදී දිලීර ඇතිවිය හැක.",
            "english": "Harvest at the right maturity. Avoid harvesting in high moisture to reduce storage fungus risk.",
        }

    if intent == "market":
        return {
            "sinhala": "වෙළඳ මිල APMC / Market කොටසෙන් පරීක්ෂා කරන්න. ගබඩා කිරීමේ පිරිවැය සහ ප්‍රවාහනයද සලකන්න.",
            "english": "Check market prices in the APMC section. Also weigh storage and transport costs.",
        }

    if intent == "suitability":
        return {
            "sinhala": f"{crop_si} වගාවට සුදුසු ස්ථානය AI Crop Suitability මගින් තීරණය කළ හැක. සිතියමෙන් කොළ / කහ / රතු කලාප බලන්න.",
            "english": f"Use AI Crop Suitability to see where {crop_en or 'this crop'} grows best (green/yellow/red zones on the map).",
        }

    return {
        "sinhala": "මට තේරුණා. කරුණාකර බෝගයේ නම සහ ගැටලුව (උදා: කොළ කහ වෙලා) කෙටියෙන් කියන්න. උදව් සඳහා හඬින් හෝ අකුරෙන් අහන්න.",
        "english": "Please tell me the crop name and problem (e.g. yellow leaves). You can speak or type.",
    }
