#!/usr/bin/env python3
"""Render baseline vs refactored bench results to a single bar chart."""
import csv
import statistics
from pathlib import Path

import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = Path(__file__).parent
BASELINE = ROOT / "results-baseline.csv"
REFACTOR = ROOT / "results-refactored.csv"
OUT_DIR = ROOT.parent / "docs" / "screenshots"
OUT_DIR.mkdir(parents=True, exist_ok=True)
OUT_PNG = OUT_DIR / "task2-pipe-filter-perf.png"


def load(csv_path):
    runs = {}
    with open(csv_path, newline="") as f:
        for row in csv.DictReader(f):
            runs.setdefault(row["project"], []).append(float(row["seconds"]))
    return runs


baseline = load(BASELINE)
refactor = load(REFACTOR)
projects = list(baseline.keys())

base_mean = [statistics.mean(baseline[p]) for p in projects]
ref_mean = [statistics.mean(refactor[p]) for p in projects]
base_std = [statistics.stdev(baseline[p]) if len(baseline[p]) > 1 else 0 for p in projects]
ref_std = [statistics.stdev(refactor[p]) if len(refactor[p]) > 1 else 0 for p in projects]

x = np.arange(len(projects))
w = 0.38

fig, ax = plt.subplots(figsize=(11, 6))
ax.bar(x - w / 2, base_mean, w, yerr=base_std, capsize=4,
       label="Original (baseline)", color="#4C72B0")
ax.bar(x + w / 2, ref_mean, w, yerr=ref_std, capsize=4,
       label="Refactored (Pipe-and-Filter)", color="#DD8452")
ax.set_xticks(x)
ax.set_xticklabels(projects, rotation=15, ha="right")
ax.set_ylabel("Mean Wall-Clock Time (s)")
ax.set_title(
    "Checkstyle Performance: Original vs Pipe-and-Filter Refactor\n"
    "(metrics + sizes checks; 1 warm-up + 3 timed runs; error bars = stdev)"
)
ax.legend(loc="upper left")
ax.grid(axis="y", linestyle=":", alpha=0.6)

for i, (b, r) in enumerate(zip(base_mean, ref_mean)):
    delta_pct = (r - b) / b * 100 if b else 0
    sign = "+" if delta_pct >= 0 else ""
    ax.text(i, max(b, r) + max(base_std[i], ref_std[i]) + 1.0,
            f"{sign}{delta_pct:.1f}%", ha="center", fontsize=9, color="#333")

plt.tight_layout()
plt.savefig(OUT_PNG, dpi=140)
print(f"wrote {OUT_PNG}")
