"""Report paths that still look like hardcoded secrets. Never prints secret values."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "app" / "src"
PATTERNS = [
    ("jwt", re.compile(r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.")),
    ("quoted_hex_32plus", re.compile(r"['\"]([a-fA-F0-9]{32,})['\"]")),
]


def main() -> None:
    for kind, rx in PATTERNS:
        hits: list[str] = []
        for path in ROOT.rglob("*"):
            if path.suffix.lower() not in {".kt", ".java", ".xml", ".json", ".properties"}:
                continue
            try:
                text = path.read_text(encoding="utf-8", errors="ignore")
            except OSError:
                continue
            if rx.search(text):
                hits.append(str(path.relative_to(ROOT.parent.parent)).replace("\\", "/"))
        print(f"{kind} hits {len(hits)}")
        for h in hits:
            print(f"  {h}")


if __name__ == "__main__":
    main()
