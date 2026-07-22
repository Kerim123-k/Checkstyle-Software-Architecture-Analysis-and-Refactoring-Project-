#!/usr/bin/env bash
# Benchmark harness for the Pipe-and-Filter slice (T087/T090/T091/T093).
# 1 warm-up + 3 timed runs per (jar, project) pair; emits two CSVs.
#
# Usage:
#   benchmarks/run-bench.sh                  # full run
#   benchmarks/run-bench.sh baseline         # baseline only
#   benchmarks/run-bench.sh refactored       # refactored only
#
# Prereqs:
#   - baseline/checkstyle-original.jar present (T005)
#   - target/checkstyle-*-all.jar built (mvn -DskipTests package)
#   - bench-config.xml at repo root
#   - JDK 11+ on PATH

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROJECTS_FILE="$ROOT/benchmarks/projects.txt"
REPOS_DIR="$ROOT/benchmarks/repos"
CONFIG="$ROOT/bench-config.xml"
BASELINE_JAR="$ROOT/baseline/checkstyle-original.jar"
REFACTORED_JAR="$(ls "$ROOT"/target/checkstyle-*-all.jar 2>/dev/null | head -1 || true)"

WARMUP=1
RUNS=3

mkdir -p "$REPOS_DIR"

clone_if_missing() {
    local name="$1" url="$2" ref="$3"
    local dest="$REPOS_DIR/$name"
    if [[ ! -d "$dest/.git" ]]; then
        echo "Cloning $name ($ref)..."
        git clone --depth 1 --branch "$ref" "$url" "$dest"
    fi
}

time_run() {
    local jar="$1" target="$2"
    local start end
    start=$(date +%s.%N)
    java -jar "$jar" -c "$CONFIG" "$target" >/dev/null 2>&1 || true
    end=$(date +%s.%N)
    awk "BEGIN{printf \"%.3f\", $end - $start}"
}

bench_pair() {
    local label="$1" jar="$2" csv="$3"
    echo "project,run_index,seconds" > "$csv"
    while IFS='|' read -r name url ref; do
        [[ "$name" =~ ^# ]] && continue
        [[ -z "$name" ]] && continue
        clone_if_missing "$name" "$url" "$ref"
        local target="$REPOS_DIR/$name"
        echo "[$label] warm-up $name..."
        for i in $(seq 1 $WARMUP); do time_run "$jar" "$target" >/dev/null; done
        for i in $(seq 1 $RUNS); do
            t=$(time_run "$jar" "$target")
            echo "$name,$i,$t" >> "$csv"
            echo "[$label] $name run $i: ${t}s"
        done
    done < "$PROJECTS_FILE"
}

mode="${1:-all}"

if [[ "$mode" == "all" || "$mode" == "baseline" ]]; then
    [[ -f "$BASELINE_JAR" ]] || { echo "missing baseline jar"; exit 1; }
    bench_pair "baseline" "$BASELINE_JAR" "$ROOT/benchmarks/results-baseline.csv"
fi

if [[ "$mode" == "all" || "$mode" == "refactored" ]]; then
    [[ -n "$REFACTORED_JAR" ]] || { echo "missing refactored jar"; exit 1; }
    bench_pair "refactored" "$REFACTORED_JAR" "$ROOT/benchmarks/results-refactored.csv"
fi

echo "Done. CSVs in benchmarks/."
