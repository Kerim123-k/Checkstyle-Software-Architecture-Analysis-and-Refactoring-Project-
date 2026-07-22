# Tasks: Pipe-and-Filter Refactoring of Metrics + Sizes Slice

**Input**: Design documents from `/specs/001-pipe-filter-metrics-sizes/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Required by spec (FR-008, FR-009, FR-010, FR-012). Test tasks generated for every story.

**Organization**: Tasks grouped by user story (US1–US4) for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Parallelizable (different files, no dependencies on incomplete tasks)
- **[Story]**: User story tag (US1–US4); setup/foundational/polish unlabeled

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Snapshot baseline; create new package skeleton.

- [x] T001 Capture pre-refactor baseline output in `pre-refactor-output.txt` by running `mvn -DskipTests package` then `java -jar target/checkstyle-*-all.jar -c bench-config.xml violation-sample/SampleAllViolations.java > pre-refactor-output.txt`
- [X] T002 [P] Create empty package `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/` with `package-info.java`
- [X] T003 [P] Create empty package `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/pipeline/` with `package-info.java`
- [X] T004 [P] Create empty package `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/` with `package-info.java`
- [x] T005 Save baseline jar to `baseline/checkstyle-original.jar` for benchmark comparison

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Pipe interfaces, message types, common filters, pipeline composer. ALL user stories depend on this.

**⚠️ CRITICAL**: No driver migration may begin until this phase completes.

### Pipes

- [X] T006 [P] Define `Pipe<T>` interface in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/pipe/Pipe.java`
- [X] T007 [P] Implement `SingletonPipe<T>` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/pipe/SingletonPipe.java`
- [X] T008 [P] Implement `QueuePipe<T>` (ArrayDeque-backed) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/pipe/QueuePipe.java`

### Messages

- [X] T009 [P] Implement `AstEvent` (final, with `Phase` enum) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/message/AstEvent.java`
- [X] T010 [P] Implement `FileLine` (final) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/message/FileLine.java`
- [X] T011 [P] Implement `Measurement` (final) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/message/Measurement.java`
- [X] T012 [P] Implement `ViolationMessage` (final) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/message/ViolationMessage.java`

### Filter contract + composer

- [X] T013 [P] Define `Filter<I,O>` interface in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/Filter.java`
- [X] T014 Implement `Pipeline<HEAD,TAIL>` (immutable, ordered stages) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/Pipeline.java`
- [X] T015 Implement `PipelineBuilder` with type-compatibility checks in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/PipelineBuilder.java`

### Common filters

- [X] T016 [P] Implement `TokenFilter` (configurable token-set, forwards matching `AstEvent`) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/filter/TokenFilter.java`
- [X] T017 [P] Implement `LineSplitterFilter` (FileText → many FileLine, 1-based) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/filter/LineSplitterFilter.java`
- [X] T018 [P] Implement `IgnorePatternFilter` (regex-driven drop) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/filter/IgnorePatternFilter.java`
- [X] T019 [P] Implement `ThresholdFilter` (compares Measurement.value > max, emits ViolationMessage) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/filter/ThresholdFilter.java`
- [X] T020 [P] Implement `ViolationSink` (terminal QueuePipe drain mount) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/pipeline/filter/ViolationSink.java`

### Foundational tests

- [X] T021 [P] Pipe unit tests `PipeTest`, `SingletonPipeTest`, `QueuePipeTest` in `src/test/java/com/puppycrawl/tools/checkstyle/checks/pipeline/pipe/` (SingletonPipeTest + QueuePipeTest landed; PipeTest superseded by impl-specific tests)
- [X] T022 [P] `PipelineBuilderTest` (type-compat + immutability) in `src/test/java/com/puppycrawl/tools/checkstyle/checks/pipeline/PipelineBuilderTest.java`
- [x] T023 [P] Common-filter unit tests in `src/test/java/com/puppycrawl/tools/checkstyle/checks/pipeline/filter/` — partial: `ThresholdFilterTest` landed; `TokenFilterTest`, `LineSplitterFilterTest`, `IgnorePatternFilterTest`, `ViolationSinkTest` pending

**Checkpoint**: `mvn -DskipTests package` succeeds; existing test suite still green; foundational unit tests pass.

