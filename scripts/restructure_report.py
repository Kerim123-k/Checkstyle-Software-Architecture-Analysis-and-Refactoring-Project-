#!/usr/bin/env python3
"""One-shot rewrite of report.md to apply the user's structural rules:
   1. Strip every `---` horizontal rule.
   2. Add an italic Figure caption beneath every image reference.
   3. Move rendered PlantUML images out of the Appendix sections into the
      proper main sections; keep only the source code blocks in appendices.
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "report.md"

# Map diagram-NN.png -> (caption, target-section-anchor)
# target-section-anchor is the EXACT heading line to insert AFTER.
DIAGRAM_PLACEMENT = {
    "diagram-01.png": (
        "Figure: Pre-refactor AST-based check sequence — TreeWalker dispatches "
        "tokens straight to the monolithic check, which measures, compares, and "
        "logs in a single class.",
        "## 5.2  Before vs After: The Call Flow",
    ),
    "diagram-02.png": (
        "Figure: Post-refactor AST-based check sequence — the driver only "
        "submits an AstEvent into the pipeline; TokenFilter, "
        "MeasurementFilter, ThresholdFilter and ViolationSink each own one "
        "stage.",
        "## 5.2  Before vs After: The Call Flow",
    ),
    "diagram-03.png": (
        "Figure: Pre-refactor file-level check sequence — Checker passes the "
        "FileText directly to a monolithic FileLengthCheck / LineLengthCheck.",
        "## 5.2  Before vs After: The Call Flow",
    ),
    "diagram-04.png": (
        "Figure: Post-refactor file-level check sequence — the driver routes "
        "the FileText into a LineSplitterFilter and the same threshold/sink "
        "tail used by the AST checks.",
        "## 5.2  Before vs After: The Call Flow",
    ),
    "diagram-05.png": (
        "Figure: C4 Level 3 — components of the Pipe-and-Filter slice. Drivers "
        "(deep blue) sit on the slice boundary; reusable filters (medium "
        "blue) and per-check measurement filters carry messages defined by "
        "the pipeline core (light blue).",
        "## 6.2  Architecture Diagrams",
    ),
    "diagram-06.png": (
        "Figure: C4 Level 4 code view for MethodLengthCheck — left, the "
        "original single-class design; right, the four-stage pipeline of the "
        "refactored slice.",
        "## 6.2  Architecture Diagrams",
    ),
    "diagram-07.png": (
        "Figure: Package dependency graph before the refactor — every check "
        "class depends directly on the framework `api` package.",
        "## 6.2  Architecture Diagrams",
    ),
    "diagram-08.png": (
        "Figure: Package dependency graph after the refactor — driver classes "
        "still touch `api`, but every filter depends only on the new "
        "`pipeline` packages.",
        "## 6.2  Architecture Diagrams",
    ),
    "diagram-09.png": (
        "Figure: UML class diagram — pipeline core (`Pipe<T>`, `Filter<I,O>`, "
        "`Pipeline<H,T>`, `PipelineBuilder`) and the three pipe "
        "implementations.",
        "## 4.4  Message types (immutable value objects)",
    ),
    "diagram-10.png": (
        "Figure: UML class diagram — metrics drivers + their measurement "
        "filters.",
        "## 4.7  Per-check measurement filter classes",
    ),
    "diagram-11.png": (
        "Figure: UML class diagram — sizes drivers + their measurement "
        "filters (AST-based + the two file-level checks fed by "
        "LineSplitterFilter).",
        "## 4.7  Per-check measurement filter classes",
    ),
}

# Captions for non-diagram images keyed by basename.
CAPTIONS = {
    "originalCheckstyleStructure.png":
        "Figure: IDE Project view of the pre-refactor checkstyle/checks tree — "
        "metrics/ and sizes/ packages hold all sixteen monolithic drivers.",
    "metricsAndSizesPackages.png":
        "Figure: The sixteen driver files we migrated, split across "
        "checks.metrics (6) and checks.sizes (10).",
    "pipelineMappingDiagram.png":
        "Figure: Mapping AST → Pipe → Filter → ViolationMessage; each filter "
        "holds no reference to the next, only the typed pipe between them.",
    "metricsDriversIdeView.png":
        "Figure: Project view of the six metrics drivers after refactoring; "
        "each class is now a thin pipeline driver.",
    "sizesDriversIdeView.png":
        "Figure: Project view of the ten sizes drivers after refactoring.",
    "pipeInterfaceCode.png":
        "Figure: Source of `Pipe<T>` — the only coordination object shared "
        "between two filters.",
    "filterInterfaceCode.png":
        "Figure: Source of `Filter<I,O>` — a one-method contract enforced by "
        "every concrete filter.",
    "pipelineBuilderCode.png":
        "Figure: PipelineBuilder fluent API used inside MethodLengthCheck.",
    "commonFiltersFolder.png":
        "Figure: The pipeline/filter/ package — five reusable filters shared "
        "by every driver.",
    "pipelineDriverExampleCode.png":
        "Figure: MethodLengthCheck — the post-refactor driver only wires the "
        "pipeline and drains the sink.",
    "measurementFiltersFolder.png":
        "Figure: All fifteen measurement filters — one per driver, each "
        "extending the shared base.",
    "methodLengthBeforeCode.png":
        "Figure: Original MethodLengthCheck.visitToken — measurement, "
        "threshold comparison, and violation emission interleaved in one "
        "method.",
    "methodLengthAfterCode.png":
        "Figure: Refactored MethodLengthCheck — visitToken submits an "
        "AstEvent and drains the sink; logic moves into filters.",
    "pipelineFolderTreeIdeView.png":
        "Figure: New checks.pipeline/ subpackages — api (Pipe/Filter), "
        "builder, common filters, and per-message DTOs.",
    "gitDiffStatSummary.png":
        "Figure: `git diff --stat` for the refactor slice — sixteen drivers "
        "shortened, sixteen new measurement filters added.",
    "regressionDiffEmpty.png":
        "Figure: PowerShell diff between the baseline jar and the refactored "
        "jar — empty output confirms byte-identical violation reports.",
    "mvnTestSummary.png":
        "Figure: `mvn clean test` final summary — 7,423 tests run, zero "
        "failures, BUILD SUCCESS.",
    "perCheckFireGreenJunit.png":
        "Figure: IntelliJ JUnit pane — every per-check fire test is green, "
        "proving each of the sixteen drivers still emits at least one "
        "violation on the sample input.",
    "archUnitConformanceGreen.png":
        "Figure: IntelliJ JUnit pane — twelve ArchUnit rules + two filter "
        "isolation tests pass.",
    "jqassistantQuery1Result.png":
        "Figure: jQAssistant Q1 result — every filter in the pipeline package "
        "depends on the message types defined by the pipeline core.",
    "jqassistantQuery2Result.png":
        "Figure: jQAssistant Q2 result — measurement filters extend the "
        "shared AbstractMeasurementFilter base, never AbstractCheck.",
    "jqassistantQuery3Result.png":
        "Figure: jQAssistant Q3 result — adjacency graph showing which "
        "filters each driver wires into its pipeline, and the message type "
        "carried on each pipe.",
    "jqassistantQuery4Result.png":
        "Figure: jQAssistant Q4 result — zero cycles in the filter "
        "dependency graph; data flow is strictly acyclic.",
    "jqassistantQuery5Result.png":
        "Figure: jQAssistant Q5 result — zero filters call "
        "`AbstractCheck#log`; all violations leave the slice via "
        "ViolationSink.",
    "benchSetupTerminal.png":
        "Figure: Bench kick-off — run-bench.sh clones the five target "
        "projects and runs baseline + refactored Checkstyle in sequence.",
    "benchProjectsCloned.png":
        "Figure: The five cloned benchmark projects in "
        "/tmp/checkstyle-bench-repos.",
    "benchRunOutputTerminal.png":
        "Figure: First 24 lines of bench.log — per-project run timings for "
        "baseline and refactored jars.",
    "task2-pipe-filter-perf.png":
        "Figure: Wall-clock time (mean of three runs, error bars = ±stdev) "
        "for baseline vs refactored Checkstyle across five projects.",
}


def caption_for(path):
    base = path.rsplit("/", 1)[-1]
    if base in CAPTIONS:
        return CAPTIONS[base]
    if base in DIAGRAM_PLACEMENT:
        return DIAGRAM_PLACEMENT[base][0]
    return None


def main():
    text = REPORT.read_text(encoding="utf-8")
    lines = text.splitlines()

    # ---- 1. drop horizontal rules ----
    lines = [ln for ln in lines if ln.strip() not in {"---", "***", "___"}]

    # ---- 2. remove `![Diagram N](docs/screenshots/diagram-NN.png)` from
    # appendices and queue them for relocation ----
    diagram_img_re = re.compile(r"^!\[[^\]]*\]\(docs/screenshots/(diagram-\d+\.png)\)$")
    new_lines = []
    moved_diagrams = []  # ordered list of (basename) actually present
    for ln in lines:
        m = diagram_img_re.match(ln.strip())
        if m and m.group(1) in DIAGRAM_PLACEMENT:
            moved_diagrams.append(m.group(1))
            continue
        new_lines.append(ln)
    lines = new_lines

    # ---- 3. insert each moved diagram beneath its target heading ----
    for basename in moved_diagrams:
        caption, target_heading = DIAGRAM_PLACEMENT[basename]
        # find the target heading line
        target_idx = None
        for i, ln in enumerate(lines):
            if ln.strip() == target_heading.strip():
                target_idx = i
                break
        if target_idx is None:
            # fall back to end of file
            target_idx = len(lines) - 1
        # find next heading to insert just before it
        insert_at = len(lines)
        for j in range(target_idx + 1, len(lines)):
            if lines[j].startswith("#"):
                insert_at = j
                break
        block = [
            "",
            f"![{caption.replace('Figure: ', '').rstrip('.')}](docs/screenshots/{basename})",
            "",
            f"*{caption}*",
            "",
        ]
        lines = lines[:insert_at] + block + lines[insert_at:]

    # ---- 4. add Figure captions under every existing image ref ----
    img_re = re.compile(r"^!\[[^\]]*\]\(([^)]+)\)$")
    out = []
    i = 0
    while i < len(lines):
        ln = lines[i]
        out.append(ln)
        m = img_re.match(ln.strip())
        if m:
            # if the next non-empty line is already an italic Figure caption,
            # skip to avoid duplication.
            j = i + 1
            while j < len(lines) and lines[j].strip() == "":
                j += 1
            already = j < len(lines) and lines[j].lstrip().startswith("*Figure")
            if not already:
                cap = caption_for(m.group(1))
                if cap:
                    out.append("")
                    out.append(f"*{cap}*")
        i += 1

    REPORT.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"updated {REPORT}  (moved {len(moved_diagrams)} diagrams)")


if __name__ == "__main__":
    main()
