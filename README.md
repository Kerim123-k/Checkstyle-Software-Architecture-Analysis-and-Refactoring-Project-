# Checkstyle — Software Architecture Analysis and Refactoring Project

SENG 326 (Software Architecture) group project: recovering the architecture of an existing
open-source system ([Checkstyle](https://checkstyle.org/) 13.2.0) and then refactoring a
defined slice of it into a target architectural style — **Pipe-and-Filter** — while proving,
with automated tests and tooling, that behaviour and performance are preserved.

**Group: THE GROUP**
- Karim Hariri — 22050941008
- Mohamed Attia Eid Attia Eid — 22050941017
- Zaid Hardan — 22050941005

Registered target architecture: **Pipe-and-Filter**

## What This Repository Contains

This repo is a fork of the real [Checkstyle](https://github.com/checkstyle/checkstyle)
codebase (a ~200-rule Java static analysis tool) with a fixed slice of it — every check in
the **Metrics** category and every check in the **Size Violations** category, 16 concrete
checks in total — rewritten from Checkstyle's original monolithic-check design into an
explicit Pipe-and-Filter pipeline.

The full write-ups for both project tasks are included:

| File | Task |
|--|--|
| [`docs/TASK-1-Architecture-Recovery.pdf`](docs/TASK-1-Architecture-Recovery.pdf) | Task 1 — Architecture recovery of the original Checkstyle codebase |
| [`report.md`](report.md) | Task 2 — Full refactoring report (also built as PDF/DOCX) |
| [`docs/SENG326-Task2-Report-Team13.pdf`](docs/SENG326-Task2-Report-Team13.pdf) | Task 2 — Rendered report |
| [`specs/001-pipe-filter-metrics-sizes/`](specs/001-pipe-filter-metrics-sizes) | Formal spec, plan, and task breakdown used to drive the refactor |
| [`structurizr/workspace.dsl`](structurizr/workspace.dsl) | C4 model of the resulting architecture |
| [`jqassistant/rules/pipe-and-filter.xml`](jqassistant/rules/pipe-and-filter.xml) | jQAssistant rules that verify the architecture from the dependency graph |

## Background: Checkstyle's Original Architecture

Checkstyle reads Java source, parses it into an Abstract Syntax Tree (AST), and walks the
tree with a central `TreeWalker` engine that calls `visitToken()` / `leaveToken()` on every
registered check. Each check originally extended `AbstractCheck` and did four jobs in one
class: lifecycle handling, measurement, threshold comparison, and violation reporting — with
no clear stage boundaries and no way to test measurement logic in isolation.

## The Refactor: Pipe-and-Filter

Pipe-and-Filter decomposes each check into independent, single-responsibility **filters**
connected by typed, unidirectional **pipes** carrying immutable **messages**. Filters never
call each other and share no state — the only thing connecting them is the data flowing
along the pipes.

Every migrated check now follows the same five-stage shape:

1. receive AST events (or raw file lines, for the two file-level checks)
2. select the events the check cares about
3. measure something (branches, statements, lengths, coupling, …)
4. compare the measurement against a threshold
5. emit a violation if the threshold was crossed

The class that Checkstyle's `TreeWalker` still sees (the "Pipeline Driver") is cut down to a
thin boundary adapter: it only translates framework callbacks into pipeline messages and
forwards violations back out. Measurement, threshold comparison, and violation emission are
now three separate, independently testable filter classes per check.

### Scope: 16 Checks Migrated

**Metrics** (`checks/metrics/`, 6 concrete checks + 1 shared abstract base):
`BooleanExpressionComplexityCheck`, `ClassDataAbstractionCouplingCheck`,
`ClassFanOutComplexityCheck`, `CyclomaticComplexityCheck`, `JavaNCSSCheck`,
`NPathComplexityCheck` (base: `AbstractClassCouplingCheck`)

**Sizes** (`checks/sizes/`, 10 concrete checks):
`AnonInnerLengthCheck`, `ExecutableStatementCountCheck`, `FileLengthCheck`,
`LambdaBodyLengthCheck`, `LineLengthCheck`, `MethodCountCheck`, `MethodLengthCheck`,
`OuterTypeNumberCheck`, `ParameterNumberCheck`, `RecordComponentNumberCheck`

`LineLengthCheck` and `FileLengthCheck` work on raw file text rather than the AST, so they
use a slightly different pipeline shape (`LineSplitterFilter` turns a `FileText` into a
stream of `FileLine` messages for the rest of the pipeline).

### New Infrastructure (`checks/pipeline/`)

Core pipeline types shared by every migrated check: `Pipe`, `SingletonPipe`, `QueuePipe`,
`Filter`, `Pipeline`, `PipelineBuilder`, `AstEvent`, `FileLine`, `Measurement`,
`ViolationMessage`, `TokenFilter`, `LineSplitterFilter`, `IgnorePatternFilter`,
`ThresholdFilter`, `ViolationSink` — plus per-check measurement filters under
`checks/metrics/pipeline/` and `checks/sizes/pipeline/`.

Net change: **+30 files** (15 new infrastructure classes, 18 new measurement filter
classes, 16 drivers rewritten in place with the same public name/config surface).

## Verification

The refactor was verified with five independent strategies:

1. **Byte-identical regression test** — baseline jar vs. refactored jar run on the same
   input; `diff` on the two violation reports produced no output (44/44 violations
   identical).
2. **Full Maven test suite** (`mvn test`, ~7,400 tests) — every test touching the slice is
   green, including `RegressionDiffTest`, `PipeAndFilterArchitectureTest` (12/12),
   `FilterIsolationArchTest` (2/2), `PerCheckFireTest` (16/16), `AllChecksTest`,
   `CheckerTest`, and the original functional test for all 16 migrated checks. Remaining
   failures/errors are host-environment issues unrelated to the refactor (Mockito/ByteBuddy
   vs. JDK 25, headless GUI tests).
3. **Per-check fire test** — all 16 checks still emit at least one violation on a shared
   sample input (16/16 pass).
4. **ArchUnit conformance** — 10 architectural rules (e.g. "no filter calls
   `AbstractCheck.log()` directly", "no cross-filter type dependencies other than `Pipe<?>`",
   "pipeline core doesn't depend on its users") verified against compiled bytecode, 10/10
   pass.
5. **jQAssistant graph queries** — 5 Neo4j-backed queries independently confirm the same
   pipeline shape from the static dependency graph, including a zero-cycles check.

## Performance

Part B repeats the Task 1 benchmark on the refactored jar across the same five open-source
Java projects (minimal-json, javapoet, gs-core, jgrapht-core, Apache Calcite core), 1
warm-up + 3 timed runs each. Result: **no measurable regression** — the two largest projects
(calcite-core, gs-core) show deltas of +0.35% and +1.35%, well inside the ±10% tolerance;
the pipeline adds two virtual calls per AST event, which the JIT inlines away after warm-up.
Full analysis, raw CSVs, and the comparison plot are in `report.md` §10 and `benchmarks/`.

## Repository Layout

```
src/main/java/.../checks/metrics/         16 metrics driver + measurement filter classes
src/main/java/.../checks/sizes/           10 sizes driver + measurement filter classes
src/main/java/.../checks/pipeline/        shared Pipe-and-Filter infrastructure
src/test/java/.../architecture/           ArchUnit rules (PipeAndFilterArchitectureTest, ...)
benchmarks/                               benchmark scripts, raw results, summary
docs/                                     rendered reports, screenshots, reference material
specs/001-pipe-filter-metrics-sizes/      spec / plan / task breakdown for the refactor
structurizr/                              C4 architecture model
jqassistant/                              jQAssistant rule definitions
report.md                                 full Task 2 write-up (source of the rendered PDF)
```

## Building and Running

This is a standard Maven project (built on the upstream Checkstyle build):

```bash
./mvnw -DskipTests package
java -jar target/checkstyle-13.2.0-all.jar -c <config.xml> <YourFile.java>
```

Run the full test suite, including the architecture conformance tests:

```bash
./mvnw test
```

## Reports

The `docs/` folder and repo root include the full deliverables for both tasks: the Task 1
architecture-recovery report, the Task 2 refactoring report (`report.md`, with generated
PDF/DOCX), the Structurizr C4 model, and the screenshots referenced throughout both reports.

## Attribution

This project is a derivative of [Checkstyle](https://github.com/checkstyle/checkstyle),
licensed under the GNU Lesser General Public License v2.1 (see `LICENSE`). All architectural
analysis and refactoring in this repository was produced as coursework for SENG 326 —
Software Architecture.