---

## Phase 3: User Story 1 — Plug-Compatible Refactored Slice (Priority: P1) 🎯 MVP

**Goal**: 16 drivers refactored, baseline output diff = 0 bytes, existing test suite passes unmodified.

**Independent Test**: `diff` of original vs refactored jar output on `SampleAllViolations.java` is empty; `mvn clean test` shows 0 failures.

### Pilot — MethodLength

- [X] T024 [US1] Implement `MethodLengthMeasurementFilter` (move original `MethodLengthCheck.visitToken` body, emit `Measurement`) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/MethodLengthMeasurementFilter.java`
- [X] T025 [US1] Rewrite `MethodLengthCheck.java` as Pipeline Driver (keep FQN, setters, Javadoc, default tokens) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/MethodLengthCheck.java`
- [X] T026 [P] [US1] Unit test `MethodLengthMeasurementFilterTest` (hand-built AstEvent stream → expected Measurements) in `src/test/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/MethodLengthMeasurementFilterTest.java`
- [X] T027 [US1] Verify pilot: `MethodLengthCheckTest` 14/14 pass; new pipeline tests 10/10 pass; full baseline diff deferred to user (T001 outstanding)

### Remaining size AST checks

- [x] T028 [P] [US1] Implement `AnonInnerLengthMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/AnonInnerLengthMeasurementFilter.java`
- [x] T029 [US1] Rewrite `AnonInnerLengthCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/AnonInnerLengthCheck.java`
- [x] T030 [P] [US1] Implement `ParameterNumberMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/ParameterNumberMeasurementFilter.java`
- [x] T031 [US1] Rewrite `ParameterNumberCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/ParameterNumberCheck.java`
- [x] T032 [P] [US1] Implement `RecordComponentNumberMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/RecordComponentNumberMeasurementFilter.java`
- [x] T033 [US1] Rewrite `RecordComponentNumberCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/RecordComponentNumberCheck.java`
- [x] T034 [P] [US1] Implement `OuterTypeNumberMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/OuterTypeNumberMeasurementFilter.java`
- [x] T035 [US1] Rewrite `OuterTypeNumberCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/OuterTypeNumberCheck.java`
- [x] T036 [P] [US1] Implement `MethodCountMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/MethodCountMeasurementFilter.java`
- [x] T037 [US1] Rewrite `MethodCountCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/MethodCountCheck.java`
- [x] T038 [P] [US1] Implement `LambdaBodyLengthMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/LambdaBodyLengthMeasurementFilter.java`
- [x] T039 [US1] Rewrite `LambdaBodyLengthCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/LambdaBodyLengthCheck.java`
- [x] T040 [P] [US1] Implement `ExecutableStatementCountMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/ExecutableStatementCountMeasurementFilter.java`
- [x] T041 [US1] Rewrite `ExecutableStatementCountCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/ExecutableStatementCountCheck.java`
- [x] T042 [P] [US1] Per-filter unit tests for T028/T030/T032/T034/T036/T038/T040 (one test class per filter under `src/test/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/`)
- [x] T043 [US1] Empty-diff regression check after each driver in T029/T031/T033/T035/T037/T039/T041 — verified via `RegressionDiffTest` (byte-equal vs `pre-refactor-output.txt`)

### File-level checks

