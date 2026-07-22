#!/usr/bin/env python3
"""Extract every ```plantuml fence from report.md and render to docs/screenshots/."""
import re
import sys
from pathlib import Path

from plantuml import PlantUML

ROOT = Path(__file__).parent
SRC = ROOT / "report.md"
OUT_DIR = ROOT / "docs" / "screenshots"
OUT_DIR.mkdir(parents=True, exist_ok=True)

server = PlantUML(url="http://www.plantuml.com/plantuml/img/")

text = SRC.read_text(encoding="utf-8")
fences = re.findall(r"```plantuml\n(.*?)```", text, re.DOTALL)
print(f"found {len(fences)} plantuml blocks")

for i, body in enumerate(fences, 1):
    out = OUT_DIR / f"diagram-{i:02d}.png"
    if out.exists():
        print(f"  [{i:02d}] cached {out.name}")
        continue
    print(f"  [{i:02d}] rendering -> {out.name}")
    try:
        png = server.processes(body)
        out.write_bytes(png)
    except Exception as e:
        print(f"      ERROR: {e}", file=sys.stderr)

print("done")
