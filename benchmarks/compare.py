#!/usr/bin/env python3
"""Compare benchmark results (T092/T093). Reads results-baseline.csv +
results-refactored.csv, computes mean / 95% CI per project, asserts the
refactored mean is within +/-10% of baseline. Exits non-zero on
violation. Writes a tiny markdown summary."""

import csv, math, statistics, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
TOL = 0.10

def load(path):
    by_project = {}
    with open(path, newline="") as f:
        for row in csv.DictReader(f):
            by_project.setdefault(row["project"], []).append(float(row["seconds"]))
    return by_project

def ci95(samples):
    if len(samples) < 2:
        return 0.0
    sd = statistics.stdev(samples)
    return 1.96 * sd / math.sqrt(len(samples))

def main():
    base = load(ROOT / "results-baseline.csv")
    refac = load(ROOT / "results-refactored.csv")
    rows = []
    failed = []
    for project in sorted(set(base) | set(refac)):
        b = statistics.mean(base.get(project, [0]))
        r = statistics.mean(refac.get(project, [0]))
        delta = (r - b) / b if b else 0.0
        rows.append((project, b, ci95(base.get(project, [])),
                     r, ci95(refac.get(project, [])), delta))
        if abs(delta) > TOL:
            failed.append((project, delta))

    out = ROOT / "summary.md"
    with open(out, "w") as f:
        f.write("| project | baseline mean (s) | +/-95% | refactored mean (s) | +/-95% | delta |\n")
        f.write("|---|---|---|---|---|---|\n")
        for p, b, bci, r, rci, d in rows:
            f.write(f"| {p} | {b:.3f} | {bci:.3f} | {r:.3f} | {rci:.3f} | {d*100:+.2f}% |\n")

    for p, d in failed:
        print(f"FAIL {p}: {d*100:+.2f}% exceeds +/-{TOL*100:.0f}%", file=sys.stderr)
    sys.exit(1 if failed else 0)

if __name__ == "__main__":
    main()