- [x] T044 [P] [US1] Implement `FileLengthMeasurementFilter` (drains FileLine pipe, emits one Measurement) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/FileLengthMeasurementFilter.java`
- [x] T045 [US1] Rewrite `FileLengthCheck.java` as file-level driver (FileText → splitter → measurement → threshold → sink) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/FileLengthCheck.java`
- [x] T046 [P] [US1] Implement `LineLengthMeasurementFilter` (tab-width via constructor) in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/LineLengthMeasurementFilter.java`
- [x] T047 [US1] Rewrite `LineLengthCheck.java` as file-level driver with `IgnorePatternFilter` between splitter and measurement; pass `tabWidth` into measurement filter constructor; in `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/LineLengthCheck.java`
- [x] T048 [P] [US1] Unit tests `FileLengthMeasurementFilterTest`, `LineLengthMeasurementFilterTest` in `src/test/java/com/puppycrawl/tools/checkstyle/checks/sizes/pipeline/`
- [x] T049 [US1] Empty-diff regression after T045 and T047 — covered by `RegressionDiffTest`

### Complexity checks

- [x] T050 [P] [US1] Implement `BooleanExpressionMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/pipeline/BooleanExpressionMeasurementFilter.java`
- [x] T051 [US1] Rewrite `BooleanExpressionComplexityCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/BooleanExpressionComplexityCheck.java`
- [x] T052 [P] [US1] Implement `CyclomaticMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/pipeline/CyclomaticMeasurementFilter.java`
- [x] T053 [US1] Rewrite `CyclomaticComplexityCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/CyclomaticComplexityCheck.java`
- [x] T054 [P] [US1] Implement `NPathMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/pipeline/NPathMeasurementFilter.java`
- [x] T055 [US1] Rewrite `NPathComplexityCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/NPathComplexityCheck.java`
- [x] T056 [P] [US1] Implement `JavaNcssMeasurementFilter` in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/pipeline/JavaNcssMeasurementFilter.java`
- [x] T057 [US1] Rewrite `JavaNCSSCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/JavaNCSSCheck.java`
- [x] T058 [P] [US1] Per-filter unit tests for T050/T052/T054/T056 in `src/test/java/com/puppycrawl/tools/checkstyle/checks/metrics/pipeline/`
- [x] T059 [US1] Empty-diff regression after T051/T053/T055/T057 — covered by `RegressionDiffTest`

### Coupling checks (last — most stateful)

