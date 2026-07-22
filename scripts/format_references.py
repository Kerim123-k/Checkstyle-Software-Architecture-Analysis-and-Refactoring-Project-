#!/usr/bin/env python3
"""Rewrite `[N] Title. https://...` reference lines to the two-line form
   [N] Title.
   https://... (hyperlink, blue, underlined)
"""
import re
from pathlib import Path

REPORT = Path(__file__).resolve().parents[1] / "report.md"

pat = re.compile(
    r"^(\[\d+\]\s+.*?\.)\s*(https?://\S+)\s*$"
)

out_lines = []
for ln in REPORT.read_text(encoding="utf-8").splitlines():
    m = pat.match(ln)
    if m:
        title, url = m.group(1), m.group(2)
        # Two-line form. Markdown link autorenders as blue+underlined.
        out_lines.append(title)
        out_lines.append("")
        out_lines.append(f"[{url}]({url})")
    else:
        out_lines.append(ln)
REPORT.write_text("\n".join(out_lines) + "\n", encoding="utf-8")
print("references reformatted")
