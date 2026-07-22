#!/usr/bin/env python3
"""Replace `*[image placeholder: NAME.png — desc]*` lines in report.md with
`![desc](docs/screenshots/NAME.png)` markdown image references."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "report.md"

pat = re.compile(r"\*\[image placeholder:\s*([A-Za-z0-9_.\-]+\.png)\s*[—-]\s*(.+?)\]\*")

text = REPORT.read_text(encoding="utf-8")
n = 0


def sub(m):
    global n
    n += 1
    name, desc = m.group(1), m.group(2).strip()
    return f"![{desc}](docs/screenshots/{name})"


new = pat.sub(sub, text)
REPORT.write_text(new, encoding="utf-8")
print(f"replaced {n} placeholders")