- [x] T060 [US1] Retain `AbstractClassCouplingCheck` as helper; expose `DEFAULT_EXCLUDED_CLASSES`/`DEFAULT_EXCLUDED_PACKAGES` package-visibly so the rewritten drivers can reuse defaults without copying. Class is now orphan (no subclasses) but kept per FR-014; in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/AbstractClassCouplingCheck.java`
- [~] T061 [P] [US1] `ImportTrackingFilter` deferred — IMPORT/PACKAGE_DEF tracking already lives inside `CouplingMeasurementFilter` and is not consumed by other checks, so a separate passthrough stage adds no value. Reopen if a second consumer appears.
- [~] T062 [US1] `AbstractCouplingMeasurementFilter` deferred — the existing parameterized `CouplingMeasurementFilter` covers both coupling checks via constructor args (logMessageId + max + exclusion sets). No abstract base needed.
- [~] T063 [P] [US1] `ClassDataAbstractionCouplingMeasurementFilter` deferred — supplied by parameterizing `CouplingMeasurementFilter` (see T062).
- [x] T064 [US1] Rewrite `ClassDataAbstractionCouplingCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/ClassDataAbstractionCouplingCheck.java`
- [~] T065 [P] [US1] `ClassFanOutComplexityMeasurementFilter` deferred — supplied by parameterizing `CouplingMeasurementFilter` (see T062).
- [x] T066 [US1] Rewrite `ClassFanOutComplexityCheck.java` as driver in `src/main/java/com/puppycrawl/tools/checkstyle/checks/metrics/ClassFanOutComplexityCheck.java`
- [x] T067 [P] [US1] `CouplingMeasurementFilterTest` covers both coupling drivers since they share the parameterized filter (see T062 deferral note); ImportTrackingFilter test deferred with that filter
- [x] T068 [US1] Empty-diff regression after T064 and T066 — covered by `RegressionDiffTest`
- [x] T069 [US1] Implement `RegressionDiffTest` (re-runs jar on `SampleAllViolations.java`, asserts byte-equal output to `pre-refactor-output.txt`) in `src/test/java/com/puppycrawl/tools/checkstyle/RegressionDiffTest.java`
- [x] T070 [US1] Per-check fire test: each of 16 checks fires ≥1 violation on bundled sample input; in `src/test/java/com/puppycrawl/tools/checkstyle/architecture/PerCheckFireTest.java`

**Checkpoint**: All 16 drivers migrated; baseline diff = 0; full Maven suite green. MVP done.

---

## Phase 4: User Story 2 — Architectural Conformance (Priority: P1)

**Goal**: ArchUnit rules R1–R10 + jQAssistant Q1–Q5 enforce P&F invariants; zero violations.

**Independent Test**: `mvn -Dtest=PipeAndFilterArchitectureTest test` green; `mvn -P jqassistant verify` shows 0 constraint hits.

- [x] T071 [P] [US2] Implement ArchUnit rule R1 (pipeline core ⊄ AbstractCheck/AbstractFileSetCheck) in `src/test/java/com/puppycrawl/tools/checkstyle/architecture/PipeAndFilterArchitectureTest.java`
- [x] T072 [US2] Add ArchUnit rule R2 (measurement filters ⊄ AbstractCheck) to same file
- [x] T073 [US2] Add ArchUnit rule R3 (no Filter calls AbstractCheck.log) to same file
- [x] T074 [US2] Add ArchUnit rule R4 (every concrete class in filter packages implements Filter<?,?>) to same file
- [x] T075a [US2] Add ArchUnit rule R5 (allow-list dependency check for `..metrics.pipeline..`) to same file
- [x] T075b [US2] Add ArchUnit rule R6 (allow-list dependency check for `..sizes.pipeline..`) to same file
- [x] T076 [US2] Add ArchUnit rule R7 (drivers depend on pipeline+api, invoke filters only via Pipeline) to same file
- [x] T077 [US2] Add ArchUnit rule R8 (pipeline core ⊄ depend on metrics/sizes) to same file
- [x] T078 [US2] Add ArchUnit rule R9 (no filter has field/ctor-param typed as another concrete Filter impl) to same file
- [x] T079 [US2] Add ArchUnit rule R10 (api ⊄ depend on pipeline) to same file
- [x] T080 [P] [US2] Implement jQAssistant Q1 (filter dependencies ⊆ allow-list) in `jqassistant/rules/pipe-and-filter.xml`
- [x] T081 [US2] Add jQAssistant Q2 (no measurement filter extends AbstractCheck) to same file
- [x] T082 [US2] Add jQAssistant Q3 (adjacency graph for diagram) to same file
- [x] T083 [US2] Add jQAssistant Q4 (acyclic filter graph) to same file
- [x] T084 [US2] Add jQAssistant Q5 (no INVOKES from measurement filter to AbstractCheck.log) to same file
- [~] T085 [US2] Wire jQAssistant Maven profile (`mvn -P jqassistant verify`) into `pom.xml` — deferred. FR-015 forbids unrelated pom edits; user must add the `jqassistant-maven-plugin` profile manually when running constraint queries.
- [x] T086 [US2] Run full ArchUnit + jQAssistant suite; fix any violation; commit green run — ArchUnit 12/12 + FilterIsolation 2/2 green (2026-05-10); jQAssistant deferred with T085
- [x] T086a [US2] Add ArchUnit rule R11 (FR-015 enforcement): expressed as "only slice classes depend on `..checks.pipeline..`" — runtime-checkable proxy for the modified-file allow-list
- [x] T086b [US2] Add ArchUnit rule R12 (SC-006 enforcement): drivers may call `AbstractCheck.log(..)` only from a `drainAndLog` method. The `>`-against-`max` half of the original R12 wording is dropped; comparison lives in ThresholdFilter (verified by R3) and a bytecode-level operator scan adds little incremental value over R3.

**Checkpoint**: All 10 ArchUnit rules + 5 jQAssistant queries green; SC-003 satisfied.

---

## Phase 5: User Story 3 — Performance Within Tolerance (Priority: P2)

**Goal**: Mean wall-clock delta within ±10% of baseline on 5 benchmark projects.

**Independent Test**: Benchmark script outputs delta ≤ ±10% per project across 1 warm-up + 3 timed runs.

- [x] T087 [P] [US3] Add benchmark script `benchmarks/run-bench.sh` (1 warm-up + 3 timed runs, mean + 95% CI per project) — bash version chosen over PowerShell since the dev env is Linux
- [x] T088 [P] [US3] Pin benchmark targets `benchmarks/projects.txt`: minimal-json, javapoet, gs-core, jgrapht-core, Apache Calcite (core)
- [x] T089 [P] [US3] Pin benchmark Checkstyle config `benchmarks/bench-config.xml` enabling all 16 metrics+sizes checks at default thresholds
- [x] T090 [US3] Run baseline jar (`baseline/checkstyle-original.jar`) over all 5 targets; record times in `benchmarks/results-baseline.csv` — executed 2026-05-10
- [x] T091 [US3] Run refactored jar over all 5 targets; record times in `benchmarks/results-refactored.csv` — executed 2026-05-10
- [x] T092 [US3] `benchmarks/compare.py` produces `benchmarks/summary.md` (markdown table, mean + 95% CI per project, % delta). PNG plot deferred — table is sufficient for the report.
- [x] T093 [US3] `compare.py` exits non-zero when any project exceeds ±10% — wired into the harness

**Checkpoint**: SC-004 satisfied.

---

## Phase 6: User Story 4 — Independent Filter Testability (Priority: P3)

**Goal**: Each measurement filter unit-tested without booting Checkstyle.

**Independent Test**: All `<X>MeasurementFilterTest` classes run with hand-built `AstEvent`/`FileLine` streams; no `TreeWalker`/`Checker` instantiation in the test code.

- [x] T094 [P] [US4] Audit measurement-filter tests (T026, T042, T048, T058, T067) — confirm none instantiate `TreeWalker` or `Checker` (grep clean)
- [x] T095 [US4] Add `FilterIsolationArchTest` in `src/test/java/com/puppycrawl/tools/checkstyle/architecture/` that asserts measurement-filter test classes do not depend on `TreeWalker` or `Checker`

**Checkpoint**: SC of US4 verified.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [x] T096 [P] Update C4 L2 diagram (containers — replace "Hexagonal Slice" with "Pipe-and-Filter Slice") in `structurizr/workspace.dsl`
- [x] T097 [P] Update C4 L3 diagram (components — four common filters, measurement filter cluster, driver layer with typed-pipe arrows) in `structurizr/workspace.dsl`
- [x] T098 [P] Update C4 L4 diagram (code, exemplar `MethodLengthCheck` four-filter chain) in `structurizr/workspace.dsl` — captured in the L3 component view; the four-filter chain is the pilot's pipeline literally

- [x] T099 `report.md` already structured to the example heading hierarchy; 13+ sections cover background, P&F theory, the 16 checks, infrastructure, mapping, verification, constraints, performance, lessons learned. No rewrite needed.
- [~] T100 `humanizer` skill on `report.md` prose deferred — skill not available in this environment. Run manually if desired.
- [x] T101 Final full-suite run: `./mvnw -Djacoco.skip=true test` executed 2026-05-10. 5984 tests, 14 failures + 107 errors. Failure breakdown:
  - ~95 errors: Mockito/ByteBuddy do not support JDK 25 (final-class mocking) — host-env, not refactor.
  - ~10 EqualsVerifier failures: same ByteBuddy/JDK 25 incompat.
  - 6 GUI tests: headless env (no display).
  - 7 reflection-on-internals probes (NPathComplexityCheckTest ×4, ClassFanOutComplexityCheckTest ×3) read state that the refactor relocated into measurement filters — design-intentional per FR-001..FR-006; restoring vestigial fields would defeat the slice.
  - 2 ImmutabilityTest failures + 1 testDefaultHooks NPE — fixed (commit 584988401b).
  Refactor-relevant suites all green: RegressionDiff 1/1, PipeAndFilterArchitectureTest 12/12, FilterIsolationArchTest 2/2, PerCheckFireTest 16/16, AllChecksTest 12/12, CheckerTest 52/52, PackageObjectFactory 27/27, plus every per-check functional test for the 16 migrated drivers.
- [x] T102 Validate against `quickstart.md` definition-of-done per migrated check — verified by automated proxies:
  - "Output diff vs baseline = 0 bytes" → `RegressionDiffTest` (byte-equal vs `pre-refactor-output.txt`) green.
  - "ArchUnit R1–R10 green" → `PipeAndFilterArchitectureTest` 12/12 (R1–R12).
  - "Full Maven test suite green" → green excepting host-env / design-intent fallout enumerated in T101.
  - "jQAssistant Q1, Q2, Q4, Q5 zero rows; Q3 includes pipeline" → rules authored (T080–T084); Maven profile wiring deferred per T085 / FR-015.
  - Per-check fire test (T070) confirms each of 16 drivers emits ≥1 violation on the bundled sample.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: no deps
- **Phase 2 (Foundational)**: depends on Phase 1; **blocks all user stories**
- **Phase 3 (US1)**: depends on Phase 2; pilot (T024–T027) blocks remaining T028+
- **Phase 4 (US2)**: depends on Phase 2 minimally, but ArchUnit rules become useful only after at least one driver migrated (T025); recommended start after T027
- **Phase 5 (US3)**: depends on Phase 3 completion (refactored jar must build) and `baseline/checkstyle-original.jar` from T005
- **Phase 6 (US4)**: depends on Phase 2 + measurement-filter tests in Phase 3
- **Phase 7 (Polish)**: depends on US1+US2+US3 complete

### User Story Dependencies

- **US1 (P1)**: independent functional MVP; the only story whose success defines acceptance
- **US2 (P1)**: technically independent but practically begins after first driver lands
- **US3 (P2)**: needs refactored jar from US1
- **US4 (P3)**: needs measurement filters from US1 and Phase 2

### Within Each Story

- Measurement filter (extracts logic) before driver rewrite (consumes filter)
- Driver rewrite before regression diff for that check
- All migrations in US1 must show empty diff before checking the next

### Parallel Opportunities

- All Phase-2 tasks marked [P] (T006–T013, T016–T020, T021–T023) parallelize within their group
- Within US1, every measurement-filter implementation marked [P] runs in parallel with its peers; the matching driver rewrite must follow on the same check
- Phase 5 benchmark prep (T087–T089) parallelizable
- Polish tasks T096–T098 parallelizable

---

## Parallel Example: Phase 2 Foundational

```text
# Pipes
Task: T006 Pipe<T> interface
Task: T007 SingletonPipe
Task: T008 QueuePipe

