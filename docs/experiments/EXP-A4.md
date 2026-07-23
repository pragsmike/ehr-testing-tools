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
   thread count (`-p` parallelism), locale, timezone, JVM version if a
   second is available.
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
