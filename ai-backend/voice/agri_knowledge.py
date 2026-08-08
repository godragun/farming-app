"""Agricultural vocabulary + keyword maps for Sinhala / English farming questions."""

# crop_key -> (English, Sinhala aliases)
CROPS = {
    "chilli": ("Chilli", ["මිරිස්", "මිරිස", "miris", "chilli", "chili", "pepper"]),
    "tomato": ("Tomato", ["තක්කාලි", "තකාලි", "thakkali", "tomato"]),
    "rice": ("Rice", ["වී", "හාල්", "නෙල්", "paddy", "rice", "nel"]),
    "tea": ("Tea", ["තේ", "tea"]),
    "coconut": ("Coconut", ["පොල්", "coconut", "pol"]),
    "onion": ("Onion", ["ලූණු", "ලුණු", "onion", "lunu"]),
    "brinjal": ("Brinjal", ["වම්බටු", "වම්බට", "brinjal", "eggplant"]),
    "banana": ("Banana", ["කෙසෙල්", "banana", "kesel"]),
    "maize": ("Maize", ["ඉරිඟු", "ඉරිගු", "maize", "corn"]),
    "potato": ("Potato", ["අල", "potato", "ala"]),
}

# symptom_key -> (English, Sinhala / English aliases)
SYMPTOMS = {
    "yellow_leaves": (
        "Yellow leaves",
        ["කොළ කහ", "කොල කහ", "කහ වෙලා", "කහවෙලා", "yellow leaf", "yellow leaves", "කහ පාට"],
    ),
    "spots": (
        "Leaf spots",
        ["පැල්ලම්", "පල්ලම්", "spots", "blight", "කැලැල්", "කලල"],
    ),
    "wilting": (
        "Wilting",
        ["මැලවී", "මැලවෙනවා", "වැලිලා", "wilt", "wilting", "මලානික"],
    ),
    "insects": (
        "Insect pests",
        ["කුඩා මකුණන්", "පණුවන්", "කෘමි", "pest", "insect", "මකුණන්", "aphid"],
    ),
    "fungus": (
        "Fungal infection",
        ["දිලීර", "හුණු", "fungus", "mold", "mildew", "දිලිර"],
    ),
    "dry_leaves": (
        "Dry / scorched leaves",
        ["වේලී", "වේලුණු", "dry leaf", "scorch", "burn"],
    ),
}

INTENT_KEYWORDS = {
    "disease_help": [
        "රෝග", "රෝගය", "මොකක්ද කරන්න", "කරන්න ඕන", "ගැටලුව", "problem",
        "disease", "sick", "කුමක්ද", "උදව්", "help", "කහ වෙලා", "මැලවී",
    ],
    "fertilizer": [
        "පොහොර", "පොහර", "fertilizer", "urea", "යූරියා", "compost", "කොම්පෝස්ට්",
    ],
    "irrigation": [
        "වතුර", "දිය", "irrigation", "water", "ජලය", "වාරිමාර්ග",
    ],
    "weather": [
        "කාලගුණ", "වැස්ස", "weather", "rain", "temperature", "උෂ්ණත්ව",
    ],
    "planting": [
        "වගා", "සිටුව", "plant", "පැළ", "seed", "බීජ", "රෝපණ",
    ],
    "harvest": [
        "අස්වැන්න", "කපන", "harvest", "yield", "අස්වන",
    ],
    "market": [
        "මිල", "වෙළඳ", "market", "price", "විකුණ",
    ],
    "suitability": [
        "කොහෙද", "සුදුසු", "suitable", "where to grow", "හොඳම තැන", "ප්‍රදේශ",
    ],
    "greeting": [
        "ආයුබෝවන්", "හලෝ", "hello", "hi", "කොහොමද",
    ],
}