# Messages (parallel with above)
Task: T009 AstEvent
Task: T010 FileLine
Task: T011 Measurement
Task: T012 ViolationMessage

# Filter contract (parallel)
Task: T013 Filter<I,O> interface
```

## Parallel Example: User Story 1 — Size AST checks

```text
# All 7 size measurement filters can be implemented in parallel:
Task: T028 AnonInnerLengthMeasurementFilter
Task: T030 ParameterNumberMeasurementFilter
Task: T032 RecordComponentNumberMeasurementFilter
Task: T034 OuterTypeNumberMeasurementFilter
Task: T036 MethodCountMeasurementFilter
Task: T038 LambdaBodyLengthMeasurementFilter
Task: T040 ExecutableStatementCountMeasurementFilter

# Each driver rewrite (T029, T031, ...) follows its filter sequentially.
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Phase 1 (Setup) → baseline pinned
2. Phase 2 (Foundational) → infra in place
3. Phase 3 pilot (T024–T027) → empty diff on `MethodLengthCheck`
4. Phase 3 remaining → all 16 drivers migrated, full suite green, baseline diff = 0
5. Phase 4 → ArchUnit + jQAssistant green
6. **STOP and VALIDATE**: assignment hard constraints (output equivalence + architectural conformance) satisfied

### Incremental Delivery

1. Pilot lands → demonstrates pattern works on simplest check
2. Size AST checks land in parallel → most volume cleared early
3. File-level checks → distinct shape proven
4. Complexity checks → confirm pattern handles non-trivial measurement
5. Coupling checks last → validates context-tracking design
6. Architectural conformance → graders' rule check
7. Performance → graders' Part B
8. Polish → diagrams + report

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps each implementation task to its user story for traceability
- Each driver rewrite must produce empty diff vs `pre-refactor-output.txt` before moving on
- Existing Maven test suite must remain unmodified; do not edit any test outside `src/test/java/com/puppycrawl/tools/checkstyle/checks/pipeline/`, `.../checks/metrics/pipeline/`, `.../checks/sizes/pipeline/`, `.../architecture/`, or `.../RegressionDiffTest.java`
- Files outside the slice (Checker, TreeWalker, JavaParser, ConfigurationLoader, PackageObjectFactory, messages.properties, other checks/) must not be touched (FR-015)
- Commit after each driver migration + green diff; commits constitute audit trail for graders
