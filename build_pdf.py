#!/usr/bin/env python3
"""Convert report.md → docs/SENG326-Task2-Report-Team13.pdf via WeasyPrint."""
from pathlib import Path

import markdown
from weasyprint import HTML, CSS

ROOT = Path(__file__).parent
SRC = ROOT / "report.md"
OUT = ROOT / "docs" / "SENG326-Task2-Report-Team13.pdf"

CSS_TEXT = """
@page { size: A4; margin: 2.0cm; @bottom-right { content: counter(page); font-size: 9pt; color: #555; } }
body { font-family: Arial, 'Liberation Sans', 'DejaVu Sans', sans-serif; font-size: 11pt; line-height: 1.4; color: #222; }
h1, h2, h3, h4, h5, h6 { color: #17365D; font-weight: bold; font-family: Arial, sans-serif; page-break-after: avoid; }
h1 { page-break-before: always; font-size: 20pt; margin: 24pt 0 12pt; }
h1:first-of-type { page-break-before: auto; }
h2 { font-size: 16pt; margin: 18pt 0 8pt; }
h3 { font-size: 14pt; margin: 14pt 0 6pt; }
h4 { font-size: 12pt; margin: 12pt 0 4pt; }
h5, h6 { font-size: 11pt; margin: 10pt 0 4pt; }
p { margin: 0 0 8pt; }
code { font-family: 'DejaVu Sans Mono', 'Liberation Mono', monospace; background: transparent; padding: 0 2px; font-size: 9.5pt; }
pre { background: #FFFFFF; border: 1px solid #000; padding: 8pt 10pt; font-size: 9pt; line-height: 1.25; overflow-x: auto; page-break-inside: avoid; margin: 8pt 0; }
pre code { background: none; padding: 0; }
table { border-collapse: collapse; width: 100%; margin: 8pt 0; font-size: 10pt; }
th { background: #000000; color: #FFFFFF; font-weight: bold; }
th, td { border: 1px solid #000; padding: 2.84pt 2.84pt; text-align: left; vertical-align: top; }
td { background: #FFFFFF; }
img { display: block; margin: 6pt auto 2pt; max-width: 100%; max-height: 20cm; height: auto; }
p.figure-caption { text-align: center; color: #595959; font-size: 9pt; font-style: italic; margin: 0 auto 12pt; }
a { color: #2962FF; text-decoration: underline; word-break: break-all; }
blockquote { border-left: 3px solid #17365D; margin-left: 0; padding-left: 10px; color: #444; }
ul, ol { margin: 4pt 0 8pt; }
"""

import re
md = SRC.read_text(encoding="utf-8")
html_body = markdown.markdown(
    md,
    extensions=["fenced_code", "tables", "toc", "sane_lists"],
)
# tag figure captions so CSS can style only them (avoids matching every italic).
html_body = re.sub(
    r"<p><em>(Figure:[^<]+)</em></p>",
    r'<p class="figure-caption"><em>\1</em></p>',
    html_body,
)
html_doc = f"""<!doctype html><html><head><meta charset='utf-8'><title>SENG326 Task 2</title></head><body>{html_body}</body></html>"""

HTML(string=html_doc, base_url=str(ROOT)).write_pdf(
    str(OUT),
    stylesheets=[CSS(string=CSS_TEXT)],
)
print(f"wrote {OUT}")
