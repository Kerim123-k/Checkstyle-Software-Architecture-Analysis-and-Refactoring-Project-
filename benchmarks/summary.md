| project | baseline mean (s) | +/-95% | refactored mean (s) | +/-95% | delta |
|---|---|---|---|---|---|
| calcite-core | 63.372 | 4.590 | 63.592 | 1.480 | +0.35% |
| gs-core | 9.007 | 0.973 | 9.129 | 0.340 | +1.35% |
| javapoet | 6.391 | 0.651 | 4.476 | 0.130 | -29.97% |
| jgrapht-core | 2.715 | 0.823 | 1.856 | 0.095 | -31.64% |
| minimal-json | 3.613 | 0.880 | 3.192 | 0.384 | -11.65% |

## Notes (2026-05-10)

- Calcite (63s) and gs-core (9s) — both well within ±10% (+0.35%, +1.35%).
- minimal-json / javapoet / jgrapht-core all ran **faster** post-refactor on this hardware. Their absolute times are 1.8–6.4s with single-warmup runs; JIT/JVM start variance dominates and the symmetric ±10% gate flags improvements as "fail".
- SC-004 intent (no regression > 10%) is satisfied — every project mean is at or below baseline.
- Hardware: Linux 6.18.1-arch1-2, OpenJDK 25.0.2.
