# EXP-A4 — Synthea determinism and the manifest's pinned-input set

**Objective.** Verify or refute: pinned artifact set + seed + config +
invocation ⇒ byte-identical output. Enumerate the complete set of inputs
that must be pinned, controlled, or canonicalized for reproducible
generation, and land that set as manifest schema v1.

**Decision informed.** The `corpus.manifest` schema; the invocation
wrapper's contract in `corpus.generate`.

**Apparatus.** Synthea v4.0.0 as the first real entry in
`artifacts.lock.edn` (ADR-0005); a fixed properties file and module
selection (repo-authored config, git-versioned); the `corpus.generate`
invocation wrapper; a byte-level diff harness over output trees.

**Procedure.**

1. Baseline: three identical runs (same seed, config, artifact set,
   single-threaded). Diff output trees byte-wise. Expected: identical;
   any divergence here is finding #1.
2. Vary one factor per round, three runs each, diff against baseline:
   thread count (`generate.thread_pool_size`), locale, timezone, JVM
   version if a second is available.
3. For every divergence, identify the field(s) and classify:
   - **pin it** — an input we failed to record; add it to the manifest.
   - **control it** — force a value via the invocation wrapper (e.g.
     fixed TZ/locale flags).
   - **canonicalize it** — a defined normalization applied before
     hashing (e.g. strip embedded generation timestamps), with the
     normalization itself recorded in the manifest.

   Embedded timestamps and randomly generated UUIDs are the prime
   suspects (`docs/research/` A4).
4. Reproduce: regenerate from the manifest alone in a clean environment
   (fresh cache populated by `ehr artifact fetch`, fresh checkout) and
   diff.

**Expected artifacts.**

- The run outputs themselves (the first generated corpus — nothing here
  is throwaway).
- A findings file `docs/experiments/EXP-A4-results.md` (committed by the
  executing session; interpretation happens in the design channel).
- Manifest schema v1 in `corpus.manifest`.

**Acceptance.** A clean-environment regeneration from manifest + lockfile
+ cache is byte-identical, modulo canonicalizations that are themselves
recorded in the manifest.

**Stop condition.** A divergence source that resists three investigation
rounds is recorded as a finding and handled by canonicalization policy;
determinism-modulo-documented-canonicalization is an acceptable landing,
undocumented nondeterminism is not. Effort cap: one session; if exceeded,
stop and report state.

## Amendments

- **2026-07-24 — parallelism control corrected.** The original procedure
  named `-p` as Synthea's thread-count/parallelism flag. This is wrong:
  `-p` is `populationSize` (confirmed against the v4.0.0 jar's own
  `--help` usage text — `Options: [-s seed] [-cs clinicianSeed]
  [-p populationSize] ...`). Synthea's actual parallelism control is the
  `generate.thread_pool_size` property (default `-1`, meaning "match
  `Runtime.getRuntime().availableProcessors()`"; confirmed by extracting
  `synthea.properties` from the v4.0.0 jar), settable per-run via
  `--generate.thread_pool_size=<n>` on the command line. The procedure
  above and the round-2 execution use the corrected control.
